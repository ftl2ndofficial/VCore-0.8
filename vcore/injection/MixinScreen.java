package vcore.injection;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.Managers;
import vcore.core.manager.client.CommandManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.ClientClickEvent;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.misc.UnHook;
import vcore.gui.misc.DialogScreen;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

@Mixin(Screen.class)
public abstract class MixinScreen {
   @Shadow
   public abstract void method_25423(MinecraftClient var1, int var2, int var3);

   @Inject(
      method = "handleTextClick",
      at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", ordinal = 1, remap = false),
      cancellable = true
   )
   private void onRunCommand(Style style, CallbackInfoReturnable<Boolean> cir) {
      if (Objects.requireNonNull(style.getClickEvent()) instanceof ClientClickEvent clientClickEvent
         && clientClickEvent.method_10844().startsWith(Managers.COMMAND.getPrefix())) {
         try {
            CommandManager manager = Managers.COMMAND;
            manager.getDispatcher().execute(style.getClickEvent().getValue().substring(Managers.COMMAND.getPrefix().length()), manager.getSource());
            cir.setReturnValue(true);
         } catch (CommandSyntaxException var5) {
         }
      }
   }

   @Inject(method = "filesDragged", at = @At("HEAD"))
   public void filesDragged(List<Path> paths, CallbackInfo ci) {
      String configPath = paths.get(0).toString();
      File cfgFile = new File(configPath);
      String fileName = cfgFile.getName();
      if (fileName.contains(".vc")) {
         DialogScreen dialogScreen = new DialogScreen(
            TextureStorage.setting, "Config detected!", "Are you sure you want to load " + fileName + "?", "Yes", "No", () -> {
               Managers.MODULE.onUnload("none");
               Managers.CONFIG.load(cfgFile);
               Managers.MODULE.onLoad("none");
               Module.mc.setScreen(null);
            }, () -> Module.mc.setScreen(null)
         );
         Module.mc.setScreen(dialogScreen);
      }
   }

   @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
   public void renderPanoramaBackgroundHook(DrawContext context, float delta, CallbackInfo ci) {
      if (shouldRenderCustomPanorama()) {
         ci.cancel();
         Render2DEngine.drawMainMenuShader(context.getMatrices(), 0.0F, 0.0F, Module.mc.getWindow().getScaledWidth(), Module.mc.getWindow().getScaledHeight());
      }
   }

   @Inject(method = "renderBackgroundTexture", at = @At("HEAD"), cancellable = true)
   private static void renderBackgroundTextureHook(
      DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, CallbackInfo ci
   ) {
      if (shouldRenderCustomPanorama()) {
         ci.cancel();
         Render2DEngine.drawMainMenuShader(context.getMatrices(), 0.0F, 0.0F, Module.mc.getWindow().getScaledWidth(), Module.mc.getWindow().getScaledHeight());
      }
   }

   @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
   private void renderInGameBackground(CallbackInfo info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue()) {
         info.cancel();
      }
   }

   @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
   public void onRenderBackground(DrawContext context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.disableGuiBackGround.getValue() && Module.mc.world != null) {
         ci.cancel();
      }
   }

   private static boolean shouldRenderCustomPanorama() {
      return !UnHook.isActive() && ClientSettings.customPanorama.getValue() && Module.mc.world == null;
   }
}
