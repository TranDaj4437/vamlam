package com.custom.nodashbhop;

/**
 * Cấu hình đồng bộ từ Server do OP quản lý (/jnodash, /cs2bhop, /forcecam).
 * Riêng showHud là tùy chọn cá nhân từng người chơi (mặc định TẮT, bật/tắt bằng /bhophud không cần OP).
 */
public final class CS2BhopConfig {
    public static final int CAM_OFF = 0;
    public static final int CAM_REPLACE = 1; // Mặc định: thay góc nhìn 3 sau lưng gốc = Shoulder Surfing có crosshair, vẫn bấm F5 đổi 1st/3rd
    public static final int CAM_LOCK = 2;    // Ép luôn ở góc nhìn thứ 3 Shoulder Surfing + khóa F5 (trừ khi ADS súng TaCZ)

    // Tùy chọn cá nhân client (mọi người chơi tự bật/tắt)
    public static volatile boolean showHud = false;

    // Trạng thái đồng bộ toàn server (chỉ OP thay đổi được)
    public static volatile boolean noDashEnabled = true;
    public static volatile boolean bhopEnabled = true;
    public static volatile boolean autoBhopHoldSpace = true;
    public static volatile boolean scrollWheelJump = false;
    public static volatile int forceCamMode = CAM_REPLACE;
    public static volatile double camOffsetZ = 2.65; // Gần nhân vật hơn mặc định 4.0 của Shoulder Surfing

    // Thông số BHop CS2 mượt & dễ ăn đà hơn (không ảnh hưởng đi bộ thường)
    public static volatile double airAccelerate = 22.0;
    public static volatile double wishSpeedCap = 0.085;
    public static volatile double maxBhopSpeed = 0.62;      // ~553 u/s
    public static volatile double landingPreserve = 1.00;
    public static volatile double jumpBoost = 0.055;

    private CS2BhopConfig() {}

    public static void applyMatchmakingPreset() {
        airAccelerate = 18.0;
        wishSpeedCap = 0.072;
        maxBhopSpeed = 0.48;     // ~428 u/s (CS2 MM mượt)
        landingPreserve = 0.985;
        jumpBoost = 0.040;
    }

    public static void applyCommunityPreset() {
        airAccelerate = 26.0;
        wishSpeedCap = 0.095;
        maxBhopSpeed = 0.74;     // ~660 u/s (Server BHop/KZ)
        landingPreserve = 1.01;
        jumpBoost = 0.065;
    }
}
