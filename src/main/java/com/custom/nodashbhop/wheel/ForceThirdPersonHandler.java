package com.custom.nodashbhop.wheel;

import com.custom.nodashbhop.CS2BhopConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Server-synced Third-Person Camera Controller (/forcecam replace | lock | off)
 * Verified against ShoulderSurfing-Forge-1.20.1-5.3.1, TaCZ 1.2.0-hotfix, and Third Person Shooting: Zero 6.0.1.
 *
 * - Mode REPLACE (default):
 *   1. Completely removes/replaces vanilla THIRD_PERSON_BACK with Shoulder Surfing over-the-shoulder view:
 *      PerspectiveConfig.isThirdPersonReplaced = true, isThirdPersonBackEnabled = false
 *   2. Makes Shoulder Surfing feel like holding a gun with crosshair at all times:
 *      CameraConfig.isCameraDecoupled = false
 *      CrosshairConfig.crosshairType = DYNAMIC
 *      CrosshairConfig.crosshairVisibility[SHOULDER_SURFING] = ALWAYS
 *   3. Players can still freely press F5 to switch between 1st-person and over-the-shoulder 3rd-person.
 * - Mode LOCK (/forcecam lock | /forcecam on):
 *   Also locks F5 and keeps players in Shoulder Surfing (still compatible with TaCZ ADS / scopes).
 * - Mode OFF (/forcecam off):
 *   Disables all camera overrides.
 */
public class ForceThirdPersonHandler {
    private int configCheckTicker = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyInput(InputEvent.Key event) {
        // Only lock F5 / Shoulder Surfing toggle keys in LOCK mode
        if (CS2BhopConfig.forceCamMode != CS2BhopConfig.CAM_LOCK) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null || mc.screen != null) return;

        KeyMapping togglePersp = mc.options.keyTogglePerspective;
        if (togglePersp != null && togglePersp.getKey().getValue() == event.getKey()) {
            togglePersp.setDown(false);
            while (togglePersp.consumeClick()) {}
        }

        for (KeyMapping km : mc.options.keyMappings) {
            String name = km.getName().toLowerCase(Locale.ROOT);
            if ((name.contains("shouldersurfing") || name.contains("shoulder_surfing"))
                    && (name.contains("toggle") || name.contains("perspective") || name.contains("decoupled"))) {
                if (km.getKey().getValue() == event.getKey()) {
                    km.setDown(false);
                    while (km.consumeClick()) {}
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int mode = CS2BhopConfig.forceCamMode;
        if (mode == CS2BhopConfig.CAM_OFF) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options == null || player.isSpectator()) return;

        // Periodically enforce Shoulder Surfing Reloaded 5.3.1 config values live
        if (configCheckTicker <= 0) {
            enforceShoulderSurfingGunStyleConfig();
            configCheckTicker = 20;
        } else {
            configCheckTicker--;
        }

        // Never fight TaCZ 1.2.0-hotfix ADS / Scope camera
        if (isTaCZAiming(player)) {
            return;
        }

        if (mode == CS2BhopConfig.CAM_LOCK) {
            KeyMapping togglePersp = mc.options.keyTogglePerspective;
            if (togglePersp != null) {
                togglePersp.setDown(false);
                while (togglePersp.consumeClick()) {}
            }

            if (!enforceShoulderSurfingPerspective()) {
                if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
            }
        } else if (mode == CS2BhopConfig.CAM_REPLACE) {
            // Allow F5 freely, but if camera ever lands on vanilla THIRD_PERSON_BACK
            // while Shoulder Surfing is not active, immediately switch to SHOULDER_SURFING.
            if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK && !isCurrentlyShoulderSurfing()) {
                enforceShoulderSurfingPerspective();
            }
        }
    }

