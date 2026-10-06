package com.motocar.obdscanner;

public enum ObdProtocol {
    AUTO(
        "⚡ Deteksi Otomatis (Semua Mobil & Motor)",
        "Otomatis memindai dan mencocokkan protokol ECU (Yamaha, Honda, Kawasaki, Vespa, Suzuki, Daihatsu, Toyota). Tidak perlu setting manual.",
        new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATST96", "ATSP0"}
    ),
    CAN_11_500(
        "Mobil Modern & Motor Euro 5 (CAN 11-bit 500k)",
        "Yamaha NMAX/Aerox Euro 5, Honda PCX/Vario 160 Euro 5, Mobil tahun 2012+.",
        new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATST96", "ATSP6"}
    ),
    CAN_29_500(
        "Mobil Modern CAN 29-bit 500k",
        "Digunakan pada beberapa varian mobil Honda, Ford, atau SUV modern.",
        new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATST96", "ATSP7"}
    ),
    KWP_FAST(
        "Motor Honda FI / K-Line Fast (ISO 14230-4 KWP)",
        "Honda Beat, Vario 125/150 K-Line, Suzuki FI, Avanza/Xenia non-CAN.",
        new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATST96", "ATSP5"}
    ),
    ISO_9141(
        "Motor Yamaha FI / K-Line Slow (ISO 9141-2)",
        "Yamaha generasi K-Line 3-pin DLC, Mobil sebelum tahun 2008.",
        new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATST96", "ATSP3"}
    ),
    DAIHATSU_TOYOTA(
        "Daihatsu & Toyota Lokal (Xenia, Ayla, Avanza)",
        "Inisialisasi khusus untuk ECU Daihatsu/Toyota KWP di Indonesia.",
        new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATST96", "ATSP0", "ATSH8110FC", "ATIB10", "ATIIA10"}
    ),
    CLONE_SAFE(
        "Safe Mode (Toleran Kloning ELM327 Murah)",
        "Timeout lebih panjang (ATSTFF) dan mematikan fungsi sensitif agar adapter murah tidak hang.",
        new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSTFF", "ATSP0"}
    );

    private final String displayName;
    private final String description;
    private final String[] initCommands;

    ObdProtocol(String displayName, String description, String[] initCommands) {
        this.displayName = displayName;
        this.description = description;
        this.initCommands = initCommands;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String[] getInitCommands() {
        return initCommands;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
