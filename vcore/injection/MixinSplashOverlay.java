package vcore.injection;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {
   @Final
   @Shadow
   private boolean field_18219;
   @Shadow
   private float field_17770;
   @Shadow
   private long field_17771 = -1L;
   @Shadow
   private long field_18220 = -1L;
   @Final
   @Shadow
   private ResourceReload field_17767;
   @Final
   @Shadow
   private Consumer<Optional<Throwable>> field_18218;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!ModuleManager.unHook.isEnabled() && ClientSettings.customLoadingScreen.getValue()) {
         ci.cancel();
         this.renderCustom(context, mouseX, mouseY, delta);
      }
   }

   public void renderCustom(DrawContext context, int mouseX, int mouseY, float delta) {
      int i = Module.mc.getWindow().getScaledWidth();
      int j = Module.mc.getWindow().getScaledHeight();
      long l = Util.getMeasuringTimeMs();
      if (this.field_18219 && this.field_18220 == -1L) {
         this.field_18220 = l;
      }

      float f = this.field_17771 > -1L ? (float)(l - this.field_17771) / 1000.0F : -1.0F;
      float g = this.field_18220 > -1L ? (float)(l - this.field_18220) / 500.0F : -1.0F;
      float h;
      if (f >= 1.0F) {
         if (Module.mc.currentScreen != null) {
            Module.mc.currentScreen.method_25394(context, 0, 0, delta);
         }

         int k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
         context.fill(0, 0, i, j, withAlpha(new Color(458773).getRGB(), k));
         h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
      } else if (this.field_18219) {
         if (Module.mc.currentScreen != null && g < 1.0F) {
            Module.mc.currentScreen.method_25394(context, mouseX, mouseY, delta);
         }

         int k = MathHelper.ceil(MathHelper.clamp(g, 0.15, 1.0) * 255.0);
         context.fill(0, 0, i, j, withAlpha(new Color(458773).getRGB(), k));
         h = MathHelper.clamp(g, 0.0F, 1.0F);
      } else {
         int k = new Color(458773).getRGB();
         float m = (k >> 16 & 0xFF) / 255.0F;
         float n = (k >> 8 & 0xFF) / 255.0F;
         float o = (k & 0xFF) / 255.0F;
         GlStateManager._clearColor(m, n, o, 1.0F);
         GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
         h = 1.0F;
      }

      int var19 = (int)(context.getScaledWindowWidth() * 0.5);
      int p = (int)(context.getScaledWindowHeight() * 0.5);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 1);
      RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, h);
      context.drawTexture(TextureStorage.thLogo, var19 - 150, p - 35, 0.0F, 0.0F, 300, 70, 300, 70);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, h);
      Render2DEngine.addWindow(context.getMatrices(), var19 - 150, p - 35, var19 - 150 + 300.0F * this.field_17770, p + 35, 1.0);
      context.drawTexture(TextureStorage.thLogo, var19 - 150, p - 35, 0.0F, 0.0F, 300, 70, 300, 70);
      Render2DEngine.popWindow();
      float t = this.field_17767.getProgress();
      this.field_17770 = MathHelper.clamp(this.field_17770 * 0.95F + t * 0.050000012F, 0.0F, 1.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      if (f >= 2.0F) {
         Module.mc.setOverlay(null);
      }

      if (this.field_17771 == -1L && this.field_17767.isComplete() && (!this.field_18219 || g >= 2.0F)) {
         try {
            this.field_17767.throwException();
            this.field_18218.accept(Optional.empty());
         } catch (Throwable var23) {
            this.field_18218.accept(Optional.of(var23));
         }

         this.field_17771 = Util.getMeasuringTimeMs();
         if (Module.mc.currentScreen != null) {
            Module.mc.currentScreen.init(Module.mc, Module.mc.getWindow().getScaledWidth(), Module.mc.getWindow().getScaledHeight());
         }
      }
   }

   private static int withAlpha(int color, int alpha) {
      return color & 16777215 | alpha << 24;
   }
}
