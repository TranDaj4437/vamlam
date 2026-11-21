package com.custom.nodashbhop.wheel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Transparent non-pausing PvP Screen shown while holding X.
 * - Preserves player crouch (Shift) seamlessly so opening/closing the Wheel NEVER forces the player to stand up!
 * - Dynamically maps mouse hover to N active non-Light slices
 * - Only exposes STAND <-> SPEC pill when player has BOTH Stand and Spec
 */
public class WheelRadialScreen extends Screen {
    private final WheelController controller;
    private int hoveredActiveIdx = -1;
    private boolean pillHovered = false;

    public WheelRadialScreen(WheelController controller) {
        super(Component.literal("JCraft Ability Wheel"));
        this.controller = controller;
    }

    @Override
    protected void init() {
        super.init();
        syncCrouchState();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        // 100% transparent background for PvP visibility
    }

    private void syncCrouchState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.keyShift.setDown(WheelController.isShiftHeldRaw());
        }
    }

    @Override
    public void tick() {
        super.tick();
        syncCrouchState();
        if (!WheelController.isWheelKeyHeldRaw()) {
            finishAndClose();
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        syncCrouchState();
        if (!WheelController.isWheelKeyHeldRaw()) {
            finishAndClose();
            return;
        }

        int cx = this.width / 2;
        int cy = this.height / 2 - 14;

        boolean canSwitch = controller.canSwitchMode(Minecraft.getInstance().player);
        updateHover(mouseX, mouseY, cx, cy, canSwitch);
        boolean shiftHeld = WheelController.isShiftHeldRaw();

        WheelRenderer.renderWheel(gui, controller, cx, cy, hoveredActiveIdx, shiftHeld, canSwitch, pillHovered);
        WheelHudOverlay.renderStandCorner(gui, controller, this.height);
    }

    private void updateHover(int mouseX, int mouseY, int cx, int cy, boolean canSwitch) {
        if (canSwitch) {
            int pillX = cx + (int) WheelRenderer.OUTER_RADIUS + 14;
            int pillY = cy - 10;
            pillHovered = mouseX >= pillX && mouseX <= pillX + 62 && mouseY >= pillY && mouseY <= pillY + 20;
            if (pillHovered) {
                hoveredActiveIdx = -1;
                return;
            }
        } else {
            pillHovered = false;
        }

        List<MoveSlotData> active = controller.getActiveSlots();
        int count = active.size();
        if (count == 0) {
            hoveredActiveIdx = -1;
            return;
        }

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        if (dist < WheelRenderer.INNER_RADIUS * 0.85) {
            hoveredActiveIdx = -1;
            return;
        }

        double sliceAngle = (Math.PI * 2.0) / count;
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0 + sliceAngle / 2.0;
        while (angle < 0) angle += Math.PI * 2.0;
        while (angle >= Math.PI * 2.0) angle -= Math.PI * 2.0;

        hoveredActiveIdx = ((int) Math.floor(angle / sliceAngle)) % count;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        int cy = this.height / 2 - 14;
        boolean canSwitch = controller.canSwitchMode(Minecraft.getInstance().player);
        updateHover((int) mouseX, (int) mouseY, cx, cy, canSwitch);

        if (button == 0 && canSwitch && pillHovered) {
            controller.toggleMode();
            return true;
        }
        List<MoveSlotData> active = controller.getActiveSlots();
        if (button == 0 && hoveredActiveIdx >= 0 && hoveredActiveIdx < active.size()) {
            boolean shift = WheelController.isShiftHeldRaw();
            WheelSlot target = active.get(hoveredActiveIdx).slot;
            controller.setSelectedWheelSlot(target);
            this.onClose();
            syncCrouchState();
            controller.castSlot(target, shift, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && controller.canSwitchMode(Minecraft.getInstance().player)) {
            controller.toggleMode();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        syncCrouchState();
    }

    private void finishAndClose() {
        boolean shift = WheelController.isShiftHeldRaw();
        List<MoveSlotData> active = controller.getActiveSlots();
        WheelSlot target = (hoveredActiveIdx >= 0 && hoveredActiveIdx < active.size())
                ? active.get(hoveredActiveIdx).slot
                : null;
        this.onClose();
        syncCrouchState();
        if (target != null) {
            controller.setSelectedWheelSlot(target);
            controller.castSlot(target, shift, false);
        }
    }
}
