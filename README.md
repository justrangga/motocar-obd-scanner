# MotoCar OBD Scanner 🚗🏍️

**MotoCar OBD Scanner** adalah aplikasi Android universal untuk diagnostik kendaraan mobil dan sepeda motor injeksi menggunakan adapter Bluetooth OBD-II standar (khususnya chip kloning **ELM327 v1.5 / v2.0 / v2.1**).

Aplikasi ini dirancang khusus agar **mudah digunakan oleh siapa saja tanpa perlu konfigurasi teknis yang rumit**. Dengan fitur **Multi-Protocol Auto-Negotiator**, aplikasi akan secara otomatis memindai dan mencocokkan protokol ECU kendaraan Anda hanya dengan satu kali klik.

---

## ✨ Fitur Utama

- ⚡ **Deteksi Otomatis Multi-Protokol (One-Click Connect):**
  Menguji protokol ECU secara berurutan (*CAN 11-bit 500k*, *K-Line KWP Fast*, *K-Line Slow ISO 9141*, *CAN 29-bit*, dan *KWP Custom*) dan langsung mengunci protokol yang merespon.
- 🎯 **Arsitektur Backend Terinspirasi Torque Pro / Lite:**
  - **PID Support Bitmasking (0100):** Membaca daftar sensor yang didukung ECU sebelum polling. Mencegah freeze atau delay pada ECU motor kecil yang tidak memiliki sensor mobil (seperti MAF/EGR).
  - **Prioritized Adaptive Polling Queue:** Sensor cepat (RPM, Kecepatan, Throttle) diperbarui setiap siklus (responsif & halus), sedangkan sensor suhu dan voltase diperbarui secara berkala.
  - **Mode 07 (Pending Fault Codes):** Mendeteksi gejala kerusakan dini sebelum lampu MIL Check Engine sempat menyala.
  - **ISO-TP Multi-Frame Reassembly:** Mendukung reassembly frame CAN bus multiline sehingga tidak ada kode DTC yang terpotong.
  - **Identifikasi Kendaraan (Mode 0902):** Membaca Nomor Rangka (VIN / Chassis Number) kendaraan secara otomatis.
- 🛡️ **Toleran Terhadap Adapter Murah (ELM327 Clone Friendly):**
  Menggunakan *RFCOMM socket fallback* dan urutan inisialisasi yang fleksibel sehingga tidak terjadi error *"Bad Clone"* atau adapter macet.
- 📊 **Live Dashboard Sensor Real-time:**
  - **RPM Mesin** (dengan indikator visual putaran mesin)
  - **Kecepatan Kendaraan** (km/h)
  - **Suhu Cairan Pendingin / Mesin (ECT/EOT)** dengan deteksi dini status normal/tinggi/overheat
  - **Bukaan Gas / TPS (Throttle Position Sensor)** (%)
  - **Voltase Aki Kendaraan (ATRV)** (sangat penting untuk memantau kesehatan aki dan spul motor)
  - **Beban Mesin (Engine Load)** (%)
  - **Suhu Udara Masuk (IAT)** (°C)
  - **Tekanan Udara Manifold (MAP)** (kPa)
- 🔍 **Pindai Kode Kerusakan (DTC Scanner):**
  Membaca kode error ECU (Mode 03) dan menyajikan arti kerusakan lengkap dalam **Bahasa Indonesia** (sensor TPS, ECT/EOT, Injektor, Koil Pengapian, Sensor CKP/Spul, O2 Sensor, dll.).
- 🧹 **Hapus Kerusakan (Reset Check Engine):**
  Mereset kode kerusakan ECU (Mode 04) dan mematikan lampu indikator MIL (*Check Engine*) di speedometer setelah perbaikan.
- 💻 **Konsol Terminal OBD:**
  Bagi mekanik atau teknisi yang ingin mengirim perintah manual (`ATRV`, `0100`, `ATDP`, `010C`, `ATZ`, dll.) dengan log TX/RX interaktif.

---

## 🏍️ Kompatibilitas Sepeda Motor di Indonesia

Aplikasi ini mendukung berbagai motor injeksi yang dihubungkan menggunakan kabel adapter soket diagnostik motor:

