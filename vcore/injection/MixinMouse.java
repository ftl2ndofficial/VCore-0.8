package vcore.injection;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.events.impl.EventMouse;
import vcore.features.modules.Module;

@Mixin(Mouse.class)
public class MixinMouse {
   @Inject(method = "onMouseButton", at = @At("HEAD"))
   public void onMouseButtonHook(long window, int button, int action, int mods, CallbackInfo ci) {
      if (window == Module.mc.getWindow().getHandle()) {
         if (action == 0) {
            Managers.MODULE.onMoseKeyReleased(button);
         }

         if (action == 1) {
            Managers.MODULE.onMoseKeyPressed(button);
         }

         Vcore.EVENT_BUS.post(new EventMouse(button, action));
      }
   }

   @Inject(method = "onMouseScroll", at = @At("HEAD"))
   private void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (window == Module.mc.getWindow().getHandle()) {
         Vcore.EVENT_BUS.post(new EventMouse((int)vertical, 2));
      }
   }
}
