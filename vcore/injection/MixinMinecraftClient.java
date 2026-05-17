package vcore.injection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventHandleBlockBreaking;
import vcore.events.impl.EventItemUse;
import vcore.events.impl.EventPostTick;
import vcore.events.impl.EventScreen;
import vcore.events.impl.EventTick;
import vcore.features.modules.Module;
import vcore.features.modules.misc.UnHook;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.WindowResizeCallback;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
   @Shadow
   @Final
   private Window field_1704;
   @Unique
   private String[] shittyServers = new String[]{"mineblaze", "musteryworld", "dexland", "masedworld", "vimeworld", "hypemc", "vimemc"};
   @Unique
   private boolean wasInGame;

   @Shadow
   public abstract void method_1507(@Nullable Screen var1);

   @Inject(method = "<init>", at = @At("TAIL"))
   void postWindowInit(RunArgs args, CallbackInfo ci) {
      try {
         FontRenderers.settings = FontRenderers.create(12.0F, "comfortaa");
         FontRenderers.modules = FontRenderers.create(15.0F, "comfortaa");
         FontRenderers.categories = FontRenderers.create(18.0F, "comfortaa");
         FontRenderers.thglitch = FontRenderers.create(36.0F, "glitched");
         FontRenderers.thglitchBig = FontRenderers.create(72.0F, "glitched");
         FontRenderers.monsterrat = FontRenderers.create(18.0F, "monsterrat");
         FontRenderers.inter_target_name = FontRenderers.create(20.0F, "inter_semibold");
         FontRenderers.inter_target_hp = FontRenderers.create(15.0F, "inter_semibold");
         FontRenderers.sf_bold = FontRenderers.create(16.0F, "sf_bold");
         FontRenderers.sf_bold_large = FontRenderers.create(72.0F, "sf_bold");
         FontRenderers.sf_medium = FontRenderers.create(16.0F, "sf_medium");
         FontRenderers.sf_medium_mini = FontRenderers.create(12.0F, "sf_medium");
         FontRenderers.sf_medium_modules = FontRenderers.create(14.0F, "sf_medium");
         FontRenderers.sf_bold_mini = FontRenderers.create(14.0F, "sf_bold");
         FontRenderers.sf_bold_micro = FontRenderers.create(12.0F, "sf_bold");
         FontRenderers.profont = FontRenderers.create(16.0F, "profont");
         FontRenderers.icons = FontRenderers.create(20.0F, "icons");
         FontRenderers.mid_icons = FontRenderers.create(46.0F, "icons");
         FontRenderers.big_icons = FontRenderers.create(72.0F, "icons");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   void preTickHook(CallbackInfo ci) {
      boolean inGame = Module.mc.player != null && Module.mc.world != null;
      if (this.wasInGame && !inGame) {
         Managers.MODULE.onLogout();
      }

      this.wasInGame = inGame;
      if (!Module.fullNullCheck()) {
         Vcore.EVENT_BUS.post(new EventTick());
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   void postTickHook(CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         Vcore.EVENT_BUS.post(new EventPostTick());
      }
   }

   @Inject(method = "onResolutionChanged", at = @At("TAIL"))
   private void captureResize(CallbackInfo ci) {
      ((WindowResizeCallback)WindowResizeCallback.EVENT.invoker()).onResized((MinecraftClient)this, this.field_1704);
   }

   @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
   private void doItemPickHook(CallbackInfo ci) {
      if (ModuleManager.keyPearlAntiPickup) {
         ci.cancel();
      }
   }

   @Inject(method = "setOverlay", at = @At("HEAD"))
   public void setOverlay(Overlay overlay, CallbackInfo ci) {
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   public void setScreenHookPre(Screen screen, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventScreen event = new EventScreen(screen);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled() || ClickGUI.close && screen == null) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "setScreen", at = @At("RETURN"))
   public void setScreenHookPost(Screen screen, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         ;
      }
   }

   @Redirect(
      method = "<init>",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V")
   )
   private void onChangeIcon(Window instance, ResourcePack resourcePack, Icons icons) throws IOException {
      try (
         InputStream img16x16 = MixinMinecraftClient.class.getResourceAsStream("/icon.png");
         InputStream img32x32 = MixinMinecraftClient.class.getResourceAsStream("/icon.png");
      ) {
         if (img16x16 != null && img32x32 != null) {
            this.setWindowIcon(img16x16, img32x32);
            return;
         }
      }

      instance.setIcon(resourcePack, icons);
   }

   public void setWindowIcon(InputStream img16x16, InputStream img32x32) {
      try {
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            Buffer buffer = GLFWImage.malloc(2, memorystack);
            List<InputStream> imgList = List.of(img16x16, img32x32);
            List<ByteBuffer> buffers = new ArrayList<>();

            for (int i = 0; i < imgList.size(); i++) {
               NativeImage nativeImage = NativeImage.read(imgList.get(i));
               ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);
               bytebuffer.asIntBuffer().put(nativeImage.copyPixelsRgba());
               buffer.position(i);
               buffer.width(nativeImage.getWidth());
               buffer.height(nativeImage.getHeight());
               buffer.pixels(bytebuffer);
               buffers.add(bytebuffer);
            }

            try {
               if (GLFW.glfwGetPlatform() != 393219) {
                  GLFW.glfwSetWindowIcon(Module.mc.getWindow().getHandle(), buffer);
               }
            } catch (Exception var11) {
            }

            buffers.forEach(MemoryUtil::memFree);
         } catch (Throwable var12) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var10) {
                  var12.addSuppressed(var10);
               }
            }

            throw var12;
         }

         if (memorystack != null) {
            memorystack.close();
         }
      } catch (IOException var13) {
      }
   }

   @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
   private void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
      EventAttack event = new EventAttack(null, true);
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
   private void doItemUseHook(CallbackInfo ci) {
      EventItemUse event = new EventItemUse();
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
   private void handleBlockBreakingHook(boolean breaking, CallbackInfo ci) {
      EventHandleBlockBreaking event = new EventHandleBlockBreaking();
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "updateWindowTitle", at = @At("HEAD"), cancellable = true)
   private void overrideWindowTitle(CallbackInfo ci) {
      if (!UnHook.isActive()) {
         String title = "Vcore 0.8";
         this.field_1704.setTitle(title);
         ci.cancel();
      }
   }

   static {
      ClassLoader loader = MixinMinecraftClient.class.getClassLoader();
      String loaderName = loader != null ? loader.getClass().getName() : "null";
      if (!loaderName.contains("LaunchClassLoader") && !loaderName.contains("Mixin") && !loaderName.contains("fabric")) {
         throw new RuntimeException("[SECURITY] MixinMinecraftClient class loader is not allowed: " + loaderName);
      }
   }
}
