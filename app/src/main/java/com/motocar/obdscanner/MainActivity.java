package com.motocar.obdscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ObdManager.ConnectionListener, ObdManager.LiveDataListener {

    private static final int PERMISSION_REQ_CODE = 1001;

    // Header Views
    private View dotStatus;
    private TextView tvHeaderStatus;
    private TextView tvHeaderVoltage;
    private MaterialButton btnHeaderDisconnect;

    // Tab Containers
    private View viewTabConnect;
    private View viewTabDashboard;
    private View viewTabDiagnostics;
    private View viewTabTerminal;
    private BottomNavigationView bottomNav;

    // Tab 1: Connect Views
    private Spinner spinnerProtocol;
    private TextView tvProtocolDesc;
    private RecyclerView rvDevices;
    private TextView tvNoDevices;
    private ImageButton btnRefreshDevices;
    private TextView tvChipInfo;
    private TextView tvActiveProto;
    private TextView tvVinInfo;
    private TextView tvBatteryVoltInfo;
    private DeviceAdapter deviceAdapter;

    // Tab 2: Dashboard Views
    private TextView tvPollingStatus;
    private MaterialButton btnTogglePolling;
    private TextView tvRpmValue;
    private ProgressBar pbRpm;
    private TextView tvSpeedValue;
    private ProgressBar pbSpeed;
    private TextView tvCoolantValue;
    private TextView tvCoolantStatus;
    private TextView tvTpsValue;
    private ProgressBar pbTps;
    private TextView tvDashVoltage;
    private TextView tvLoadValue;
    private ProgressBar pbLoad;
    private TextView tvIatValue;
    private TextView tvMapValue;

    // Tab 3: Diagnostics Views
    private MaterialButton btnScanDtc;
    private MaterialButton btnClearDtc;
    private ProgressBar pbDtcLoading;
    private ImageView ivDtcStatusIcon;
    private TextView tvDtcStatusTitle;
    private TextView tvDtcStatusDesc;
    private RecyclerView rvDtcList;
    private DtcAdapter dtcAdapter;

    // Tab 4: Terminal Views
    private ScrollView scrollTerminal;
    private TextView tvTerminalOutput;
    private EditText etTerminalInput;
    private ImageButton btnSendCmd;
    private ImageButton btnClearTerminal;

    private ObdManager obdManager;
    private BluetoothAdapter bluetoothAdapter;
    private final StringBuilder terminalBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obdManager = ObdManager.getInstance();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        initHeader();
        initTabs();
        initConnectTab();
        initDashboardTab();
        initDiagnosticsTab();
        initTerminalTab();

        checkPermissionsAndLoadDevices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        obdManager.addConnectionListener(this);
        obdManager.addLiveDataListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        obdManager.removeConnectionListener(this);
        obdManager.removeLiveDataListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        obdManager.disconnect();
    }

    private void initHeader() {
        dotStatus = findViewById(R.id.dot_status);
        tvHeaderStatus = findViewById(R.id.tv_header_status);
        tvHeaderVoltage = findViewById(R.id.tv_header_voltage);
        btnHeaderDisconnect = findViewById(R.id.btn_header_disconnect);

        btnHeaderDisconnect.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Putus Koneksi")
                    .setMessage("Yakin ingin memutuskan koneksi dengan adapter OBD?")
                    .setPositiveButton("Ya", (d, w) -> obdManager.disconnect())
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void initTabs() {
        viewTabConnect = findViewById(R.id.view_tab_connect);
        viewTabDashboard = findViewById(R.id.view_tab_dashboard);
        viewTabDiagnostics = findViewById(R.id.view_tab_diagnostics);
        viewTabTerminal = findViewById(R.id.view_tab_terminal);
        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            viewTabConnect.setVisibility(id == R.id.nav_connect ? View.VISIBLE : View.GONE);
            viewTabDashboard.setVisibility(id == R.id.nav_dashboard ? View.VISIBLE : View.GONE);
            viewTabDiagnostics.setVisibility(id == R.id.nav_diagnostics ? View.VISIBLE : View.GONE);
            viewTabTerminal.setVisibility(id == R.id.nav_terminal ? View.VISIBLE : View.GONE);
            return true;
        });
    }

    private void initConnectTab() {
        spinnerProtocol = findViewById(R.id.spinner_protocol);
        tvProtocolDesc = findViewById(R.id.tv_protocol_desc);
        rvDevices = findViewById(R.id.rv_devices);
        tvNoDevices = findViewById(R.id.tv_no_devices);
        btnRefreshDevices = findViewById(R.id.btn_refresh_devices);
        tvChipInfo = findViewById(R.id.tv_chip_info);
        tvActiveProto = findViewById(R.id.tv_active_proto);
        tvVinInfo = findViewById(R.id.tv_vin_info);
        tvBatteryVoltInfo = findViewById(R.id.tv_battery_volt_info);

        ArrayAdapter<ObdProtocol> protoAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                ObdProtocol.values()
        );
        spinnerProtocol.setAdapter(protoAdapter);

        spinnerProtocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ObdProtocol p = ObdProtocol.values()[position];
                obdManager.setProtocol(p);
                tvProtocolDesc.setText(p.getDescription());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(this::onConnectToDevice);
        rvDevices.setAdapter(deviceAdapter);

        btnRefreshDevices.setOnClickListener(v -> checkPermissionsAndLoadDevices());
    }

    private void initDashboardTab() {
        tvPollingStatus = findViewById(R.id.tv_polling_status);
        btnTogglePolling = findViewById(R.id.btn_toggle_polling);
        tvRpmValue = findViewById(R.id.tv_rpm_value);
        pbRpm = findViewById(R.id.pb_rpm);
        tvSpeedValue = findViewById(R.id.tv_speed_value);
        pbSpeed = findViewById(R.id.pb_speed);
        tvCoolantValue = findViewById(R.id.tv_coolant_value);
        tvCoolantStatus = findViewById(R.id.tv_coolant_status);
        tvTpsValue = findViewById(R.id.tv_tps_value);
        pbTps = findViewById(R.id.pb_tps);
        tvDashVoltage = findViewById(R.id.tv_dash_voltage);
        tvLoadValue = findViewById(R.id.tv_load_value);
        pbLoad = findViewById(R.id.pb_load);
        tvIatValue = findViewById(R.id.tv_iat_value);
        tvMapValue = findViewById(R.id.tv_map_value);

        btnTogglePolling.setOnClickListener(v -> {
            if (obdManager.isPolling()) {
                obdManager.stopPollingLoop();
                btnTogglePolling.setText("Lanjut");
                tvPollingStatus.setText("○ Polling Dijeda");
                tvPollingStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_amber));
            } else {
                obdManager.startPollingLoop();
                btnTogglePolling.setText("Jeda");
                tvPollingStatus.setText("● Polling Sensor Aktif");
                tvPollingStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            }
        });
    }

    private void initDiagnosticsTab() {
        btnScanDtc = findViewById(R.id.btn_scan_dtc);
        btnClearDtc = findViewById(R.id.btn_clear_dtc);
        pbDtcLoading = findViewById(R.id.pb_dtc_loading);
        ivDtcStatusIcon = findViewById(R.id.iv_dtc_status_icon);
        tvDtcStatusTitle = findViewById(R.id.tv_dtc_status_title);
        tvDtcStatusDesc = findViewById(R.id.tv_dtc_status_desc);
        rvDtcList = findViewById(R.id.rv_dtc_list);

        rvDtcList.setLayoutManager(new LinearLayoutManager(this));
        dtcAdapter = new DtcAdapter();
        rvDtcList.setAdapter(dtcAdapter);

        btnScanDtc.setOnClickListener(v -> performScanDtc());
        btnClearDtc.setOnClickListener(v -> performClearDtc());
    }

    private void initTerminalTab() {
        scrollTerminal = findViewById(R.id.scroll_terminal);
        tvTerminalOutput = findViewById(R.id.tv_terminal_output);
        etTerminalInput = findViewById(R.id.et_terminal_input);
        btnSendCmd = findViewById(R.id.btn_send_cmd);
        btnClearTerminal = findViewById(R.id.btn_clear_terminal);

        findViewById(R.id.btn_chip_atrv).setOnClickListener(v -> sendTerminalCommand("ATRV"));
        findViewById(R.id.btn_chip_0100).setOnClickListener(v -> sendTerminalCommand("0100"));
        findViewById(R.id.btn_chip_atdp).setOnClickListener(v -> sendTerminalCommand("ATDP"));
        findViewById(R.id.btn_chip_010c).setOnClickListener(v -> sendTerminalCommand("010C"));
        findViewById(R.id.btn_chip_03).setOnClickListener(v -> sendTerminalCommand("03"));
        findViewById(R.id.btn_chip_atz).setOnClickListener(v -> sendTerminalCommand("ATZ"));

        btnSendCmd.setOnClickListener(v -> {
            String cmd = etTerminalInput.getText().toString().trim();
            if (!cmd.isEmpty()) {
                sendTerminalCommand(cmd);
                etTerminalInput.setText("");
            }
        });

        btnClearTerminal.setOnClickListener(v -> {
            terminalBuffer.setLength(0);
            tvTerminalOutput.setText("Konsol OBD dibersihkan.\n");
        });
    }

    private void sendTerminalCommand(String cmd) {
        new Thread(() -> {
            try {
                String resp = obdManager.sendRawCommand(cmd, 3000);
                runOnUiThread(() -> {
                    appendTerminalText("-> " + cmd + "\n<- " + resp + "\n");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendTerminalText("-> " + cmd + "\n[ERROR]: " + e.getMessage() + "\n");
                });
            }
        }).start();
    }

    private void appendTerminalText(String text) {
        terminalBuffer.append(text);
        if (terminalBuffer.length() > 6000) {
            terminalBuffer.delete(0, 2000);
        }
        tvTerminalOutput.setText(terminalBuffer.toString());
        scrollTerminal.post(() -> scrollTerminal.fullScroll(View.FOCUS_DOWN));
    }

    private void checkPermissionsAndLoadDevices() {
        List<String> neededPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), PERMISSION_REQ_CODE);
        } else {
            loadPairedDevices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            loadPairedDevices();
        }
    }

    @SuppressLint("MissingPermission")
    private void loadPairedDevices() {
        if (bluetoothAdapter == null) {
            tvNoDevices.setText("Perangkat ini tidak mendukung Bluetooth.");
            tvNoDevices.setVisibility(View.VISIBLE);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Silakan nyalakan Bluetooth terlebih dahulu!", Toast.LENGTH_SHORT).show();
            tvNoDevices.setText("Bluetooth nonaktif. Mohon nyalakan Bluetooth HP.");
            tvNoDevices.setVisibility(View.VISIBLE);
            return;
        }

        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> deviceList = new ArrayList<>();
        if (paired != null && !paired.isEmpty()) {
            deviceList.addAll(paired);
            tvNoDevices.setVisibility(View.GONE);
        } else {
            tvNoDevices.setText("Belum ada perangkat OBD yang di-pair.\nMasuk ke Pengaturan HP -> Bluetooth, lalu pair adapter ELM327 (PIN 1234 atau 0000).");
            tvNoDevices.setVisibility(View.VISIBLE);
        }
        deviceAdapter.setDevices(deviceList);
    }

    private void onConnectToDevice(BluetoothDevice device) {
        ObdProtocol protocol = (ObdProtocol) spinnerProtocol.getSelectedItem();
        obdManager.connect(device, protocol);
    }

    private void performScanDtc() {
        if (obdManager.getState() == ObdManager.ConnectionState.DISCONNECTED) {
            Toast.makeText(this, "Harap hubungkan ke adapter OBD terlebih dahulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        pbDtcLoading.setVisibility(View.VISIBLE);
        btnScanDtc.setEnabled(false);

        obdManager.readDtc(new ObdManager.DtcCallback() {
            @Override
            public void onSuccess(List<DtcDatabase.DtcInfo> dtcItems) {
                pbDtcLoading.setVisibility(View.GONE);
                btnScanDtc.setEnabled(true);
                dtcAdapter.setDtcItems(dtcItems);

                if (dtcItems.isEmpty()) {
                    ivDtcStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivDtcStatusIcon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.neon_green));
                    tvDtcStatusTitle.setText("Sistem ECU Normal");
                    tvDtcStatusDesc.setText("Tidak ada kode kerusakan aktif maupun pending.");
                    Toast.makeText(MainActivity.this, "Scan Selesai: ECU Bersih, Tidak Ada Error!", Toast.LENGTH_SHORT).show();
                } else {
                    ivDtcStatusIcon.setImageResource(R.drawable.ic_diagnostics);
                    ivDtcStatusIcon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.neon_red));
                    tvDtcStatusTitle.setText(dtcItems.size() + " Kerusakan Terdeteksi!");
                    tvDtcStatusDesc.setText("Periksa daftar kode kerusakan di bawah (termasuk kode pending).");
                    Toast.makeText(MainActivity.this, "Ditemukan " + dtcItems.size() + " kode error!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                pbDtcLoading.setVisibility(View.GONE);
                btnScanDtc.setEnabled(true);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performClearDtc() {
        if (obdManager.getState() == ObdManager.ConnectionState.DISCONNECTED) {
            Toast.makeText(this, "Harap hubungkan ke adapter OBD terlebih dahulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Reset MIL")
                .setMessage("Hapus semua kode kerusakan dari ECU dan reset lampu Check Engine?\n\nCatatan: Kunci kontak harus dalam posisi ON.")
                .setPositiveButton("Hapus Sekarang", (dialog, which) -> {
                    pbDtcLoading.setVisibility(View.VISIBLE);
                    btnClearDtc.setEnabled(false);

                    obdManager.clearDtc(new ObdManager.ClearDtcCallback() {
                        @Override
                        public void onSuccess() {
                            pbDtcLoading.setVisibility(View.GONE);
                            btnClearDtc.setEnabled(true);
                            dtcAdapter.clear();
                            ivDtcStatusIcon.setImageResource(R.drawable.ic_check_circle);
                            ivDtcStatusIcon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.neon_green));
                            tvDtcStatusTitle.setText("Kode Kerusakan Direset");
                            tvDtcStatusDesc.setText("Perintah Clear DTC (Mode 04) berhasil dikirim ke ECU.");
                            Toast.makeText(MainActivity.this, "Kode error berhasil direset!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            pbDtcLoading.setVisibility(View.GONE);
                            btnClearDtc.setEnabled(true);
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // OBD Listener Callbacks
    @Override
    public void onStateChanged(ObdManager.ConnectionState state, String message) {
        switch (state) {
            case DISCONNECTED:
                dotStatus.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_disconnected));
                tvHeaderStatus.setText("Terputus");
                btnHeaderDisconnect.setVisibility(View.GONE);
                tvChipInfo.setText("Belum Terhubung");
                tvActiveProto.setText("-");
                tvVinInfo.setText("-");
                break;
            case CONNECTING:
                dotStatus.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_connecting));
                tvHeaderStatus.setText("Menghubungkan…");
                btnHeaderDisconnect.setVisibility(View.VISIBLE);
                break;
            case CONNECTED_ELM:
                dotStatus.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_connecting));
                tvHeaderStatus.setText("Adapter OK (Menunggu ECU)");
                btnHeaderDisconnect.setVisibility(View.VISIBLE);
                tvChipInfo.setText(obdManager.getElmVersion());
                tvActiveProto.setText(obdManager.getActiveProtocolStr());
                tvVinInfo.setText("-");
                break;
            case CONNECTED_ECU:
                dotStatus.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_connected));
                tvHeaderStatus.setText("ECU Terhubung (" + obdManager.getConnectedDeviceName() + ")");
                btnHeaderDisconnect.setVisibility(View.VISIBLE);
                tvChipInfo.setText(obdManager.getElmVersion());
                tvActiveProto.setText(obdManager.getActiveProtocolStr());
                tvVinInfo.setText(obdManager.getVehicleVin());
                break;
        }
        if (message != null && !message.isEmpty()) {
            appendTerminalText("[STATUS] " + message + "\n");
        }
    }

    @Override
    public void onLog(String line, boolean isTx) {
        appendTerminalText(line + "\n");
    }

    @Override
    public void onDataUpdated(LiveSensorData data) {
        // Battery Voltage
        String volt = data.batteryVoltage;
        tvHeaderVoltage.setText(volt);
        tvBatteryVoltInfo.setText(volt);
        tvDashVoltage.setText(volt);

        // RPM
        if (data.hasRpm()) {
            tvRpmValue.setText(String.valueOf(data.rpm));
            pbRpm.setProgress(Math.min(data.rpm, 10000));
        }

        // Speed
        if (data.hasSpeed()) {
            tvSpeedValue.setText(String.valueOf(data.speed));
            pbSpeed.setProgress(Math.min(data.speed, 200));
        }

        // Coolant Temp
        if (data.hasCoolant()) {
            tvCoolantValue.setText(String.valueOf(data.coolantTemp));
            if (data.coolantTemp > 105) {
                tvCoolantStatus.setText("OVERHEAT!");
                tvCoolantStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_red));
            } else if (data.coolantTemp > 95) {
                tvCoolantStatus.setText("Tinggi");
                tvCoolantStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_amber));
            } else {
                tvCoolantStatus.setText("Normal");
                tvCoolantStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_green));
            }
        }

        // Throttle (TPS)
        if (data.hasThrottle()) {
            tvTpsValue.setText(String.valueOf(data.throttlePos));
            pbTps.setProgress(data.throttlePos);
        }

        // Engine Load
        if (data.hasEngineLoad()) {
            tvLoadValue.setText(String.valueOf(data.engineLoad));
            pbLoad.setProgress(data.engineLoad);
        }

        // Intake Air Temp
        if (data.hasIntakeTemp()) {
            tvIatValue.setText(data.intakeAirTemp + " °C");
        }

        // MAP Pressure
        if (data.hasMapPressure()) {
            tvMapValue.setText(data.mapPressure + " kPa");
        }
    }
}
