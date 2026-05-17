package vcore.features.modules.combat.aura.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.player.PlayerManager;
import vcore.features.modules.combat.Aura;
import vcore.utility.player.PlayerUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class AuraRotationManager {
   private final Aura aura;
   private final RotationModeHandler trackRotation = new Track();
   private final RotationModeHandler snapRotation = new Snap();
   private final RotationModeHandler grimRotation = new Grim();
   private final RotationModeHandler noneRotation = new None();

   public AuraRotationManager(Aura aura) {
      this.aura = aura;
   }

   private RotationModeHandler getModeHandler() {
      return switch ((Aura.Mode)this.aura.rotationMode.getValue()) {
         case Track -> this.trackRotation;
         case Grim -> this.grimRotation;
         case Snap -> this.snapRotation;
         case None -> this.noneRotation;
      };
   }

   public void rotate(boolean ready) {
      float previousYaw = this.aura.rotationYaw;
      float previousPitch = this.aura.rotationPitch;
      this.getModeHandler().rotate(this.aura, ready);
      this.aura.captureRotationState(previousYaw, previousPitch);
   }

   public boolean skipRayTraceCheck() {
      return this.aura.rotationMode.getValue() == Aura.Mode.None
         || !this.aura.rayTrace.getValue()
         || this.aura.rotationMode.is(Aura.Mode.Snap)
            && (
               this.aura.snapTicks.getValue() <= 1
                  || Aura.mc
                     .world
                     .method_20812(Aura.mc.player, Aura.mc.player.method_5829().expand(-0.25, 0.0, -0.25).offset(0.0, 1.0, 0.0))
                     .iterator()
                     .hasNext()
            );
   }

   public void onRender3D() {
      if (this.aura.clientLook.getValue() && this.aura.rotationMode.getValue() != Aura.Mode.None) {
         Aura.mc
            .player
            .method_36456((float)Render2DEngine.interpolate(Aura.mc.player.field_5982, this.aura.getRenderRotationYaw(), Render3DEngine.getTickDelta()));
         Aura.mc
            .player
            .method_36457((float)Render2DEngine.interpolate(Aura.mc.player.field_6004, this.aura.getRenderRotationPitch(), Render3DEngine.getTickDelta()));
      }
   }

   public boolean canAttackElytraTarget(LivingEntity livingTarget) {
      Vec3d targetVec = this.aura.getElytraTargetVec(livingTarget, true);
      if (targetVec == null) {
         return false;
      }

      double currentDistance = Aura.mc.player.method_19538().distanceTo(targetVec);
      double prediction = ModuleManager.elytraTarget.getPrediction(livingTarget);
      return currentDistance <= prediction - 0.5 || Aura.mc.player.method_5829().intersects(new Box(targetVec, targetVec));
   }

   public boolean isWallsBypassYawOffset() {
      return ModuleManager.wallsBypass.isEnabled() && ModuleManager.wallsBypass.isYawOffset();
   }

   public boolean isWallsBypassPeekHigh() {
      return ModuleManager.wallsBypass.isEnabled() && ModuleManager.wallsBypass.isPeekHigh();
   }

   public float getSquaredRotateDistance() {
      float dst = this.aura.getRange();
      boolean needsAimRange = this.aura.rotationMode.getValue() == Aura.Mode.Track || this.aura.rotationMode.getValue() == Aura.Mode.Grim;
      if (needsAimRange) {
         dst += this.aura.getAimRange();
      }

      if ((Aura.mc.player.method_6128() || ModuleManager.elytraPlus.isEnabled()) && Aura.target != null) {
         dst += 4.0F;
      }

      if (!needsAimRange) {
         dst = this.aura.getRange();
      }

      return dst * dst;
   }

   public boolean isInRange(Entity target) {
      if (target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
         Vec3d targetVec = this.aura.getElytraTargetVec(livingTarget, true);
         return targetVec != null && PlayerUtility.squaredDistanceFromEyes(targetVec) <= this.getSquaredRotateDistance();
      } else {
         if (PlayerUtility.squaredDistanceFromEyes(target.getPos().add(0.0, target.getEyeHeight(target.getPose()), 0.0))
            > this.getSquaredRotateDistance() + 4.0F) {
            return false;
         }

         float halfBox = (float)(target.method_5829().getLengthX() / 2.0);

         for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.15F) {
            for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.15F) {
               for (float y1 = 0.05F; y1 <= target.method_5829().getLengthY(); y1 += 0.25F) {
                  if (!(
                     PlayerUtility.squaredDistanceFromEyes(new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1))
                        > this.getSquaredRotateDistance()
                  )) {
                     float[] rotation = PlayerManager.calcAngle(new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1));
                     if (Managers.PLAYER
                        .checkRtx(
                           rotation[0], rotation[1], (float)Math.sqrt(this.getSquaredRotateDistance()), this.aura.getWallRange(), this.aura.rayTrace.getValue()
                        )) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }
}
