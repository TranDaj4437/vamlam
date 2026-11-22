package com.custom.nodashbhop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cleans Creative Mode tabs:
 * 1. Removes all extra items from `jcraft_scr` (spawn eggs, duplicate requiem arrow,
 *    passione computer, requiem trophy, music discs) while keeping SCR Stand Discs (`jcraft:stand_disc`).
 * 2. Removes all spawn eggs from `jcraft` as well.
 */
public final class ScrCreativeTabCleaner {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry : event.getEntries()) {
            ItemStack stack = entry.getKey();
            if (stack == null || stack.isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) continue;

            String ns = id.getNamespace();
            if ("jcraft_scr".equals(ns)) {
                toRemove.add(stack);
            } else if ("jcraft".equals(ns)
                    && (stack.getItem() instanceof SpawnEggItem || id.getPath().contains("spawn_egg"))) {
                toRemove.add(stack);
            }
        }
        for (ItemStack stack : toRemove) {
            event.getEntries().remove(stack);
        }
    }
}
