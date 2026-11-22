package com.custom.nodashbhop;

import com.custom.nodashbhop.wheel.WheelClientBootstrap;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NoDashBhopMod.MOD_ID)
public class NoDashBhopMod {
    public static final String MOD_ID = "jcraftnodashbhop";

    public static final GameRules.Key<GameRules.BooleanValue> RULE_NO_DASH =
            GameRules.register(
                    "jcraftNoStandNoDash",
                    GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true)
            );

    // 0 = off, 1 = replace vanilla 3rd-person back (default), 2 = lock in 3rd-person
    public static final GameRules.Key<GameRules.IntegerValue> RULE_FORCE_CAM =
            GameRules.register(
                    "jcraftForceThirdPerson",
                    GameRules.Category.PLAYER,
                    GameRules.IntegerValue.create(CS2BhopConfig.CAM_REPLACE)
            );

    public NoDashBhopMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(BuiltinSkinPackRegistrar::onAddPackFinders);
        FMLJavaModLoadingContext.get().getModEventBus().register(new ScrCreativeTabCleaner());
        CombinedNetwork.register();
        MinecraftForge.EVENT_BUS.register(new NoDashServerHandler());
        MinecraftForge.EVENT_BUS.register(new StandArrowSkinHandler());
        MinecraftForge.EVENT_BUS.register(new PurpleHazeEvoHandler());
        MinecraftForge.EVENT_BUS.register(new TimeFlightServerHandler());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(new NoDashClientHandler());
            MinecraftForge.EVENT_BUS.register(new JCraftHudHider());
            CS2BhopEngine bhopEngine = new CS2BhopEngine();
            MinecraftForge.EVENT_BUS.register(bhopEngine);
            MinecraftForge.EVENT_BUS.register(new CS2VelocityHud(bhopEngine));
            WheelClientBootstrap.init();
        });
    }
}
