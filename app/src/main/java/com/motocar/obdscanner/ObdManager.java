package com.motocar.obdscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObdManager {
    private static final String TAG = "ObdManager";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED_ELM,
        CONNECTED_ECU
    }

    public interface ConnectionListener {
        void onStateChanged(ConnectionState state, String message);
        void onLog(String line, boolean isTx);
    }

    public interface LiveDataListener {
        void onDataUpdated(LiveSensorData data);
    }

    private static ObdManager instance;

    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private ConnectionState currentState = ConnectionState.DISCONNECTED;
    private final List<ConnectionListener> connectionListeners = new ArrayList<>();
    private final List<LiveDataListener> liveDataListeners = new ArrayList<>();

    private final LiveSensorData currentSensorData = new LiveSensorData();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ObdProtocol currentProtocol = ObdProtocol.AUTO;
    private String connectedDeviceName = "";
    private String connectedDeviceAddress = "";
    private String elmVersion = "Unknown";
    private String activeProtocolStr = "AUTO";

    private Thread workerThread;
    private final AtomicBoolean isPollingActive = new AtomicBoolean(false);
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);

    private ObdManager() {}

    public static synchronized ObdManager getInstance() {
        if (instance == null) {
            instance = new ObdManager();
        }
        return instance;
    }

    public ConnectionState getState() {
        return currentState;
    }

    public LiveSensorData getCurrentSensorData() {
        return currentSensorData;
    }

    public String getConnectedDeviceName() {
        return connectedDeviceName;
    }

    public String getElmVersion() {
        return elmVersion;
    }

    public String getActiveProtocolStr() {
        return activeProtocolStr;
    }

    public void setProtocol(ObdProtocol protocol) {
        this.currentProtocol = protocol;
    }

    public ObdProtocol getProtocol() {
        return currentProtocol;
    }

    public synchronized void addConnectionListener(ConnectionListener listener) {
        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
            listener.onStateChanged(currentState, "");
        }
    }

    public synchronized void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public synchronized void addLiveDataListener(LiveDataListener listener) {
        if (!liveDataListeners.contains(listener)) {
            liveDataListeners.add(listener);
        }
    }

    public synchronized void removeLiveDataListener(LiveDataListener listener) {
        liveDataListeners.remove(listener);
    }

    private void notifyState(ConnectionState state, String msg) {
        this.currentState = state;
        mainHandler.post(() -> {
            for (ConnectionListener l : connectionListeners) {
                l.onStateChanged(state, msg);
            }
        });
    }

    private void notifyLog(String line, boolean isTx) {
        mainHandler.post(() -> {
            for (ConnectionListener l : connectionListeners) {
                l.onLog(line, isTx);
            }
        });
    }

    private void notifyLiveData() {
        currentSensorData.lastUpdate = System.currentTimeMillis();
        mainHandler.post(() -> {
            for (LiveDataListener l : liveDataListeners) {
                l.onDataUpdated(currentSensorData);
            }
        });
    }

    public synchronized void connect(BluetoothDevice device, ObdProtocol protocol) {
        if (isConnecting.get() || currentState == ConnectionState.CONNECTED_ECU) {
            return;
        }

        this.currentProtocol = protocol;
        this.connectedDeviceName = device.getName() != null ? device.getName() : device.getAddress();
        this.connectedDeviceAddress = device.getAddress();

        disconnect();

        isConnecting.set(true);
        notifyState(ConnectionState.CONNECTING, "Membuka soket Bluetooth ke " + connectedDeviceName + "...");

        workerThread = new Thread(() -> {
            try {
                BluetoothSocket s = null;
                try {
                    s = device.createRfcommSocketToServiceRecord(SPP_UUID);
                    s.connect();
                } catch (Exception e1) {
                    Log.w(TAG, "Standard SPP connect failed, trying reflection fallback port 1...", e1);
                    try {
                        Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        s = (BluetoothSocket) m.invoke(device, 1);
                        if (s != null) {
                            s.connect();
                        }
                    } catch (Exception e2) {
                        throw new IOException("Gagal menghubungkan Bluetooth ke adapter: " + e2.getMessage());
                    }
                }

                socket = s;
                inputStream = s.getInputStream();
                outputStream = s.getOutputStream();

                notifyState(ConnectionState.CONNECTING, "Bluetooth terhubung! Menginisialisasi chip ELM327...");
                notifyLog("=== MULAI HANDSHAKE DENGAN ADAPTER ===", false);

                // Wake-up clear buffer
                try {
                    outputStream.write("\r\r\r".getBytes());
                    outputStream.flush();
                    Thread.sleep(300);
                    clearInputBuffer();
                } catch (Exception ignored) {}

                // Send reset
                String rz = sendRawCommand("ATZ", 2000);
                notifyLog("<- " + rz, false);
                elmVersion = rz.replace("\r", " ").replace("\n", " ").trim();
                if (elmVersion.isEmpty()) {
                    elmVersion = "ELM327 v1.5 (Clone)";
                }

                notifyState(ConnectionState.CONNECTED_ELM, "Adapter terdeteksi: " + elmVersion);

                // If AUTO, run multi-protocol auto-detect sequence
                boolean ecuFound = false;

                if (currentProtocol == ObdProtocol.AUTO) {
                    notifyState(ConnectionState.CONNECTING, "Mendeteksi protokol kendaraan secara otomatis...");

                    // Candidate protocols in order of most common
                    class ProtoCandidate {
                        final String name;
                        final String label;
                        final String[] setup;

                        ProtoCandidate(String name, String label, String[] setup) {
                            this.name = name;
                            this.label = label;
                            this.setup = setup;
                        }
                    }

                    List<ProtoCandidate> candidates = new ArrayList<>();
                    candidates.add(new ProtoCandidate(
                            "ISO 15765-4 CAN (11-bit 500k)",
                            "CAN 11-bit (Motor Euro 5 Yamaha/Honda & Mobil Modern)",
                            new String[]{"ATSP6", "ATST64"}
                    ));
                    candidates.add(new ProtoCandidate(
                            "ISO 14230-4 KWP (Fast Init)",
                            "K-Line Fast (Honda FI K-Line / Suzuki / Vespa / Avanza lama)",
                            new String[]{"ATSP5", "ATST96"}
                    ));
                    candidates.add(new ProtoCandidate(
                            "ISO 9141-2 (5-Baud Init)",
                            "K-Line Slow (Yamaha 3-pin DLC / Mobil <2008)",
                            new String[]{"ATSP3", "ATST96"}
                    ));
                    candidates.add(new ProtoCandidate(
                            "ISO 15765-4 CAN (29-bit 500k)",
                            "CAN 29-bit (Mobil Modern Varian Khusus)",
                            new String[]{"ATSP7", "ATST64"}
                    ));
                    candidates.add(new ProtoCandidate(
                            "Daihatsu/Toyota KWP Khusus",
                            "ECU Lokal Daihatsu Xenia/Ayla/Avanza KWP",
                            new String[]{"ATSP0", "ATSH8110FC", "ATIB10", "ATIIA10", "ATST96"}
                    ));
                    candidates.add(new ProtoCandidate(
                            "ELM327 Standard Auto",
                            "Auto Search Bawaan Adapter",
                            new String[]{"ATSP0", "ATST96"}
                    ));

                    int step = 1;
                    for (ProtoCandidate cand : candidates) {
                        if (!isConnecting.get()) break;

                        notifyState(ConnectionState.CONNECTING, "[" + step + "/" + candidates.size() + "] Mencoba: " + cand.label + "...");
                        notifyLog("=== COBA PROTOKOL: " + cand.name + " ===", false);

                        for (String cmd : cand.setup) {
                            try {
                                sendRawCommand(cmd, 1000);
                            } catch (Exception ignored) {}
                            Thread.sleep(80);
                        }

                        // Ping ECU with 0100
                        String pingResp = sendRawCommand("0100", 2500);
                        notifyLog("<- Ping " + cand.name + ": " + pingResp, false);

                        if (isEcuResponseValid(pingResp)) {
                            ecuFound = true;
                            activeProtocolStr = cand.name;
                            notifyState(ConnectionState.CONNECTED_ECU, "Terhubung: " + cand.label);
                            notifyLog(">>> BERHASIL TERHUBUNG DENGAN PROTOKOL: " + cand.name + " <<<", false);
                            break;
                        }
                        step++;
                        Thread.sleep(120);
                    }
                } else {
                    // Manual protocol selected by user
                    notifyState(ConnectionState.CONNECTING, "Menerapkan protokol: " + currentProtocol.getDisplayName() + "...");
                    for (String cmd : currentProtocol.getInitCommands()) {
                        if (cmd.equals("ATZ")) continue;
                        Thread.sleep(100);
                        sendRawCommand(cmd, 1200);
                    }

                    Thread.sleep(150);
                    String ping = sendRawCommand("0100", 3500);
                    if (isEcuResponseValid(ping)) {
                        ecuFound = true;
                        activeProtocolStr = currentProtocol.getDisplayName();
                        notifyState(ConnectionState.CONNECTED_ECU, "Terhubung ke ECU (" + activeProtocolStr + ")");
                    }
                }

                if (!ecuFound) {
                    notifyState(ConnectionState.CONNECTED_ELM,
                            "Adapter OK! Kunci kontak wajib ON & saklar Run ON agar ECU merespon.");
                }

                isConnecting.set(false);
                startPollingLoop();

            } catch (Exception e) {
                Log.e(TAG, "Connection failed", e);
                isConnecting.set(false);
                disconnect();
                notifyState(ConnectionState.DISCONNECTED, "Koneksi gagal: " + e.getMessage());
            }
        });
        workerThread.start();
    }

    public synchronized void disconnect() {
        stopPollingLoop();
        isConnecting.set(false);

        try {
            if (inputStream != null) inputStream.close();
        } catch (Exception ignored) {}
        try {
            if (outputStream != null) outputStream.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}

        socket = null;
        inputStream = null;
        outputStream = null;

        currentSensorData.reset();
        notifyLiveData();
        notifyState(ConnectionState.DISCONNECTED, "Koneksi diputus.");
    }

    private void clearInputBuffer() {
        try {
            while (inputStream != null && inputStream.available() > 0) {
                inputStream.read();
            }
        } catch (Exception ignored) {}
    }

    public synchronized String sendRawCommand(String cmd, long timeoutMs) throws IOException {
        if (socket == null || outputStream == null || inputStream == null) {
            throw new IOException("Bluetooth belum terhubung!");
        }

        clearInputBuffer();
        notifyLog("-> " + cmd, true);

        String toSend = cmd.trim() + "\r";
        outputStream.write(toSend.getBytes());
        outputStream.flush();

        StringBuilder sb = new StringBuilder();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (inputStream.available() > 0) {
                int b = inputStream.read();
                if (b == -1) break;
                char c = (char) b;
                if (c == '>') {
                    // Prompt reached, command completed
                    break;
                }
                sb.append(c);
            } else {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        String raw = sb.toString().trim();
        // Remove echo of the command if present
        if (raw.startsWith(cmd)) {
            raw = raw.substring(cmd.length()).trim();
        }
        return raw;
    }

    public void startPollingLoop() {
        if (isPollingActive.get()) return;
        isPollingActive.set(true);

        Thread pollingThread = new Thread(() -> {
            int cycle = 0;
            while (isPollingActive.get() && socket != null && socket.isConnected()) {
                try {
                    // Query RPM (010C) - highest priority
                    String rpmResp = sendRawCommand("010C", 800);
                    parseRpm(rpmResp);

                    // Query Speed (010D)
                    String speedResp = sendRawCommand("010D", 800);
                    parseSpeed(speedResp);

                    // Query Throttle (0111)
                    String tpsResp = sendRawCommand("0111", 800);
                    parseThrottle(tpsResp);

                    // Periodic sensors every 3 cycles
                    if (cycle % 3 == 0) {
                        // Coolant Temp (0105)
                        String ectResp = sendRawCommand("0105", 800);
                        parseCoolant(ectResp);

                        // Engine Load (0104)
                        String loadResp = sendRawCommand("0104", 800);
                        parseEngineLoad(loadResp);
                    }

                    // Periodic voltage every 6 cycles
                    if (cycle % 6 == 0) {
                        String voltResp = sendRawCommand("ATRV", 800);
                        parseVoltage(voltResp);

                        // Intake Temp (010F)
                        String iatResp = sendRawCommand("010F", 800);
                        parseIntakeTemp(iatResp);

                        // MAP (010B)
                        String mapResp = sendRawCommand("010B", 800);
                        parseMap(mapResp);
                    }

                    notifyLiveData();
                    cycle++;

                    Thread.sleep(150); // slight rest between loops
                } catch (Exception e) {
                    Log.w(TAG, "Polling error", e);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                }
            }
            isPollingActive.set(false);
        });
        pollingThread.start();
    }

    public void stopPollingLoop() {
        isPollingActive.set(false);
    }

    public boolean isPolling() {
        return isPollingActive.get();
    }

    private boolean isEcuResponseValid(String resp) {
        if (resp == null) return false;
        String clean = resp.replaceAll("\\s+", "").toUpperCase();
        if (clean.contains("UNABLE") || clean.contains("NODATA") || clean.contains("ERROR") || clean.contains("STOPPED") || clean.contains("?")) {
            return false;
        }
        return clean.contains("4100");
    }

    // Parsing methods
    private void parseRpm(String raw) {
        String clean = cleanHexResponse(raw, "41 0C", "410C");
        if (clean != null && clean.length() >= 4) {
            try {
                int a = Integer.parseInt(clean.substring(0, 2), 16);
                int b = Integer.parseInt(clean.substring(2, 4), 16);
                currentSensorData.rpm = ((a * 256) + b) / 4;
            } catch (Exception ignored) {}
        }
    }

    private void parseSpeed(String raw) {
        String clean = cleanHexResponse(raw, "41 0D", "410D");
        if (clean != null && clean.length() >= 2) {
            try {
                currentSensorData.speed = Integer.parseInt(clean.substring(0, 2), 16);
            } catch (Exception ignored) {}
        }
    }

    private void parseCoolant(String raw) {
        String clean = cleanHexResponse(raw, "41 05", "4105");
        if (clean != null && clean.length() >= 2) {
            try {
                int a = Integer.parseInt(clean.substring(0, 2), 16);
                currentSensorData.coolantTemp = a - 40;
            } catch (Exception ignored) {}
        }
    }

    private void parseThrottle(String raw) {
        String clean = cleanHexResponse(raw, "41 11", "4111");
        if (clean != null && clean.length() >= 2) {
            try {
                int a = Integer.parseInt(clean.substring(0, 2), 16);
                currentSensorData.throttlePos = (int) Math.round((a * 100.0) / 255.0);
            } catch (Exception ignored) {}
        }
    }

    private void parseEngineLoad(String raw) {
        String clean = cleanHexResponse(raw, "41 04", "4104");
        if (clean != null && clean.length() >= 2) {
            try {
                int a = Integer.parseInt(clean.substring(0, 2), 16);
                currentSensorData.engineLoad = (int) Math.round((a * 100.0) / 255.0);
            } catch (Exception ignored) {}
        }
    }

    private void parseIntakeTemp(String raw) {
        String clean = cleanHexResponse(raw, "41 0F", "410F");
        if (clean != null && clean.length() >= 2) {
            try {
                int a = Integer.parseInt(clean.substring(0, 2), 16);
                currentSensorData.intakeAirTemp = a - 40;
            } catch (Exception ignored) {}
        }
    }

    private void parseMap(String raw) {
        String clean = cleanHexResponse(raw, "41 0B", "410B");
        if (clean != null && clean.length() >= 2) {
            try {
                currentSensorData.mapPressure = Integer.parseInt(clean.substring(0, 2), 16);
            } catch (Exception ignored) {}
        }
    }

    private void parseVoltage(String raw) {
        if (raw == null) return;
        String trimmed = raw.replace("\r", " ").replace("\n", " ").trim();
        if (trimmed.endsWith("V") || trimmed.matches(".*\\d+\\.\\d+.*")) {
            currentSensorData.batteryVoltage = trimmed;
        }
    }

    private String cleanHexResponse(String raw, String prefixWithSpace, String prefixWithoutSpace) {
        if (raw == null) return null;
        String s = raw.replace("\r", " ").replace("\n", " ").trim();
        int idx = s.indexOf(prefixWithSpace);
        if (idx != -1) {
            String sub = s.substring(idx + prefixWithSpace.length()).replaceAll("[^0-9A-Fa-f]", "");
            return sub;
        }
        idx = s.indexOf(prefixWithoutSpace);
        if (idx != -1) {
            String sub = s.substring(idx + prefixWithoutSpace.length()).replaceAll("[^0-9A-Fa-f]", "");
            return sub;
        }
        return null;
    }

    // DTC Operations
    public interface DtcCallback {
        void onSuccess(List<String> dtcCodes);
        void onError(String message);
    }

    public void readDtc(DtcCallback callback) {
        new Thread(() -> {
            boolean wasPolling = isPollingActive.get();
            if (wasPolling) stopPollingLoop();

            try {
                Thread.sleep(200);
                String resp = sendRawCommand("03", 4000);
                notifyLog("DTC Response: " + resp, false);

                List<String> codes = parseDtcResponse(resp);
                mainHandler.post(() -> callback.onSuccess(codes));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal membaca DTC: " + e.getMessage()));
            } finally {
                if (wasPolling) startPollingLoop();
            }
        }).start();
    }

    public interface ClearDtcCallback {
        void onSuccess();
        void onError(String message);
    }

    public void clearDtc(ClearDtcCallback callback) {
        new Thread(() -> {
            boolean wasPolling = isPollingActive.get();
            if (wasPolling) stopPollingLoop();

            try {
                Thread.sleep(200);
                String resp = sendRawCommand("04", 4000);
                notifyLog("Clear DTC: " + resp, false);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal menghapus DTC: " + e.getMessage()));
            } finally {
                if (wasPolling) startPollingLoop();
            }
        }).start();
    }

    private List<String> parseDtcResponse(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.contains("NO DATA") || raw.contains("UNABLE TO CONNECT")) {
            return result;
        }

        // Clean lines
        String[] lines = raw.split("[\r\n]+");
        for (String line : lines) {
            String clean = line.replaceAll("[^0-9A-Fa-f]", "");
            int idx43 = clean.indexOf("43");
            if (idx43 != -1) {
                String hexData = clean.substring(idx43 + 2);
                // Each DTC is 2 bytes (4 hex chars)
                for (int i = 0; i + 3 < hexData.length(); i += 4) {
                    String dtcHex = hexData.substring(i, i + 4);
                    if (dtcHex.equals("0000")) continue;

                    String code = decodeDtcCode(dtcHex);
                    if (code != null && !result.contains(code)) {
                        result.add(code);
                    }
                }
            }
        }
        return result;
    }

    private String decodeDtcCode(String hex4) {
        if (hex4.length() < 4) return null;
        try {
            int b1 = Integer.parseInt(hex4.substring(0, 2), 16);
            int b2 = Integer.parseInt(hex4.substring(2, 4), 16);

            char prefix;
            int type = (b1 >> 6) & 0x03;
            switch (type) {
                case 0: prefix = 'P'; break;
                case 1: prefix = 'C'; break;
                case 2: prefix = 'B'; break;
                case 3: prefix = 'U'; break;
                default: prefix = 'P'; break;
            }

            int digit2 = (b1 >> 4) & 0x03;
            int digit3 = b1 & 0x0F;
            int digit4 = (b2 >> 4) & 0x0F;
            int digit5 = b2 & 0x0F;

            return String.format("%c%X%X%X%X", prefix, digit2, digit3, digit4, digit5);
        } catch (Exception e) {
            return null;
        }
    }
}
