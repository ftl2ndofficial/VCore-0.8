package vcore.injection;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;

@Mixin(LightmapTextureManager.class)
public class MixinLightmapTextureManager {
   @Inject(method = "getDarknessFactor(F)F", at = @At("HEAD"), cancellable = true)
   private void getDarknessFactor(float tickDelta, CallbackInfoReturnable<Float> info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.darkness.getValue()) {
         info.setReturnValue(0.0F);
      }
   }

   @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
   private static void getBrightnessHook(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.fullbright.isEnabled()) {
         float f = lightLevel / 15.0F;
         float g = f / (4.0F - 3.0F * f);
         cir.setReturnValue(Math.max(MathHelper.lerp(type.ambientLight(), g, 1.0F), 0.5F));
      }
   }
}
