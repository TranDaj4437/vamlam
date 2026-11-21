package com.custom.nodashbhop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side detector for JCraft 0.18.2 Time Stop (active mover) and King Crimson Ultimate (Time Erase).
 * Syncs `canTimeFlight` state to the client and prevents fall damage when landing after time-flight.
 */
public class TimeFlightServerHandler {
    private final Map<UUID, Boolean> activeMap = new HashMap<>();
    private final Map<UUID, Integer> safeFallTicks = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        boolean active = isTimeStopMover(player) || isKingCrimsonTimeErase(player);
        boolean prev = activeMap.getOrDefault(uuid, false);

        if (active != prev) {
            activeMap.put(uuid, active);
            CombinedNetwork.sendTimeFlightState(player, active);
            if (!active) {
                // Grace window so landing right after Time Stop / Time Erase never deals fall damage
                safeFallTicks.put(uuid, 60);
            }
        }

        if (active) {
            player.fallDistance = 0.0F;
        } else {
            int grace = safeFallTicks.getOrDefault(uuid, 0);
            if (grace > 0) {
                player.fallDistance = 0.0F;
                if (player.onGround()) {
                    safeFallTicks.remove(uuid);
                } else {
                    safeFallTicks.put(uuid, grace - 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        activeMap.remove(uuid);
        safeFallTicks.remove(uuid);
    }

    private boolean isTimeStopMover(ServerPlayer player) {
        try {
            Class<?> timestopsCls = Class.forName("net.arna.jcraft.common.tickable.Timestops");
            // 1. Direct check: player is the active Time Stopper
            Method getTs = timestopsCls.getMethod("getTimestop", Entity.class);
            if (getTs.invoke(null, player) != null) {
                return true;
            }
            // 2. Inside someone else's Time Stop range AND player itself is NOT frozen (e.g. TS-resistant Stand)
            Method getTicksInRange = timestopsCls.getMethod("getTicksIfInTSRange", BlockPos.class);
            Object ticksObj = getTicksInRange.invoke(null, player.blockPosition());
            if (ticksObj instanceof Number n && n.intValue() > 0) {
                int frozenTicks = getEntityFrozenTicks(player);
                if (frozenTicks <= 0) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private int getEntityFrozenTicks(ServerPlayer player) {
        try {
            Class<?> utils = Class.forName("net.arna.jcraft.platform.JComponentPlatformUtils");
            for (Method m : utils.getMethods()) {
                if ((m.getName().equals("getTimeStopData") || m.getName().equals("getTimeStopComponent"))
                        && m.getParameterCount() == 1) {
                    Object comp = m.invoke(null, player);
                    if (comp != null) {
                        Object ticks = JCraftStandCheck.invokeNoArg(comp, "getTicks");
                        if (ticks instanceof Number n) return n.intValue();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private boolean isKingCrimsonTimeErase(ServerPlayer player) {
        Object stand = JCraftStandCheck.getStandEntity(player);
        if (stand == null) return false;
        if (!stand.getClass().getSimpleName().contains("KingCrimson")) return false;
        Object teTime = JCraftStandCheck.invokeNoArg(stand, "getTETime");
        return (teTime instanceof Number n) && n.intValue() > 0;
    }
}
