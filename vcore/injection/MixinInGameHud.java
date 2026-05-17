package vcore.injection;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {
   @Inject(at = @At("HEAD"), method = "render")
   public void renderHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         Managers.MODULE.onRender2D(context);
         Managers.NOTIFICATION.onRender2D(context);
      }
   }

   @Inject(at = @At("HEAD"), method = "renderHeldItemTooltip", cancellable = true)
   public void renderHeldItemTooltipHook(DrawContext context, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hotbarItemName.getValue()) {
         ci.cancel();
      }
   }

   @Inject(at = @At("HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
   public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (ModuleManager.hudEditor.isHudEnabled("PotionHud")) {
         ci.cancel();
      }
   }

   @Inject(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (ModuleManager.noRender.noScoreBoard.getValue() && ModuleManager.noRender.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
   private void renderVignetteOverlayHook(DrawContext context, Entity entity, CallbackInfo ci) {
      if (ModuleManager.noRender.vignette.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void renderPortalOverlayHook(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      if (ModuleManager.noRender.portal.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   public void renderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (ModuleManager.crosshair.isEnabled()) {
         ci.cancel();
      }
   }
}
