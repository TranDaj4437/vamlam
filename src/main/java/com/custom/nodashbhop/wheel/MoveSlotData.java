package com.custom.nodashbhop.wheel;

import net.minecraft.resources.ResourceLocation;

public class MoveSlotData {
    public final WheelSlot slot;
    public boolean present;
    public String normalName;
    public String normalDescription;
    public String crouchName;
    public String crouchDescription;
    public boolean hasCrouchVariant;
    public ResourceLocation normalIcon;
    public ResourceLocation crouchIcon;

    public MoveSlotData(WheelSlot slot) {
        this.slot = slot;
        this.present = false;
        this.normalName = slot.defaultName;
        this.normalDescription = "";
        this.crouchName = "";
        this.crouchDescription = "";
        this.hasCrouchVariant = false;
        this.normalIcon = null;
        this.crouchIcon = null;
    }

    public boolean isShiftedView(boolean shiftHeld) {
        return shiftHeld && hasCrouchVariant;
    }

    public String getDisplayName(boolean shiftHeld) {
        if (isShiftedView(shiftHeld) && crouchName != null && !crouchName.isEmpty()) {
            return crouchName;
        }
        return (normalName != null && !normalName.isEmpty()) ? normalName : slot.defaultName;
    }

    /**
     * Only returns crouchIcon when shiftHeld AND this slot genuinely has a Shift variant.
     * Otherwise strictly keeps normalIcon unchanged.
     */
    public ResourceLocation getDisplayIcon(boolean shiftHeld) {
        if (isShiftedView(shiftHeld) && crouchIcon != null) {
            return crouchIcon;
        }
        return normalIcon;
    }
}
