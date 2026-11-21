package com.custom.nodashbhop.wheel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the transparent PvP Ronova-style radial Ability Wheel v3.2:
 * - Dynamic slice count (Mandom = 2 wide 180-deg halves; Specs exclude Utility & empty slots; Stands exclude Light M1)
 * - Center hub circle displays Mandom's Rewind icon (for Mandom) or Stand/Spec Light (M1) icon (no "STAND"/"SPEC" text)
 * - Bound keybind shown cleanly underneath each move's label when assigned
 * - Inward-shrinking radial cooldown overlay + timer (syncs with Cooldown Cancel RA)
 * - STAND <-> SPEC pill ONLY shown when player has BOTH Stand and Spec
 * - Hover-only /stand about EN + Vietnamese cover description panel
 */
public final class WheelRenderer {
    public static final float INNER_RADIUS = 38.0F;
    public static final float OUTER_RADIUS = 104.0F;
    private static final int ARC_SEGMENTS = 26;

    private WheelRenderer() {}

    public static void renderWheel(GuiGraphics gui, WheelController controller,
                                   int centerX, int centerY, int hoveredActiveIdx,
                                   boolean shiftHeld, boolean canSwitch, boolean pillHovered) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Matrix4f pose = gui.pose().last().pose();

        List<MoveSlotData> activeSlots = controller.getActiveSlots();
        int count = activeSlots.size();
        if (count == 0) return;

        float sliceAngle = (float) (Math.PI * 2.0 / count);
        float gapAngle = count > 1 ? 0.032F : 0.0F;
        WheelSlot selectedSlot = controller.getSelectedWheelSlot();

        // 1. Draw N dynamic radial wedges + inward-shrinking cooldown fills
        for (int i = 0; i < count; i++) {
            MoveSlotData data = activeSlots.get(i);
            boolean isHovered = (i == hoveredActiveIdx);
            boolean isSelected = (data.slot == selectedSlot);
            boolean isShifted = data.isShiftedView(shiftHeld);

            float startA = (float) (-Math.PI / 2.0) + i * sliceAngle - sliceAngle / 2.0F + gapAngle / 2.0F;
            float endA = startA + sliceAngle - gapAngle;

            int baseColor;
            if (isHovered) {
                baseColor = isShifted ? 0xA85C3310 : 0xA8163A5C;
            } else if (isSelected) {
                baseColor = isShifted ? 0x8A3B2412 : 0x8A132842;
            } else {
                baseColor = 0x700B0F1A;
            }
            drawAnnularSector(pose, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, startA, endA, baseColor);

            if (isHovered || isSelected || isShifted) {
                int rimColor = isShifted
                        ? (isHovered ? 0xFFF59E0B : 0xBBF59E0B)
                        : (isHovered ? 0xFF38BDF8 : 0xAA60A5FA);
                drawAnnularSector(pose, centerX, centerY, OUTER_RADIUS - 2.6F, OUTER_RADIUS, startA, endA, rimColor);
            }

            // Cooldown radial darkening overlay shrinking FROM OUTER EDGE INWARD TOWARD CENTER
            float cdRatio = controller.cooldowns.getCooldownRatio(data.slot.index);
            if (cdRatio > 0.005F) {
                float rCd = INNER_RADIUS + (OUTER_RADIUS - INNER_RADIUS) * cdRatio;
                drawAnnularSector(pose, centerX, centerY, INNER_RADIUS, rCd, startA, endA, 0xBE06080F);
                float bandInner = Math.max(INNER_RADIUS, rCd - 2.0F);
                drawAnnularSector(pose, centerX, centerY, bandInner, rCd, startA, endA, 0xEEFF5E57);
            }
        }

        // 2. Center hub circle (Rewind icon for Mandom; Light M1 icon for all other Stands/Specs; NO text!)
        drawAnnularSector(pose, centerX, centerY, 0.0F, INNER_RADIUS - 1.5F, 0.0F, (float) (Math.PI * 2.0), 0xA80B0F1A);
        int hubRingColor = shiftHeld ? 0xCCF59E0B : (controller.isSpecMode() ? 0xCC34D399 : 0xCC38BDF8);
        drawAnnularSector(pose, centerX, centerY, INNER_RADIUS - 2.0F, INNER_RADIUS, 0.0F, (float) (Math.PI * 2.0), hubRingColor);

