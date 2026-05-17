package vcore.features.modules.combat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventSync;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.math.PredictUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class AimBot extends Module {
   private final Setting<AimBot.Mode> mode = new Setting<>("Mode", AimBot.Mode.BowAim);
   private final Setting<AimBot.Rotation> rotation = new Setting<>("Rotation", AimBot.Rotation.Silent, v -> this.mode.getValue() != AimBot.Mode.AimAssist);
   private final Setting<Float> aimRange = new Setting<>("Range", 20.0F, 1.0F, 30.0F, v -> this.mode.getValue() != AimBot.Mode.AimAssist);
   private final Setting<Integer> aimStrength = new Setting<>("AimStrength", 30, 1, 100, v -> this.mode.getValue() == AimBot.Mode.AimAssist);
   private final Setting<Integer> aimSmooth = new Setting<>("AimSmooth", 45, 1, 180, v -> this.mode.getValue() == AimBot.Mode.AimAssist);
   private final Setting<Integer> aimtime = new Setting<>("AimTime", 2, 1, 10, v -> this.mode.getValue() == AimBot.Mode.AimAssist);
   private final Setting<Boolean> ignoreWalls = new Setting<>(
      "IgnoreWalls", true, v -> this.mode.getValue() == AimBot.Mode.CSAim || this.mode.is(AimBot.Mode.AimAssist)
   );
   private final Setting<Boolean> ignoreTeam = new Setting<>(
      "IgnoreTeam", true, v -> this.mode.getValue() == AimBot.Mode.CSAim || this.mode.is(AimBot.Mode.AimAssist)
   );
   private final Setting<Integer> reactionTime = new Setting<>(
      "ReactionTime", 80, 1, 500, v -> this.mode.getValue() == AimBot.Mode.AimAssist && !this.ignoreWalls.getValue()
   );
   private final Setting<Boolean> ignoreInvisible = new Setting<>("IgnoreInvis", false, v -> this.mode.is(AimBot.Mode.AimAssist));
   private final Setting<Float> rotYawRandom = new Setting<>("YawRandom", 0.0F, 0.0F, 3.0F, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Float> rotPitchRandom = new Setting<>("PitchRandom", 0.0F, 0.0F, 3.0F, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Float> predict = new Setting<>("AimPredict", 0.5F, 0.5F, 8.0F, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Integer> delay = new Setting<>("Shoot delay", 5, 0, 10, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Integer> fov = new Setting<>("FOV", 65, 10, 360, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private final Setting<Integer> predictTicks = new Setting<>("PredictTicks", 2, 0, 20, v -> this.mode.getValue() == AimBot.Mode.BowAim);
   private final Setting<AimBot.Bone> part = new Setting<>("Bone", AimBot.Bone.Head, v -> this.mode.getValue() == AimBot.Mode.CSAim);
   private Entity target;
   private float rotationYaw;
   private float rotationPitch;
   private float assistAcceleration;
   private int aimTicks = 0;
   private Timer visibleTime = new Timer();

   public AimBot() {
      super("AimBot", "Smooth legit aiming.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (this.mode.getValue() == AimBot.Mode.BowAim) {
         if (!(mc.player.method_6030().getItem() instanceof BowItem)) {
            return;
         }

         PlayerEntity nearestTarget = Managers.COMBAT.getTargetByFOV(128.0F);
         if (nearestTarget == null) {
            return;
         }

         float currentDuration = (mc.player.method_6030().getMaxUseTime(mc.player) - mc.player.method_6048()) / 20.0F;
         currentDuration = (currentDuration * currentDuration + currentDuration * 2.0F) / 3.0F;
         if (currentDuration >= 1.0F) {
            currentDuration = 1.0F;
         }

         float pitch = (float)(-Math.toDegrees(this.calculateArc(nearestTarget, currentDuration * 3.0F)));
         if (Float.isNaN(pitch)) {
            return;
         }

         PlayerEntity predictedEntity = PredictUtility.predictPlayer(nearestTarget, this.predictTicks.getValue());
         double iX = predictedEntity.method_23317() - predictedEntity.field_6014;
         double iZ = predictedEntity.method_23321() - predictedEntity.field_5969;
         double distance = mc.player.method_5739(predictedEntity);
         distance -= distance % 2.0;
         iX = distance / 2.0 * iX * (mc.player.method_5624() ? 1.3 : 1.1);
         iZ = distance / 2.0 * iZ * (mc.player.method_5624() ? 1.3 : 1.1);
         this.rotationYaw = (float)Math.toDegrees(
               Math.atan2(predictedEntity.method_23321() + iZ - mc.player.method_23321(), predictedEntity.method_23317() + iX - mc.player.method_23317())
            )
            - 90.0F;
         this.rotationPitch = pitch;
      } else if (this.mode.getValue() == AimBot.Mode.CSAim) {
         this.calcThread();
      } else {
         if (mc.crosshairTarget.getType() == Type.ENTITY) {
            this.aimTicks++;
         } else {
            this.aimTicks = 0;
         }

         if (this.aimTicks >= this.aimtime.getValue()) {
            this.assistAcceleration = 0.0F;
            return;
         }

         PlayerEntity nearestTarget = Managers.COMBAT.getNearestTarget(5.0F);
         this.assistAcceleration = this.assistAcceleration + this.aimStrength.getValue().intValue() / 10000.0F;
         if (nearestTarget != null) {
            if (!mc.player.method_6057(nearestTarget) && !this.ignoreWalls.getValue()) {
               this.visibleTime.reset();
            }

            if (!this.visibleTime.passedMs(this.reactionTime.getValue().intValue())) {
               this.rotationYaw = Float.NaN;
               return;
            }

            if (Float.isNaN(this.rotationYaw)) {
               this.rotationYaw = mc.player.method_36454();
            }

            float delta_yaw = MathHelper.wrapDegrees(
               (float)MathHelper.wrapDegrees(
                     Math.toDegrees(
                           Math.atan2(nearestTarget.method_33571().z - mc.player.method_23321(), nearestTarget.method_33571().x - mc.player.method_23317())
                        )
                        - 90.0
                  )
                  - this.rotationYaw
            );
            if (delta_yaw > 180.0F) {
               delta_yaw -= 180.0F;
            }

            float deltaYaw = MathHelper.clamp(MathHelper.abs(delta_yaw), -this.aimSmooth.getValue(), this.aimSmooth.getValue().intValue());
            float newYaw = this.rotationYaw + (delta_yaw > 0.0F ? deltaYaw : -deltaYaw);
            double gcdFix = Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
            this.rotationYaw = (float)(newYaw - (newYaw - this.rotationYaw) % gcdFix);
         } else {
            this.rotationYaw = Float.NaN;
         }
      }

      if (!Float.isNaN(this.rotationYaw)) {
         ModuleManager.moveFix.fixRotation = this.rotationYaw;
      }
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (!this.mode.is(AimBot.Mode.AimAssist)) {
         if (this.mode.is(AimBot.Mode.CSAim)) {
            if (this.target != null && (mc.player.method_6057(this.target) || this.ignoreWalls.getValue())) {
               if (mc.player.field_6012 % this.delay.getValue() == 0) {
                  event.addPostAction(
                     () -> this.sendSequencedPacket(
                        id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.method_36454(), mc.player.method_36455())
                     )
                  );
               }
            } else {
               this.rotationYaw = mc.player.method_36454();
               this.rotationPitch = mc.player.method_36455();
            }
         }

         if ((this.target != null || this.mode.getValue() == AimBot.Mode.BowAim && mc.player.method_6030().getItem() instanceof BowItem)
            && this.rotation.getValue() == AimBot.Rotation.Silent) {
            mc.player.method_36456(this.rotationYaw);
            mc.player.method_36457(this.rotationPitch);
         }
      }
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.rotationYaw = mc.player.method_36454();
      this.rotationPitch = mc.player.method_36455();
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.mode.getValue() == AimBot.Mode.AimAssist) {
         if (!Float.isNaN(this.rotationYaw)) {
            mc.player.method_36456((float)Render2DEngine.interpolate(mc.player.method_36454(), this.rotationYaw, this.assistAcceleration));
         }
      } else {
         if (this.target != null && (mc.player.method_6057(this.target) || this.ignoreWalls.getValue())) {
            if (this.rotation.getValue() == AimBot.Rotation.Client) {
               mc.player.method_36456((float)Render2DEngine.interpolate(mc.player.field_5982, this.rotationYaw, Render3DEngine.getTickDelta()));
               mc.player.method_36457((float)Render2DEngine.interpolate(mc.player.field_6004, this.rotationPitch, Render3DEngine.getTickDelta()));
            }
         } else if (this.mode.getValue() == AimBot.Mode.CSAim) {
            this.rotationYaw = mc.player.method_36454();
            this.rotationPitch = mc.player.method_36455();
         }

         if (this.rotation.getValue() == AimBot.Rotation.Client
            && this.mode.getValue() == AimBot.Mode.BowAim
            && mc.player.method_6030().getItem() instanceof BowItem) {
            mc.player.method_36456((float)Render2DEngine.interpolate(mc.player.field_5982, this.rotationYaw, Render3DEngine.getTickDelta()));
            mc.player.method_36457((float)Render2DEngine.interpolate(mc.player.field_6004, this.rotationPitch, Render3DEngine.getTickDelta()));
         }
      }
   }

   private float calculateArc(@NotNull PlayerEntity target, double duration) {
      double yArc = target.method_23318()
         + target.method_18381(target.method_18376())
         - (mc.player.method_23318() + mc.player.method_18381(mc.player.method_18376()));
      double dX = target.method_23317() - mc.player.method_23317();
      double dZ = target.method_23321() - mc.player.method_23321();
      double dirRoot = Math.sqrt(dX * dX + dZ * dZ);
      return this.calculateArc(duration, dirRoot, yArc);
   }

   private float calculateArc(double d, double dr, double y) {
      y = 2.0 * y * (d * d);
      y = 0.05F * (0.05F * (dr * dr) + y);
      y = Math.sqrt(d * d * d * d - y);
      d = d * d - y;
      y = Math.atan2(d * d + y, 0.05F * dr);
      d = Math.atan2(d, 0.05F * dr);
      return (float)Math.min(y, d);
   }

   private void calcThread() {
      if (this.target == null) {
         this.findTarget();
      } else if (this.skipEntity(this.target)) {
         this.target = null;
      } else {
         Vec3d targetVec = this.getResolvedPos(this.target).add(0.0, this.part.getValue().getH(), 0.0);
         if (targetVec != null) {
            float delta_yaw = MathHelper.wrapDegrees(
               (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(targetVec.z - mc.player.method_23321(), targetVec.x - mc.player.method_23317())) - 90.0)
                  - this.rotationYaw
            );
            float delta_pitch = (float)(
                  -Math.toDegrees(
                     Math.atan2(
                        targetVec.y - (mc.player.method_19538().y + mc.player.method_18381(mc.player.method_18376())),
                        Math.sqrt(Math.pow(targetVec.x - mc.player.method_23317(), 2.0) + Math.pow(targetVec.z - mc.player.method_23321(), 2.0))
                     )
                  )
               )
               - this.rotationPitch;
            if (delta_yaw > 180.0F) {
               delta_yaw -= 180.0F;
            }

            float deltaYaw = MathHelper.clamp(MathHelper.abs(delta_yaw), MathUtility.random(-40.0F, -60.0F), MathUtility.random(40.0F, 60.0F));
            float newYaw = this.rotationYaw
               + (delta_yaw > 0.0F ? deltaYaw : -deltaYaw)
               + MathUtility.random(-this.rotYawRandom.getValue(), this.rotYawRandom.getValue());
            float newPitch = MathHelper.clamp(
                  this.rotationPitch + MathHelper.clamp(delta_pitch, MathUtility.random(-10.0F, -20.0F), MathUtility.random(10.0F, 20.0F)), -90.0F, 90.0F
               )
               + MathUtility.random(-this.rotPitchRandom.getValue(), this.rotPitchRandom.getValue());
            double gcdFix = Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 8.0 * 0.15F;
            this.rotationYaw = (float)(newYaw - (newYaw - this.rotationYaw) % gcdFix);
            this.rotationPitch = (float)(newPitch - (newPitch - this.rotationPitch) % gcdFix);
         }
      }
   }

   public void findTarget() {
      List<Entity> first_stage = new CopyOnWriteArrayList<>();

      for (Entity entity : mc.world.getEntities()) {
         if (!this.skipEntity(entity)) {
            first_stage.add(entity);
         }
      }

      float best_fov = this.fov.getValue().intValue();
      Entity best_entity = null;

      for (Entity ent : first_stage) {
         float temp_fov = Math.abs(
            (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(ent.getZ() - mc.player.method_23321(), ent.getX() - mc.player.method_23317())) - 90.0)
               - MathHelper.wrapDegrees(mc.player.method_36454())
         );
         if (temp_fov < best_fov) {
            best_entity = ent;
            best_fov = temp_fov;
         }
      }

      this.target = best_entity;
   }

   private boolean skipEntity(Entity entity) {
      if (entity instanceof LivingEntity ent) {
         if (ent.isDead()) {
            return true;
         }

         if (!entity.isAlive()) {
            return true;
         }

         if (entity instanceof ArmorStandEntity) {
            return true;
         }

         if (ModuleManager.antiBot.isEnabled() && ModuleManager.antiBot.mode.getValue() == AntiBot.Mode.Matrix && AntiBot.isBot(entity)) {
            return true;
         }

         if (entity instanceof PlayerEntity pl) {
            if (entity == mc.player) {
               return true;
            } else if (entity.isInvisible() && this.ignoreInvisible.getValue()) {
               return true;
            } else if (Managers.FRIEND.isFriend(pl)) {
               return true;
            } else if (Math.abs(this.getYawToEntityNew(entity)) > this.fov.getValue().intValue()) {
               return true;
            } else {
               return pl.method_22861() == mc.player.method_22861() && this.ignoreTeam.getValue() && mc.player.method_22861() != 16777215
                  ? true
                  : mc.player.method_5707(this.getResolvedPos(entity)) > this.aimRange.getPow2Value();
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public float getYawToEntityNew(@NotNull Entity entity) {
      return this.getYawBetween(mc.player.method_36454(), mc.player.method_23317(), mc.player.method_23321(), entity.getX(), entity.getZ());
   }

   public float getYawBetween(float yaw, double srcX, double srcZ, double destX, double destZ) {
      double xDist = destX - srcX;
      double zDist = destZ - srcZ;
      float yaw1 = (float)(StrictMath.atan2(zDist, xDist) * 180.0 / Math.PI) - 90.0F;
      return yaw + MathHelper.wrapDegrees(yaw1 - yaw);
   }

   private Vec3d getResolvedPos(@NotNull Entity pl) {
      return new Vec3d(
         pl.getX() + (pl.getX() - pl.prevX) * this.predict.getValue().floatValue(),
         pl.getY(),
         pl.getZ() + (pl.getZ() - pl.prevZ) * this.predict.getValue().floatValue()
      );
   }

   private enum Bone {
      Head(1.7F),
      Neck(1.5F),
      Torso(1.0F),
      Tights(0.8F),
      Feet(0.25F);

      private final float h;

      Bone(float h) {
         this.h = h;
      }

      public float getH() {
         return this.h;
      }
   }

   private enum Mode {
      CSAim,
      AimAssist,
      BowAim;
   }

   private enum Rotation {
      Client,
      Silent;
   }
}