| Merk | Tipe / Generasi | Jenis Soket DLC | Status Kompatibilitas |
| :--- | :--- | :--- | :--- |
| **Yamaha** | All New NMAX (2020+), Aerox Connected, R15 V4, WR155R, Fazzio, Grand Filano, XSR155, MT-15, XMAX | Putih 4-Pin (Euro 5) | ✅ **Terbaca Penuh (CAN 11-bit)** |
| **Yamaha** | NMAX Old (2015–2019), Aerox Old, Vixion lama, Mio J / M3 | Putih 3-Pin (K-Line) | ⚠️ Terbaca terbatas (ISO 9141) |
| **Honda** | PCX 160, Vario 160, ADV 160, Stylo 160, CBR250RR baru, Forza 250 | Merah 6-Pin (Euro 5) | ✅ **Terbaca Penuh (CAN 11-bit)** |
| **Honda** | Beat FI, Scoopy FI, Vario 125/150 lama, CB150R/CBR150R lama | Merah/Hitam 4-Pin DLC | ⚠️ Terbaca via K-Line Fast KWP (Khusus ECU yang mendukung mode standar OBD) |
| **Kawasaki** | Ninja 250 FI (2018+), ZX-25R, ZX-4R, KLX 230, Z900, Versys | Putih 4-Pin / 6-Pin | ✅ **Terbaca Penuh (OBD2 CAN)** |
| **Vespa / Piaggio** | Sprint, Primavera, GTS, LX, S 125 i-Get | Hitam 3-Pin / Merah 6-Pin | ✅ **Terbaca Penuh (K-Line / CAN)** |
| **Suzuki** | GSX-R150, GSX-S150, Satria F150 FI, V-Strom 250 | Putih 6-Pin | ✅ **Terbaca (KWP / CAN)** |
| **KTM / Royal Enfield** | Duke 200/250/390, RC 250/390, Hunter 350, Classic 350 | Standar 6-Pin OBD2 | ✅ **Terbaca Penuh** |

---

## 🚗 Kompatibilitas Mobil

- **Mobil Standar Global (2008 ke atas):** Toyota, Honda, Daihatsu, Suzuki, Mitsubishi, Nissan, Mazda, Hyundai, Kia, Wuling, Ford, Chevrolet, dll.
- **Mobil Lokal Indonesia:**
  - Avanza / Xenia / Rush / Terios (Generasi CAN maupun K-Line)
  - Agya / Ayla / Calya / Sigra
  - Brio / Mobilio / Jazz / HRV / BRV
  - Ertiga / XL7 / Ignis
  - Xpander / Pajero Sport
  - Innova / Fortuner (Bensin & Diesel)

---

## 📲 Panduan Pemasangan & Pemakaian

### 1. Persiapan Perangkat
1. Pasang adapter Bluetooth ELM327 ke port OBD-II mobil, atau sambungkan kabel adapter motor ke soket DLC motor Anda.
2. Putar kunci kontak kendaraan ke posisi **ON** (pastikan saklar mesin *Engine Cut-Off* di stang motor berada di posisi **RUN**).
3. Buka **Pengaturan Bluetooth HP** Anda, cari perangkat (biasanya bernama `OBDII` atau `OBD2`), lalu lakukan pairing dengan PIN: `1234` atau `0000`.

### 2. Membuka Aplikasi
1. Buka aplikasi **MotoCar OBD**.
2. Berikan izin Bluetooth yang diminta oleh aplikasi.
3. Di tab **Koneksi**, biarkan opsi tetap pada **⚡ Deteksi Otomatis (Semua Mobil & Motor)**.
4. Klik tombol **Sambung** pada nama adapter Anda.
5. Tunggu 2–5 detik hingga lampu status berubah menjadi hijau (**ECU Terhubung**).
6. Buka tab **Dashboard** untuk memantau data sensor atau tab **DTC Scan** untuk memeriksa kode error.

---

## 🛠️ Build dari Source Code

Aplikasi ini dibangun menggunakan Android Native (Java) murni tanpa ketergantungan library berat:

- **Min SDK:** Android 5.0 (Lollipop - API 21)
- **Target SDK:** Android 14 (API 34)
- **Build System:** Gradle 8.3 & Android Gradle Plugin 8.1.0

### Langkah Kompilasi:
```bash
# Clone repository
git clone https://github.com/justrangga/motocar-obd-scanner.git
cd motocar-obd-scanner

# Build APK Debug
./gradlew assembleDebug

# Lokasi APK:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Lisensi
Didistribusikan di bawah Lisensi MIT. Bebas digunakan, dimodifikasi, dan dikembangkan untuk keperluan pribadi maupun komersial.
