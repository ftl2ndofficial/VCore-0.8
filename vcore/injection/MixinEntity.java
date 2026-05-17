package vcore.injection;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventFixVelocity;
import vcore.features.modules.Module;
import vcore.features.modules.combat.HitBox;
import vcore.features.modules.movement.freelook.CameraOverriddenEntity;
import vcore.features.modules.movement.freelook.FreeLookState;
import vcore.utility.interfaces.IEntity;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity, CameraOverriddenEntity {
   @Unique
   private float cameraYaw;
   @Unique
   private float cameraPitch;
   @Shadow
   private Box field_6005;

   @Shadow
   protected abstract BlockPos method_23314();

   @Override
   public BlockPos Vcore_Recode$getVelocityBP() {
      return this.method_23314();
   }

   @ModifyArgs(method = "pushAwayFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
   public void pushAwayFromHook(Args args) {
      if (this == Module.mc.player && ModuleManager.noPush.isEnabled() && ModuleManager.noPush.players.getValue()) {
         args.set(0, 0.0);
         args.set(1, 0.0);
         args.set(2, 0.0);
      }
   }

   @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true)
   public void updateVelocityHook(float speed, Vec3d movementInput, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (this == Module.mc.player) {
            ci.cancel();
            EventFixVelocity event = new EventFixVelocity(
               movementInput, speed, Module.mc.player.method_36454(), movementInputToVelocityC(movementInput, speed, Module.mc.player.method_36454())
            );
            Vcore.EVENT_BUS.post(event);
            Module.mc.player.method_18799(Module.mc.player.method_18798().add(event.getVelocity()));
         }
      }
   }

   @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
   private void onChangeLookDirection(double deltaX, double deltaY, CallbackInfo ci) {
      Entity self = (Entity)this;
      if (FreeLookState.active && self instanceof ClientPlayerEntity) {
         this.cameraYaw += (float)deltaX * 0.15F;
         this.cameraPitch = MathHelper.clamp(this.cameraPitch + (float)deltaY * 0.15F, -90.0F, 90.0F);
         ci.cancel();
      }
   }

   @Override
   public float getCameraYaw() {
      return this.cameraYaw;
   }

   @Override
   public float getCameraPitch() {
      return this.cameraPitch;
   }

   @Override
   public void setCameraYaw(float yaw) {
      this.cameraYaw = yaw;
   }

   @Override
   public void setCameraPitch(float pitch) {
      this.cameraPitch = pitch;
   }

   @Unique
   private static Vec3d movementInputToVelocityC(Vec3d movementInput, float speed, float yaw) {
      double d = movementInput.lengthSquared();
      if (d < 1.0E-7) {
         return Vec3d.ZERO;
      }

      Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
      float f = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
      float g = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
      return new Vec3d(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
   }

   @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
   public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
      Entity self = (Entity)this;
      if (!HitBox.isSkippingExpand()
         && ModuleManager.hitBox.isEnabled()
         && Module.mc != null
         && Module.mc.player != null
         && self instanceof PlayerEntity
         && self.method_5628() != Module.mc.player.method_5628()
         && ModuleManager.hitBox.shouldExpand(self)) {
         cir.setReturnValue(
            new Box(
               this.field_6005.minX - HitBox.XZExpand.getValue() / 2.0F,
               this.field_6005.minY - HitBox.YExpand.getValue() / 2.0F,
               this.field_6005.minZ - HitBox.XZExpand.getValue() / 2.0F,
               this.field_6005.maxX + HitBox.XZExpand.getValue() / 2.0F,
               this.field_6005.maxY + HitBox.YExpand.getValue() / 2.0F,
               this.field_6005.maxZ + HitBox.XZExpand.getValue() / 2.0F
            )
         );
      }
   }

   @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
   public void isOnFireHook(CallbackInfoReturnable<Boolean> cir) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fireEntity.getValue()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isInLava", at = @At("HEAD"), cancellable = true)
   public void isInLavaHook(CallbackInfoReturnable<Boolean> cir) {
   }

   @Inject(method = "isTouchingWater", at = @At("HEAD"), cancellable = true)
   public void isTouchingWaterHook(CallbackInfoReturnable<Boolean> cir) {
   }

   @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
   public void setSwimmingHook(boolean swimming, CallbackInfo ci) {
   }
}