        ItemStack hubItem = controller.getCenterHubItemStack();
        if (!hubItem.isEmpty()) {
            gui.pose().pushPose();
            gui.pose().translate(centerX - 14.0F, centerY - 14.0F, 0.0F);
            gui.pose().scale(1.75F, 1.75F, 1.0F);
            gui.renderItem(hubItem, 0, 0);
            gui.pose().popPose();
        } else {
            MoveSlotData hubData = controller.getCenterHubSlotData();
            ResourceLocation centerIcon = hubData != null ? hubData.getDisplayIcon(false) : null;
            if (centerIcon != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                gui.blit(centerIcon, centerX - 14, centerY - 14, 0, 0, 28, 28, 28, 28);
            } else {
                gui.fill(centerX - 10, centerY - 10, centerX + 10, centerY + 10, 0x661E293B);
                gui.renderOutline(centerX - 10, centerY - 10, 20, 20, hubRingColor);
            }
        }

        // 3. Draw per-Stand icons, move label, bound keybind underneath move label, and cooldown countdown
        float iconRadius = (INNER_RADIUS + OUTER_RADIUS) * 0.51F;
        for (int i = 0; i < count; i++) {
            MoveSlotData data = activeSlots.get(i);
            boolean isShifted = data.isShiftedView(shiftHeld);
            float midA = (float) (-Math.PI / 2.0) + i * sliceAngle;

            int ix = Math.round(centerX + (float) Math.cos(midA) * iconRadius);
            int iy = Math.round(centerY + (float) Math.sin(midA) * iconRadius);

            ResourceLocation icon = data.getDisplayIcon(shiftHeld);
            float cdRatio = controller.cooldowns.getCooldownRatio(data.slot.index);

            if (icon != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                if (cdRatio > 0.005F) {
                    RenderSystem.setShaderColor(0.55F, 0.55F, 0.62F, 0.90F);
                } else {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                gui.blit(icon, ix - 11, iy - 15, 0, 0, 22, 22, 22, 22);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                int badgeColor = isShifted ? 0xCCF59E0B : (data.slot.accentColor & 0x00FFFFFF) | 0xCC000000;
                gui.fill(ix - 11, iy - 15, ix + 11, iy + 7, 0x99101624);
                gui.renderOutline(ix - 11, iy - 15, 22, 22, badgeColor);
                gui.drawCenteredString(font, data.slot.shortLabel, ix, iy - 8, isShifted ? 0xFFFFD166 : 0xFFEAF2FF);
            }

            String cdText = controller.cooldowns.formatCooldown(data.slot.index);
            String boundKey = controller.getBoundKeyLabel(data.slot, controller.isSpecMode());
            String shortName = abbreviateMoveName(data.getDisplayName(shiftHeld), 12);

            if (!cdText.isEmpty()) {
                gui.fill(ix - 14, iy + 7, ix + 14, iy + 17, 0xCC090D16);
                gui.drawCenteredString(font, cdText, ix, iy + 8, 0xFFFF7A70);
            } else {
                gui.pose().pushPose();
                gui.pose().scale(0.65F, 0.65F, 1.0F);
                int sx = Math.round(ix / 0.65F);
                int sy = Math.round((iy + 8) / 0.65F);
                int nameCol = isShifted ? 0xFFFFD166 : 0xFFE2E8F0;
                gui.drawCenteredString(font, shortName, sx, sy, nameCol);

                if (!boundKey.isEmpty()) {
                    gui.drawCenteredString(font, "[" + boundKey + "]", sx, sy + 10, 0xFF7DD3FC);
                } else if (isShifted) {
                    gui.drawCenteredString(font, "SHIFT", sx, sy + 10, 0xFFFFB703);
                }
                gui.pose().popPose();
            }
        }

        // 4. Compact STAND <-> SPEC switch pill ONLY when player has BOTH Stand and Spec
        if (canSwitch) {
            int pillX = centerX + (int) OUTER_RADIUS + 14;
            int pillY = centerY - 10;
            int pillW = 62;
            int pillH = 20;
            int pillBg = pillHovered ? 0xCC1E3A5F : 0x880B101C;
            int pillBorder = controller.isSpecMode() ? 0xCC34D399 : 0xCC38BDF8;
            gui.fill(pillX, pillY, pillX + pillW, pillY + pillH, pillBg);
            gui.renderOutline(pillX, pillY, pillW, pillH, pillBorder);
            String pillText = controller.isSpecMode() ? "SPEC ⇄" : "STAND ⇄";
            gui.drawCenteredString(font, pillText, pillX + pillW / 2, pillY + 6, controller.isSpecMode() ? 0xFF6EE7B7 : 0xFF7DD3FC);
        }

        // 5. Description card ONLY when hovering over a move slice
        if (hoveredActiveIdx >= 0 && hoveredActiveIdx < count) {
            MoveSlotData hov = activeSlots.get(hoveredActiveIdx);
            String boundKey = controller.getBoundKeyLabel(hov.slot, controller.isSpecMode());
            renderDescriptionCard(gui, font, hov, shiftHeld,
                    controller.cooldowns.formatCooldown(hov.slot.index),
                    boundKey, centerX, centerY + (int) OUTER_RADIUS + 10);
        }
    }

