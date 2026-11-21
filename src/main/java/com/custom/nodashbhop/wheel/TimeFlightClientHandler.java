package com.custom.nodashbhop.wheel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Enables fast, responsive 3D free flight whenever the player holds Space
 * during JCraft Time Stop or King Crimson's Ultimate (Time Erase).
 * Holding WASD + Space flies forward and climbs upward simultaneously.
 */
public class TimeFlightClientHandler {
    public static volatile boolean canTimeFlight = false;
    public static volatile boolean activelyFlying = false;

    private static final double FLY_SPEED = 0.56;
    private static final double MOVE_CLIMB_BOOST = 0.20;
    private static final double HOVER_RISE_SPEED = 0.30;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || !canTimeFlight || player.getAbilities().flying || player.isSpectator()) {
            activelyFlying = false;
            return;
        }

        if (mc.screen != null || !mc.options.keyJump.isDown()) {
            activelyFlying = false;
            return;
        }

        activelyFlying = true;

        float forward = 0.0F;
        float strafe = 0.0F;
        if (mc.options.keyUp.isDown()) forward += 1.0F;
        if (mc.options.keyDown.isDown()) forward -= 1.0F;
        if (mc.options.keyLeft.isDown()) strafe += 1.0F;
        if (mc.options.keyRight.isDown()) strafe -= 1.0F;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() > 1.0E-4) {
            right = right.normalize();
        } else {
            right = Vec3.ZERO;
        }

        Vec3 targetVel;
        if (forward != 0.0F || strafe != 0.0F) {
            // Fly fast along camera look + WASD strafe, and climb upward while holding Space
            Vec3 moveDir = look.scale(forward).add(right.scale(-strafe));
            if (moveDir.lengthSqr() > 1.0E-4) {
                moveDir = moveDir.normalize();
            }
            double vy = moveDir.y * FLY_SPEED;
            if (mc.options.keyShift.isDown()) {
                vy -= 0.28;
            } else {
                // Ensure holding WASD + Space always lifts/climbs forward & upward smoothly
                vy = Math.max(vy + MOVE_CLIMB_BOOST, MOVE_CLIMB_BOOST);
                if (vy > FLY_SPEED) vy = FLY_SPEED;
            }
            targetVel = new Vec3(moveDir.x * FLY_SPEED, vy, moveDir.z * FLY_SPEED);
        } else {
            // Holding Space without directional keys -> fast upward levitation
            double vy = mc.options.keyShift.isDown() ? 0.0 : HOVER_RISE_SPEED;
            Vec3 cur = player.getDeltaMovement();
            targetVel = new Vec3(cur.x * 0.72, vy, cur.z * 0.72);
        }

        Vec3 cur = player.getDeltaMovement();
        Vec3 blended = new Vec3(
                cur.x + (targetVel.x - cur.x) * 0.50,
                cur.y + (targetVel.y - cur.y) * 0.52,
                cur.z + (targetVel.z - cur.z) * 0.50
        );

        player.setDeltaMovement(blended);
        player.fallDistance = 0.0F;
    }
}
