package vcore.features.modules.player;

import java.util.Comparator;
import java.util.HashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventEntitySpawn;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.player.MovementUtility;

public class PearlChaser extends Module {
   private final Setting<BooleanSettingGroup> stopMotion = new Setting<>("StopMotion", new BooleanSettingGroup(false));
   private final Setting<Boolean> legitStop = new Setting<>("LegitStop", false).addToGroup(this.stopMotion);
   private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);
   private final Setting<Boolean> onlyOnGround = new Setting<>("OnlyOnGround", false);
   private final Setting<Boolean> noMove = new Setting<>("NoMove", false);
   private final Setting<Boolean> onlyTarget = new Setting<>("OnlyTarget", false);
   private Runnable postSyncAction;
   private final Timer delayTimer = new Timer();
   private BlockPos targetBlock;
   private int lastPearlId;
   private int lastOurPearlId;
   private HashMap<PlayerEntity, Long> targets = new HashMap<>();

   public PearlChaser() {
      super("PearlChaser", "Auto pursuit with pearls.", Module.Category.PLAYER);
   }

   @EventHandler
   public void onEntitySpawn(EventEntitySpawn e) {
      if (e.getEntity() instanceof EnderPearlEntity) {
         mc.world.method_18456().stream().min(Comparator.comparingDouble(p -> p.method_5707(e.getEntity().getPos()))).ifPresent(player -> {
            if (player.equals(mc.player)) {
               this.lastOurPearlId = e.getEntity().method_5628();
            }
         });
      }
   }

   @EventHandler(priority = -100)
   public void onSync(EventSync event) {
      if (this.onlyTarget.getValue()
         && Aura.target != null
         && ModuleManager.aura.isEnabled()
         && Aura.target instanceof PlayerEntity pl
         && !this.targets.containsKey(pl)) {
         this.targets.put(pl, System.currentTimeMillis());
      }

      if (!(mc.player.method_6032() < 5.0F)) {
         if (this.delayTimer.passedMs(1000L)) {
            for (Entity ent : mc.world.getEntities()) {
               if (ent instanceof EnderPearlEntity && ent.method_5628() != this.lastPearlId && ent.method_5628() != this.lastOurPearlId) {
                  mc.world
                     .method_18456()
                     .stream()
                     .filter(e -> this.targets.containsKey(e) || !this.onlyTarget.getValue())
                     .min(Comparator.comparingDouble(p -> p.method_5707(ent.getPos())))
                     .ifPresent(player -> {
                        if (!player.equals(mc.player)) {
                           this.targetBlock = this.calcTrajectory(ent);
                           this.lastPearlId = ent.method_5628();
                        }
                     });
               }
            }

            if (this.targetBlock != null) {
               if (!(mc.player.method_5707(this.targetBlock.toCenterPos()) < 49.0)) {
                  float rotationPitch = (float)(-Math.toDegrees(this.calcTrajectory(this.targetBlock)));
                  float rotationYaw = (float)Math.toDegrees(
                        Math.atan2(
                           this.targetBlock.method_10260() + 0.5F - mc.player.method_23321(), this.targetBlock.method_10263() + 0.5F - mc.player.method_23317()
                        )
                     )
                     - 90.0F;
                  BlockPos tracedBP = this.checkTrajectory(rotationYaw, rotationPitch);
                  if (tracedBP != null && !(this.targetBlock.method_19770(tracedBP.toCenterPos()) > 36.0)) {
                     if (this.pauseAura.getValue() && ModuleManager.aura.isEnabled()) {
                        ModuleManager.aura.pause();
                     }

                     if (!this.onlyOnGround.getValue() || mc.player.method_24828()) {
                        if (!this.noMove.getValue() || !MovementUtility.isMoving()) {
                           if (this.stopMotion.getValue().isEnabled()) {
                              if (!this.legitStop.getValue()) {
                                 mc.player.method_18800(0.0, 0.0, 0.0);
                              }

                              mc.options.forwardKey.setPressed(false);
                              mc.options.backKey.setPressed(false);
                              mc.options.leftKey.setPressed(false);
                              mc.options.rightKey.setPressed(false);
                              mc.player.input.movementForward = 0.0F;
                              mc.player.input.movementSideways = 0.0F;
                           } else {
                              this.sendMessage(
                                 "Chasing pearl on X:"
                                    + tracedBP.method_10263()
                                    + " Y:"
                                    + tracedBP.method_10264()
                                    + " Z:"
                                    + tracedBP.method_10260()
                                    + " Angle Y:"
                                    + rotationYaw
                                    + " P:"
                                    + rotationPitch
                              );
                              mc.player.method_36456(rotationYaw);
                              mc.player.method_36457(MathUtility.clamp(rotationPitch, -89.0F, 89.0F));
                              float yaw = mc.player.method_36454();
                              float pitch = mc.player.method_36455();
                              this.postSyncAction = () -> {
                                 int epSlot = this.findEPSlot();
                                 int originalSlot = mc.player.method_31548().selectedSlot;
                                 if (epSlot != -1) {
                                    mc.player.method_31548().selectedSlot = epSlot;
                                    this.sendPacket(new UpdateSelectedSlotC2SPacket(epSlot));
                                    this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
                                    this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    mc.player.method_31548().selectedSlot = originalSlot;
                                    this.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
                                 }
                              };
                              this.targetBlock = null;
                              this.delayTimer.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPostSync(EventPostSync event) {
      if (this.postSyncAction != null) {
         this.postSyncAction.run();
         this.postSyncAction = null;
      }
   }

   private int findEPSlot() {
      int epSlot = -1;
      if (mc.player.method_6047().getItem() == Items.ENDER_PEARL) {
         epSlot = mc.player.method_31548().selectedSlot;
      }

      if (epSlot == -1) {
         for (int l = 0; l < 9; l++) {
            if (mc.player.method_31548().method_5438(l).getItem() == Items.ENDER_PEARL) {
               epSlot = l;
               break;
            }
         }
      }

      return epSlot;
   }

   private float calcTrajectory(@NotNull BlockPos bp) {
      double a = Math.hypot(bp.method_10263() + 0.5F - mc.player.method_23317(), bp.method_10260() + 0.5F - mc.player.method_23321());
      double y = 6.125 * (bp.method_10264() + 1.0F - (mc.player.method_23318() + mc.player.method_18381(mc.player.method_18376())));
      y = 0.05F * (0.05F * (a * a) + y);
      y = Math.sqrt(9.378906F - y);
      double d = 3.0625 - y;
      y = Math.atan2(d * d + y, 0.05F * a);
      d = Math.atan2(d, 0.05F * a);
      return (float)Math.min(y, d);
   }

   private BlockPos calcTrajectory(Entity e) {
      return this.traceTrajectory(e.getX(), e.getY(), e.getZ(), e.getVelocity().x, e.getVelocity().y, e.getVelocity().z);
   }

   private BlockPos checkTrajectory(float yaw, float pitch) {
      if (Float.isNaN(pitch)) {
         return null;
      }

      float yawRad = yaw / 180.0F * (float) Math.PI;
      float pitchRad = pitch / 180.0F * (float) Math.PI;
      double x = mc.player.method_23317() - MathHelper.cos(yawRad) * 0.16F;
      double y = mc.player.method_23318() + mc.player.method_18381(mc.player.method_18376()) - 0.1000000014901161;
      double z = mc.player.method_23321() - MathHelper.sin(yawRad) * 0.16F;
      double motionX = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad) * 0.4F;
      double motionY = -MathHelper.sin(pitchRad) * 0.4F;
      double motionZ = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad) * 0.4F;
      float distance = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= distance;
      motionY /= distance;
      motionZ /= distance;
      motionX *= 1.5;
      motionY *= 1.5;
      motionZ *= 1.5;
      if (!mc.player.method_24828()) {
         motionY += mc.player.method_18798().method_10214();
      }

      return this.traceTrajectory(x, y, z, motionX, motionY, motionZ);
   }

   private BlockPos traceTrajectory(double x, double y, double z, double mx, double my, double mz) {
      for (int i = 0; i < 300; i++) {
         Vec3d lastPos = new Vec3d(x, y, z);
         x += mx;
         y += my;
         z += mz;
         mx *= 0.99;
         my *= 0.99;
         mz *= 0.99;
         my -= 0.03F;
         Vec3d pos = new Vec3d(x, y, z);
         BlockHitResult bhr = mc.world.method_17742(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (bhr != null && bhr.method_17783() == Type.BLOCK) {
            return bhr.getBlockPos();
         }

         for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof ArrowEntity)
               && ent != mc.player
               && !(ent instanceof EnderPearlEntity)
               && ent.method_5829().intersects(new Box(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.2))) {
               return null;
            }
         }

         if (y <= -65.0) {
            break;
         }
      }

      return null;
   }
}
