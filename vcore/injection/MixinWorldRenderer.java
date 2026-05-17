package vcore.injection;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
   @ModifyArg(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"
      ),
      index = 3
   )
   private boolean renderSetupTerrainModifyArg(boolean spectator) {
      return ModuleManager.freeCam.isEnabled() || spectator;
   }

   @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
   private void renderWeatherHook(LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noWeather.getValue()) {
         ci.cancel();
      }
   }
}
