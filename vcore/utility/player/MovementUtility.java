package vcore.utility.player;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import vcore.core.Managers;
import vcore.events.impl.EventMove;
import vcore.features.modules.Module;

public final class MovementUtility {
   public static boolean isMoving() {
      return Module.mc.player != null
         && Module.mc.world != null
         && Module.mc.player.input != null
         && (Module.mc.player.input.movementForward != 0.0 || Module.mc.player.input.movementSideways != 0.0);
   }

   public static double getSpeed() {
      return Math.hypot(Module.mc.player.method_18798().x, Module.mc.player.method_18798().z);
   }

   public static double[] forward(double d) {
      float f = Module.mc.player.input.movementForward;
      float f2 = Module.mc.player.input.movementSideways;
      float f3 = Module.mc.player.method_36454();
      if (f != 0.0F) {
         if (f2 > 0.0F) {
            f3 += f > 0.0F ? -45 : 45;
         } else if (f2 < 0.0F) {
            f3 += f > 0.0F ? 45 : -45;
         }

         f2 = 0.0F;
         if (f > 0.0F) {
            f = 1.0F;
         } else if (f < 0.0F) {
            f = -1.0F;
         }
      }

      double d2 = Math.sin(Math.toRadians(f3 + 90.0F));
      double d3 = Math.cos(Math.toRadians(f3 + 90.0F));
      double d4 = f * d * d3 + f2 * d * d2;
      double d5 = f * d * d2 - f2 * d * d3;
      return new double[]{d4, d5};
   }

   public static void setMotion(double speed) {
      double forward = Module.mc.player.input.movementForward;
      double strafe = Module.mc.player.input.movementSideways;
      float yaw = Module.mc.player.method_36454();
      if (forward == 0.0 && strafe == 0.0) {
         Module.mc.player.method_18800(0.0, Module.mc.player.method_18798().y, 0.0);
      } else {
         if (forward != 0.0) {
            if (strafe > 0.0) {
               yaw += forward > 0.0 ? -45 : 45;
            } else if (strafe < 0.0) {
               yaw += forward > 0.0 ? 45 : -45;
            }

            strafe = 0.0;
            if (forward > 0.0) {
               forward = 1.0;
            } else if (forward < 0.0) {
               forward = -1.0;
            }
         }

         double sin = MathHelper.sin((float)Math.toRadians(yaw + 90.0F));
         double cos = MathHelper.cos((float)Math.toRadians(yaw + 90.0F));
         Module.mc
            .player
            .method_18800(forward * speed * cos + strafe * speed * sin, Module.mc.player.method_18798().y, forward * speed * sin - strafe * speed * cos);
      }
   }

   public static float getMoveDirection() {
      double forward = Module.mc.player.input.movementForward;
      double strafe = Module.mc.player.input.movementSideways;
      if (strafe > 0.0) {
         strafe = 1.0;
      } else if (strafe < 0.0) {
         strafe = -1.0;
      }

      float yaw = Module.mc.player.method_36454();
      if (forward == 0.0 && strafe == 0.0) {
         return yaw;
      }

      if (forward != 0.0) {
         if (strafe > 0.0) {
            yaw += forward > 0.0 ? -45.0F : -135.0F;
         } else if (strafe < 0.0) {
            yaw += forward > 0.0 ? 45.0F : 135.0F;
         } else if (forward < 0.0) {
            yaw += 180.0F;
         }
      }

      if (forward == 0.0) {
         if (strafe > 0.0) {
            yaw -= 90.0F;
         } else if (strafe < 0.0) {
            yaw += 90.0F;
         }
      }

      return yaw;
   }

   public static double[] forwardWithoutStrafe(double d) {
      float f3 = Module.mc.player.method_36454();
      double d4 = d * Math.cos(Math.toRadians(f3 + 90.0F));
      double d5 = d * Math.sin(Math.toRadians(f3 + 90.0F));
      return new double[]{d4, d5};
   }

   public static double getJumpSpeed() {
      double jumpSpeed = 0.39999995F;
      if (Module.mc.player.method_6059(StatusEffects.JUMP_BOOST)) {
         double amplifier = Module.mc.player.method_6112(StatusEffects.JUMP_BOOST).getAmplifier();
         jumpSpeed += (amplifier + 1.0) * 0.1;
      }

      return jumpSpeed;
   }

   public static void modifyEventSpeed(EventMove event, double d) {
      double d2 = Module.mc.player.input.movementForward;
      double d3 = Module.mc.player.input.movementSideways;
      float f = Module.mc.player.method_36454();
      if (d2 == 0.0 && d3 == 0.0) {
         event.setX(0.0);
         event.setZ(0.0);
      } else {
         if (d2 != 0.0) {
            if (d3 > 0.0) {
               f += d2 > 0.0 ? -45 : 45;
            } else if (d3 < 0.0) {
               f += d2 > 0.0 ? 45 : -45;
            }

            d3 = 0.0;
            if (d2 > 0.0) {
               d2 = 1.0;
            } else if (d2 < 0.0) {
               d2 = -1.0;
            }
         }

         double sin = Math.sin(Math.toRadians(f + 90.0F));
         double cos = Math.cos(Math.toRadians(f + 90.0F));
         event.setX(d2 * d * cos + d3 * d * sin);
         event.setZ(d2 * d * sin - d3 * d * cos);
      }
   }

   public static double getBaseMoveSpeed() {
      double d = 0.2873;
      if (Module.fullNullCheck()) {
         return d;
      }

      if (Module.mc.player.method_6059(StatusEffects.SPEED)) {
         int n = Module.mc.player.method_6112(StatusEffects.SPEED).getAmplifier();
         d *= 1.0 + 0.2 * (n + 1);
      }

      if (Module.mc.player.method_6059(StatusEffects.JUMP_BOOST)) {
         int n = Module.mc.player.method_6112(StatusEffects.JUMP_BOOST).getAmplifier();
         d /= 1.0 + 0.2 * (n + 1);
      }

      if (Module.mc.player.method_6059(StatusEffects.SLOWNESS)) {
         int n = Module.mc.player.method_6112(StatusEffects.SLOWNESS).getAmplifier();
         d /= 1.0 + 0.2 * (n + 1);
      }

      return d;
   }

   public static boolean sprintIsLegit(float yaw) {
      return Math.abs(Math.abs(MathHelper.wrapDegrees(yaw)) - Math.abs(MathHelper.wrapDegrees(Managers.PLAYER.yaw))) < 40.0F;
   }
}
