package vcore.injection;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.movement.freelook.CameraOverriddenEntity;
import vcore.features.modules.movement.freelook.FreeLookState;

@Mixin(Camera.class)
public abstract class MixinCamera {
   @Shadow
   private boolean field_18719;
   @Unique
   private Entity cameraEntity;

   @Shadow
   protected abstract float method_19318(float var1);

   @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
   private void modifyCameraDistance(Args args) {
      if (ModuleManager.noCameraClip.isEnabled()) {
         args.set(0, -this.method_19318(ModuleManager.noCameraClip.getDistance()));
      }
   }

   @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
   private void onClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.noCameraClip.isEnabled()) {
         cir.setReturnValue(ModuleManager.noCameraClip.getDistance());
      }
   }

   @Inject(method = "update", at = @At("TAIL"))
   private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      this.cameraEntity = focusedEntity;
      if (ModuleManager.freeCam.isEnabled()) {
         this.field_18719 = true;
      }
   }

   @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
   private void setRotationHook(Args args) {
      if (ModuleManager.freeCam.isEnabled()) {
         args.setAll(new Object[]{ModuleManager.freeCam.getFakeYaw(), ModuleManager.freeCam.getFakePitch()});
      }

      if (FreeLookState.active && this.cameraEntity instanceof CameraOverriddenEntity camera) {
         args.setAll(new Object[]{camera.getCameraYaw(), camera.getCameraPitch()});
      }
   }

   @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
   private void setPosHook(Args args) {
      if (ModuleManager.freeCam.isEnabled()) {
         args.setAll(new Object[]{ModuleManager.freeCam.getFakeX(), ModuleManager.freeCam.getFakeY(), ModuleManager.freeCam.getFakeZ()});
      }
   }
}