    /**
     * Checks if the local player is currently aiming down sights (ADS) with a TaCZ 1.2.0-hotfix gun.
     */
    private boolean isTaCZAiming(LocalPlayer player) {
        try {
            Class<?> clientOpCls = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
            Method fromLocal = clientOpCls.getMethod("fromLocalPlayer", LocalPlayer.class);
            Object op = fromLocal.invoke(null, player);
            if (op != null) {
                try {
                    Method isAim = op.getClass().getMethod("isAim");
                    if (Boolean.TRUE.equals(isAim.invoke(op))) return true;
                } catch (Exception ignored) {
                }
                try {
                    Method prog = op.getClass().getMethod("getClientAimingProgress", float.class);
                    Object val = prog.invoke(op, 1.0F);
                    if (val instanceof Number n && n.floatValue() > 0.05F) return true;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Class<?> gunOpCls = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            Method fromLiving = gunOpCls.getMethod("fromLivingEntity", LivingEntity.class);
            Object op = fromLiving.invoke(null, player);
            if (op != null) {
                try {
                    Method synAim = op.getClass().getMethod("getSynIsAiming");
                    if (Boolean.TRUE.equals(synAim.invoke(op))) return true;
                } catch (Exception ignored) {
                }
                try {
                    Method synProg = op.getClass().getMethod("getSynAimingProgress");
                    Object val = synProg.invoke(op);
                    if (val instanceof Number n && n.floatValue() > 0.05F) return true;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private Object getShoulderSurfingInstance() {
        String[] ssClasses = {
                "com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing",
                "com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing"
        };
        for (String clsName : ssClasses) {
            try {
                Class<?> ssCls = Class.forName(clsName);
                Method getInst = ssCls.getMethod("getInstance");
                Object inst = getInst.invoke(null);
                if (inst != null) return inst;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean isCurrentlyShoulderSurfing() {
        Object inst = getShoulderSurfingInstance();
        if (inst == null) return false;
        try {
            Method isSS = inst.getClass().getMethod("isShoulderSurfing");
            return Boolean.TRUE.equals(isSS.invoke(inst));
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Switches to Shoulder Surfing Reloaded 5.x SHOULDER_SURFING perspective via reflection.
     */
    private boolean enforceShoulderSurfingPerspective() {
        Object inst = getShoulderSurfingInstance();
        if (inst == null) return false;

        try {
            boolean alreadySS = false;
            try {
                Method isSS = inst.getClass().getMethod("isShoulderSurfing");
                alreadySS = Boolean.TRUE.equals(isSS.invoke(inst));
            } catch (Exception ignored) {
            }

            if (!alreadySS) {
                for (Method m : inst.getClass().getMethods()) {
                    if (m.getName().equals("changePerspective") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isEnum()) {
                        Object[] constants = m.getParameterTypes()[0].getEnumConstants();
                        if (constants != null) {
                            for (Object c : constants) {
                                if (c.toString().equalsIgnoreCase("SHOULDER_SURFING")) {
                                    m.invoke(inst, c);
                                    return true;
                                }
                            }
                        }
                    }
                }
                try {
                    Method toggle = inst.getClass().getMethod("togglePerspective");
                    toggle.invoke(inst);
                    return true;
                } catch (Exception ignored) {
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Directly configures Shoulder Surfing Reloaded 5.3.1 Config.CLIENT:
     * - perspectiveConfig.isThirdPersonReplaced = true
     * - perspectiveConfig.isThirdPersonBackEnabled = false
     * - cameraConfig.isCameraDecoupled = false
     * - crosshairConfig.crosshairType = CrosshairType.DYNAMIC
     * - crosshairConfig.crosshairVisibility[Perspective.SHOULDER_SURFING] = CrosshairVisibility.ALWAYS
     */
    private void enforceShoulderSurfingGunStyleConfig() {
        try {
            Class<?> cfgCls = Class.forName("com.github.exopandora.shouldersurfing.config.Config");
            Field clientField = cfgCls.getField("CLIENT");
            Object clientCfg = clientField.get(null);
            if (clientCfg == null) return;

            boolean changed = false;

            // 1. PerspectiveConfig: replace & disable vanilla THIRD_PERSON_BACK
            Object perspCfg = invokeNoArg(clientCfg, "getPerspectiveConfig");
            if (perspCfg != null) {
                changed |= setBooleanFieldValue(perspCfg, "isThirdPersonReplaced", true);
                changed |= setBooleanFieldValue(perspCfg, "isThirdPersonBackEnabled", false);
            }

            // 2. CameraConfig: couple camera to player rotation & pull camera closer to character
            Object camCfg = invokeNoArg(clientCfg, "getCameraConfig");
            if (camCfg != null) {
                changed |= setBooleanFieldValue(camCfg, "isCameraDecoupled", false);
                changed |= setDoubleFieldValue(camCfg, "offsetZ", CS2BhopConfig.camOffsetZ);
                changed |= setDoubleFieldValue(camCfg, "offsetX", -0.65D);
                changed |= setDoubleFieldValue(camCfg, "offsetY", 0.15D);
            }

            // 3. CrosshairConfig: DYNAMIC crosshair + ALWAYS visible in SHOULDER_SURFING
            Object crossCfg = invokeNoArg(clientCfg, "getCrosshairConfig");
            if (crossCfg != null) {
                changed |= setEnumFieldValue(crossCfg, "crosshairType", "DYNAMIC");
                try {
                    Field visField = crossCfg.getClass().getDeclaredField("crosshairVisibility");
                    visField.setAccessible(true);
                    Object visMap = visField.get(crossCfg);
                    if (visMap instanceof Map<?, ?> map) {
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            if (entry.getKey() != null && entry.getKey().toString().equalsIgnoreCase("SHOULDER_SURFING")) {
                                changed |= setEnumConfigValue(entry.getValue(), "ALWAYS");
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // Save config if any value was updated
            if (changed) {
                try {
                    Method saveM = clientCfg.getClass().getMethod("save");
                    saveM.invoke(clientCfg);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Object invokeNoArg(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean setDoubleFieldValue(Object holder, String fieldName, double target) {
        try {
            Field f = holder.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object configVal = f.get(holder);
            if (configVal == null) return false;
            Method getM = configVal.getClass().getMethod("get");
            Object cur = getM.invoke(configVal);
            if (cur instanceof Number n && Math.abs(n.doubleValue() - target) > 0.01D) {
                Method setM = configVal.getClass().getMethod("set", Object.class);
                setM.invoke(configVal, Double.valueOf(target));
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean setBooleanFieldValue(Object holder, String fieldName, boolean target) {
        try {
            Field f = holder.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object configVal = f.get(holder);
            if (configVal == null) return false;
            Method getM = configVal.getClass().getMethod("get");
            Object cur = getM.invoke(configVal);
            if (cur instanceof Boolean b && b != target) {
                Method setM = configVal.getClass().getMethod("set", Object.class);
                setM.invoke(configVal, Boolean.valueOf(target));
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean setEnumFieldValue(Object holder, String fieldName, String targetEnumName) {
        try {
            Field f = holder.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return setEnumConfigValue(f.get(holder), targetEnumName);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean setEnumConfigValue(Object configVal, String targetEnumName) {
        if (configVal == null) return false;
        try {
            Method getM = configVal.getClass().getMethod("get");
            Object cur = getM.invoke(configVal);
            if (cur != null && cur.getClass().isEnum()) {
                if (!cur.toString().equalsIgnoreCase(targetEnumName)) {
                    Object[] constants = cur.getClass().getEnumConstants();
                    if (constants != null) {
                        for (Object c : constants) {
                            if (c.toString().equalsIgnoreCase(targetEnumName)) {
                                Method setM = configVal.getClass().getMethod("set", Object.class);
                                setM.invoke(configVal, c);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