    private static String abbreviateMoveName(String name, int maxLen) {
        if (name == null) return "";
        String trimmed = name.trim();
        if (trimmed.length() <= maxLen) return trimmed;
        return trimmed.substring(0, maxLen - 1) + "…";
    }

    private static void renderDescriptionCard(GuiGraphics gui, Font font, MoveSlotData data,
                                              boolean shiftHeld, String cdText, String boundKey,
                                              int centerX, int topY) {
        int cardW = 308;
        int pad = 7;
        int textMaxW = cardW - pad * 2;

        String title = data.getDisplayName(shiftHeld);
        if (!boundKey.isEmpty()) {
            title += "  [" + boundKey + "]";
        }
        if (!cdText.isEmpty()) {
            title += "  §c(" + cdText + ")";
        }

        String normalDescEn = (data.normalDescription != null && !data.normalDescription.isEmpty())
                ? data.normalDescription
                : "No description synced yet (run /stand about or /jwheel reload)";
        String normalDescVi = (data.normalDescription != null && !data.normalDescription.isEmpty())
                ? JCraftViTranslator.translate(data.normalDescription)
                : "Chưa đồng bộ mô tả (gõ /stand about hoặc /jwheel reload)";

        List<FormattedCharSequence> lines = new ArrayList<>();
        List<Integer> lineColors = new ArrayList<>();

        String normalPrefix = data.hasCrouchVariant ? "Normal (" + data.normalName + "): " : "";
        for (FormattedCharSequence seq : font.split(Component.literal(normalPrefix + normalDescEn), textMaxW)) {
            lines.add(seq);
            lineColors.add(shiftHeld && data.hasCrouchVariant ? 0xFF9FB0C4 : 0xFFEAF2FC);
        }
        for (FormattedCharSequence seq : font.split(Component.literal("↳ VI: " + normalDescVi), textMaxW)) {
            lines.add(seq);
            lineColors.add(shiftHeld && data.hasCrouchVariant ? 0xFF7FA9C2 : 0xFF7DD3FC);
        }

        if (data.hasCrouchVariant) {
            String sName = (data.crouchName != null && !data.crouchName.isEmpty()) ? data.crouchName : (data.normalName + " (Shift)");
            String sDescEn = (data.crouchDescription != null && !data.crouchDescription.isEmpty())
                    ? data.crouchDescription
                    : "Shift variant from /stand about";
            String sDescVi = (data.crouchDescription != null && !data.crouchDescription.isEmpty())
                    ? JCraftViTranslator.translate(data.crouchDescription)
                    : "Biến thể Shift từ /stand about";

            for (FormattedCharSequence seq : font.split(Component.literal("[SHIFT] " + sName + ": " + sDescEn), textMaxW)) {
                lines.add(seq);
                lineColors.add(shiftHeld ? 0xFFFFD166 : 0xFFE5B567);
            }
            for (FormattedCharSequence seq : font.split(Component.literal("↳ VI: " + sDescVi), textMaxW)) {
                lines.add(seq);
                lineColors.add(shiftHeld ? 0xFFFDE68A : 0xFFD4B27A);
            }
        }

        int maxLines = Math.min(lines.size(), 8);
        int cardH = pad + 12 + maxLines * 10 + pad;
        int leftX = centerX - cardW / 2;

        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (topY + cardH > screenH - 6) {
            topY = screenH - cardH - 6;
        }

        gui.fill(leftX, topY, leftX + cardW, topY + cardH, 0xBC0A0E17);
        int borderCol = data.isShiftedView(shiftHeld) ? 0xCCF59E0B : 0xAA38BDF8;
        gui.renderOutline(leftX, topY, cardW, cardH, borderCol);

        gui.drawString(font, title, leftX + pad, topY + pad, data.isShiftedView(shiftHeld) ? 0xFFFFD166 : 0xFFFFFFFF, true);

        int curY = topY + pad + 12;
        for (int i = 0; i < maxLines; i++) {
            gui.drawString(font, lines.get(i), leftX + pad, curY, lineColors.get(i), false);
            curY += 10;
        }
    }

    public static void drawAnnularSector(Matrix4f pose, float cx, float cy,
                                         float rIn, float rOut,
                                         float startAngle, float endAngle,
                                         int argb) {
        if (rOut <= rIn) return;
        float a = ((argb >>> 24) & 0xFF) / 255.0F;
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float g = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int s = 0; s <= ARC_SEGMENTS; s++) {
            float t = (float) s / (float) ARC_SEGMENTS;
            float ang = startAngle + (endAngle - startAngle) * t;
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);

            bb.vertex(pose, cx + cos * rOut, cy + sin * rOut, 0.0F).color(r, g, b, a).endVertex();
            bb.vertex(pose, cx + cos * rIn, cy + sin * rIn, 0.0F).color(r, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
