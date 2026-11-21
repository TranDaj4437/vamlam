package com.custom.nodashbhop;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Tự động ẩn hình ảnh các ô chiêu thức bên phải màn hình của JCraft (Move UI / Icon HUD).
 */
public class JCraftHudHider {
    private boolean configApplied = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        String ns = event.getOverlay().id().getNamespace().toLowerCase(Locale.ROOT);
        String path = event.getOverlay().id().getPath().toLowerCase(Locale.ROOT);
        if (ns.contains("jcraft") || path.contains("jcraft") || path.contains("stand_hud") || path.contains("move_icon")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (configApplied || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            return;
        }
        configApplied = true;
        tryDisableJCraftIconHudViaConfig();
    }

    private void tryDisableJCraftIconHudViaConfig() {
        String[] configClasses = {
                "net.arna.jcraft.client.JClientConfig",
                "net.arna.jcraft.common.config.JClientConfig",
                "net.arna.jcraft.client.config.JClientConfig"
        };
        for (String clsName : configClasses) {
            try {
                Class<?> cls = Class.forName(clsName);
                Object instance = null;
                for (String mName : new String[]{"getInstance", "get"}) {
                    try {
                        Method m = cls.getMethod(mName);
                        instance = m.invoke(null);
                        break;
                    } catch (Exception ignored) {
                    }
                }
                if (instance == null) {
                    for (Field f : cls.getDeclaredFields()) {
                        if (f.getType().equals(cls)) {
                            f.setAccessible(true);
                            instance = f.get(null);
                            break;
                        }
                    }
                }
                if (instance == null) continue;

                // Tắt cả Icon HUD lẫn Text Cooldown HUD (Combo Breaker / Right Alt / Mouse 4 - 0.1s) trong JClientConfig
                for (Field f : cls.getDeclaredFields()) {
                    String fn = f.getName().toLowerCase(Locale.ROOT);
                    boolean isHudField = fn.contains("icon") || fn.contains("moveui") || fn.contains("move_ui")
                            || fn.contains("hud") || fn.contains("cooldown") || fn.contains("overlay");
                    if (!isHudField) continue;

                    f.setAccessible(true);
                    Class<?> ft = f.getType();
                    if (ft == boolean.class || ft == Boolean.class) {
                        if (fn.contains("hide") || fn.contains("disable")) {
                            f.set(instance, true);
                        } else {
                            f.set(instance, false);
                        }
                    } else if (ft.isEnum()) {
                        for (Object c : ft.getEnumConstants()) {
                            String cn = c.toString().toUpperCase(Locale.ROOT);
                            if (cn.equals("NEVER") || cn.equals("OFF") || cn.equals("NONE") || cn.equals("HIDDEN") || cn.equals("DISABLED")) {
                                f.set(instance, c);
                                break;
                            }
                        }
                    } else if (ft == int.class || ft == Integer.class) {
                        // Đẩy tọa độ X/Y của HUD chữ hồi chiêu ra ngoài màn hình nếu là tọa độ
                        if (fn.contains("x") || fn.contains("y") || fn.contains("offset") || fn.contains("pos")) {
                            f.set(instance, 9999);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
