package vcore.injection;

import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.events.impl.EventKeyPress;
import vcore.events.impl.EventKeyRelease;
import vcore.features.modules.Module;
import vcore.gui.clickui.ClickGUI;

@Mixin(Keyboard.class)
public class MixinKeyboard {
   @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
   private void onKey(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         boolean whitelist = Module.mc.currentScreen == null || Module.mc.currentScreen instanceof ClickGUI;
         if (whitelist) {
            if (action == 0) {
               Managers.MODULE.onKeyReleased(key);
            }

            if (action == 1) {
               Managers.MODULE.onKeyPressed(key);
            }

            if (action == 2) {
               action = 1;
            }

            switch (action) {
               case 0:
                  EventKeyRelease eventx = new EventKeyRelease(key, scanCode);
                  Vcore.EVENT_BUS.post(eventx);
                  if (eventx.isCancelled()) {
                     ci.cancel();
                  }
                  break;
               case 1:
                  EventKeyPress event = new EventKeyPress(key, scanCode);
                  Vcore.EVENT_BUS.post(event);
                  if (event.isCancelled()) {
                     ci.cancel();
                  }
            }
         }
      }
   }
}
