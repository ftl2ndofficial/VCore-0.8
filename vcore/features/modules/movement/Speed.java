package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.events.impl.EventMove;
import vcore.events.impl.EventPlayerTravel;
import vcore.events.impl.EventPostTick;
import vcore.events.impl.EventSync;
import vcore.events.impl.EventTick;
import vcore.events.impl.PostPlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IInteractionManager;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.interfaces.IEntity;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.player.SearchInvResult;

public class Speed extends Module {
   public final Setting<Speed.Mode> mode = new Setting<>("Mode", Speed.Mode.NCP);
   public Setting<Boolean> useTimer = new Setting<>("Use Timer", false);
   public Setting<Boolean> pauseInLiquids = new Setting<>("PauseInLiquids", false);
   public Setting<Boolean> pauseWhileSneaking = new Setting<>("PauseWhileSneaking", false);
   public final Setting<Integer> hurttime = new Setting<>("HurtTime", 0, 0, 10, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Float> boostFactor = new Setting<>(
      "BoostFactor", 2.0F, 0.0F, 10.0F, v -> this.mode.is(Speed.Mode.MatrixDamage) || this.mode.is(Speed.Mode.Vanilla)
   );
   public final Setting<Boolean> allowOffGround = new Setting<>("AllowOffGround", true, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Integer> shiftTicks = new Setting<>("ShiftTicks", 0, 0, 10, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Integer> fireWorkSlot = new Setting<>("FireSlot", 1, 1, 9, v -> this.mode.getValue() == Speed.Mode.FireWork);
   public final Setting<Integer> delay = new Setting<>("Delay", 8, 1, 20, v -> this.mode.getValue() == Speed.Mode.FireWork);
   public final Setting<Boolean> strict = new Setting<>("Strict", false, v -> this.mode.is(Speed.Mode.GrimIce));
   public final Setting<Float> matrixJBSpeed = new Setting<>("TimerSpeed", 1.088F, 1.0F, 2.0F, v -> this.mode.is(Speed.Mode.MatrixJB));
   public final Setting<Boolean> armorStands = new Setting<>(
      "ArmorStands", false, v -> this.mode.is(Speed.Mode.GrimCombo) || this.mode.is(Speed.Mode.GrimEntity2)
   );
   public double baseSpeed;
   private int stage;
   private int ticks;
   private int prevSlot;
   private float prevForward = 0.0F;
   private Timer elytraDelay = new Timer();
   private Timer startDelay = new Timer();

   public Speed() {
      super("Speed", "Increases movement speed.", Module.Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      Vcore.TICK_TIMER = 1.0F;
   }

   @Override
   public void onEnable() {
      this.stage = 1;
      this.ticks = 0;
      this.baseSpeed = 0.2873;
      this.startDelay.reset();
      this.prevSlot = -1;
   }

   @EventHandler
   public void onSync(EventSync e) {
      if ((!mc.player.method_52535() || !this.pauseInLiquids.getValue()) && (!mc.player.method_5715() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.MatrixJB) {
            boolean closeToGround = false;

            for (VoxelShape a : mc.world.method_20812(mc.player, mc.player.method_5829().expand(0.5, 0.0, 0.5).offset(0.0, -1.0, 0.0))) {
               if (a != VoxelShapes.empty()) {
                  closeToGround = true;
                  break;
               }
            }

            if (MovementUtility.isMoving() && closeToGround && mc.player.field_6017 <= 0.0F) {
               Vcore.TICK_TIMER = 1.0F;
               mc.player.method_24830(true);
               mc.player.method_6043();
            } else if (mc.player.field_6017 > 0.0F && this.useTimer.getValue()) {
               Vcore.TICK_TIMER = this.matrixJBSpeed.getValue();
               mc.player.method_5762(0.0, -0.003F, 0.0);
            }
         }
      }
   }

   @EventHandler
   public void modifyVelocity(EventPlayerTravel e) {
      if (this.mode.getValue() == Speed.Mode.GrimEntity && !e.isPre() && Vcore.core.getSetBackTime() > 1000L) {
         for (PlayerEntity ent : Managers.ASYNC.getAsyncPlayers()) {
            if (ent != mc.player && mc.player.method_5858(ent) <= 2.25) {
               float p = mc.world.method_8320(((IEntity)mc.player).Vcore_Recode$getVelocityBP()).method_26204().getSlipperiness();
               float f = mc.player.method_24828() ? p * 0.91F : 0.91F;
               float f2 = mc.player.method_24828() ? p : 0.99F;
               mc.player
                  .method_18800(
                     mc.player.method_18798().method_10216() / f * f2,
                     mc.player.method_18798().method_10214(),
                     mc.player.method_18798().method_10215() / f * f2
                  );
               break;
            }
         }
      }

      if ((this.mode.is(Speed.Mode.GrimEntity2) || this.mode.is(Speed.Mode.GrimCombo))
         && !e.isPre()
         && Vcore.core.getSetBackTime() > 1000L
         && MovementUtility.isMoving()) {
         int collisions = 0;

         for (Entity ent : mc.world.getEntities()) {
            if (ent != mc.player
               && (!(ent instanceof ArmorStandEntity) || this.armorStands.getValue())
               && (ent instanceof LivingEntity || ent instanceof BoatEntity)
               && mc.player.method_5829().expand(1.0).intersects(ent.method_5829())) {
               collisions++;
            }
         }

         double[] motion = MovementUtility.forward(0.08 * collisions);
         mc.player.method_5762(motion[0], 0.0, motion[1]);
      }
   }

   @EventHandler
   public void onTick(EventTick e) {
      if ((this.mode.is(Speed.Mode.GrimIce) || this.mode.is(Speed.Mode.GrimCombo)) && mc.player.method_24828()) {
         BlockPos pos = ((IEntity)mc.player).Vcore_Recode$getVelocityBP();
         SearchInvResult result = InventoryUtility.findBlockInHotBar(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
         if (mc.world.method_22347(pos) || !result.found() || !mc.options.jumpKey.isPressed()) {
            return;
         }

         this.prevSlot = mc.player.method_31548().selectedSlot;
         result.switchTo();
         this.sendPacket(
            new Full(mc.player.method_23317(), mc.player.method_23318(), mc.player.method_23321(), mc.player.method_36454(), 90.0F, mc.player.method_24828())
         );
         if (this.strict.getValue()) {
            this.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, Direction.UP));
            this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
         }

         this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
         this.sendSequencedPacket(
            id -> new PlayerInteractBlockC2SPacket(
               Hand.MAIN_HAND, new BlockHitResult(pos.down().toCenterPos().add(0.0, 0.5, 0.0), Direction.UP, pos.down(), false), id
            )
         );
         mc.world.method_8501(pos, Blocks.ICE.getDefaultState());
      }
   }

   @EventHandler
   public void onPostTick(EventPostTick e) {
      if ((this.mode.is(Speed.Mode.GrimIce) || this.mode.is(Speed.Mode.GrimCombo)) && this.prevSlot != -1) {
         mc.player.method_31548().selectedSlot = this.prevSlot;
         ((IInteractionManager)mc.interactionManager).syncSlot();
         this.prevSlot = -1;
      }
   }

   @Override
   public void onUpdate() {
      if ((!mc.player.method_52535() || !this.pauseInLiquids.getValue()) && (!mc.player.method_5715() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.FireWork) {
            this.ticks--;
            int ellySlot = InventoryUtility.getElytra();
            int fireSlot = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).slot();
            boolean inOffHand = mc.player.method_6079().getItem() == Items.FIREWORK_ROCKET;
            if (fireSlot == -1) {
               int fireInInv = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
               if (fireInInv != -1) {
                  mc.interactionManager.clickSlot(mc.player.field_7512.syncId, fireInInv, this.fireWorkSlot.getValue() - 1, SlotActionType.SWAP, mc.player);
               }
            }

            if (ellySlot != -1 && (fireSlot != -1 || inOffHand) && !mc.player.method_24828() && mc.player.field_6017 > 0.0F && this.ticks <= 0) {
               if (ellySlot != -2) {
                  mc.interactionManager.clickSlot(0, ellySlot, 1, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
               }

               mc.player
                  .networkHandler
                  .method_52787(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               int prevSlot = mc.player.method_31548().selectedSlot;
               if (prevSlot != fireSlot && !inOffHand) {
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(fireSlot));
               }

               mc.interactionManager.interactItem(mc.player, inOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
               if (prevSlot != fireSlot && !inOffHand) {
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
               }

               if (ellySlot != -2) {
                  mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, ellySlot, 1, SlotActionType.PICKUP, mc.player);
               }

               mc.player.networkHandler.method_52787(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
               this.ticks = this.delay.getValue();
            }
         }

         if (this.mode.getValue() == Speed.Mode.ElytraLowHop) {
            if (mc.player.method_24828()) {
               mc.player.method_6043();
               return;
            }

            if (mc.world.method_20812(mc.player, mc.player.method_5829().expand(-0.29, 0.0, -0.29).offset(0.0, -3.0, 0.0)).iterator().hasNext()
               && this.elytraDelay.passedMs(150L)
               && this.startDelay.passedMs(500L)) {
               int elytra = InventoryUtility.getElytra();
               if (elytra == -1) {
                  this.disable("You need elytra for this mode!");
               }

               mc.player.method_18800(mc.player.method_18798().method_10216(), 0.0, mc.player.method_18798().method_10215());
               if (MovementUtility.isMoving()) {
                  MovementUtility.setMotion(0.85);
               }

               this.elytraDelay.reset();
            }
         }
      }
   }

   @EventHandler
   public void onPostPlayerUpdate(PostPlayerUpdateEvent event) {
      if (this.mode.getValue() == Speed.Mode.MatrixDamage && MovementUtility.isMoving() && mc.player.field_6235 > this.hurttime.getValue()) {
         if (mc.player.method_24828()) {
            MovementUtility.setMotion(0.387F * this.boostFactor.getValue());
         } else if (mc.player.method_5799()) {
            MovementUtility.setMotion(0.346F * this.boostFactor.getValue());
         } else if (!mc.player.method_24828() && this.allowOffGround.getValue()) {
            MovementUtility.setMotion(0.448F * this.boostFactor.getValue());
         }

         if (this.shiftTicks.getValue() > 0) {
            event.cancel();
            event.setIterations(this.shiftTicks.getValue());
         }
      }
   }

   @EventHandler
   public void onMove(EventMove event) {
      if ((!mc.player.method_52535() || !this.pauseInLiquids.getValue()) && (!mc.player.method_5715() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.NCP || this.mode.getValue() == Speed.Mode.StrictStrafe) {
            if (!mc.player.method_31549().flying) {
               if (!mc.player.method_6128()) {
                  if (mc.player.method_7344().getFoodLevel() > 6) {
                     if (!event.isCancelled()) {
                        event.cancel();
                        if (MovementUtility.isMoving()) {
                           Vcore.TICK_TIMER = this.useTimer.getValue() ? 1.088F : 1.0F;
                           float currentSpeed = this.mode.getValue() == Speed.Mode.NCP && mc.player.input.movementForward <= 0.0F && this.prevForward > 0.0F
                              ? Managers.PLAYER.currentPlayerSpeed * 0.66F
                              : Managers.PLAYER.currentPlayerSpeed;
                           boolean canJump = !mc.player.field_5976;
                           if (this.stage == 1 && mc.player.method_24828() && canJump) {
                              mc.player.method_18800(mc.player.method_18798().x, MovementUtility.getJumpSpeed(), mc.player.method_18798().z);
                              event.setY(MovementUtility.getJumpSpeed());
                              this.baseSpeed *= 2.149;
                              this.stage = 2;
                           } else if (this.stage == 2) {
                              this.baseSpeed = currentSpeed - 0.66 * (currentSpeed - MovementUtility.getBaseMoveSpeed());
                              this.stage = 3;
                           } else {
                              if (mc.world
                                    .method_20812(mc.player, mc.player.method_5829().offset(0.0, mc.player.method_18798().method_10214(), 0.0))
                                    .iterator()
                                    .hasNext()
                                 || mc.player.field_5992) {
                                 this.stage = 1;
                              }

                              this.baseSpeed = currentSpeed - currentSpeed / 159.0;
                           }

                           this.baseSpeed = Math.max(this.baseSpeed, MovementUtility.getBaseMoveSpeed());
                           double ncpSpeed = this.mode.getValue() != Speed.Mode.StrictStrafe && !(mc.player.input.movementForward < 1.0F) ? 0.576 : 0.465;
                           double ncpBypassSpeed = this.mode.getValue() != Speed.Mode.StrictStrafe && !(mc.player.input.movementForward < 1.0F) ? 0.57 : 0.44;
                           if (mc.player.method_6059(StatusEffects.SPEED)) {
                              double amplifier = mc.player.method_6112(StatusEffects.SPEED).getAmplifier();
                              ncpSpeed *= 1.0 + 0.2 * (amplifier + 1.0);
                              ncpBypassSpeed *= 1.0 + 0.2 * (amplifier + 1.0);
                           }

                           if (mc.player.method_6059(StatusEffects.SLOWNESS)) {
                              double amplifier = mc.player.method_6112(StatusEffects.SLOWNESS).getAmplifier();
                              ncpSpeed /= 1.0 + 0.2 * (amplifier + 1.0);
                              ncpBypassSpeed /= 1.0 + 0.2 * (amplifier + 1.0);
                           }

                           this.baseSpeed = Math.min(this.baseSpeed, this.ticks > 25 ? ncpSpeed : ncpBypassSpeed);
                           if (this.ticks++ > 50) {
                              this.ticks = 0;
                           }

                           MovementUtility.modifyEventSpeed(event, this.baseSpeed);
                           this.prevForward = mc.player.input.movementForward;
                        } else {
                           Vcore.TICK_TIMER = 1.0F;
                           event.setX(0.0);
                           event.setZ(0.0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public enum Mode {
      ElytraLowHop,
      FireWork,
      GrimCombo,
      GrimEntity,
      GrimEntity2,
      GrimIce,
      MatrixDamage,
      MatrixJB,
      NCP,
      StrictStrafe,
      Vanilla;
   }
}
