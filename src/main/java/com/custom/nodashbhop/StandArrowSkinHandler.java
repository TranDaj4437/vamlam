package com.custom.nodashbhop;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Grants a configurable chance (default 35%) to roll an alternate Stand skin (1..3)
 * whenever a player obtains a new Stand via a Stand Arrow OR evolves a Stand using
 * ANY evolution item (Requiem Arrow, Living Arrow, DIO's Diary, Diary Page, Green Baby,
 * or Purple Haze Flower evolution).
 * Displays ONLY on the hotbar actionbar ("Skin #N - <Skin Name>") with firework sounds.
 */
public class StandArrowSkinHandler {
    private static volatile int altSkinChancePercent = 35;

    private static final Map<UUID, Long> evoWindowEndMs = new HashMap<>();
    private final Map<UUID, String> lastStandId = new HashMap<>();
    private final Random random = new Random();

    public static void markEvoWindow(ServerPlayer player) {
        if (player != null) {
            evoWindowEndMs.put(player.getUUID(), System.currentTimeMillis() + 15000L);
        }
    }

    private boolean isStandOrEvolutionItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ItemTags.FLOWERS)) {
            String standId = JCraftStandCheck.getStandId(player);
            return standId.equals("purple_haze");
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;
        String full = itemId.toString().toLowerCase(Locale.ROOT);
        if (!full.contains("jcraft")) return false;
        return full.contains("arrow")
                || full.contains("diary")
                || full.contains("green_baby")
                || full.contains("greenbaby")
                || full.contains("requiem");
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isStandOrEvolutionItem(player, event.getItemStack())) {
            markEvoWindow(player);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isStandOrEvolutionItem(player, event.getItemStack())) {
            markEvoWindow(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if ((player.tickCount % 4) != 0) return;

        UUID uuid = player.getUUID();
        String currentStand = JCraftStandCheck.getStandFullId(player);
        String previousStand = lastStandId.put(uuid, currentStand);

        if (previousStand == null) return;
        if (currentStand.equals(previousStand)) return;
        if (!JCraftStandCheck.hasStand(player)) return;

        Long windowEnd = evoWindowEndMs.get(uuid);
        if (windowEnd == null || System.currentTimeMillis() > windowEnd) return;
        evoWindowEndMs.remove(uuid);

        if (altSkinChancePercent <= 0) return;
        if (random.nextInt(100) < altSkinChancePercent) {
            int skinIndex = 1 + random.nextInt(3); // Skins 1, 2, or 3
            if (applyStandSkin(player, skinIndex)) {
                String ns = currentStand.contains(":") ? currentStand.substring(0, currentStand.indexOf(':')) : "jcraft";
                String slug = currentStand.contains(":") ? currentStand.substring(currentStand.indexOf(':') + 1) : currentStand;
                slug = slug.toLowerCase(Locale.ROOT).replace("_entity", "").replace("entity", "");
                String langKey = "entity." + ns + "." + slug + ".info.skin" + skinIndex;

                Component hotbarMsg = Component.literal("§6Skin #" + skinIndex + " §f- §e")
                        .append(Component.translatable(langKey));
                player.displayClientMessage(hotbarMsg, true);

                player.level().playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.05F
                );
                player.level().playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.FIREWORK_ROCKET_TWINKLE,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.15F
                );
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        lastStandId.remove(uuid);
        evoWindowEndMs.remove(uuid);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("jstandskin")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    altSkinChancePercent = IntegerArgumentType.getInteger(ctx, "percent");
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§a[Server] Tỉ lệ ra Skin Stand (Mũi Tên & Tiến Hóa): §e" + altSkinChancePercent + "%"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§b[Server] Tỉ lệ ra Skin Stand hiện tại: §e" + altSkinChancePercent + "% §7(/jstandskin <0-100>)"),
                                    false
                            );
                            return 1;
                        })
        );
    }

    private boolean applyStandSkin(ServerPlayer player, int skin) {
        Object standComp = JCraftStandCheck.getStandComponent(player);
        if (standComp == null) return false;
        try {
            Method setSkin = standComp.getClass().getMethod("setSkin", int.class);
            setSkin.invoke(standComp, skin);

            try {
                Method sync = standComp.getClass().getMethod("sync", Entity.class);
                sync.invoke(standComp, player);
            } catch (Exception ignored) {
            }

            Object standEntity = JCraftStandCheck.getStandEntity(player);
            if (standEntity != null) {
                try {
                    Method entitySetSkin = standEntity.getClass().getMethod("setSkin", int.class);
                    entitySetSkin.invoke(standEntity, skin);
                } catch (Exception ignored) {
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
