package com.motocar.obdscanner;

import java.util.HashMap;
import java.util.Map;

public class DtcDatabase {

    public static class DtcInfo {
        public final String code;
        public final String title;
        public final String description;
        public final String severity; // Ringan, Sedang, Kritis
        public boolean isPending = false;

        public DtcInfo(String code, String title, String description, String severity) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.severity = severity;
        }

        public DtcInfo(String code, String title, String description, String severity, boolean isPending) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.isPending = isPending;
        }
    }

    private static final Map<String, DtcInfo> DB = new HashMap<>();

    static {
        // Fuel and Air Metering (P0100 - P0199)
        add("P0100", "Sensor MAF / VAF Sirkuit Malfungsi", "Massa aliran udara tidak terbaca akurat oleh ECU. Periksa soket atau bersihkan sensor MAF.", "Sedang");
        add("P0101", "Sensor MAF Sirkuit Rentang/Kinerja", "Sinyal aliran udara di luar batas normal.", "Sedang");
        add("P0102", "Sensor MAF Input Rendah", "Tegangan sinyal sensor MAF terlalu rendah (kabel putus atau kotor).", "Sedang");
        add("P0103", "Sensor MAF Input Tinggi", "Tegangan sinyal sensor MAF terlalu tinggi (konsleting).", "Sedang");

        add("P0105", "Sensor MAP / Sirkuit Tekanan Barometrik", "Sensor tekanan udara manifold (MAP) bermasalah. Sangat umum pada motor injeksi & mobil.", "Sedang");
        add("P0106", "Sensor MAP Sirkuit Rentang/Kinerja", "Nilai vakum intake tidak sesuai bukaan gas.", "Sedang");
        add("P0107", "Sensor MAP Input Rendah", "Sinyal tegangan MAP rendah / ground bermasalah.", "Sedang");
        add("P0108", "Sensor MAP Input Tinggi", "Sinyal tegangan MAP tinggi / selang vakum bocor.", "Sedang");

        add("P0110", "Sensor IAT Sirkuit Malfungsi", "Sensor suhu udara masuk (Intake Air Temperature) bermasalah.", "Ringan");
        add("P0111", "Sensor IAT Sirkuit Rentang/Kinerja", "Suhu udara masuk tidak rasional.", "Ringan");
        add("P0112", "Sensor IAT Input Rendah", "Suhu terbaca terlalu panas (konsleting ground).", "Ringan");
        add("P0113", "Sensor IAT Input Tinggi", "Suhu terbaca terlalu dingin / soket IAT terlepas.", "Ringan");

        add("P0115", "Sensor Suhu Mesin / Coolant (ECT/EOT)", "Sensor suhu cairan pendingin atau oli mesin malfungsi.", "Kritis");
        add("P0116", "Sensor ECT/EOT Sirkuit Rentang/Kinerja", "Perubahan suhu mesin tidak wajar.", "Sedang");
        add("P0117", "Sensor ECT/EOT Input Rendah", "Sinyal suhu terbaca mendidih ekstrim (short circuit). Kipas radiator bisa menyala terus.", "Kritis");
        add("P0118", "Sensor ECT/EOT Input Tinggi", "Soket sensor ECT lepas atau kabel putus.", "Kritis");

        add("P0120", "Sensor TPS (Throttle Position) Sirkuit A Malfungsi", "Sensor bukaan gas (TPS) tidak merespon putaran grip/pedal gas.", "Sedang");
        add("P0121", "Sensor TPS Sirkuit Rentang/Kinerja", "Sinyal TPS melompat-lompat / aus di titik tertentu.", "Sedang");
        add("P0122", "Sensor TPS Input Rendah", "Tegangan TPS terlalu rendah (kabel putus atau posisi geser).", "Sedang");
        add("P0123", "Sensor TPS Input Tinggi", "Tegangan TPS terlalu tinggi / kabel 5V konslet.", "Sedang");

        add("P0130", "Sensor O2 (Oksigen) Sirkuit Bank 1 Sensor 1", "Sensor oksigen knalpot gagal mendeteksi campuran bahan bakar.", "Sedang");
        add("P0131", "Sensor O2 Tegangan Rendah (Lean)", "Campuran udara/bensin terdeteksi terlalu kurus.", "Sedang");
        add("P0132", "Sensor O2 Tegangan Tinggi (Rich)", "Campuran udara/bensin terlalu boros/kaya.", "Sedang");
        add("P0133", "Sensor O2 Respon Lambat", "Sensor O2 mulai aus atau tertutup kerak karbon.", "Ringan");
        add("P0134", "Sensor O2 Tidak Ada Aktivitas", "Sensor O2 mati atau tidak terhubung.", "Sedang");
        add("P0135", "Sensor O2 Heater Sirkuit Rusak", "Pemanas sensor oksigen putus.", "Ringan");

        // Fuel Injection (P0200 - P0299)
        add("P0201", "Injektor Silinder 1 Sirkuit Malfungsi", "Injektor 1 tidak menyemprot atau soket kendur. Mesin pincang.", "Kritis");
        add("P0202", "Injektor Silinder 2 Sirkuit Malfungsi", "Injektor 2 tidak menyemprot.", "Kritis");
        add("P0203", "Injektor Silinder 3 Sirkuit Malfungsi", "Injektor 3 tidak menyemprot.", "Kritis");
        add("P0204", "Injektor Silinder 4 Sirkuit Malfungsi", "Injektor 4 tidak menyemprot.", "Kritis");
        add("P0217", "Engine Over-Temperature Condition", "Mesin mengalami overheat (suhu terlalu tinggi)! Segera matikan kendaraan.", "Kritis");
        add("P0230", "Pompa Bensin (Fuel Pump) Primer Malfungsi", "Relay atau jalur pompa bensin bermasalah, tekanan bensin drop.", "Kritis");

        // Ignition & Misfire (P0300 - P0399)
        add("P0300", "Random / Multiple Cylinder Misfire", "Pengapian hilang acak di beberapa silinder. Periksa busi, koil, kompresi.", "Kritis");
        add("P0301", "Silinder 1 Misfire Terdeteksi", "Busi atau koil pengapian silinder 1 mati / pincang.", "Kritis");
        add("P0302", "Silinder 2 Misfire Terdeteksi", "Pengapian silinder 2 gagal.", "Kritis");
        add("P0303", "Silinder 3 Misfire Terdeteksi", "Pengapian silinder 3 gagal.", "Kritis");
        add("P0304", "Silinder 4 Misfire Terdeteksi", "Pengapian silinder 4 gagal.", "Kritis");

        add("P0335", "Sensor Posisi Crankshaft (CKP / Spul) Malfungsi", "ECU tidak menerima sinyal putaran kruk as. Mesin bisa mogok / tidak mau hidup.", "Kritis");
        add("P0336", "Sensor CKP Rentang/Kinerja", "Sinyal pulser/CKP tidak sinkron atau pick-up kotor.", "Kritis");
        add("P0340", "Sensor Posisi Camshaft (CMP) Sirkuit Malfungsi", "Sensor noken as tidak terbaca, timing katup tidak sinkron.", "Sedang");

        add("P0351", "Koil Pengapian A (Cylinder 1) Primer/Sekunder", "Koil pengapian 1 rusak, konslet, atau kabel putus.", "Kritis");
        add("P0352", "Koil Pengapian B (Cylinder 2) Primer/Sekunder", "Koil pengapian 2 rusak.", "Kritis");
        add("P0353", "Koil Pengapian C (Cylinder 3) Primer/Sekunder", "Koil pengapian 3 rusak.", "Kritis");
        add("P0354", "Koil Pengapian D (Cylinder 4) Primer/Sekunder", "Koil pengapian 4 rusak.", "Kritis");

        // Speed & Idle Control (P0500 - P0599)
        add("P0500", "Sensor Kecepatan Kendaraan (VSS / Spidometer)", "Sensor kecepatan roda atau girboks tidak ada sinyal. Spidometer mati.", "Ringan");
        add("P0505", "Sistem Kontrol Udara Idle (IACV / ISC)", "Katup langsam bermasalah. Mesin mati saat lepas gas atau RPM meraung.", "Sedang");
        add("P0506", "RPM Idle Lebih Rendah dari Target", "Langsam terlalu rendah, berpotensi mati-matian.", "Sedang");
        add("P0507", "RPM Idle Lebih Tinggi dari Target", "Langsam terlalu tinggi (kebocoran vakum intake).", "Sedang");

        add("P0560", "Tegangan Sistem Kelistrikan Malfungsi", "Tegangan aki/ECU tidak stabil.", "Sedang");
        add("P0562", "Tegangan Sistem Rendah (Aki Drop / Spul)", "Tegangan di bawah 11V. Aki tekor atau alternator/kiprok rusak.", "Kritis");
        add("P0563", "Tegangan Sistem Terlalu Tinggi (Overcharge)", "Tegangan di atas 15V! Bahaya merusak ECU dan aki kembung (kiprok jebol).", "Kritis");

        // Computer & Output (P0600 - P0699)
        add("P0600", "Komunikasi Serial ECU Terganggu", "Jalur komunikasi CAN / K-Line mengalami error.", "Sedang");
        add("P0601", "Internal Control Module Memory Checksum Error", "ROM ECU korup atau error pada memori ECU.", "Kritis");
        add("P0602", "Control Module Programming Error", "Software ECU belum diprogram atau gagal flashing.", "Kritis");
        add("P0606", "ECM / PCM Processor Fault", "Prosesor ECU mendeteksi kegagalan internal.", "Kritis");
    }

    private static void add(String code, String title, String desc, String severity) {
        DB.put(code.toUpperCase(), new DtcInfo(code.toUpperCase(), title, desc, severity));
    }

    public static DtcInfo lookup(String code) {
        return lookup(code, false);
    }

    public static DtcInfo lookup(String code, boolean isPending) {
        if (code == null) return null;
        code = code.trim().toUpperCase();
        DtcInfo info = DB.get(code);
        if (info != null) {
            return new DtcInfo(info.code, info.title, info.description, info.severity, isPending);
        }

        // Fallback generic interpretation based on standard OBD2 prefixes
        String category = "Kerusakan Komponen Kendaraan";
        if (code.startsWith("P01") || code.startsWith("P02")) {
            category = "Sistem Pengabutan Bensin / Udara (Fuel & Air)";
        } else if (code.startsWith("P03")) {
            category = "Sistem Pengapian / Misfire Mesin (Ignition)";
        } else if (code.startsWith("P04")) {
            category = "Sistem Emisi Gas Buang (Emission Control)";
        } else if (code.startsWith("P05")) {
            category = "Kontrol Kecepatan & Sistem Langsam (Speed & Idle)";
        } else if (code.startsWith("P06")) {
            category = "Komputer ECU & Rangkaian Komunikasi (ECU / Computer)";
        } else if (code.startsWith("P07") || code.startsWith("P08")) {
            category = "Transmisi Otomatis / CVT (Transmission)";
        } else if (code.startsWith("C")) {
            category = "Sistem Sasis / Rem ABS / Kemudi (Chassis)";
        } else if (code.startsWith("B")) {
            category = "Sistem Bodi & Kelistrikan Interior (Body)";
        } else if (code.startsWith("U")) {
            category = "Komunikasi Jaringan Data Kendaraan (Network CAN)";
        }

        return new DtcInfo(
            code,
            category,
            "Kode diagnostik OBD terdeteksi (" + code + "). Silakan periksa modul terkait sesuai buku servis.",
            "Sedang",
            isPending
        );
    }
}
