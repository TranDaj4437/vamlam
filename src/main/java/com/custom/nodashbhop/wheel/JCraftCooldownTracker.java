package com.custom.nodashbhop.wheel;

import com.custom.nodashbhop.JCraftStandCheck;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Tracks live JCraft 0.18.2 ability cooldowns using exact CooldownType enum names:
 * - Stand: STAND_LIGHT, STAND_HEAVY, STAND_BARRAGE, STAND_SP1, STAND_SP2, STAND_SP3, STAND_ULTIMATE, UTILITY
 * - Spec:  LIGHT, HEAVY, BARRAGE, SPECIAL1, SPECIAL2, SPECIAL3, ULTIMATE, UTILITY
 * Automatically clears immediately when JCraft Cooldown Cancel (RA) is used!
 */
public class JCraftCooldownTracker {
    private static final String[] STAND_CD_NAMES = {
            "STAND_LIGHT", "STAND_HEAVY", "STAND_BARRAGE", "STAND_SP1",
            "STAND_SP2", "STAND_SP3", "STAND_ULTIMATE", "UTILITY"
    };

    private static final String[] SPEC_CD_NAMES = {
            "LIGHT", "HEAVY", "BARRAGE", "SPECIAL1",
            "SPECIAL2", "SPECIAL3", "ULTIMATE", "UTILITY"
    };

    private final int[] remainingTicks = new int[8];
    private final int[] maxTicks = new int[8];

    private Class<?> cachedEnumCls = null;
    private boolean enumChecked = false;
    private KeyMapping cooldownCancelKey = null;
    private boolean cancelKeyScanned = false;
    private boolean cancelWasDown = false;

    public void tick(Player player, boolean specMode) {
        if (player == null) {
            resetAll();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!cancelKeyScanned && mc.options != null) {
            cooldownCancelKey = findCooldownCancelKey(mc);
            cancelKeyScanned = true;
        }
        if (cooldownCancelKey != null) {
            boolean down = cooldownCancelKey.isDown();
            if (down && !cancelWasDown) {
                resetAll();
            }
            cancelWasDown = down;
        }

        if (!enumChecked) {
            cachedEnumCls = JCraftStandCheck.findCooldownTypeEnum();
            enumChecked = true;
        }

        Object cdComp = JCraftStandCheck.getCooldownComponent(player);
        Object[] enumConstants = (cachedEnumCls != null && cachedEnumCls.isEnum())
                ? cachedEnumCls.getEnumConstants()
                : null;

        String[] names = specMode ? SPEC_CD_NAMES : STAND_CD_NAMES;

        for (int i = 0; i < 8; i++) {
            int liveRem = 0;
            int liveMax = 0;

            if (cdComp != null && enumConstants != null) {
                Object matched = findEnumByName(enumConstants, names[i]);
                if (matched != null) {
                    liveRem = invokeIntGetter(cdComp, cachedEnumCls, matched,
                            "getCooldown", "get", "getRemaining");
                    liveMax = invokeIntGetter(cdComp, cachedEnumCls, matched,
                            "getInitialDuration", "getMaxCooldown", "getDuration");
                }
            }

            if (liveRem > 0) {
                remainingTicks[i] = liveRem;
                if (liveMax >= liveRem) {
                    maxTicks[i] = liveMax;
                } else if (liveRem > maxTicks[i]) {
                    maxTicks[i] = liveRem;
                }
                if (maxTicks[i] <= 0) {
                    maxTicks[i] = liveRem;
                }
            } else {
                remainingTicks[i] = 0;
                maxTicks[i] = 0;
            }
        }
    }

    private KeyMapping findCooldownCancelKey(Minecraft mc) {
        for (KeyMapping km : mc.options.keyMappings) {
            String raw = km.getName().toLowerCase(Locale.ROOT).replace("_", "").replace(".", "");
            if (raw.contains("jcraft") && raw.contains("cooldowncancel")) {
                return km;
            }
        }
        return null;
    }

    private Object findEnumByName(Object[] constants, String exactName) {
        for (Object c : constants) {
            if (c.toString().equalsIgnoreCase(exactName)) return c;
        }
        return null;
    }

    public int getRemainingTicks(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 8) return 0;
        return remainingTicks[slotIndex];
    }

    public float getRemainingSeconds(int slotIndex) {
        return getRemainingTicks(slotIndex) / 20.0F;
    }

    public float getCooldownRatio(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 8) return 0.0F;
        int rem = remainingTicks[slotIndex];
        int max = maxTicks[slotIndex];
        if (rem <= 0 || max <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, (float) rem / (float) max));
    }

    public String formatCooldown(int slotIndex) {
        float sec = getRemainingSeconds(slotIndex);
        if (sec <= 0.05F) return "";
        if (sec >= 10.0F) {
            return String.format(Locale.ROOT, "%.0fs", sec);
        }
        return String.format(Locale.ROOT, "%.1fs", sec);
    }

    public void resetAll() {
        for (int i = 0; i < 8; i++) {
            remainingTicks[i] = 0;
            maxTicks[i] = 0;
        }
    }

    private int invokeIntGetter(Object target, Class<?> enumCls, Object enumVal, String... names) {
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name, enumCls);
                Object res = m.invoke(target, enumVal);
                if (res instanceof Number n) return n.intValue();
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
