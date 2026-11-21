package com.custom.nodashbhop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Đồng hồ tốc độ BHop nhỏ gọn đặt ở GÓC DƯỚI BÊN PHẢI màn hình.
 * Gắn vào lớp "hotbar" để KHÔNG bao giờ bị Custom Crosshair Mod ẩn mất!
 */
public class CS2VelocityHud {
    private final CS2BhopEngine engine;

    public CS2VelocityHud(CS2BhopEngine engine) {
        this.engine = engine;
    }

    @SubscribeEvent
    public void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!CS2BhopConfig.showHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        // Gắn vào lớp hotbar (luôn render ngay cả khi cài Custom Crosshair Mod)
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;

        int speed = engine.getHudSpeedUnits();
        int delta = engine.getHudDeltaUnits();
        int chain = engine.getHopChain();

        int color;
        if (speed >= 360) color = 0xFF55FFFF;
        else if (delta > 0 && speed > 250) color = 0xFF55FF55;
        else if (delta < 0 && speed > 255) color = 0xFFFFAA55;
        else color = 0xFFFFFFFF;

        String line1 = speed + " u/s";
        String line2 = "HOP x" + chain + " | " + engine.getSyncPercent() + "%";

        GuiGraphics gui = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Thu nhỏ 0.8x đặt gọn ở góc dưới bên phải (không che tầm nhìn PvP)
        float scale = 0.80F;
        int boxW = 84;
        int boxH = 22;

        gui.pose().pushPose();
        gui.pose().scale(scale, scale, 1.0F);

        int drawX = (int) (screenW / scale) - boxW - 6;
        int drawY = (int) (screenH / scale) - boxH - 6;

        gui.fill(drawX, drawY, drawX + boxW, drawY + boxH, 0x660B0E18);
        gui.renderOutline(drawX, drawY, boxW, boxH, speed >= 300 ? 0xAA55FFFF : 0x668899BB);
        gui.drawCenteredString(mc.font, line1, drawX + boxW / 2, drawY + 2, color);
        gui.drawCenteredString(mc.font, "§7" + line2, drawX + boxW / 2, drawY + 12, 0xFFD0D8E8);

        gui.pose().popPose();
    }
}
