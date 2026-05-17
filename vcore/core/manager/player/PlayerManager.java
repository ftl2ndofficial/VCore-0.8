package vcore.core.manager.player;

import java.util.ArrayDeque;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventFixVelocity;
import vcore.events.impl.EventKeyboardInput;
import vcore.events.impl.EventPlayerJump;
import vcore.events.impl.EventPlayerTravel;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSync;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.world.ExplosionUtility;

public class PlayerManager implements IManager {
   public float yaw;
   public float pitch;
   public float lastYaw;
   public float lastPitch;
   public float currentPlayerSpeed;
   public float averagePlayerSpeed;
   public int ticksElytraFlying;
   public int serverSideSlot;
   public final Timer switchTimer = new Timer();
   private final ArrayDeque<Float> speedResult = new ArrayDeque<>(20);
   public float bodyYaw;
   public float prevBodyYaw;
   public boolean inInventory;

   @EventHandler(priority = 200)
   public void onSync(EventSync event) {
      if (!Module.fullNullCheck()) {
         this.yaw = mc.player.method_36454();
         this.pitch = mc.player.method_36455();
         this.lastYaw = ((IClientPlayerEntity)mc.player).getLastYaw();
         this.lastPitch = ((IClientPlayerEntity)mc.player).getLastPitch();
         if (mc.currentScreen == null) {
            this.inInventory = false;
         }

         if (mc.player.method_6128() && mc.player.method_6118(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            this.ticksElytraFlying++;
         } else {
            this.ticksElytraFlying = 0;
         }
      }
   }

   @EventHandler
   public void onTick(EventTick e) {
      this.currentPlayerSpeed = (float)Math.hypot(mc.player.method_23317() - mc.player.field_6014, mc.player.method_23321() - mc.player.field_5969);
      if (this.speedResult.size() > 20) {
         this.speedResult.poll();
      }

      this.speedResult.add(this.currentPlayerSpeed);
      float average = 0.0F;

      for (Float value : this.speedResult) {
         average += MathUtility.clamp(value, 0.0F, 20.0F);
      }

      this.averagePlayerSpeed = average / this.speedResult.size();
   }

   @EventHandler(priority = -200)
   public void postSync(EventPostSync event) {
      if (mc.player != null) {
         this.prevBodyYaw = this.bodyYaw;
         this.bodyYaw = this.getBodyYaw();
         mc.player.method_36456(this.yaw);
         mc.player.method_36457(this.pitch);
         ModuleManager.moveFix.fixRotation = Float.NaN;
      }
   }

   @EventHandler
   public void onJump(EventPlayerJump e) {
      ModuleManager.moveFix.onJump(e);
   }

   @EventHandler
   public void onPlayerMove(EventFixVelocity e) {
      ModuleManager.moveFix.onPlayerMove(e);
   }

   @EventHandler
   public void modifyVelocity(EventPlayerTravel e) {
      ModuleManager.moveFix.modifyVelocity(e);
   }

   @EventHandler
   public void onKeyInput(EventKeyboardInput e) {
      ModuleManager.moveFix.onKeyInput(e);
   }

   @EventHandler
   public void onSyncWithServer(PacketEvent.@NotNull Send event) {
      if (event.getPacket() instanceof ClickSlotC2SPacket) {
         this.inInventory = true;
      }

      if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket slot) {
         this.switchTimer.reset();
         this.serverSideSlot = slot.getSelectedSlot();
      }

      if (event.getPacket() instanceof CloseHandledScreenC2SPacket) {
         this.inInventory = false;
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.@NotNull Receive event) {
      if (event.getPacket() instanceof UpdateSelectedSlotS2CPacket slot) {
         this.switchTimer.reset();
         this.serverSideSlot = slot.getSlot();
      }
   }

   private float getBodyYaw() {
      double x = mc.player.method_23317() - mc.player.field_6014;
      double z = mc.player.method_23321() - mc.player.field_5969;
      float offset = this.bodyYaw;
      if (x * x + z * z > 0.0025000002F) {
         offset = (float)(MathHelper.atan2(z, x) * 180.0F / (float)Math.PI - 90.0);
      }

      if (mc.player.field_6251 > 0.0F) {
         offset = ((IClientPlayerEntity)MinecraftClient.getInstance().player).getLastYaw();
      }

      float deltaBodyYaw = MathHelper.clamp(
         MathHelper.wrapDegrees(
            ((IClientPlayerEntity)MinecraftClient.getInstance().player).getLastYaw() - (this.bodyYaw + MathHelper.wrapDegrees(offset - this.bodyYaw) * 0.3F)
         ),
         -45.0F,
         75.0F
      );
      return (deltaBodyYaw > 50.0F ? deltaBodyYaw * 0.2F : 0.0F) + ((IClientPlayerEntity)MinecraftClient.getInstance().player).getLastYaw() - deltaBodyYaw;
   }

   public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, boolean rayTraceEnabled) {
      if (!rayTraceEnabled) {
         return true;
      }

      HitResult result = this.rayTrace(distance, yaw, pitch);
      Vec3d startPoint = mc.player.method_19538().add(0.0, mc.player.method_18381(mc.player.method_18376()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      if (result != null) {
         distancePow2 = startPoint.squaredDistanceTo(result.getPos());
      }

      Vec3d rotationVector = this.getRotationVector(pitch, yaw).multiply(distance);
      Vec3d endPoint = startPoint.add(rotationVector);
      Box entityArea = mc.player.method_5829().stretch(rotationVector).expand(1.0, 1.0, 1.0);
      double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2.0));
      EntityHitResult ehr = ProjectileUtil.raycast(
         mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.canHit() && (Aura.target == null || e == Aura.target), maxDistance
      );
      if (ehr != null) {
         boolean allowedWallDistance = startPoint.squaredDistanceTo(ehr.method_17784()) <= Math.pow(wallDistance, 2.0);
         boolean wallMissing = result == null;
         boolean wallBehindEntity = startPoint.squaredDistanceTo(ehr.method_17784()) < distancePow2;
         boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;
         if (allowWallHit && startPoint.squaredDistanceTo(ehr.method_17784()) <= Math.pow(distance, 2.0)) {
            return Aura.target == null || ehr.getEntity() == Aura.target;
         }
      }

      return false;
   }

