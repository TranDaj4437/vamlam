package com.custom.nodashbhop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Toàn bộ lệnh quản trị /jnodash, /cs2bhop và /forcecam (trừ /bhophud cá nhân) đều CHỈ DÀNH CHO OP
 * và tự động đồng bộ áp dụng cho toàn bộ server.
 */
public class NoDashServerHandler {
    private final Map<UUID, Boolean> wasLocked = new HashMap<>();

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 1. Lệnh /jnodash — CHỈ OP
        dispatcher.register(
            Commands.literal("jnodash")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on")
                    .executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        GameRules.BooleanValue rule = server.getGameRules().getRule(NoDashBhopMod.RULE_NO_DASH);
                        rule.set(true, server);
                        CS2BhopConfig.noDashEnabled = true;
                        CombinedNetwork.syncAllFromConfig();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§a[Server] Đã BẬT khóa Dash / CB / RA khi không có Stand & Spec cho toàn server."),
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        GameRules.BooleanValue rule = server.getGameRules().getRule(NoDashBhopMod.RULE_NO_DASH);
                        rule.set(false, server);
                        CS2BhopConfig.noDashEnabled = false;
                        CombinedNetwork.syncAllFromConfig();
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            JCraftStandCheck.lockUniversalCooldowns(p, 0);
                            wasLocked.put(p.getUUID(), false);
                        }
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§e[Server] Đã TẮT chế độ khóa Dash cho toàn server."),
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("zoom")
                    .then(Commands.argument("distance", DoubleArgumentType.doubleArg(1.5, 5.0))
                        .executes(ctx -> {
                            double dist = DoubleArgumentType.getDouble(ctx, "distance");
                            CS2BhopConfig.camOffsetZ = dist;
                            CombinedNetwork.syncAllFromConfig();
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(String.format("§a[Server] Khoảng cách camera thứ 3: §e%.2f §7(nhỏ hơn = sát nhân vật hơn, gốc = 4.0).", dist)),
                                true
                            );
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        boolean enabled = server.getGameRules().getBoolean(NoDashBhopMod.RULE_NO_DASH);
                        String extra = "";
                        if (ctx.getSource().getEntity() instanceof ServerPlayer sp) {
                            String standId = JCraftStandCheck.getStandId(sp);
                            String specId = JCraftStandCheck.getSpecId(sp);
                            boolean blocking = enabled && !JCraftStandCheck.hasStandOrSpec(sp);
                            extra = " | Stand=" + standId + " | Spec=" + specId + " | Đang khóa=" + (blocking ? "CÓ" : "KHÔNG");
                        }
                        String msg = "§b[Server] JCraft No Dash: " + (enabled ? "§aĐANG BẬT" : "§cĐANG TẮT") + "§f" + extra;
                        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                        return 1;
                    })
                )
        );

        // 2. Lệnh quản trị BHop toàn server — CHỈ OP
        dispatcher.register(
            Commands.literal("cs2bhop")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on").executes(ctx -> {
                    CS2BhopConfig.bhopEnabled = true;
                    CombinedNetwork.syncAllFromConfig();
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Server] Đã BẬT CS2 BHop cho toàn server."), true);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    CS2BhopConfig.bhopEnabled = false;
                    CombinedNetwork.syncAllFromConfig();
                    ctx.getSource().sendSuccess(() -> Component.literal("§e[Server] Đã TẮT CS2 BHop cho toàn server."), true);
                    return 1;
                }))
                .then(Commands.literal("autohop")
                    .then(Commands.literal("on").executes(ctx -> {
                        CS2BhopConfig.autoBhopHoldSpace = true;
                        CombinedNetwork.syncAllFromConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Server] Giữ Space tự BHop: BẬT."), true);
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx -> {
                        CS2BhopConfig.autoBhopHoldSpace = false;
                        CombinedNetwork.syncAllFromConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§e[Server] Giữ Space tự BHop: TẮT."), true);
                        return 1;
                    }))
                )
                .then(Commands.literal("scrolljump")
                    .then(Commands.literal("on").executes(ctx -> {
                        CS2BhopConfig.scrollWheelJump = true;
                        CombinedNetwork.syncAllFromConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Server] Nhảy con lăn chuột: BẬT."), true);
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx -> {
                        CS2BhopConfig.scrollWheelJump = false;
                        CombinedNetwork.syncAllFromConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§e[Server] Nhảy con lăn chuột: TẮT."), true);
                        return 1;
                    }))
                )
                .then(Commands.literal("preset")
                    .then(Commands.literal("mm").executes(ctx -> {
                        CS2BhopConfig.applyMatchmakingPreset();
                        CombinedNetwork.syncAllFromConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§b[Server] CS2 BHop Preset: Matchmaking (~428 u/s max)."), true);
                        return 1;
                    }))
                    .then(Commands.literal("community").executes(ctx -> {
                        CS2BhopConfig.applyCommunityPreset();
                        CombinedNetwork.syncAllFromConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("§d[Server] CS2 BHop Preset: Community (~660 u/s max)."), true);
                        return 1;
                    }))
                )
                .then(Commands.literal("maxspeed")
                    .then(Commands.argument("units", DoubleArgumentType.doubleArg(280.0, 950.0))
                        .executes(ctx -> {
                            double units = DoubleArgumentType.getDouble(ctx, "units");
                            CS2BhopConfig.maxBhopSpeed = units / CS2BhopEngine.MC_TO_CS2_UNITS;
                            CombinedNetwork.syncAllFromConfig();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Server] Giới hạn tốc độ BHop: " + (int) units + " u/s."), true);
                            return 1;
                        })
                    )
                )
        );

        // 3. Lệnh /forcecam replace | lock | on | off | status — CHỈ OP, đồng bộ toàn server
        dispatcher.register(
            Commands.literal("forcecam")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("replace")
                    .executes(ctx -> setCamMode(ctx.getSource(), CS2BhopConfig.CAM_REPLACE,
                            "§a[Server] Camera: THAY THẾ góc nhìn 3 sau lưng gốc bằng góc nhìn qua vai có crosshair (vẫn bấm F5 đổi 1st/3rd)."))
                )
                .then(Commands.literal("lock")
                    .executes(ctx -> setCamMode(ctx.getSource(), CS2BhopConfig.CAM_LOCK,
                            "§a[Server] Camera: KHÓA CỨNG góc nhìn thứ 3 qua vai có crosshair + khóa F5 (giữ ADS TaCZ)."))
                )
                .then(Commands.literal("on")
                    .executes(ctx -> setCamMode(ctx.getSource(), CS2BhopConfig.CAM_LOCK,
                            "§a[Server] Camera: KHÓA CỨNG góc nhìn thứ 3 qua vai có crosshair + khóa F5 (giữ ADS TaCZ)."))
                )
                .then(Commands.literal("off")
                    .executes(ctx -> setCamMode(ctx.getSource(), CS2BhopConfig.CAM_OFF,
                            "§e[Server] Camera: Đã TẮT can thiệp góc nhìn cho toàn server."))
                )
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        int mode = server.getGameRules().getInt(NoDashBhopMod.RULE_FORCE_CAM);
                        String label = switch (mode) {
                            case CS2BhopConfig.CAM_REPLACE -> "§aREPLACE (thay góc nhìn 3 sau lưng gốc = qua vai có crosshair, F5 bình thường)";
                            case CS2BhopConfig.CAM_LOCK -> "§dLOCK (ép luôn ở góc nhìn qua vai + khóa F5)";
                            default -> "§cOFF (tắt)";
                        };
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§b[Server] Camera (/forcecam): " + label),
                            false
                        );
                        return 1;
                    })
                )
        );
    }

    private int setCamMode(CommandSourceStack source, int mode, String message) {
        MinecraftServer server = source.getServer();
        GameRules.IntegerValue rule = server.getGameRules().getRule(NoDashBhopMod.RULE_FORCE_CAM);
        rule.set(mode, server);
        CS2BhopConfig.forceCamMode = mode;
        CombinedNetwork.syncAllFromConfig();
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                CS2BhopConfig.noDashEnabled = server.getGameRules().getBoolean(NoDashBhopMod.RULE_NO_DASH);
                CS2BhopConfig.forceCamMode = server.getGameRules().getInt(NoDashBhopMod.RULE_FORCE_CAM);
                CombinedNetwork.syncPlayerFromConfig(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        boolean enabled = server.getGameRules().getBoolean(NoDashBhopMod.RULE_NO_DASH);
        CS2BhopConfig.noDashEnabled = enabled;
        CS2BhopConfig.forceCamMode = server.getGameRules().getInt(NoDashBhopMod.RULE_FORCE_CAM);

        boolean shouldLock = enabled && !JCraftStandCheck.hasStandOrSpec(player);
        UUID id = player.getUUID();
        boolean prevLocked = Boolean.TRUE.equals(wasLocked.get(id));

        if (shouldLock) {
            JCraftStandCheck.lockUniversalCooldowns(player, 4);
            wasLocked.put(id, true);
        } else if (prevLocked) {
            JCraftStandCheck.lockUniversalCooldowns(player, 0);
            wasLocked.put(id, false);
        }
    }
}
