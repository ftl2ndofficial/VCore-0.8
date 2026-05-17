package vcore.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.InputBlocker;

@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding {
   @Shadow
   public abstract boolean method_1435(KeyBinding var1);

   @Shadow
   public abstract boolean method_1434();

   @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
   private void pressHook(CallbackInfoReturnable<Boolean> cir) {
      try {
         KeyBinding self = (KeyBinding)this;
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc == null) {
            return;
         }

         if (InputBlocker.isBlocked()
            && (
               self.equals(mc.options.forwardKey)
                  || self.equals(mc.options.backKey)
                  || self.equals(mc.options.leftKey)
                  || self.equals(mc.options.rightKey)
                  || self.equals(mc.options.jumpKey)
                  || self.equals(mc.options.sneakKey)
                  || self.equals(mc.options.sprintKey)
            )) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
         }

         if (InputBlocker.isUseBlocked() && self.equals(mc.options.useKey)) {
            cir.setReturnValue(false);
            cir.cancel();
         }
      } catch (Throwable var4) {
      }
   }
}
