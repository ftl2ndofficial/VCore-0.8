package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventFireworkMotion;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.SettingGroup;

public final class ElytraBoost extends Module {
   private static final int[] YAW_VECTORS = new int[]{-45, 45, 135, -135};
   private static final int[] PITCH_VECTORS = new int[]{-45, 45};
   private final Setting<Boolean> smartSpeed = new Setting<>("SmartSpeed", false);
   private final Setting<Float> fireworkSpeed = new Setting<>("FireworkSpeed", 1.6F, 1.5F, 2.0F, v -> !this.smartSpeed.getValue());
   private final Setting<SettingGroup> mode = new Setting<>("Mode", new SettingGroup(false, 0));
   private final Setting<Boolean> modeV1 = new Setting<>("V1", false).addToGroup(this.mode);
   private final Setting<Boolean> modeV2 = new Setting<>("V2", false).addToGroup(this.mode);
   private final Setting<Boolean> matrix = new Setting<>("Matrix", false, v -> this.smartSpeed.getValue() && this.modeV1.getValue());
   private final Setting<Boolean> untrusted = new Setting<>(
      "Untrusted", false, v -> this.smartSpeed.getValue() && this.modeV1.getValue() && this.matrix.getValue()
   );
   private final Setting<Float> fireworkSpeedMinGlobal = new Setting<>(
      "MinSpeedGlobal", 1.65F, 1.5F, 5.0F, v -> this.smartSpeed.getValue() && this.modeV2.getValue()
   );
   private final Setting<Float> fireworkSpeedMaxGlobal = new Setting<>(
      "MaxSpeedGlobal", 2.1F, 1.5F, 5.0F, v -> this.smartSpeed.getValue() && this.modeV2.getValue()
   );
   private final Setting<Float> fireworkSpeedMaxYaw = new Setting<>("MaxSpeedYaw", 1.95F, 1.5F, 5.0F, v -> this.smartSpeed.getValue() && this.modeV2.getValue());
   private final Setting<Float> fireworkSpeedMaxPitch = new Setting<>(
      "MaxSpeedPitch", 2.5F, 1.5F, 5.0F, v -> this.smartSpeed.getValue() && this.modeV2.getValue()
   );

