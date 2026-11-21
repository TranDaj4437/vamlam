package com.custom.nodashbhop.wheel;

public enum WheelSlot {
    LIGHT(0, "M1", "Light", 0xFF60A5FA,
            new String[]{"LIGHT"},
            new String[]{"STAND_LIGHT", "SPEC_LIGHT", "LIGHT"},
            new String[]{"light", "m1", "punch", "attack"},
            new String[]{"light", "m1"}),
    HEAVY(1, "HVY", "Heavy", 0xFFF59E0B,
            new String[]{"HEAVY"},
            new String[]{"STAND_HEAVY", "SPEC_HEAVY", "HEAVY"},
            new String[]{"heavy"},
            new String[]{"heavy", "hvy"}),
    BARRAGE(2, "BRG", "Barrage", 0xFF34D399,
            new String[]{"BARRAGE"},
            new String[]{"STAND_BARRAGE", "SPEC_BARRAGE", "BARRAGE"},
            new String[]{"barrage"},
            new String[]{"barrage", "brg"}),
    SPECIAL1(3, "SP1", "Special 1", 0xFFA78BFA,
            new String[]{"SPECIAL1", "SPECIAL_1"},
            new String[]{"STAND_SP1", "SPEC_SP1", "SPECIAL1", "SP1"},
            new String[]{"special1", "special_1", "sp1", "special 1"},
            new String[]{"special1", "special_1", "sp1"}),
    SPECIAL2(4, "SP2", "Special 2", 0xFFEC4899,
            new String[]{"SPECIAL2", "SPECIAL_2"},
            new String[]{"STAND_SP2", "SPEC_SP2", "SPECIAL2", "SP2"},
            new String[]{"special2", "special_2", "sp2", "special 2"},
            new String[]{"special2", "special_2", "sp2"}),
    SPECIAL3(5, "SP3", "Special 3", 0xFFFB7185,
            new String[]{"SPECIAL3", "SPECIAL_3"},
            new String[]{"STAND_SP3", "SPEC_SP3", "SPECIAL3", "SP3"},
            new String[]{"special3", "special_3", "sp3", "special 3"},
            new String[]{"special3", "special_3", "sp3"}),
    ULTIMATE(6, "ULT", "Ultimate", 0xFFEF4444,
            new String[]{"ULTIMATE", "ULT"},
            new String[]{"STAND_ULT", "SPEC_ULT", "ULTIMATE", "ULT"},
            new String[]{"ult", "ultimate"},
            new String[]{"ult", "ultimate"}),
    UTILITY(7, "UTL", "Utility", 0xFF38BDF8,
            new String[]{"UTILITY", "STANDBY_OFF"},
            new String[]{"UTILITY", "STAND_MM", "SPEC_UTILITY"},
            new String[]{"utility", "util", "movement"},
            new String[]{"utility", "util", "movement"});

    public final int index;
    public final String shortLabel;
    public final String defaultName;
    public final int accentColor;
    public final String[] moveClassTokens;
    public final String[] cooldownTokens;
    public final String[] keybindTokens;
    public final String[] iconTokens;

    WheelSlot(int index, String shortLabel, String defaultName, int accentColor,
              String[] moveClassTokens, String[] cooldownTokens,
              String[] keybindTokens, String[] iconTokens) {
        this.index = index;
        this.shortLabel = shortLabel;
        this.defaultName = defaultName;
        this.accentColor = accentColor;
        this.moveClassTokens = moveClassTokens;
        this.cooldownTokens = cooldownTokens;
        this.keybindTokens = keybindTokens;
        this.iconTokens = iconTokens;
    }

    public static WheelSlot byIndex(int idx) {
        WheelSlot[] all = values();
        return all[((idx % all.length) + all.length) % all.length];
    }
}
