package com.custom.nodashbhop.wheel;

import com.custom.nodashbhop.JCraftStandCheck;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Corner HUD overlay synchronized strictly with the Wheel's active mode (STAND vs SPEC):
 * - Stand/Spec name uses its signature thematic color from StandThemeRegistry
 * - Evolved Stands wrap their name with animated obfuscated (§k) glitch/error characters on both ends
 * - Evolvable Stands center the name directly above centered "RequireEvo: <item>"
 * - Non-evolving / evolved Stands & Specs hide RequireEvo so the name fills the entire frame cleanly
 */
public class WheelHudOverlay {
    private final WheelController controller;

    public WheelHudOverlay(WheelController controller) {
        this.controller = controller;
    }

    @SubscribeEvent
    public void onRenderHud(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        if (!JCraftStandCheck.hasStandOrSpec(mc.player)) return;

        int screenH = mc.getWindow().getGuiScaledHeight();
        renderStandCorner(event.getGuiGraphics(), controller, screenH);
    }

    public static void renderStandCorner(GuiGraphics gui, WheelController controller, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Font font = mc.font;

        boolean specMode = controller.isSpecMode();
        String entityId = specMode
                ? JCraftStandCheck.getSpecId(mc.player)
                : JCraftStandCheck.getStandId(mc.player);
        String rawName = specMode
                ? JCraftStandCheck.getSpecDisplayName(mc.player)
                : JCraftStandCheck.getStandDisplayName(mc.player);
        if (rawName == null || rawName.isEmpty()) return;

        int themeColor = StandThemeRegistry.getThemeColor(entityId, specMode);
        boolean evolved = !specMode && StandThemeRegistry.isEvolvedStand(entityId);
        // Animated glitch/error jumping characters (§k) on both ends for evolved Stands!
        String displayName = evolved ? ("§k#§r " + rawName + " §k#§r") : rawName;

        String evoItem = (!specMode && JCraftStandCheck.hasStand(mc.player))
                ? StandEvoRegistry.getRequiredEvoItem(entityId)
                : null;

        boolean shiftHeld = WheelController.isShiftHeldRaw();
        WheelSlot selSlot = controller.getSelectedWheelSlot();
        MoveSlotData activeMove = controller.slots[selSlot.index];
        String cdText = controller.cooldowns.formatCooldown(selSlot.index);
        float cdRatio = controller.cooldowns.getCooldownRatio(selSlot.index);

        int baseX = 8;
        int baseY = screenH - 36;

        // 1. Compact active cast-key ([C]) move icon box
        int boxSize = 26;
        gui.fill(baseX, baseY, baseX + boxSize, baseY + boxSize, 0x880B0F1A);
        int borderCol = activeMove.isShiftedView(shiftHeld)
                ? 0xCCF59E0B
                : ((themeColor & 0x00FFFFFF) | 0xBB000000);
        gui.renderOutline(baseX, baseY, boxSize, boxSize, borderCol);

        ResourceLocation icon = activeMove.getDisplayIcon(shiftHeld);
        if (icon != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (cdRatio > 0.005F) {
                RenderSystem.setShaderColor(0.55F, 0.55F, 0.62F, 0.90F);
            } else {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            gui.blit(icon, baseX + 3, baseY + 3, 0, 0, 20, 20, 20, 20);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            gui.drawCenteredString(font, activeMove.slot.shortLabel, baseX + boxSize / 2, baseY + 9, 0xFFEAF2FF);
        }

        if (cdRatio > 0.005F) {
            int coverH = Math.max(1, Math.round((boxSize - 2) * cdRatio));
            gui.fill(baseX + 1, baseY + boxSize - 1 - coverH, baseX + boxSize - 1, baseY + boxSize - 1, 0xAA070A12);
            gui.drawCenteredString(font, cdText, baseX + boxSize / 2, baseY + 9, 0xFFFF7A70);
        } else {
            String castKey = controller.getCastKeyShortLabel();
            gui.pose().pushPose();
            gui.pose().scale(0.60F, 0.60F, 1.0F);
            int kx = Math.round((baseX + boxSize - 5) / 0.60F);
            int ky = Math.round((baseY + boxSize - 7) / 0.60F);
            gui.drawCenteredString(font, castKey, kx, ky, 0xFFE2E8F0);
            gui.pose().popPose();
        }

        // 2. Stand / Spec name box (thematic color + §k glitch on evolved Stands)
        int textBoxX = baseX + boxSize + 6;
        int nameW = font.width(displayName);
        int panelBorder = (themeColor & 0x00FFFFFF) | 0x88000000;

        if (evoItem != null) {
            String evoLine = "RequireEvo: " + evoItem;
            int evoW = font.width(evoLine);
            int panelW = Math.max(Math.max(nameW, evoW) + 16, 112);

            gui.fill(textBoxX, baseY, textBoxX + panelW, baseY + boxSize, 0x780B0F1A);
            gui.renderOutline(textBoxX, baseY, panelW, boxSize, panelBorder);

            int centerTextX = textBoxX + panelW / 2;
            gui.drawCenteredString(font, displayName, centerTextX, baseY + 3, themeColor);
            gui.drawCenteredString(font, evoLine, centerTextX, baseY + 14, 0xFFFFC857);
        } else {
            int panelW = Math.max(nameW + 18, 96);

            gui.fill(textBoxX, baseY, textBoxX + panelW, baseY + boxSize, 0x780B0F1A);
            gui.renderOutline(textBoxX, baseY, panelW, boxSize, panelBorder);

            int centerTextX = textBoxX + panelW / 2;
            gui.drawCenteredString(font, displayName, centerTextX, baseY + 9, themeColor);
        }
    }
}
