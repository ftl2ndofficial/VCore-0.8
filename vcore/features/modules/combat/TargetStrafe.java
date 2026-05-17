package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventMove;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.MovementUtility;

public class TargetStrafe extends Module {
   public static boolean strafesCheck;
   private final Setting<TargetStrafe.Mode> mode = new Setting<>("Mode", TargetStrafe.Mode.Collision);
   public final Setting<Boolean> jump = new Setting<>("Jump", true, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Float> defaultDistance = new Setting<>("Distance", 2.4F, 0.1F, 6.0F, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Float> defaultSpeed = new Setting<>("Speed", 0.4F, 0.05F, 1.0F, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Boolean> damageBoost = new Setting<>("DamageBoost", false, v -> this.mode.is(TargetStrafe.Mode.Default));
   private final Setting<Float> boostSpeed = new Setting<>(
      "BoostSpeed", 0.5F, 0.1F, 1.5F, v -> this.mode.is(TargetStrafe.Mode.Default) && this.damageBoost.getValue()
   );
   private final Setting<Float> collisionSpeed = new Setting<>("Speed", 0.08F, 0.01F, 0.1F, v -> this.mode.is(TargetStrafe.Mode.Collision));
   private final Setting<Float> collisionDistance = new Setting<>("Distance", 1.2F, 1.0F, 6.0F, v -> this.mode.is(TargetStrafe.Mode.Collision));
   private boolean clockwise = true;
   private boolean hasQueuedMotion;
   private double queuedMotionX;
   private double queuedMotionZ;
   private int jumpTicks;

   public TargetStrafe() {
      super("TargetStrafe", "Strafes around target.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.hasQueuedMotion = false;
      strafesCheck = false;
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (event != null) {
         if (fullNullCheck()) {
            this.hasQueuedMotion = false;
            strafesCheck = false;
         } else {
            ClientPlayerEntity player = mc.player;
            ClientWorld world = mc.world;
            if (player != null && world != null) {
               this.jumpTicks = Math.max(0, this.jumpTicks - 1);
               this.hasQueuedMotion = false;
               strafesCheck = false;
               if (this.mode.is(TargetStrafe.Mode.Collision)) {
                  this.handleCollisionMode(player);
               } else {
                  this.handleDefaultMode(player, world);
               }
            } else {
               this.hasQueuedMotion = false;
               strafesCheck = false;
            }
         }
      }
   }

   @EventHandler
   public void onMove(EventMove event) {
      if (!this.mode.not(TargetStrafe.Mode.Default) && this.hasQueuedMotion) {
         event.setX(this.queuedMotionX);
         event.setZ(this.queuedMotionZ);
         event.cancel();
         this.hasQueuedMotion = false;
      }
   }

   private void handleCollisionMode(ClientPlayerEntity player) {
      if (MovementUtility.isMoving()) {
         if (Aura.target instanceof LivingEntity target) {
            if (!(player.method_5739(target) >= this.collisionDistance.getValue())) {
               Vec3d direction = target.method_19538().subtract(player.method_19538()).normalize();
               double speed = this.collisionSpeed.getValue().floatValue();
               player.method_18800(player.method_18798().x + direction.x * speed, player.method_18798().y, player.method_18798().z + direction.z * speed);
            }
         }
      }
   }

   private void handleDefaultMode(ClientPlayerEntity player, ClientWorld world) {
      if (Aura.target instanceof LivingEntity target && target.method_5805()) {
         if (this.jump.getValue() && player.method_24828()) {
            mc.options.jumpKey.setPressed(false);
            player.method_6043();
         }

         float maxDistance = ModuleManager.aura.attackRange.getValue() + ModuleManager.aura.aimRange.getValue();
         double distanceToTarget = player.method_5739(target);
         if (!(distanceToTarget > maxDistance)) {
            mc.options.forwardKey.setPressed(false);
            float speed = this.defaultSpeed.getValue();
            if (this.damageBoost.getValue() && player.field_6235 > 0 && player.method_5805()) {
               speed += this.boostSpeed.getValue();
            }

            float clampDist = (float)MathHelper.clamp(distanceToTarget, 0.01F, maxDistance);
            double baseAngle = Math.atan2(player.method_23321() - target.method_23321(), player.method_23317() - target.method_23317());
            float angleStep = MathHelper.clamp(speed / clampDist, 0.01F, 1.0F);
            double orbitAngle = baseAngle + (this.clockwise ? angleStep : -angleStep);
            double orbitX = target.method_23317() + this.defaultDistance.getValue().floatValue() * Math.cos(orbitAngle);
            double orbitZ = target.method_23321() + this.defaultDistance.getValue().floatValue() * Math.sin(orbitAngle);
            if (this.isUnsafeAreaAround(orbitX, orbitZ, player, world)) {
               this.clockwise = !this.clockwise;
               orbitAngle = baseAngle + (this.clockwise ? angleStep : -angleStep);
               orbitX = target.method_23317() + this.defaultDistance.getValue().floatValue() * Math.cos(orbitAngle);
               orbitZ = target.method_23321() + this.defaultDistance.getValue().floatValue() * Math.sin(orbitAngle);
            }

            strafesCheck = true;
            double angleTo = Math.toRadians(this.calculateAngleToTarget(orbitX, orbitZ, player));
            this.queuedMotionX = speed * -Math.sin(angleTo);
            this.queuedMotionZ = speed * Math.cos(angleTo);
            this.hasQueuedMotion = !Double.isNaN(this.queuedMotionX) && !Double.isNaN(this.queuedMotionZ);
         }
      }
   }

   private boolean isUnsafeAreaAround(double x, double z, ClientPlayerEntity player, ClientWorld world) {
      if (!player.field_5976 && (!mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed() || this.jumpTicks > 0)) {
         Mutable mutable = new Mutable();
         int top = (int)(player.method_23318() + 4.0);

         for (int y = top; y >= world.method_31607(); y--) {
            mutable.set(x, y, z);
            BlockState state = world.method_8320(mutable);
            Block block = state.method_26204();
            if (block == Blocks.LAVA || block == Blocks.FIRE || block == Blocks.COBWEB) {
               return true;
            }

            if (!state.method_26215()) {
               return false;
            }
         }

         return this.isVoidAboveVoid(x, z, player, world);
      } else {
         this.jumpTicks = 10;
         return true;
      }
   }

   private boolean isVoidAboveVoid(double x, double z, ClientPlayerEntity player, ClientWorld world) {
      Mutable mutable = new Mutable();

      for (int y = (int)player.method_23318(); y > world.method_31607(); y--) {
         mutable.set(x, y, z);
         if (!world.method_8320(mutable).method_26215()) {
            return false;
         }
      }

      return true;
   }

   private float calculateAngleToTarget(double x, double z, ClientPlayerEntity player) {
      double diffX = x - player.method_23317();
      double diffZ = z - player.method_23321();
      return (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI - 90.0);
   }

   private enum Mode {
      Default,
      Collision;
   }
}