   public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
      HitResult result = this.rayTrace(distance, yaw, pitch);
      Vec3d startPoint = mc.player.method_19538().add(0.0, mc.player.method_18381(mc.player.method_18376()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      if (result != null) {
         distancePow2 = startPoint.squaredDistanceTo(result.getPos());
      }

      Vec3d rotationVector = this.getRotationVector(pitch, yaw).multiply(distance);
      Vec3d endPoint = startPoint.add(rotationVector);
      Box entityArea = mc.player.method_5829().stretch(rotationVector).expand(1.0, 1.0, 1.0);
      double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2.0));
      EntityHitResult ehr = ProjectileUtil.raycast(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.canHit() && e == entity, maxDistance);
      if (ehr != null) {
         boolean allowedWallDistance = startPoint.squaredDistanceTo(ehr.method_17784()) <= Math.pow(wallDistance, 2.0);
         boolean wallMissing = result == null;
         boolean wallBehindEntity = startPoint.squaredDistanceTo(ehr.method_17784()) < distancePow2;
         boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;
         if (allowWallHit && startPoint.squaredDistanceTo(ehr.method_17784()) <= Math.pow(distance, 2.0)) {
            return ehr.getEntity() == entity;
         }
      }

      return false;
   }

   public Entity getRtxTarget(float yaw, float pitch, float distance, boolean ignoreWalls) {
      Entity targetedEntity = null;
      HitResult result = ignoreWalls ? null : this.rayTrace(distance, yaw, pitch);
      Vec3d vec3d = mc.player.method_19538().add(0.0, mc.player.method_18381(mc.player.method_18376()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      if (result != null) {
         distancePow2 = result.getPos().squaredDistanceTo(vec3d);
      }

      Vec3d vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
      Box box = mc.player.method_5829().stretch(vec3d2.multiply(distance)).expand(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.canHit(), distancePow2);
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3d vec3d4 = entityHitResult.method_17784();
         double g = vec3d.squaredDistanceTo(vec3d4);
         if ((g < distancePow2 || result == null) && entity2 instanceof LivingEntity) {
            return entity2;
         }
      }

      return targetedEntity;
   }

   public Vec3d getRtxPoint(float yaw, float pitch, float distance) {
      Vec3d vec3d = mc.player.method_19538().add(0.0, mc.player.method_18381(mc.player.method_18376()), 0.0);
      double distancePow2 = Math.pow(distance, 2.0);
      Vec3d vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
      Box box = mc.player.method_5829().stretch(vec3d2.multiply(distance)).expand(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.canHit(), distancePow2);
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3d vec3d4 = entityHitResult.method_17784();
         if (entity2 instanceof LivingEntity) {
            return vec3d4;
         }
      }

      return null;
   }

   public boolean isLookingAtBox(float yaw, float pitch, BlockPos blockPos) {
      Vec3d vec3d = mc.player.method_5836(1.0F);
      Vec3d vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * 7.0, vec3d2.y * 7.0, vec3d2.z * 7.0);
      BlockHitResult result = ExplosionUtility.rayCastBlock(new RaycastContext(vec3d, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE, mc.player), blockPos);
      return result != null && result.method_17783() == Type.BLOCK && result.getBlockPos().equals(blockPos);
   }

   public HitResult rayTrace(double dst, float yaw, float pitch) {
      Vec3d vec3d = mc.player.method_5836(1.0F);
      Vec3d vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
      return mc.world.method_17742(new RaycastContext(vec3d, vec3d3, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
   }

   public HitResult getRtxTarget(float yaw, float pitch, double x, double y, double z) {
      HitResult result = this.rayTrace(5.0, yaw, pitch, x, y, z);
      Vec3d vec3d = new Vec3d(x, y, z).add(0.0, mc.player.method_18381(mc.player.method_18376()), 0.0);
      double distancePow2 = 25.0;
      if (result != null) {
         distancePow2 = result.getPos().squaredDistanceTo(vec3d);
      }

      Vec3d vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * 5.0, vec3d2.y * 5.0, vec3d2.z * 5.0);
      Box box = new Box(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3).stretch(vec3d2.multiply(5.0)).expand(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.canHit(), distancePow2);
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3d vec3d4 = entityHitResult.method_17784();
         double g = vec3d.squaredDistanceTo(vec3d4);
         if ((g < distancePow2 || result == null) && entity2 instanceof LivingEntity) {
            return entityHitResult;
         }
      }

      return result;
   }

   public boolean isInWeb() {
      Box pBox = mc.player.method_5829();
      BlockPos pBlockPos = BlockPos.ofFloored(mc.player.method_19538());

      for (int x = pBlockPos.method_10263() - 2; x <= pBlockPos.method_10263() + 2; x++) {
         for (int y = pBlockPos.method_10264() - 1; y <= pBlockPos.method_10264() + 4; y++) {
            for (int z = pBlockPos.method_10260() - 2; z <= pBlockPos.method_10260() + 2; z++) {
               BlockPos bp = new BlockPos(x, y, z);
               if (pBox.intersects(new Box(bp)) && mc.world.method_8320(bp).method_26204() == Blocks.COBWEB) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public HitResult rayTrace(double dst, float yaw, float pitch, double x, double y, double z) {
      Vec3d vec3d = new Vec3d(x, y, z);
      Vec3d vec3d2 = this.getRotationVector(pitch, yaw);
      Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
      return mc.world.method_17742(new RaycastContext(vec3d, vec3d3, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
   }

   public static float[] calcAngle(Vec3d to) {
      if (to == null) {
         return null;
      }

      double difX = to.x - mc.player.method_33571().x;
      double difY = (to.y - mc.player.method_33571().y) * -1.0;
      double difZ = to.z - mc.player.method_33571().z;
      double dist = MathHelper.sqrt((float)(difX * difX + difZ * difZ));
      return new float[]{
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))
      };
   }

   public static Vec2f calcAngleVec(Vec3d to) {
      if (to == null) {
         return null;
      }

      double difX = to.x - mc.player.method_33571().x;
      double difY = (to.y - mc.player.method_33571().y) * -1.0;
      double difZ = to.z - mc.player.method_33571().z;
      double dist = MathHelper.sqrt((float)(difX * difX + difZ * difZ));
      return new Vec2f(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))
      );
   }

   @NotNull
   public Vec3d getRotationVector(float yaw, float pitch) {
      return new Vec3d(
         MathHelper.sin(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0)),
         -MathHelper.sin(yaw * (float) (Math.PI / 180.0)),
         MathHelper.cos(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0))
      );
   }

   public static float[] calcAngle(Vec3d from, Vec3d to) {
      if (to == null) {
         return null;
      }

      double difX = to.x - from.x;
      double difY = (to.y - from.y) * -1.0;
      double difZ = to.z - from.z;
      double dist = MathHelper.sqrt((float)(difX * difX + difZ * difZ));
      return new float[]{
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))
      };
   }
}
