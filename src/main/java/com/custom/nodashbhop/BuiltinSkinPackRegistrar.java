package com.custom.nodashbhop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Registers:
 * 1. CLIENT_RESOURCES: Bundled JCraft Custom Stand Skins (auto-enabled, TOP priority above JCraft).
 * 2. SERVER_DATA: When `jcraft_scr` is installed, registers `jcraft_scr_clean` (TOP priority) to:
 *    - Evolve Silver Chariot -> Silver Chariot Requiem using vanilla JCraft's `jcraft:requiem_arrow`
 *    - Disable Rome structure / boss worldgen, Passione Computer crafting, and extra chest loot
 *      so only the Silver Chariot Requiem Stand & its skills remain active.
 */
public final class BuiltinSkinPackRegistrar {
    private static final String SKIN_PACK_ID = "jcraftnodashbhop_custom_skins";
    private static final String SCR_DATA_PACK_ID = "jcraftnodashbhop_scr_clean";

    private BuiltinSkinPackRegistrar() {}

    public static void onAddPackFinders(AddPackFindersEvent event) {
        var modFileInfo = ModList.get().getModFileById(NoDashBhopMod.MOD_ID);
        if (modFileInfo == null) return;

        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path resourcePath = modFileInfo.getFile().findResource("resourcepacks", "jcraft_custom_skins");
            if (Files.exists(resourcePath)) {
                event.addRepositorySource(packConsumer -> {
                    Pack pack = Pack.readMetaAndCreate(
                            SKIN_PACK_ID,
                            Component.literal("JCraft Custom Stand Skins"),
                            true,
                            id -> new PathPackResources(id, true, resourcePath),
                            PackType.CLIENT_RESOURCES,
                            Pack.Position.TOP,
                            PackSource.BUILT_IN
                    );
                    if (pack != null) {
                        packConsumer.accept(pack);
                    }
                });
            }
        } else if (event.getPackType() == PackType.SERVER_DATA) {
            if (!ModList.get().isLoaded("jcraft_scr")) return;
            Path dataPath = modFileInfo.getFile().findResource("datapacks", "jcraft_scr_clean");
            if (Files.exists(dataPath)) {
                event.addRepositorySource(packConsumer -> {
                    Pack pack = Pack.readMetaAndCreate(
                            SCR_DATA_PACK_ID,
                            Component.literal("JCraft SCR Clean (Stand & JCraft Requiem Arrow Only)"),
                            true,
                            id -> new PathPackResources(id, true, dataPath),
                            PackType.SERVER_DATA,
                            Pack.Position.TOP,
                            PackSource.BUILT_IN
                    );
                    if (pack != null) {
                        packConsumer.accept(pack);
                    }
                });
            }
        }
    }
}
