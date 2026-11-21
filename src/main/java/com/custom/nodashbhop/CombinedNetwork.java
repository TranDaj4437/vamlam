package com.custom.nodashbhop;

import com.custom.nodashbhop.wheel.TimeFlightClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class CombinedNetwork {
    private static final String PROTOCOL = "7";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NoDashBhopMod.MOD_ID, "main"),
            () -> PROTOCOL,
            s -> true,
            s -> true
    );

    private static final String[] MOVE_CLASS_NAMES = {
            "LIGHT", "HEAVY", "BARRAGE", "SPECIAL1", "SPECIAL2", "SPECIAL3", "ULTIMATE", "UTILITY"
    };

    private CombinedNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(
                0,
                SyncStatePacket.class,
                SyncStatePacket::encode,
                SyncStatePacket::decode,
                SyncStatePacket::handle
        );
        CHANNEL.registerMessage(
                1,
                WheelCastPacket.class,
                WheelCastPacket::encode,
                WheelCastPacket::decode,
                WheelCastPacket::handle
        );
        CHANNEL.registerMessage(
                2,
                WheelHoldReleasePacket.class,
                WheelHoldReleasePacket::encode,
                WheelHoldReleasePacket::decode,
                WheelHoldReleasePacket::handle
        );
        CHANNEL.registerMessage(
                3,
                TimeFlightSyncPacket.class,
                TimeFlightSyncPacket::encode,
                TimeFlightSyncPacket::decode,
                TimeFlightSyncPacket::handle
        );
    }

    public static void syncAllFromConfig() {
        CHANNEL.send(PacketDistributor.ALL.noArg(), SyncStatePacket.fromCurrentConfig());
    }

    public static void syncPlayerFromConfig(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncStatePacket.fromCurrentConfig());
    }

    public static void sendWheelCast(int slotIndex, boolean specMode, boolean shiftHeld, boolean holdRequested) {
        CHANNEL.sendToServer(new WheelCastPacket(slotIndex, specMode, shiftHeld, holdRequested));
    }

    public static void sendWheelHoldRelease(int slotIndex, boolean specMode) {
        CHANNEL.sendToServer(new WheelHoldReleasePacket(slotIndex, specMode));
    }

    public static void sendTimeFlightState(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TimeFlightSyncPacket(active));
    }

    // ==================== Packet 0: SyncStatePacket ====================
    public static final class SyncStatePacket {
        private final boolean noDashEnabled;
        private final boolean bhopEnabled;
        private final boolean autoBhop;
        private final boolean scrollJump;
        private final int forceCamMode;
        private final double camOffsetZ;
        private final double airAccel;
        private final double wishCap;
        private final double maxSpeed;
        private final double landPreserve;
        private final double jumpBoost;

        public SyncStatePacket(
                boolean noDashEnabled,
                boolean bhopEnabled,
                boolean autoBhop,
                boolean scrollJump,
                int forceCamMode,
                double camOffsetZ,
                double airAccel,
                double wishCap,
                double maxSpeed,
                double landPreserve,
                double jumpBoost
        ) {
            this.noDashEnabled = noDashEnabled;
            this.bhopEnabled = bhopEnabled;
            this.autoBhop = autoBhop;
            this.scrollJump = scrollJump;
            this.forceCamMode = forceCamMode;
            this.camOffsetZ = camOffsetZ;
            this.airAccel = airAccel;
            this.wishCap = wishCap;
            this.maxSpeed = maxSpeed;
            this.landPreserve = landPreserve;
            this.jumpBoost = jumpBoost;
        }

        public static SyncStatePacket fromCurrentConfig() {
            return new SyncStatePacket(
                    CS2BhopConfig.noDashEnabled,
                    CS2BhopConfig.bhopEnabled,
                    CS2BhopConfig.autoBhopHoldSpace,
                    CS2BhopConfig.scrollWheelJump,
                    CS2BhopConfig.forceCamMode,
                    CS2BhopConfig.camOffsetZ,
                    CS2BhopConfig.airAccelerate,
                    CS2BhopConfig.wishSpeedCap,
                    CS2BhopConfig.maxBhopSpeed,
                    CS2BhopConfig.landingPreserve,
                    CS2BhopConfig.jumpBoost
            );
        }

        public static void encode(SyncStatePacket pkt, FriendlyByteBuf buf) {
            buf.writeBoolean(pkt.noDashEnabled);
            buf.writeBoolean(pkt.bhopEnabled);
            buf.writeBoolean(pkt.autoBhop);
            buf.writeBoolean(pkt.scrollJump);
            buf.writeVarInt(pkt.forceCamMode);
            buf.writeDouble(pkt.camOffsetZ);
            buf.writeDouble(pkt.airAccel);
            buf.writeDouble(pkt.wishCap);
            buf.writeDouble(pkt.maxSpeed);
            buf.writeDouble(pkt.landPreserve);
            buf.writeDouble(pkt.jumpBoost);
        }

        public static SyncStatePacket decode(FriendlyByteBuf buf) {
            return new SyncStatePacket(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            );
        }

        public static void handle(SyncStatePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                CS2BhopConfig.noDashEnabled = pkt.noDashEnabled;
                CS2BhopConfig.bhopEnabled = pkt.bhopEnabled;
                CS2BhopConfig.autoBhopHoldSpace = pkt.autoBhop;
                CS2BhopConfig.scrollWheelJump = pkt.scrollJump;
                CS2BhopConfig.forceCamMode = pkt.forceCamMode;
                CS2BhopConfig.camOffsetZ = pkt.camOffsetZ;
                CS2BhopConfig.airAccelerate = pkt.airAccel;
                CS2BhopConfig.wishSpeedCap = pkt.wishCap;
                CS2BhopConfig.maxBhopSpeed = pkt.maxSpeed;
                CS2BhopConfig.landingPreserve = pkt.landPreserve;
                CS2BhopConfig.jumpBoost = pkt.jumpBoost;
            });
            ctx.setPacketHandled(true);
        }
    }

    // ==================== Packet 1: WheelCastPacket ====================
    public static final class WheelCastPacket {
        private final int slotIndex;
        private final boolean specMode;
        private final boolean shiftHeld;
        private final boolean holdRequested;

        public WheelCastPacket(int slotIndex, boolean specMode, boolean shiftHeld, boolean holdRequested) {
            this.slotIndex = slotIndex;
            this.specMode = specMode;
            this.shiftHeld = shiftHeld;
            this.holdRequested = holdRequested;
        }

        public static void encode(WheelCastPacket pkt, FriendlyByteBuf buf) {
            buf.writeVarInt(pkt.slotIndex);
            buf.writeBoolean(pkt.specMode);
            buf.writeBoolean(pkt.shiftHeld);
            buf.writeBoolean(pkt.holdRequested);
        }

        public static WheelCastPacket decode(FriendlyByteBuf buf) {
            return new WheelCastPacket(
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }

        public static void handle(WheelCastPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null || pkt.slotIndex < 0 || pkt.slotIndex >= MOVE_CLASS_NAMES.length) return;
                executeServerMove(player, pkt.slotIndex, pkt.specMode, pkt.shiftHeld, pkt.holdRequested);
            });
            ctx.setPacketHandled(true);
        }
    }

    // ==================== Packet 2: WheelHoldReleasePacket ====================
    public static final class WheelHoldReleasePacket {
        private final int slotIndex;
        private final boolean specMode;

        public WheelHoldReleasePacket(int slotIndex, boolean specMode) {
            this.slotIndex = slotIndex;
            this.specMode = specMode;
        }

        public static void encode(WheelHoldReleasePacket pkt, FriendlyByteBuf buf) {
            buf.writeVarInt(pkt.slotIndex);
            buf.writeBoolean(pkt.specMode);
        }

        public static WheelHoldReleasePacket decode(FriendlyByteBuf buf) {
            return new WheelHoldReleasePacket(buf.readVarInt(), buf.readBoolean());
        }

        public static void handle(WheelHoldReleasePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null || pkt.slotIndex < 0 || pkt.slotIndex >= MOVE_CLASS_NAMES.length) return;
                releaseServerHold(player, pkt.slotIndex, pkt.specMode);
            });
            ctx.setPacketHandled(true);
        }
    }

    // ==================== Packet 3: TimeFlightSyncPacket ====================
    public static final class TimeFlightSyncPacket {
        private final boolean active;

        public TimeFlightSyncPacket(boolean active) {
            this.active = active;
        }

        public static void encode(TimeFlightSyncPacket pkt, FriendlyByteBuf buf) {
            buf.writeBoolean(pkt.active);
        }

        public static TimeFlightSyncPacket decode(FriendlyByteBuf buf) {
            return new TimeFlightSyncPacket(buf.readBoolean());
        }

        public static void handle(TimeFlightSyncPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                TimeFlightClientHandler.canTimeFlight = pkt.active;
                if (!pkt.active) {
                    TimeFlightClientHandler.activelyFlying = false;
                }
            }));
            ctx.setPacketHandled(true);
        }
    }

    private static void executeServerMove(ServerPlayer player, int slotIndex, boolean specMode,
                                          boolean shiftHeld, boolean holdRequested) {
        try {
            String enumName = MOVE_CLASS_NAMES[slotIndex];
            Class<?> moveClassCls = Class.forName("net.arna.jcraft.api.attack.enums.MoveClass");
            Class<?> moveInputCls = Class.forName("net.arna.jcraft.api.attack.enums.MoveInputType");

            Object moveClassVal = Enum.valueOf((Class<? extends Enum>) moveClassCls, enumName);
            Object moveInputVal = Enum.valueOf((Class<? extends Enum>) moveInputCls, enumName);

            boolean prevShift = player.isShiftKeyDown();
            if (shiftHeld) {
                player.setShiftKeyDown(true);
            }

            try {
                if (specMode) {
                    Object spec = JCraftStandCheck.getSpecInstance(player);
                    if (spec != null) {
                        boolean initiated = invokeInitMove(spec, moveClassCls, moveClassVal);
                        if (holdRequested) {
                            invokeHoldState(spec, moveInputCls, moveInputVal, true, initiated);
                        }
                    }
                } else {
                    Object stand = JCraftStandCheck.getStandEntity(player);
                    if (stand == null && JCraftStandCheck.hasStand(player)) {
                        stand = summonStandImmediately(player);
                    }
                    if (stand != null) {
                        boolean initiated = invokeInitMove(stand, moveClassCls, moveClassVal);
                        if (holdRequested) {
                            invokeHoldState(stand, moveInputCls, moveInputVal, true, initiated);
                        }
                    }
                }
            } finally {
                if (shiftHeld && !prevShift) {
                    player.setShiftKeyDown(false);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void releaseServerHold(ServerPlayer player, int slotIndex, boolean specMode) {
        try {
            String enumName = MOVE_CLASS_NAMES[slotIndex];
            Class<?> moveInputCls = Class.forName("net.arna.jcraft.api.attack.enums.MoveInputType");
            Object moveInputVal = Enum.valueOf((Class<? extends Enum>) moveInputCls, enumName);

            Object attacker = specMode
                    ? JCraftStandCheck.getSpecInstance(player)
                    : JCraftStandCheck.getStandEntity(player);
            if (attacker != null) {
                invokeHoldState(attacker, moveInputCls, moveInputVal, false, false);
            }
        } catch (Exception ignored) {
        }
    }

    private static Object summonStandImmediately(ServerPlayer player) {
        try {
            Class<?> jcraftCls = Class.forName("net.arna.jcraft.JCraft");
            Method summon = jcraftCls.getMethod("summon", LivingEntity.class);
            Object stand = summon.invoke(null, player);
            if (stand != null) return stand;
        } catch (Exception ignored) {
        }
        return JCraftStandCheck.getStandEntity(player);
    }

    private static boolean invokeInitMove(Object attacker, Class<?> moveClassCls, Object moveClassVal) {
        try {
            Method initMove = attacker.getClass().getMethod("initMove", moveClassCls);
            Object res = initMove.invoke(attacker, moveClassVal);
            return Boolean.TRUE.equals(res);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void invokeHoldState(Object attacker, Class<?> moveInputCls, Object moveInputVal,
                                        boolean pressed, boolean moveInitiated) {
        try {
            if (pressed) {
                Method canHold = attacker.getClass().getMethod("canHoldMove", moveInputCls);
                if (!Boolean.TRUE.equals(canHold.invoke(attacker, moveInputVal))) {
                    return;
                }
                Method setHolding = attacker.getClass().getMethod("setHolding", boolean.class);
                Method setHoldingType = attacker.getClass().getMethod("setHoldingType", moveInputCls);
                setHolding.invoke(attacker, true);
                setHoldingType.invoke(attacker, moveInputVal);
            } else {
                Method setHolding = attacker.getClass().getMethod("setHolding", boolean.class);
                setHolding.invoke(attacker, false);
            }
            try {
                Method onInput3 = attacker.getClass().getMethod("onUserMoveInput", moveInputCls, boolean.class, boolean.class);
                onInput3.invoke(attacker, moveInputVal, pressed, moveInitiated);
            } catch (NoSuchMethodException e) {
                Object curMove = JCraftStandCheck.invokeNoArg(attacker, "getCurrentMove", "getCurMove");
                Class<?> absMoveCls = Class.forName("net.arna.jcraft.api.attack.moves.AbstractMove");
                Method onInput4 = attacker.getClass().getMethod("onUserMoveInput", absMoveCls, moveInputCls, boolean.class, boolean.class);
                onInput4.invoke(attacker, curMove, moveInputVal, pressed, moveInitiated);
            }
        } catch (Exception ignored) {
        }
    }
}
