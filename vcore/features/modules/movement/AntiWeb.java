package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventMove;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.MovementUtility;

public class AntiWeb extends Module {
   public static final Setting<AntiWeb.Mode> mode = new Setting<>("Mode", AntiWeb.Mode.Fly);
   public static final Setting<Boolean> grim = new Setting<>("Grim", false, v -> mode.is(AntiWeb.Mode.Ignore));
   public final Setting<Float> ySpeed = new Setting<>("Y Speed", 1.4F, 0.1F, 4.0F, v -> mode.getValue() == AntiWeb.Mode.Fly);
   public final Setting<Float> xzSpeed = new Setting<>("XZ Speed", 0.6F, 0.1F, 1.0F, v -> mode.getValue() == AntiWeb.Mode.Fly);

   public AntiWeb() {
      super("AntiWeb", "Prevents web slowdown.", Module.Category.MOVEMENT);
   }

   public static boolean shouldControlFlyMovement() {
      return ModuleManager.antiWeb != null
         && ModuleManager.antiWeb.isEnabled()
         && mode.is(AntiWeb.Mode.Fly)
         && mc.player != null
         && mc.world != null
         && Managers.PLAYER.isInWeb();
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (shouldControlFlyMovement()) {
         mc.player.method_18800(0.0, 0.0, 0.0);
      }
   }

   @EventHandler
   public void onMove(EventMove event) {
      if (shouldControlFlyMovement()) {
         double vertical = 0.0;
         if (mc.options.jumpKey.isPressed() != mc.options.sneakKey.isPressed()) {
            vertical = mc.options.jumpKey.isPressed() ? this.ySpeed.getValue().floatValue() : -this.ySpeed.getValue();
         }

         MovementUtility.modifyEventSpeed(event, this.xzSpeed.getValue().floatValue());
         event.setY(vertical);
         event.cancel();
      }
   }

   public enum Mode {
      Ignore,
      Fly;
   }
}
