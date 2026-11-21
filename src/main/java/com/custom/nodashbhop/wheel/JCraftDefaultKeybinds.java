package com.custom.nodashbhop.wheel;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Sets custom default JCraft 0.18.2 keybinds on first launch (and updates KeyMapping defaultKey
 * so clicking "Reset" in Controls also restores these defaults):
 * - Barrage: Mouse Button 5
 * - Cooldown Cancel: Right Alt
 * - Dash: Left Control
 * - Stand Summon: Tab
 * - Utility: Mouse Button 4
 * - Heavy / Special 1 / Special 2 / Special 3 / Ultimate: Not Bound
 */
public class JCraftDefaultKeybinds {
    private static final String MARKER_FILE = "jcraftwheel_default_keybinds_v1.marker";
    private boolean initialized = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (initialized || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || mc.options.keyMappings == null) return;

        initialized = true;
        applyCustomDefaults(mc);
    }

    private void applyCustomDefaults(Minecraft mc) {
        Map<String, InputConstants.Key> targetDefaults = new HashMap<>();
        targetDefaults.put("key.jcraft.barrage", InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_5));
        targetDefaults.put("key.jcraft.cooldowncancel", InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_ALT));
        targetDefaults.put("key.jcraft.dash", InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL));
        targetDefaults.put("key.jcraft.standsummon", InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_TAB));
        targetDefaults.put("key.jcraft.utility", InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_4));
        targetDefaults.put("key.jcraft.heavy", InputConstants.UNKNOWN);
        targetDefaults.put("key.jcraft.special1", InputConstants.UNKNOWN);
        targetDefaults.put("key.jcraft.special2", InputConstants.UNKNOWN);
        targetDefaults.put("key.jcraft.special3", InputConstants.UNKNOWN);
        targetDefaults.put("key.jcraft.ultimate", InputConstants.UNKNOWN);

        Path markerPath = FMLPaths.CONFIGDIR.get().resolve(MARKER_FILE);
        boolean firstRun = !Files.exists(markerPath);
        boolean changedAny = false;

        for (KeyMapping km : mc.options.keyMappings) {
            String keyName = km.getName().toLowerCase(Locale.ROOT);
            if (!targetDefaults.containsKey(keyName)) continue;

            InputConstants.Key desired = targetDefaults.get(keyName);
            // Always update defaultKey so clicking "Reset" uses these defaults
            updateDefaultKeyField(km, desired);

            // Apply to active binding once on first run so players can still customize afterwards
            if (firstRun) {
                km.setKey(desired);
                changedAny = true;
            }
        }

        if (changedAny) {
            KeyMapping.resetMapping();
            mc.options.save();
        }

        if (firstRun) {
            try {
                Files.createDirectories(markerPath.getParent());
                Files.writeString(markerPath, "applied=true\n");
            } catch (Exception ignored) {
            }
        }
    }

    private void updateDefaultKeyField(KeyMapping km, InputConstants.Key newDefault) {
        try {
            // Find the final InputConstants.Key field holding defaultKey (works across official & SRG names)
            for (Field f : KeyMapping.class.getDeclaredFields()) {
                if (f.getType().equals(InputConstants.Key.class)) {
                    f.setAccessible(true);
                    Object current = f.get(km);
                    if (current != null && current.equals(km.getDefaultKey())) {
                        f.set(km, newDefault);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
