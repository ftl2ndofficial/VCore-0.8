package vcore.features.modules.movement;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventFixVelocity;
import vcore.events.impl.EventKeyboardInput;
import vcore.events.impl.EventPlayerJump;
import vcore.events.impl.EventPlayerTravel;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;

public class MoveFix extends Module {
   private final Setting<MoveFix.Mode> mode = new Setting<>("Mode", MoveFix.Mode.Focused);
   public float fixRotation = Float.NaN;
   private float prevYaw;
   private float prevPitch;

   public MoveFix() {
      super("MoveFix", "Keeps movement aligned to rotations.", Module.Category.MOVEMENT);
   }

   public void onJump(EventPlayerJump e) {
      if (!this.isAutoCartMoveFixActive() && !this.shouldSkipAuraMoveFix() && !this.isDisabled() && !Float.isNaN(this.fixRotation) && !mc.player.isRiding()) {
         if (e.isPre()) {
            this.prevYaw = mc.player.method_36454();
            mc.player.method_36456(this.fixRotation);
         } else {
            mc.player.method_36456(this.prevYaw);
         }
      }
   }

   public void onPlayerMove(EventFixVelocity event) {
      if (!this.isAutoCartMoveFixActive() && !this.shouldSkipAuraMoveFix() && !this.isDisabled() && this.mode.getValue() == MoveFix.Mode.Free) {
         if (!Float.isNaN(this.fixRotation) && !mc.player.isRiding()) {
            event.setVelocity(this.fix(this.fixRotation, event.getMovementInput(), event.getSpeed()));
         }
      }
   }

   public void modifyVelocity(EventPlayerTravel e) {
      if (!this.isAutoCartMoveFixActive() && !this.shouldSkipAuraMoveFix() && !this.isDisabled()) {
         if (ModuleManager.aura.isEnabled()
            && Aura.target != null
            && ModuleManager.aura.rotationMode.not(Aura.Mode.None)
            && mc.player != null
            && mc.player.method_6128()
            && Managers.PLAYER.ticksElytraFlying > 5) {
            if (e.isPre()) {
               this.prevYaw = mc.player.method_36454();
               this.prevPitch = mc.player.method_36455();
               mc.player.method_36456(this.fixRotation);
               mc.player.method_36457(ModuleManager.aura.rotationPitch);
            } else {
               mc.player.method_36456(this.prevYaw);
               mc.player.method_36457(this.prevPitch);
            }
         } else {
            if (this.mode.getValue() == MoveFix.Mode.Focused && !Float.isNaN(this.fixRotation) && !mc.player.isRiding()) {
               this.applyFocusedRotation(e);
            } else if (this.mode.getValue() == MoveFix.Mode.Full && !Float.isNaN(this.fixRotation) && !mc.player.isRiding()) {
               this.applyFocusedRotation(e);
            }
         }
      }
   }

   public void onKeyInput(EventKeyboardInput e) {
      if (!this.isAutoCartMoveFixActive() && !this.shouldSkipAuraMoveFix() && !this.isDisabled()) {
         if (!Float.isNaN(this.fixRotation) && !mc.player.isRiding()) {
            if (this.mode.getValue() == MoveFix.Mode.Free) {
               float mF = mc.player.input.movementForward;
               float mS = mc.player.input.movementSideways;
               float delta = (mc.player.method_36454() - this.fixRotation) * (float) (Math.PI / 180.0);
               float cos = MathHelper.cos(delta);
               float sin = MathHelper.sin(delta);
               mc.player.input.movementSideways = Math.round(mS * cos - mF * sin);
               mc.player.input.movementForward = Math.round(mF * cos + mS * sin);
            } else if (this.mode.getValue() != MoveFix.Mode.Focused) {
               if (this.mode.getValue() == MoveFix.Mode.Full) {
                  this.applyFullFocusedInput();
               }
            }
         }
      }
   }

   private void applyFocusedRotation(EventPlayerTravel e) {
      if (e.isPre()) {
         this.prevYaw = mc.player.method_36454();
         this.prevPitch = mc.player.method_36455();
         mc.player.method_36456(this.fixRotation);
         if (ModuleManager.aura != null && Aura.target != null) {
            mc.player.method_36457(ModuleManager.aura.rotationPitch);
         }
      } else {
         mc.player.method_36456(this.prevYaw);
         mc.player.method_36457(this.prevPitch);
      }
   }

   private void applyFullFocusedInput() {
      if (ModuleManager.aura != null && Aura.target != null) {
         Entity target = Aura.target;
         float forward = mc.player.input.movementForward;
         float strafe = mc.player.input.movementSideways;
         float directionYaw = this.getQuantizedYawTo(target.getPos());
         float angle = MathHelper.wrapDegrees((float)Math.toDegrees(direction(directionYaw, forward, strafe)));
         if (forward != 0.0F || strafe != 0.0F) {
            float closestForward = 0.0F;
            float closestStrafe = 0.0F;
            float closestDifference = Float.MAX_VALUE;

            for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
               for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                  if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                     float predictedAngle = MathHelper.wrapDegrees((float)Math.toDegrees(direction(this.fixRotation, predictedForward, predictedStrafe)));
                     float difference = Math.abs(angle - predictedAngle);
                     if (difference < closestDifference) {
                        closestDifference = difference;
                        closestForward = predictedForward;
                        closestStrafe = predictedStrafe;
                     }
                  }
               }
            }

            mc.player.input.movementForward = closestForward;
            mc.player.input.movementSideways = closestStrafe;
         }
      }
   }

   private float getQuantizedYawTo(Vec3d targetPos) {
      double posX = targetPos.x - mc.player.method_23317();
      double posY = targetPos.y - (mc.player.method_23318() + mc.player.method_18381(mc.player.method_18376()));
      double posZ = targetPos.z - mc.player.method_23321();
      double sqrt = MathHelper.sqrt((float)(posX * posX + posZ * posZ));
      float yaw = (float)(Math.atan2(posZ, posX) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-(Math.atan2(posY, sqrt) * 180.0 / Math.PI));
      float sens = (float)(Math.pow((Double)mc.options.getMouseSensitivity().getValue(), 1.5) * 0.05 + 0.1);
      float pow = sens * sens * sens * 1.2F;
      yaw -= yaw % pow;
      pitch -= pitch % (pow * sens);
      return yaw;
   }

   private boolean isAutoCartMoveFixActive() {
      return ModuleManager.autoCart != null && ModuleManager.autoCart.isAutoCartMoveFixActive();
   }

   private boolean shouldSkipAuraMoveFix() {
      return ModuleManager.aura != null
         && ModuleManager.aura.externalPause
         && ModuleManager.aura.isEnabled()
         && Aura.target != null
         && ModuleManager.aura.rotationMode.not(Aura.Mode.None);
   }

   private static double direction(float rotationYaw, double moveForward, double moveStrafing) {
      if (moveForward < 0.0) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0) {
         forward = -0.5F;
      } else if (moveForward > 0.0) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians(rotationYaw);
   }

   private Vec3d fix(float yaw, Vec3d movementInput, float speed) {
      double d = movementInput.lengthSquared();
      if (d < 1.0E-7) {
         return Vec3d.ZERO;
      }

      Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
      float f = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
      float g = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
      return new Vec3d(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
   }

   public enum Mode {
      Free,
      Focused,
      Full;
   }
}
