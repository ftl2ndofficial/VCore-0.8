package vcore;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vcore.core.Core;
import vcore.core.Managers;
import vcore.core.hooks.ManagerShutdownHook;
import vcore.core.hooks.ModuleShutdownHook;
import vcore.core.manager.client.ModuleManager;
import vcore.utility.render.Render2DEngine;

public class Vcore implements ModInitializer {
   public static final ModMetadata MOD_META = ((ModContainer)FabricLoader.getInstance().getModContainer("vcore").orElseThrow()).getMetadata();
   public static final String RANK = "User";
   public static final String USER = "Anonymous ";
   public static final String MOD_ID = "vcore";
   public static final String VERSION = "0.8";
   public static final Logger LOGGER = LoggerFactory.getLogger("Vcore");
   public static final Runtime RUNTIME = Runtime.getRuntime();
   public static final boolean baritone = FabricLoader.getInstance().isModLoaded("baritone") || FabricLoader.getInstance().isModLoaded("baritone-meteor");
   public static final IEventBus EVENT_BUS = new EventBus();
   public static Color copy_color = new Color(-1);
   public static Vcore.KeyListening currentKeyListener;
   public static BlockPos gps_position;
   public static float TICK_TIMER = 1.0F;
   public static MinecraftClient mc;
   public static long initTime;
   public static Core core = new Core();

   public void onInitialize() {
      mc = MinecraftClient.getInstance();
      initTime = System.currentTimeMillis();
      EVENT_BUS.registerLambdaFactory("vcore", (lookupInMethod, klass) -> (Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
      EVENT_BUS.subscribe(core);
      Managers.init();
      Managers.subscribe();
      Render2DEngine.initShaders();
      ModuleManager.rpc.startRpc();
      LOGGER.info("[Vcore] Init time: {} ms.", System.currentTimeMillis() - initTime);
      initTime = System.currentTimeMillis();
      RUNTIME.addShutdownHook(new ManagerShutdownHook());
      RUNTIME.addShutdownHook(new ModuleShutdownHook());
   }

   public static boolean isFuturePresent() {
      return FabricLoader.getInstance().getModContainer("future").isPresent();
   }

   public enum KeyListening {
      ClickGui,
      Search,
      Sliders,
      Strings;
   }
}
