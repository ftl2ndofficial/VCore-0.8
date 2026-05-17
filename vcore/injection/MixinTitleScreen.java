package vcore.injection;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.misc.UnHook;
import vcore.gui.mainmenu.MainMenuScreen;
import vcore.utility.render.Render2DEngine;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
   protected MixinTitleScreen(Text title) {
      super(title);
   }

   @Inject(method = "init", at = @At("RETURN"))
   public void postInitHook(CallbackInfo ci) {
      if (ModuleManager.clickGui.getBind().getKey() == -1) {
         ModuleManager.clickGui.setBind(InputUtil.fromTranslationKey("key.keyboard.right.shift").getCode(), false, false);
      }

      if (!UnHook.isActive() && ClientSettings.customMainMenu.getValue()) {
         MainMenuScreen mainMenuScreen = MainMenuScreen.getInstance();
         if (!mainMenuScreen.confirm) {
            Module.mc.setScreen(mainMenuScreen);
         }
      }
   }

   @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
   private void renderPanoramaBackgroundHook(DrawContext context, float delta, CallbackInfo ci) {
      if (!UnHook.isActive() && ClientSettings.customPanorama.getValue() && Module.mc.world == null) {
         ci.cancel();
         Render2DEngine.drawMainMenuShader(context.getMatrices(), 0.0F, 0.0F, Module.mc.getWindow().getScaledWidth(), Module.mc.getWindow().getScaledHeight());
      }
   }
}
