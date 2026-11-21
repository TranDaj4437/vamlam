package com.custom.nodashbhop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Custom Purple Haze -> Purple Haze: Distortion evolution:
 * 1. Kill 1 Stand User (counts BOTH mobs with a Stand AND players with a Stand) while owning Purple Haze.
 * 2. Gift/use 1 Flower (right-click a player/mob/block or in air while holding any flower)
 *    to consume the flower and evolve into Purple Haze: Distortion (with skin gacha chance!).
 */
public class PurpleHazeEvoHandler {
    private static final String TAG_KILL_READY = "jcraft_ph_stand_user_kill";

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        // Check if victim is a Stand User (either a player or a mob with a Stand)
        if (!entityHasStand(victim)) return;

        ServerPlayer killer = resolveKillerPlayer(event.getSource().getEntity());
        if (killer == null) {
            killer = resolveKillerPlayer(event.getSource().getDirectEntity());
        }
        if (killer == null || killer == victim) return;

        // Only count if killer currently owns base Purple Haze (not already Distortion)
        if (!isBasePurpleHaze(killer)) return;

        if (!killer.getPersistentData().getBoolean(TAG_KILL_READY)) {
            killer.getPersistentData().putBoolean(TAG_KILL_READY, true);
            killer.displayClientMessage(
                    Component.literal("§d[Purple Haze] §fĐã hạ gục Stand User! Tặng/dùng §e1 bông hoa §fđể tiến hóa lên §5Purple Haze: Distortion§f."),
                    true
            );
            killer.level().playSound(
                    null,
                    killer.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.2F
            );
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(TAG_KILL_READY)) {
            event.getEntity().getPersistentData().putBoolean(TAG_KILL_READY, true);
        }
    }

    @SubscribeEvent
    public void onGiftFlowerToEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (tryEvolveWithFlower(player, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onUseFlowerInAir(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (tryEvolveWithFlower(player, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onUseFlowerOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (tryEvolveWithFlower(player, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private boolean tryEvolveWithFlower(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ItemTags.FLOWERS)) return false;
        if (!isBasePurpleHaze(player)) return false;
        if (!player.getPersistentData().getBoolean(TAG_KILL_READY)) return false;

        StandArrowSkinHandler.markEvoWindow(player);
        if (!evolveToPurpleHazeDistortion(player)) {
            return false;
        }

        player.getPersistentData().putBoolean(TAG_KILL_READY, false);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                1.0F,
                1.1F
        );
        player.displayClientMessage(
                Component.literal("§5✦ Purple Haze §fđã tiến hóa thành §dPurple Haze: Distortion§f!"),
                true
        );
        return true;
    }

    private boolean isBasePurpleHaze(ServerPlayer player) {
        String id = JCraftStandCheck.getStandId(player);
        return id.equals("purple_haze");
    }

    private ServerPlayer resolveKillerPlayer(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof ServerPlayer sp) return sp;
        // If a StandEntity dealt the killing blow, resolve its owner via getUser()
        Object user = JCraftStandCheck.invokeNoArg(entity, "getUser", "getOwner");
        if (user instanceof ServerPlayer sp) return sp;
        return null;
    }

    private boolean entityHasStand(LivingEntity entity) {
        try {
            Class<?> platformUtils = Class.forName("net.arna.jcraft.platform.JComponentPlatformUtils");
            Method getComp = platformUtils.getMethod("getStandComponent", LivingEntity.class);
            Object comp = getComp.invoke(null, entity);
            if (comp == null) return false;
            Object type = JCraftStandCheck.invokeNoArg(comp, "getType", "getStandType");
            if (type == null) return false;
            String s = type.toString().toLowerCase(java.util.Locale.ROOT);
            return !s.isEmpty() && !s.contains("none") && !s.equals("null");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean evolveToPurpleHazeDistortion(ServerPlayer player) {
        Object standComp = JCraftStandCheck.getStandComponent(player);
        if (standComp == null) return false;
        try {
            Class<?> regCls = Class.forName("net.arna.jcraft.api.registry.JStandTypeRegistry");
            Field phdField = regCls.getField("PURPLE_HAZE_DISTORTION");
            Object supplier = phdField.get(null);
            if (supplier == null) return false;

            Method getM = supplier.getClass().getMethod("get");
            Object phdType = getM.invoke(supplier);
            if (phdType == null) return false;

            Class<?> standTypeCls = Class.forName("net.arna.jcraft.api.stand.StandType");
            Method setType = standComp.getClass().getMethod("setType", standTypeCls);
            setType.invoke(standComp, phdType);

            try {
                Method sync = standComp.getClass().getMethod("sync", Entity.class);
                sync.invoke(standComp, player);
            } catch (Exception ignored) {
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
