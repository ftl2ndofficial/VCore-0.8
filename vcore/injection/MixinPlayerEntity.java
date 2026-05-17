package vcore.injection;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventEatFood;
import vcore.events.impl.EventPlayerJump;
import vcore.events.impl.EventPlayerTravel;
import vcore.features.modules.Module;
import vcore.features.modules.movement.AutoSprint;
import vcore.features.modules.movement.Speed;

@Mixin(value = PlayerEntity.class, priority = 800)
public class MixinPlayerEntity {
   @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", shift = Shift.AFTER))
   public void attackAHook(CallbackInfo callbackInfo) {
      if (ModuleManager.autoSprint.isEnabled() && AutoSprint.sprint.getValue()) {
         float multiplier = 0.6F + 0.4F * AutoSprint.motion.getValue();
         Module.mc
            .player
            .method_18800(
               Module.mc.player.method_18798().x / 0.6 * multiplier, Module.mc.player.method_18798().y, Module.mc.player.method_18798().z / 0.6 * multiplier
            );
         Module.mc.player.method_5728(true);
      }
   }

   @Inject(method = "getMovementSpeed", at = @At("HEAD"), cancellable = true)
   public void getMovementSpeedHook(CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.speed.isEnabled() && ModuleManager.speed.mode.is(Speed.Mode.Vanilla)) {
         cir.setReturnValue(ModuleManager.speed.boostFactor.getValue());
      }
   }

   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void attackAHook2(Entity target, CallbackInfo ci) {
      EventAttack event = new EventAttack(target, false);
      Vcore.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void onTravelhookPre(Vec3d movementInput, CallbackInfo ci) {
      if (Module.mc.player != null) {
         EventPlayerTravel event = new EventPlayerTravel(movementInput, true);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            Module.mc.player.method_5784(MovementType.SELF, Module.mc.player.method_18798());
            ci.cancel();
         }
      }
   }

   @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
   private void onTravelhookPost(Vec3d movementInput, CallbackInfo ci) {
      if (Module.mc.player != null) {
         EventPlayerTravel event = new EventPlayerTravel(movementInput, false);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            Module.mc.player.method_5784(MovementType.SELF, Module.mc.player.method_18798());
            ci.cancel();
         }
      }
   }

   @Inject(method = "jump", at = @At("HEAD"))
   private void onJumpPre(CallbackInfo ci) {
      Vcore.EVENT_BUS.post(new EventPlayerJump(true));
   }

   @Inject(method = "jump", at = @At("RETURN"))
   private void onJumpPost(CallbackInfo ci) {
      Vcore.EVENT_BUS.post(new EventPlayerJump(false));
   }

   @Inject(method = "eatFood", at = @At("RETURN"))
   public void eatFoodHook(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
      Vcore.EVENT_BUS.post(new EventEatFood((ItemStack)cir.getReturnValue()));
   }

   @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (ModuleManager.reach.isEnabled()) {
         if (ModuleManager.reach.Creative.getValue() && Module.mc.player.method_7337()) {
            cir.setReturnValue((double)ModuleManager.reach.creativeBlocksRange.getValue().floatValue());
         } else {
            cir.setReturnValue((double)ModuleManager.reach.blocksRange.getValue().floatValue());
         }
      }
   }

   @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (ModuleManager.reach.isEnabled()) {
         if (ModuleManager.reach.Creative.getValue() && Module.mc.player.method_7337()) {
            cir.setReturnValue((double)ModuleManager.reach.creativeEntityRange.getValue().floatValue());
         } else {
            cir.setReturnValue((double)ModuleManager.reach.entityRange.getValue().floatValue());
         }
      }
   }
}
