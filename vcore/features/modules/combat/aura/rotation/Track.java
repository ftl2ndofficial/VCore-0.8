package vcore.features.modules.combat.aura.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.player.PlayerManager;
import vcore.features.modules.combat.Aura;
import vcore.utility.math.MathUtility;
import vcore.utility.player.PlayerUtility;

public class Track implements RotationModeHandler {
   @Override
   public void rotate(Aura aura, boolean ready) {
      rotateClassic(aura, ready);
   }

   static void rotateClassic(Aura aura, boolean ready) {
      if (ready) {
         aura.trackticks = Aura.mc
               .world
               .method_20812(Aura.mc.player, Aura.mc.player.method_5829().expand(-0.25, 0.0, -0.25).offset(0.0, 1.0, 0.0))
               .iterator()
               .hasNext()
            ? 1
            : aura.snapTicks.getValue();
      } else if (aura.trackticks > 0) {
         aura.trackticks--;
      }

      if (Aura.target != null) {
         Entity targetEntity = Aura.target;
         Vec3d targetVec;
         if (!Aura.mc.player.method_6128() && !ModuleManager.elytraPlus.isEnabled()) {
            targetVec = getDynamicLookPoint(aura, targetEntity);
         } else if (targetEntity instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
            targetVec = aura.getElytraTargetVec(livingTarget, true);
         } else {
            targetVec = targetEntity.getEyePos();
         }

         targetVec = ModuleManager.eMaceHelper.applyPeakAssistAimOffset(targetEntity, targetVec);
         if (targetVec != null) {
            aura.pitchAcceleration = Managers.PLAYER
                  .checkRtx(
                     aura.rotationYaw, aura.rotationPitch, aura.getRange() + aura.getAimRange(), aura.getRange() + aura.getAimRange(), aura.rayTrace.getValue()
                  )
               ? aura.aimedPitchStep.getValue()
               : (
                  aura.pitchAcceleration < aura.maxPitchStep.getValue()
                     ? aura.pitchAcceleration * aura.pitchAccelerate.getValue()
                     : aura.maxPitchStep.getValue()
               );
            float deltaYawRaw = MathHelper.wrapDegrees(
                  (float)MathHelper.wrapDegrees(
                        Math.toDegrees(Math.atan2(targetVec.z - Aura.mc.player.method_23321(), targetVec.x - Aura.mc.player.method_23317())) - 90.0
                     )
                     - aura.rotationYaw
               )
               + (aura.isWallsBypassYawOffset() && !ready && !Aura.mc.player.method_6057(targetEntity) ? 20 : 0);
            float deltaPitch = (float)(
               -Math.toDegrees(
                     Math.atan2(
                        targetVec.y - (Aura.mc.player.method_19538().y + Aura.mc.player.method_18381(Aura.mc.player.method_18376())),
                        Math.sqrt(Math.pow(targetVec.x - Aura.mc.player.method_23317(), 2.0) + Math.pow(targetVec.z - Aura.mc.player.method_23321(), 2.0))
                     )
                  )
                  - aura.rotationPitch
            );
            float yawStep = aura.rotationMode.getValue() != Aura.Mode.Track
               ? 360.0F
               : MathUtility.random(aura.minYawStep.getValue().intValue(), aura.maxYawStep.getValue().intValue());
            float pitchStep = aura.rotationMode.getValue() != Aura.Mode.Track
               ? 180.0F
               : (Managers.PLAYER.ticksElytraFlying > 5 ? 180.0F : aura.pitchAcceleration + MathUtility.random(-1.0F, 1.0F));
            if (ready) {
               switch ((Aura.AccelerateOnHit)aura.accelerateOnHit.getValue()) {
                  case Off:
                  default:
                     break;
                  case Yaw:
                     yawStep = 180.0F;
                     break;
                  case Pitch:
                     pitchStep = 90.0F;
                     break;
                  case Both:
                     yawStep = 180.0F;
                     pitchStep = 90.0F;
               }
            }

            if (deltaYawRaw > 180.0F) {
               deltaYawRaw -= 180.0F;
            }

            float deltaYaw = MathHelper.clamp(MathHelper.abs(deltaYawRaw), -yawStep, yawStep);
            float clampedPitch = MathHelper.clamp(deltaPitch, -pitchStep, pitchStep);
            float newYaw = aura.rotationYaw + (deltaYawRaw > 0.0F ? deltaYaw : -deltaYaw);
            float newPitch = MathHelper.clamp(aura.rotationPitch + clampedPitch, -90.0F, 90.0F);
            double gcdFix = Math.pow((Double)Aura.mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
            if (aura.trackticks <= 0 && aura.rotationMode.getValue() != Aura.Mode.Track) {
               aura.rotationYaw = Aura.mc.player.method_36454();
               aura.rotationPitch = Aura.mc.player.method_36455();
            } else {
               aura.rotationYaw = (float)(newYaw - (newYaw - aura.rotationYaw) % gcdFix);
               aura.rotationPitch = (float)(newPitch - (newPitch - aura.rotationPitch) % gcdFix);
            }

            ModuleManager.moveFix.fixRotation = aura.rotationYaw;
            aura.lookingAtHitbox = Managers.PLAYER
               .checkRtx(aura.rotationYaw, aura.rotationPitch, aura.getRange(), aura.getWallRange(), aura.rayTrace.getValue());
            if (targetEntity instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
               aura.lookingAtHitbox = aura.canAttackElytraTarget(livingTarget);
            }
         }
      }
   }

   private static Vec3d getDynamicLookPoint(Aura aura, Entity targetEntity) {
      float minMotionXZ = 0.003F;
      float maxMotionXZ = 0.03F;
      float minMotionY = 0.001F;
      float maxMotionY = 0.03F;
      double lengthX = targetEntity.method_5829().getLengthX();
      double lengthY = targetEntity.method_5829().getLengthY();
      double lengthZ = targetEntity.method_5829().getLengthZ();
      if (aura.rotationMotion.equals(Vec3d.ZERO)) {
         aura.rotationMotion = new Vec3d(MathUtility.random(-0.05F, 0.05F), MathUtility.random(-0.05F, 0.05F), MathUtility.random(-0.05F, 0.05F));
      }

      aura.rotationPoint = aura.rotationPoint.add(aura.rotationMotion);
      if (aura.rotationPoint.x >= (lengthX - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3d(-MathUtility.random(minMotionXZ, maxMotionXZ), aura.rotationMotion.method_10214(), aura.rotationMotion.method_10215());
      }

      if (aura.rotationPoint.y >= lengthY) {
         aura.rotationMotion = new Vec3d(aura.rotationMotion.method_10216(), -MathUtility.random(minMotionY, maxMotionY), aura.rotationMotion.method_10215());
      }

      if (aura.rotationPoint.z >= (lengthZ - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3d(aura.rotationMotion.method_10216(), aura.rotationMotion.method_10214(), -MathUtility.random(minMotionXZ, maxMotionXZ));
      }

      if (aura.rotationPoint.x <= -(lengthX - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3d(MathUtility.random(minMotionXZ, 0.03F), aura.rotationMotion.method_10214(), aura.rotationMotion.method_10215());
      }

      if (aura.rotationPoint.y <= 0.05) {
         aura.rotationMotion = new Vec3d(aura.rotationMotion.method_10216(), MathUtility.random(minMotionY, maxMotionY), aura.rotationMotion.method_10215());
      }

      if (aura.rotationPoint.z <= -(lengthZ - 0.05) / 2.0) {
         aura.rotationMotion = new Vec3d(aura.rotationMotion.method_10216(), aura.rotationMotion.method_10214(), MathUtility.random(minMotionXZ, maxMotionXZ));
      }

      aura.rotationPoint.add(MathUtility.random(-0.03F, 0.03F), 0.0, MathUtility.random(-0.03F, 0.03F));
      if (aura.igoneWall.getValue() && !Aura.mc.player.method_6057(targetEntity) && aura.isWallsBypassPeekHigh()) {
         return targetEntity.getPos().add(MathUtility.random(-0.15, 0.15), lengthY, MathUtility.random(-0.15, 0.15));
      }

      if (!Managers.PLAYER.checkRtx(aura.rotationYaw, aura.rotationPitch, aura.getRange(), aura.getWallRange(), aura.rayTrace.getValue())) {
         float[] baseRotation = PlayerManager.calcAngle(targetEntity.getPos().add(0.0, targetEntity.getEyeHeight(targetEntity.getPose()) / 2.0F, 0.0));
         if (PlayerUtility.squaredDistanceFromEyes(targetEntity.getPos().add(0.0, targetEntity.getEyeHeight(targetEntity.getPose()) / 2.0F, 0.0))
               <= aura.attackRange.getPow2Value()
            && Managers.PLAYER.checkRtx(baseRotation[0], baseRotation[1], aura.getRange(), 0.0F, aura.rayTrace.getValue())) {
            aura.rotationPoint = new Vec3d(
               MathUtility.random(-0.1F, 0.1F),
               targetEntity.getEyeHeight(targetEntity.getPose()) / MathUtility.random(1.8F, 2.5F),
               MathUtility.random(-0.1F, 0.1F)
            );
         } else {
            float halfBox = (float)(lengthX / 2.0);

            for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.05F) {
               for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.05F) {
                  for (float y1 = 0.05F; y1 <= targetEntity.method_5829().getLengthY(); y1 += 0.15F) {
                     Vec3d point = new Vec3d(targetEntity.getX() + x1, targetEntity.getY() + y1, targetEntity.getZ() + z1);
                     if (!(PlayerUtility.squaredDistanceFromEyes(point) > aura.attackRange.getPow2Value())) {
                        float[] rotation = PlayerManager.calcAngle(point);
                        if (Managers.PLAYER.checkRtx(rotation[0], rotation[1], aura.getRange(), 0.0F, aura.rayTrace.getValue())) {
                           aura.rotationPoint = new Vec3d(x1, y1, z1);
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      return targetEntity.getPos().add(aura.rotationPoint);
   }
}
