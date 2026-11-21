package com.custom.nodashbhop;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Động cơ BHop CS2 đã tinh chỉnh:
 * - Đi bộ / chạy bộ bình thường trên mặt đất: 100% tốc độ gốc Minecraft, không bị nhanh quá.
 * - Khi BHop (nhảy liên tiếp + Air-Strafe): khử bớt lực cản không khí của Minecraft, ăn tốc mượt và dễ hơn hẳn.
 * - Tự động tạm dừng khi đang dùng chế độ Pilot/Remote của Whitesnake (và các Stand điều khiển từ xa khác) hoặc khi đang bay Time Stop / King Crimson.
 */
public class CS2BhopEngine {
    private boolean wasOnGround = true;
    private int groundTicks = 0;
    private int hopChain = 0;
    private int scrollJumpBufferTicks = 0;
    private int jumpCooldownTicks = 0;

    private double preLandSpeed = 0.0;
    private double lastHorizontalSpeed = 0.0;
    private float lastYaw = 0.0F;
    private int airTicks = 0;
    private int syncedAirTicks = 0;
    private int hudSpeedUnits = 0;
    private int hudDeltaUnits = 0;

    public static final double MC_TO_CS2_UNITS = 250.0 / 0.28;

    public int getHudSpeedUnits() { return hudSpeedUnits; }
    public int getHudDeltaUnits() { return hudDeltaUnits; }
    public int getHopChain() { return hopChain; }
    public int getSyncPercent() {
        return airTicks <= 0 ? 100 : (int) Math.round((syncedAirTicks * 100.0) / airTicks);
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!CS2BhopConfig.bhopEnabled || !CS2BhopConfig.scrollWheelJump) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (isStandRemoteOrPilot(mc.player)) return;
        if (event.getScrollDelta() != 0) {
            scrollJumpBufferTicks = 3;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (jumpCooldownTicks > 0) jumpCooldownTicks--;

        if (!CS2BhopConfig.bhopEnabled
                || com.custom.nodashbhop.wheel.TimeFlightClientHandler.activelyFlying
                || isStandRemoteOrPilot(player)) {
            resetChain(player);
            updateTelemetry(player);
            return;
        }

        if (player.isSpectator() || player.getAbilities().flying || player.isFallFlying()
                || player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger()) {
            resetChain(player);
            updateTelemetry(player);
            return;
        }

        if (scrollJumpBufferTicks > 0) scrollJumpBufferTicks--;

        Vec3 vel = player.getDeltaMovement();
        double horizSpeed = Math.hypot(vel.x, vel.z);
        boolean onGround = player.onGround();

        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        boolean wantsJump = (CS2BhopConfig.autoBhopHoldSpace && mc.options.keyJump.isDown())
                || scrollJumpBufferTicks > 0;

        if (onGround) {
            if (!wasOnGround) {
                preLandSpeed = Math.max(lastHorizontalSpeed, horizSpeed);
                groundTicks = 0;
            }
            groundTicks++;

            // Cửa sổ bhop dễ hơn (3 tick sau khi chạm đất) kèm jumpCooldownTicks tránh nhảy lặp khi bị giữ chân
            if (wantsJump && mc.screen == null && groundTicks <= 3 && jumpCooldownTicks == 0) {
                scrollJumpBufferTicks = 0;
                jumpCooldownTicks = 4;
                double jumpY = 0.42;
                if (player.hasEffect(MobEffects.JUMP)) {
                    var eff = player.getEffect(MobEffects.JUMP);
                    if (eff != null) {
                        jumpY += (eff.getAmplifier() + 1) * 0.1;
                    }
                }

                double baseCarry = Math.max(preLandSpeed * CS2BhopConfig.landingPreserve, horizSpeed);
                double targetSpeed = Math.min(baseCarry + CS2BhopConfig.jumpBoost, CS2BhopConfig.maxBhopSpeed);

                double vx = vel.x;
                double vz = vel.z;
                double dirLen = Math.hypot(vx, vz);
                if (dirLen > 1.0E-5 && targetSpeed > dirLen) {
                    double scale = targetSpeed / dirLen;
                    vx *= scale;
                    vz *= scale;
                }
                player.setDeltaMovement(vx, jumpY, vz);
                player.hasImpulse = true;
                player.resetFallDistance();
                hopChain++;
                airTicks = 0;
                syncedAirTicks = 0;
            } else if (groundTicks > 3) {
                hopChain = 0;
                preLandSpeed = 0.0;
            }
        } else {
            groundTicks = 0;
            applySourceAirStrafe(player, forward, strafe);
        }

        wasOnGround = onGround;
        updateTelemetry(player);
    }

