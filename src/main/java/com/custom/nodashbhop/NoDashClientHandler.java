package com.custom.nodashbhop;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoDashClientHandler {
    private final List<KeyMapping> blockedKeys = new ArrayList<>();
    private boolean keysScanned = false;

    private void ensureKeysScanned(Minecraft mc) {
        if (keysScanned && !blockedKeys.isEmpty()) return;
        blockedKeys.clear();
        if (mc.options == null || mc.options.keyMappings == null) return;

        for (KeyMapping mapping : mc.options.keyMappings) {
            String rawName = mapping.getName().toLowerCase(Locale.ROOT);
            String category = mapping.getCategory().toLowerCase(Locale.ROOT);
            String translated = Component.translatable(mapping.getName()).getString().toLowerCase(Locale.ROOT);

            boolean isJCraft = rawName.contains("jcraft") || category.contains("jcraft");
            if (!isJCraft) continue;

            if (isTargetUtilityKey(rawName) || isTargetUtilityKey(translated)) {
                blockedKeys.add(mapping);
            }
        }
        keysScanned = true;
    }

    private boolean isTargetUtilityKey(String textLower) {
        String compact = textLower.replace("_", "").replace(".", "").replace("-", "").replace(" ", "");
        return compact.contains("dash")
                || compact.contains("dodge")
                || compact.contains("combobreaker")
                || compact.contains("cooldowncancel");
    }

    private boolean shouldBlockNow(Minecraft mc) {
        if (!CS2BhopConfig.noDashEnabled || mc.player == null || mc.level == null) {
            return false;
        }
        return !JCraftStandCheck.hasStandOrSpec(mc.player);
    }

    private void suppressBlockedKeys() {
        for (KeyMapping key : blockedKeys) {
            key.setDown(false);
            while (key.consumeClick()) {}
        }
    }

    private boolean isSharedWithNonJCraftMouse(Minecraft mc, int button) {
        for (KeyMapping mapping : mc.options.keyMappings) {
            if (blockedKeys.contains(mapping)) continue;
            if (mapping.matchesMouse(button)) return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldBlockNow(mc)) return;
        ensureKeysScanned(mc);
        suppressBlockedKeys();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || !shouldBlockNow(mc)) return;
        ensureKeysScanned(mc);
        for (KeyMapping key : blockedKeys) {
            if (key.matches(event.getKey(), event.getScanCode())) {
                key.setDown(false);
                while (key.consumeClick()) {}
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || !shouldBlockNow(mc)) return;
        ensureKeysScanned(mc);
        boolean matched = false;
        for (KeyMapping key : blockedKeys) {
            if (key.matchesMouse(event.getButton())) {
                key.setDown(false);
                while (key.consumeClick()) {}
                matched = true;
            }
        }
        if (matched && !isSharedWithNonJCraftMouse(mc, event.getButton())) {
            event.setCanceled(true);
        }
    }
}
