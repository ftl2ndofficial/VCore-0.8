package vcore.injection;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.events.impl.EventKeyboardInput;
import vcore.features.modules.Module;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
   @Unique
   private boolean vcore$clearMovementInput;

   @Inject(
      method = "tick",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z", shift = Shift.BEFORE),
      cancellable = true
   )
   private void onSneak(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventKeyboardInput event = new EventKeyboardInput((Input)this);
         Vcore.EVENT_BUS.post(event);
         this.vcore$clearMovementInput = event.shouldClearMovementInput();
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void onTickReturn(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
      if (this.vcore$clearMovementInput) {
         EventKeyboardInput.clearMovementInput((Input)this);
         this.vcore$clearMovementInput = false;
      }
   }
}