    /**
     * Checks if the player's Stand (e.g., Whitesnake, Aerosmith, Hierophant Green, Purple Haze)
     * is currently in Remote / Pilot / Free mode so BHop never forces the player to jump!
     */
    private boolean isStandRemoteOrPilot(LocalPlayer player) {
        Object stand = JCraftStandCheck.getStandEntity(player);
        if (stand == null) return false;
        Object remote = JCraftStandCheck.invokeNoArg(stand, "isRemote", "isRemoteAndControllable", "isFree");
        if (Boolean.TRUE.equals(remote)) return true;
        // Also check StandEntity.getModeOrdinal() > 0 (remote/pilot modes in JCraft)
        Object mode = JCraftStandCheck.invokeNoArg(stand, "getModeOrdinal");
        if (mode instanceof Number n && n.intValue() > 0) {
            return true;
        }
        return false;
    }

    private void applySourceAirStrafe(LocalPlayer player, float forward, float strafe) {
        float yawDelta = player.getYRot() - lastYaw;
        while (yawDelta > 180.0F) yawDelta -= 360.0F;
        while (yawDelta < -180.0F) yawDelta += 360.0F;

        boolean hasStrafe = Math.abs(strafe) > 0.05F;
        if (hasStrafe) {
            airTicks++;
            boolean turningLeft = yawDelta < -0.12F;
            boolean turningRight = yawDelta > 0.12F;
            if ((strafe > 0 && turningLeft) || (strafe < 0 && turningRight)) {
                syncedAirTicks++;
            }
        }

        Vec3 vel = player.getDeltaMovement();
        double vx = vel.x;
        double vz = vel.z;
        double curHoriz = Math.hypot(vx, vz);

        if (hopChain > 0 && lastHorizontalSpeed > 0.24 && curHoriz > 0.05 && curHoriz < lastHorizontalSpeed) {
            double restored = Math.min(lastHorizontalSpeed * 0.988, CS2BhopConfig.maxBhopSpeed);
            double boost = restored / curHoriz;
            vx *= boost;
            vz *= boost;
        }

        if (Math.abs(forward) < 0.01F && !hasStrafe) {
            player.setDeltaMovement(vx, vel.y, vz);
            return;
        }

        double yawRad = Math.toRadians(player.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double wishX = strafe * cos - forward * sin;
        double wishZ = forward * cos + strafe * sin;
        double wishLen = Math.hypot(wishX, wishZ);
        if (wishLen < 1.0E-5) return;
        wishX /= wishLen;
        wishZ /= wishLen;

        double strafeFactor = hasStrafe ? (Math.abs(forward) < 0.05F ? 1.0 : 0.78) : 0.35;
        double wishSpeed = CS2BhopConfig.wishSpeedCap * strafeFactor;

        double currentSpeed = vx * wishX + vz * wishZ;
        double addSpeed = wishSpeed - currentSpeed;
        if (addSpeed > 0.0) {
            double accelSpeed = CS2BhopConfig.airAccelerate * 0.05 * wishSpeed;
            if (accelSpeed > addSpeed) accelSpeed = addSpeed;
            vx += accelSpeed * wishX;
            vz += accelSpeed * wishZ;
        }

        double newHoriz = Math.hypot(vx, vz);
        if (newHoriz > CS2BhopConfig.maxBhopSpeed) {
            double clamp = CS2BhopConfig.maxBhopSpeed / newHoriz;
            vx *= clamp;
            vz *= clamp;
        }
        player.setDeltaMovement(vx, vel.y, vz);
    }

    private void resetChain(LocalPlayer player) {
        hopChain = 0;
        groundTicks = 0;
        preLandSpeed = 0.0;
        wasOnGround = player.onGround();
    }

    private void updateTelemetry(LocalPlayer player) {
        Vec3 vel = player.getDeltaMovement();
        double speed = Math.hypot(vel.x, vel.z);
        int units = (int) Math.round(speed * MC_TO_CS2_UNITS);
        hudDeltaUnits = units - hudSpeedUnits;
        hudSpeedUnits = units;
        lastHorizontalSpeed = speed;
        lastYaw = player.getYRot();
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("bhophud")
                .executes(ctx -> {
                    CS2BhopConfig.showHud = !CS2BhopConfig.showHud;
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        CS2BhopConfig.showHud
                            ? "§a[CS2 BHop] Đã BẬT đồng hồ tốc độ góc dưới phải."
                            : "§e[CS2 BHop] Đã TẮT đồng hồ tốc độ."
                    ), false);
                    return 1;
                })
                .then(Commands.literal("on").executes(ctx -> {
                    CS2BhopConfig.showHud = true;
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[CS2 BHop] Đã BẬT đồng hồ tốc độ góc dưới phải."), false);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    CS2BhopConfig.showHud = false;
                    ctx.getSource().sendSuccess(() -> Component.literal("§e[CS2 BHop] Đã TẮT đồng hồ tốc độ."), false);
                    return 1;
                }))
        );
    }
}
