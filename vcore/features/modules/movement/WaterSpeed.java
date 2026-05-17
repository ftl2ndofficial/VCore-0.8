package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import vcore.events.impl.EventMove;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.math.MathUtility;
import vcore.utility.player.MovementUtility;

public class WaterSpeed extends Module {
   public final Setting<WaterSpeed.Mode> mode = new Setting<>("Mode", WaterSpeed.Mode.DolphinGrace);
   private float acceleration = 0.0F;

   public WaterSpeed() {
      super("WaterSpeed", "Faster movement in water.", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      if (this.mode.getValue() == WaterSpeed.Mode.DolphinGrace) {
         if (mc.player.method_5681()) {
            mc.player.method_6092(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 2, 2));
         } else {
            mc.player.method_6016(StatusEffects.DOLPHINS_GRACE);
         }
      }
   }

   @EventHandler
   public void onMove(EventMove e) {
      if (this.mode.getValue() == WaterSpeed.Mode.Intave) {
         if (mc.player.method_5681()) {
            double[] dirSpeed = MovementUtility.forward(this.acceleration / (mc.player.input.movementSideways != 0.0F ? 2.2F : 2.0F));
            e.setX(e.getX() + dirSpeed[0]);
            e.setZ(e.getZ() + dirSpeed[1]);
            e.cancel();
            this.acceleration += 0.05F;
            this.acceleration = MathUtility.clamp(this.acceleration, 0.0F, 1.0F);
         } else {
            this.acceleration = 0.0F;
         }

         if (!MovementUtility.isMoving()) {
            this.acceleration = 0.0F;
         }
      }

      if (this.mode.getValue() == WaterSpeed.Mode.FunTimeNew) {
         if (mc.player.method_5681()) {
            mc.player.input.movementSideways = 0.0F;
            double[] dirSpeed = MovementUtility.forward(this.acceleration / 6.3447F);
            e.setX(e.getX() + dirSpeed[0]);
            e.setZ(e.getZ() + dirSpeed[1]);
            e.cancel();
            if (Math.abs(mc.player.method_36454() - mc.player.field_5982) > 3.0F) {
               this.acceleration -= 0.1F;
            } else {
               this.acceleration += 0.015F;
            }

            this.acceleration = MathUtility.clamp(this.acceleration, 0.0F, 1.0F);
         } else {
            this.acceleration = 0.0F;
         }

         if (!MovementUtility.isMoving() || mc.player.field_5976 || mc.player.field_5992) {
            this.acceleration = 0.0F;
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.mode.getValue() == WaterSpeed.Mode.DolphinGrace) {
         mc.player.method_6016(StatusEffects.DOLPHINS_GRACE);
      }
   }

   public enum Mode {
      DolphinGrace,
      Intave,
      CancelResurface,
      FunTimeNew;
   }
}
