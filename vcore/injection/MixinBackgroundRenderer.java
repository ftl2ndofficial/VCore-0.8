package vcore.injection;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.WorldTweaks;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
   @Inject(method = "applyFog", at = @At("TAIL"))
   private static void onApplyFog(Camera camera, FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fog.getValue() && fogType == FogType.FOG_TERRAIN) {
         RenderSystem.setShaderFogStart(viewDistance * 4.0F);
         RenderSystem.setShaderFogEnd(viewDistance * 4.25F);
      }

      if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
         RenderSystem.setShaderFogStart(WorldTweaks.fogStart.getValue().intValue());
         RenderSystem.setShaderFogEnd(WorldTweaks.fogEnd.getValue().intValue());
         RenderSystem.setShaderFogColor(
            WorldTweaks.fogColor.getValue().getGlRed(), WorldTweaks.fogColor.getValue().getGlGreen(), WorldTweaks.fogColor.getValue().getGlBlue()
         );
      }
   }

   @Inject(
      method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.blindness.getValue()) {
         info.setReturnValue(null);
      }
   }
}
