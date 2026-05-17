package vcore.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Core;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAfterRotate;
import vcore.events.impl.EventMove;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSprint;
import vcore.events.impl.EventSync;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.events.impl.PostPlayerUpdateEvent;
import vcore.features.modules.Module;

@Mixin(value = ClientPlayerEntity.class, priority = 800)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
   @Unique
   boolean pre_sprint_state = false;
   @Unique
   private boolean updateLock = false;
   @Unique
   private Runnable postAction;

   @Shadow
   public abstract float method_5695(float var1);

   @Shadow
   protected abstract void method_3136();

   public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(method = "tick", at = @At("HEAD"))
   public void tickHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         Vcore.EVENT_BUS.post(new PlayerUpdateEvent());
      }
   }

   @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
   private boolean tickMovementHook(ClientPlayerEntity player) {
      return ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.canNoSlow() ? false : player.method_6115();
   }

   @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
   public void shouldSlowDownHook(CallbackInfoReturnable<Boolean> cir) {
      if (ModuleManager.noSlow.isEnabled()) {
         if (this.method_20448()) {
            if (ModuleManager.noSlow.crawl.getValue()) {
               cir.setReturnValue(false);
            }
         } else if (ModuleManager.noSlow.sneak.getValue()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(
      method = "move",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
      ),
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventMove event = new EventMove(movement.x, movement.y, movement.z);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            super.method_5784(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
         }
      }
   }

   @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
   private void sendMovementPacketsHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         EventSync event = new EventSync(this.method_36454(), this.method_36455());
         Vcore.EVENT_BUS.post(event);
         this.postAction = event.getPostAction();
         EventSprint e = new EventSprint(this.method_5624());
         Vcore.EVENT_BUS.post(e);
         Vcore.EVENT_BUS.post(new EventAfterRotate());
         if (e.getSprintState() != Module.mc.player.lastSprinting) {
            if (e.getSprintState()) {
               Module.mc.player.networkHandler.method_52787(new ClientCommandC2SPacket(this, Mode.START_SPRINTING));
            } else {
               Module.mc.player.networkHandler.method_52787(new ClientCommandC2SPacket(this, Mode.STOP_SPRINTING));
            }

            Module.mc.player.lastSprinting = e.getSprintState();
         }

         this.pre_sprint_state = Module.mc.player.lastSprinting;
         Core.lockSprint = true;
         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(method = "sendMovementPackets", at = @At("RETURN"), cancellable = true)
   private void sendMovementPacketsPostHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         Module.mc.player.lastSprinting = this.pre_sprint_state;
         Core.lockSprint = false;
         EventPostSync event = new EventPostSync();
         Vcore.EVENT_BUS.post(event);
         if (this.postAction != null) {
            this.postAction.run();
            this.postAction = null;
         }

         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V", ordinal = 0, shift = Shift.AFTER),
      cancellable = true
   )
   private void PostUpdateHook(CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         if (!this.updateLock) {
            PostPlayerUpdateEvent playerUpdateEvent = new PostPlayerUpdateEvent();
            Vcore.EVENT_BUS.post(playerUpdateEvent);
            if (playerUpdateEvent.isCancelled()) {
               info.cancel();
               if (playerUpdateEvent.getIterations() > 0) {
                  for (int i = 0; i < playerUpdateEvent.getIterations(); i++) {
                     this.updateLock = true;
                     this.method_5773();
                     this.updateLock = false;
                     this.method_3136();
                  }
               }
            }
         }
      }
   }

   @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
   private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
      if (ModuleManager.noPush.isEnabled() && ModuleManager.noPush.blocks.getValue()) {
         info.cancel();
      }
   }

   @Inject(method = "tickNausea", at = @At("HEAD"), cancellable = true)
   private void updateNauseaHook(CallbackInfo ci) {
      if (ModuleManager.portalInventory.isEnabled()) {
         ci.cancel();
      }
   }
}