   public ElytraBoost() {
      super("ElytraBoost", "Boosts firework power while elytra flying.", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onFireworkMotion(EventFireworkMotion event) {
      if (event != null && mc.player != null) {
         boolean boosterWorking = true;
         if (Aura.target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
            Vec3d targetVec = ModuleManager.aura.getElytraTargetVec(livingTarget, true);
            if (targetVec == null) {
               return;
            }

            double currentDistance = mc.player.method_19538().distanceTo(targetVec);
            if (currentDistance <= 1.0) {
               boosterWorking = false;
            }
         }

         double speedXZ = this.fireworkSpeed.getValue().floatValue();
         double speedY = this.fireworkSpeed.getValue().floatValue();
         if (this.smartSpeed.getValue()) {
            if (this.modeV1.getValue() && this.modeV2.getValue()) {
               speedXZ = Math.max(this.getBoostV1(), this.getBoostV2());
            } else if (this.modeV1.getValue()) {
               speedXZ = this.getBoostV1();
            } else if (this.modeV2.getValue()) {
               speedXZ = this.getBoostV2();
            }

            speedY = 1.6F;
         }

         if (this.modeV2.getValue()) {
            speedXZ = Math.max(this.fireworkSpeedMinGlobal.getValue().floatValue(), speedXZ);
            speedXZ = Math.min(this.fireworkSpeedMaxGlobal.getValue().floatValue(), speedXZ);
         }

         event.setVector(new Vec3d(speedXZ, speedY, speedXZ));
         if (boosterWorking) {
            event.cancel();
         }
      }
   }

   public double getBoostV2() {
      LivingEntity target = Aura.target instanceof LivingEntity living ? living : null;
      float lastYaw = target != null ? ModuleManager.aura.rotationYaw : mc.player.method_36454();
      float lastPitch = target != null ? ModuleManager.aura.rotationPitch : mc.player.method_36455();
      if (Math.abs(lastPitch) > 55.0F) {
         return 1.55;
      }

      double yawRad = Math.toRadians(lastYaw);
      double pitchRad = Math.toRadians(lastPitch);
      double sinYaw = Math.sin(yawRad);
      double cosYaw = Math.cos(yawRad);
      double cosPitch = Math.cos(pitchRad);
      if (cosPitch < 1.0E-6) {
         return 1.55;
      }

      double m = Math.max(Math.abs(sinYaw), Math.abs(cosYaw));
      double pitchContrib = 1.0 / cosPitch - 1.0;
      double yawContrib = 1.0 / m - 1.0;
      double a = 0.15;
      double b = 1.45;
      double desiredYawMaxBoost = this.fireworkSpeedMaxYaw.getValue().floatValue();
      double desiredPitchMaxBoost = this.fireworkSpeedMaxPitch.getValue().floatValue();
      double yawMaxContrib = (desiredYawMaxBoost - a) / b - 1.0;
      double pitchMaxContrib = (desiredPitchMaxBoost - a) / b - 1.0;
      pitchContrib = Math.min(pitchContrib, pitchMaxContrib);
      yawContrib = Math.min(yawContrib, yawMaxContrib);
      double inv = 1.0 + pitchContrib + yawContrib;
      double a2 = 0.15;
      double b2 = 1.45;
      return a2 + b2 * inv;
   }

   public double getBoostV1() {
      LivingEntity target = Aura.target instanceof LivingEntity living ? living : null;
      float lastYaw = target != null ? ModuleManager.aura.rotationYaw : mc.player.method_36454();
      float lastPitch = target != null ? ModuleManager.aura.rotationPitch : mc.player.method_36455();
      if (Math.abs(lastPitch) > 55.0F) {
         return 1.55;
      }

      float boostYaw = this.adjustBoostForYaw(lastYaw);
      double boostPitch = this.adjustBoostForPitch(lastYaw, lastPitch);
      double boost = boostYaw + (boostPitch - 1.6F);
      boost = Math.max(1.6, boost);
      return this.matrix.getValue() ? Math.min(boost, 2.1) : boost;
   }

   private float adjustBoostForYaw(float lastYaw) {
      int closestYawIndex = findClosestVector(lastYaw, YAW_VECTORS);
      if (closestYawIndex == -1) {
         return 1.6F;
      }

      float yawDistance = Math.abs(MathHelper.wrapDegrees(lastYaw) - YAW_VECTORS[closestYawIndex]);
      float maxBoost = 2.2F;
      float minBoostValue = 1.6F;
      float maxDistance = 12.0F;
      float variableSpeedSmart = 0.0F;
      if (yawDistance <= maxDistance) {
         float ratio = yawDistance / maxDistance;
         variableSpeedSmart = maxBoost - (maxBoost - minBoostValue) * ratio;
      }

      float variableSpeed = getVariableSpeed(yawDistance);
      float finalSpeed = Math.max(variableSpeedSmart, variableSpeed);
      float max = this.untrusted.getValue() ? 1.95F : 1.8F;
      return this.matrix.getValue() ? Math.min(finalSpeed, max) : finalSpeed;
   }

   private static float getVariableSpeed(float yawDistance) {
      float[] thresholds = new float[]{4.0F, 8.0F, 11.0F, 15.0F, 21.0F, 28.0F};
      float[] speeds = new float[]{2.2F, 2.1F, 2.0F, 1.9F, 1.8F, 1.7F, 1.6F};
      int level = 0;

      while (level < thresholds.length && yawDistance >= thresholds[level]) {
         level++;
      }

      return speeds[level];
   }

   private double adjustBoostForPitch(float lastYaw, float lastPitch) {
      int closestYawIndex = findClosestVector(lastPitch, PITCH_VECTORS);
      if (closestYawIndex == -1) {
         return 1.6F;
      }

      int closestYawIndex1 = findClosestVector(lastYaw, YAW_VECTORS);
      float yawDistance1 = Math.abs(MathHelper.wrapDegrees(lastYaw) - YAW_VECTORS[closestYawIndex1]);
      float yawDistance = Math.abs(MathHelper.wrapDegrees(lastPitch) - PITCH_VECTORS[closestYawIndex]);
      float maxBoost = getVariableSpeed(yawDistance);
      float minBoostValue = 1.6F;
      float maxDistance = 45.0F;
      float variableSpeedSmart = 0.0F;
      if (yawDistance <= maxDistance) {
         float ratio = yawDistance / maxDistance;
         variableSpeedSmart = maxBoost - (maxBoost - minBoostValue) * ratio;
      }

      return variableSpeedSmart;
   }

   private static int findClosestVector(float angle, int[] vectors) {
      int minDistIndex = -1;
      float minDist = Float.MAX_VALUE;

      for (int i = 0; i < vectors.length; i++) {
         float dist = Math.abs(MathHelper.wrapDegrees(angle) - vectors[i]);
         if (dist < minDist) {
            minDist = dist;
            minDistIndex = i;
         }
      }

      return minDistIndex;
   }
}
