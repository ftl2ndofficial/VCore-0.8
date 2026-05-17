package vcore.features.modules.misc;

import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import vcore.core.Managers;
import vcore.events.impl.EventKeyboardInput;
import vcore.events.impl.EventSetting;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.MovementUtility;

public class AntiAFK extends Module {
   private final Setting<AntiAFK.Mode> mode = new Setting<>("Mode", AntiAFK.Mode.Simple);
   private final Setting<Boolean> onlyWhenAfk = new Setting<>("OnlyWhenAFK", false);
   private final Setting<Boolean> command = new Setting<>("Command", false, v -> this.mode.getValue() == AntiAFK.Mode.Simple);
   private final Setting<Boolean> move = new Setting<>("Move", false, v -> this.mode.getValue() == AntiAFK.Mode.Simple);
   private final Setting<Boolean> spin = new Setting<>("Spin", false, v -> this.mode.getValue() == AntiAFK.Mode.Simple);
   private final Setting<Float> rotateSpeed = new Setting<>("RotateSpeed", 5.0F, 1.0F, 7.0F, v -> this.mode.getValue() == AntiAFK.Mode.Simple);
   private final Setting<Boolean> jump = new Setting<>("Jump", false, v -> this.mode.getValue() == AntiAFK.Mode.Simple);
   private final Setting<Boolean> swing = new Setting<>("Swing", false, v -> this.mode.getValue() == AntiAFK.Mode.Simple);
   private final Setting<Boolean> alwayssneak = new Setting<>("AlwaysSneak", false, v -> this.mode.getValue() == AntiAFK.Mode.Simple);
   private final Setting<Integer> radius = new Setting<>("Radius", 64, 1, 128, v -> this.mode.getValue() == AntiAFK.Mode.Baritone);
   private int step;
   private Timer inactiveTime = new Timer();

   public AntiAFK() {
      super("AntiAFK", "Prevents AFK kicks.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      if (this.alwayssneak.getValue()) {
         mc.options.sneakKey.setPressed(true);
      }

      this.step = 0;
   }

   @EventHandler
   public void onSettingChange(EventSetting e) {
      if (e.getSetting() == this.mode) {
         this.step = 0;
      }
   }

   @EventHandler
   public void onKeyboardInput(EventKeyboardInput e) {
      if (mc.player != null && this.mode.is(AntiAFK.Mode.Simple) && !MovementUtility.isMoving() && this.move.getValue() && this.isAfk()) {
         float angleToRad = (float)Math.toRadians(9 * (mc.player.field_6012 % 40));
         float sin = (float)Math.clamp(Math.sin(angleToRad), -1.0, 1.0);
         float cos = (float)Math.clamp(Math.cos(angleToRad), -1.0, 1.0);
         mc.player.input.movementForward = Math.round(sin);
         mc.player.input.movementSideways = Math.round(cos);
      }
   }

   @EventHandler(priority = -100)
   public void onUpdate(PlayerUpdateEvent e) {
      if (this.mode.is(AntiAFK.Mode.Simple) ? this.isActive() : Managers.PLAYER.currentPlayerSpeed > 0.07) {
         this.inactiveTime.reset();
      }

      if (this.mode.getValue() == AntiAFK.Mode.Simple) {
         if (!this.isAfk()) {
            return;
         }

         if (this.move.getValue()) {
            mc.player.method_5728(false);
         }

         if (this.spin.getValue()) {
            double gcdFix = Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
            float newYaw = mc.player.method_36454() + this.rotateSpeed.getValue();
            mc.player.method_36456((float)(newYaw - (newYaw - mc.player.method_36454()) % gcdFix));
         }

         if (this.jump.getValue() && mc.player.method_24828()) {
            mc.player.method_6043();
         }

         if (this.swing.getValue() && ThreadLocalRandom.current().nextInt(99) == 0) {
            mc.player.method_6104(mc.player.method_6058());
         }

         if (this.command.getValue() && ThreadLocalRandom.current().nextInt(99) == 0) {
            mc.player.networkHandler.sendChatCommand("qwerty");
         }
      } else if (this.inactiveTime.every(5000L)) {
         if (this.step > 3) {
            this.step = 0;
         }

         switch (this.step) {
            case 0:
               mc.player.networkHandler.sendChatMessage("#goto ~ ~" + this.radius.getValue());
               break;
            case 1:
               mc.player.networkHandler.sendChatMessage("#goto ~" + this.radius.getValue() + " ~");
               break;
            case 2:
               mc.player.networkHandler.sendChatMessage("#goto ~ ~-" + this.radius.getValue());
               break;
            case 3:
               mc.player.networkHandler.sendChatMessage("#goto ~-" + this.radius.getValue() + " ~");
         }

         this.step++;
      }
   }

   @Override
   public void onDisable() {
      if (this.alwayssneak.getValue()) {
         mc.options.sneakKey.setPressed(false);
      }

      if (this.mode.getValue() == AntiAFK.Mode.Baritone) {
         mc.player.networkHandler.sendChatMessage("#stop");
      }
   }

   private boolean isAfk() {
      return !this.onlyWhenAfk.getValue() || this.inactiveTime.passedS(10.0);
   }

   private boolean isActive() {
      return mc.options.forwardKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.backKey.isPressed();
   }

   private enum Mode {
      Simple,
      Baritone;
   }
}
