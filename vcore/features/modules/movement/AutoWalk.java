package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import vcore.events.impl.EventKeyboardInput;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class AutoWalk extends Module {
   private final Setting<AutoWalk.Mode> mode = new Setting<>("Mode", AutoWalk.Mode.Simple);

   public AutoWalk() {
      super("AutoWalk", "Auto walks forward.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (this.mode.getValue() == AutoWalk.Mode.Baritone) {
         mc.player
            .networkHandler
            .sendChatMessage(
               "#goto "
                  + 3000000.0 * Math.cos(Math.toRadians(mc.player.method_36454() + 90.0F))
                  + " "
                  + 3000000.0 * Math.sin(Math.toRadians(mc.player.method_36454() + 90.0F))
            );
      }
   }

   @Override
   public void onDisable() {
      if (this.mode.getValue() == AutoWalk.Mode.Baritone) {
         mc.player.networkHandler.sendChatMessage("#stop");
      }
   }

   @EventHandler
   public void onKey(EventKeyboardInput e) {
      if (this.mode.getValue() == AutoWalk.Mode.Simple) {
         mc.player.input.movementForward = 1.0F;
      }
   }

   public enum Mode {
      Simple,
      Baritone;
   }
}
