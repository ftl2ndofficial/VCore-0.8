package vcore.injection;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventTravel;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.movement.WaterSpeed;
import vcore.features.modules.render.Animations;
import vcore.utility.interfaces.IEntityLiving;

@Mixin(LivingEntity.class)
public class MixinEntityLiving implements IEntityLiving {
   @Shadow
   protected double field_6224;
   @Shadow
   protected double field_6245;
   @Shadow
   protected double field_6263;
   @Unique
   double prevServerX;
   @Unique
   double prevServerY;
   @Unique
   double prevServerZ;
   @Unique
   public List<Aura.Position> positonHistory = new ArrayList<>();
   @Unique
   private boolean prevFlying = false;

   @Override
   public List<Aura.Position> getPositionHistory() {
      return this.positonHistory;
   }

   @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
   private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> info) {
      if (ModuleManager.animations.shouldChangeAnimationDuration() && Animations.slowAnimation.getValue()) {
         info.setReturnValue(Animations.slowAnimationVal.getValue());
      }
   }

   @Inject(method = "updateTrackedPositionAndAngles", at = @At("HEAD"))
   private void updateTrackedPositionAndAnglesHook(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         this.prevServerX = this.field_6224;
         this.prevServerY = this.field_6245;
         this.prevServerZ = this.field_6263;
         this.positonHistory.add(new Aura.Position(this.field_6224, this.field_6245, this.field_6263));
         this.positonHistory.removeIf(Aura.Position::shouldRemove);
      }
   }

   @Override
   public double getPrevServerX() {
      return this.prevServerX;
   }

   @Override
   public double getPrevServerY() {
      return this.prevServerY;
   }

   @Override
   public double getPrevServerZ() {
      return this.prevServerZ;
   }

   @Inject(method = "isFallFlying", at = @At("TAIL"), cancellable = true)
   public void isFallFlyingHook(CallbackInfoReturnable<Boolean> cir) {
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   public void travelHook(Vec3d movementInput, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if ((LivingEntity)this == Module.mc.player) {
            EventTravel event = new EventTravel(Module.mc.player.method_18798(), true);
            Vcore.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               Module.mc.player.method_5784(MovementType.SELF, event.getmVec());
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
   public void travelPostHook(Vec3d movementInput, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if ((LivingEntity)this == Module.mc.player) {
            EventTravel event = new EventTravel(movementInput, false);
            Vcore.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               Module.mc.player.method_5784(MovementType.SELF, Module.mc.player.method_18798());
               ci.cancel();
            }
         }
      }
   }

   @ModifyVariable(method = "setSprinting", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private boolean setSprintingHook(boolean sprinting) {
      return Module.mc.player == null
            || Module.mc.world == null
            || !ModuleManager.waterSpeed.isEnabled()
            || !ModuleManager.waterSpeed.mode.is(WaterSpeed.Mode.CancelResurface)
            || !Module.mc.player.method_5799()
               && !(Module.mc.world.method_8320(BlockPos.ofFloored(Module.mc.player.method_19538().add(0.0, -0.5, 0.0))).method_26204() instanceof FluidBlock)
         ? sprinting
         : true;
   }
}
