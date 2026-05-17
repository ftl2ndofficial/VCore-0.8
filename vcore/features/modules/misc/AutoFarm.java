package vcore.features.modules.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import vcore.events.impl.EventFixVelocity;
import vcore.events.impl.EventSync;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.features.modules.movement.freelook.CameraOverriddenEntity;
import vcore.features.modules.movement.freelook.FreeLookState;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;
import vcore.utility.player.InventoryUtility;

public class AutoFarm extends Module {
   private static final int HUNGER_THRESHOLD = 6;
   private static final int FIX_THRESHOLD = 20;
   private static final int LAYOUT_SCAN = 7;
   private static final int FARM_LOOKAHEAD = 7;
   private static final int ENTRY_SCAN = 10;
   private static final int ENTRY_PATCH_SIZE = 4;
   private static final int MIN_PARTIAL_ENTRY_PUMPKINS = 1;
   private static final int ENTRY_PATCH_SUPPORT_SCAN = 4;
   private static final int END_CONFIRM_TICKS = 8;
   private static final int EXIT_TIMEOUT_TICKS = 80;
   private static final int TURN_TIMEOUT_TICKS = 140;
   private static final int NO_ENTRY_RETURN_TICKS = 16;
   private static final int ENTER_ROW_TIMEOUT_TICKS = 60;
   private static final int ENTER_ROW_RETRY_LIMIT = 3;
   private static final int ALIGN_EDGE_TIMEOUT_TICKS = 40;
   private static final int MIN_NEXT_PAIR_DISTANCE = 3;
   private static final int MAX_NEXT_PAIR_DISTANCE = 7;
   private static final int PREP_AREA_DEPTH = 2;
   private static final long COMMAND_DELAY_MS = 1500L;
   private static final long FIX_DELAY_MS = 3000L;
   private static final long STUCK_CHECK_MS = 250L;
   private static final long EAT_DELAY_MS = 200L;
   private static final long FLAG_CONFIRM_WINDOW_MS = 200L;
   private static final long FLAG_USE_HOLD_MS = 250L;
   private static final double RETURN_DISTANCE = 0.75;
   private static final double STUCK_MOVEMENT = 0.035;
   private static final double AREA_EPSILON = 0.05;
   private static final double ENTRY_LOCK_DISTANCE = 16.0;
   private static final double PAIR_DISTANCE_WEIGHT = 1000.0;
   private static final double BREAK_ALIGNMENT_DOT = 0.82;
   private static final float FARM_PITCH = 35.5F;
   private static final float PAUSE_PITCH = 90.0F;
   private static final float BREAK_RANGE = 5.0F;
   private static final float[][] SAMPLES = new float[][]{
      {0.5F, 0.5F, 0.5F},
      {0.5F, 0.5F, 0.1F},
      {0.5F, 0.5F, 0.9F},
      {0.1F, 0.5F, 0.5F},
      {0.9F, 0.5F, 0.5F},
      {0.5F, 0.1F, 0.5F},
      {0.5F, 0.9F, 0.5F},
      {0.2F, 0.2F, 0.2F},
      {0.8F, 0.2F, 0.2F},
      {0.2F, 0.8F, 0.2F},
      {0.8F, 0.8F, 0.2F},
      {0.2F, 0.2F, 0.8F},
      {0.8F, 0.2F, 0.8F},
      {0.2F, 0.8F, 0.8F},
      {0.8F, 0.8F, 0.8F}
   };
   private final Setting<AutoFarm.Mode> mode = new Setting<>("Mode", AutoFarm.Mode.Pumpkin);
   private final Setting<AutoFarm.AutoFeed> autoFeed = new Setting<>("Auto Feed", AutoFarm.AutoFeed.Command);
   private final Setting<Boolean> autoFix = new Setting<>("Auto Fix", true);
   private final Setting<Boolean> pauseWhileFlag = new Setting<>("Pause While Flag", true);
   private final Setting<Integer> pauseTime = new Setting<>("Pause Time", 3, 1, 10, v -> this.pauseWhileFlag.getValue());
   private final Setting<Boolean> limitBps = new Setting<>("Limit BPS", false);
   private final Setting<Float> limit = new Setting<>("Limit", 8.0F, 4.0F, 10.0F, v -> this.limitBps.getValue()).step(0.1F);
   private AutoFarm.Phase phase = AutoFarm.Phase.Farming;
   private Vec3d startPos;
   private Direction rowDirection = Direction.NORTH;
   private Direction sideDirection = Direction.EAST;
   private Direction exitDirection = Direction.NORTH;
   private Direction nextRowDirection;
   private Direction transitionSideDirection;
   private Direction alignDirection = Direction.WEST;
   private AutoFarm.EntryPatch entryPatch;
   private AutoFarm.PairKey currentPair;
   private AutoFarm.PairKey firstPair;
   private AutoFarm.PairKey targetEntryPair;
   private Direction firstRowDirection = Direction.NORTH;
   private Direction firstSideDirection = Direction.EAST;
   private final Set<AutoFarm.PairKey> completedPairs = new HashSet<>();
   private BlockHitResult pendingBreak;
   private float moveFixYaw = Float.NaN;
   private float rotationYaw;
   private float rotationPitch;
   private boolean initialized;
   private boolean controllingMovement;
   private boolean eating;
   private boolean switchedFoodSlot;
   private int previousFoodSlot = -1;
   private int noPumpkinAheadTicks;
   private int noEntryTargetTicks;
   private int enterRowRetries;
   private int phaseTicks;
   private Vec3d lastPosition;
   private long lastMoveCheckMs;
   private int stuckTicks;
   private long lastFeedCommandMs;
   private long lastFixCommandMs;
   private long eatStartDelayUntilMs;
   private long eatResumeDelayUntilMs;
   private long flagPauseUntilMs;
   private long flagUseStartMs;
   private long flagWindowStartMs;
   private int flagWindowCount;
   private boolean flagUsePending;
   private boolean flagUseKeyHeld;
   private Vec3d farmLimitLastPos;
   private long farmLimitLastMs;
   private double farmLimitCredit;
   private boolean autoFarmFreeLookActive;

   public AutoFarm() {
      super("AutoFarm", "Automatically farms crop lanes.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.resetState(false);
      if (mc.player != null) {
         this.startPos = mc.player.method_19538();
         this.rotationYaw = mc.player.method_36454();
         this.rotationPitch = mc.player.method_36455();
         this.rowDirection = this.nearestHorizontal(this.rotationYaw);
         this.sideDirection = this.rotateRight(this.rowDirection);
         this.updateAutoFarmFreeLook();
      }
   }

   @Override
   public void onDisable() {
      this.resetState(true);
   }

   @Override
   public void onLogout() {
      this.resetState(true);
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue() == AutoFarm.Mode.Pumpkin ? this.phase.name() : null;
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (!fullNullCheck() && mc.player != null && !mc.player.method_7325()) {
         this.updateAutoFarmFreeLook();
         this.handleAutoFix();
         if (this.shouldAutoFeedInterruptFlagUse()) {
            this.releaseFlagUseHold();
         }

         if (this.handleAutoFeed()) {
            this.pendingBreak = null;
         } else {
            if (this.mode.getValue() == AutoFarm.Mode.Pumpkin) {
               this.handlePumpkin();
            }
         }
      } else {
         this.resetRuntimeControls();
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck()
         && mc.player != null
         && this.mode.getValue() == AutoFarm.Mode.Pumpkin
         && this.pauseWhileFlag.getValue()
         && this.phase == AutoFarm.Phase.Farming
         && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         long now = System.currentTimeMillis();
         if (this.flagPauseUntilMs > now) {
            this.clearFlagWindow();
         } else if (this.shouldStartFlagPause(now)) {
            this.releaseFlagUseHold();
            this.flagPauseUntilMs = now + this.pauseTime.getValue().intValue() * 1000L;
            this.flagUseStartMs = 0L;
            this.flagUsePending = true;
            this.clearFlagWindow();
            this.noPumpkinAheadTicks = 0;
            this.pendingBreak = null;
            this.resetStuckState();
            this.releaseMovement();
         }
      }
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (!fullNullCheck() && mc.player != null) {
         if (this.phase != AutoFarm.Phase.Idle) {
            mc.player.method_36456(this.rotationYaw);
            mc.player.method_36457(this.rotationPitch);
         }

         if (this.pendingBreak != null) {
            BlockHitResult hit = this.pendingBreak;
            float breakYaw = this.rotationYaw;
            float breakPitch = this.rotationPitch;
            Runnable previous = event.getPostAction();
            event.addPostAction(() -> {
               if (previous != null) {
                  previous.run();
               }

               this.breakBlock(hit, breakYaw, breakPitch);
            });
            this.pendingBreak = null;
         }
      }
   }

   @EventHandler(priority = -200)
   public void onFixVelocity(EventFixVelocity event) {
      if (this.shouldApplyPumpkinMoveFix()) {
         event.setVelocity(this.movementInputToVelocity(event.getMovementInput(), event.getSpeed(), this.moveFixYaw));
      }
   }

   private boolean shouldApplyPumpkinMoveFix() {
      return this.mode.getValue() == AutoFarm.Mode.Pumpkin
         && this.controllingMovement
         && !this.eating
         && this.phase != AutoFarm.Phase.Idle
         && !Float.isNaN(this.moveFixYaw)
         && mc.player != null
         && !mc.player.isRiding();
   }

   private void handlePumpkin() {
      if (!this.initialized) {
         this.initializePumpkinLayout();
      }

      this.pendingBreak = null;
      this.phaseTicks++;
      switch (this.phase) {
         case Farming:
            this.handleFarming();
            break;
         case ExitingRow:
            this.handleExitingRow();
            break;
         case Turning:
            this.handleTurning();
            break;
         case ClearingEntry:
            this.handleClearingEntry();
            break;
         case EnteringRow:
            this.handleEnteringRow();
            break;
         case AligningEdge:
            this.handleAligningEdge();
            break;
         case ReturningHome:
            this.handleReturningHome();
            break;
         case Idle:
            this.releaseMovement();
      }
   }

   private void initializePumpkinLayout() {
      AutoFarm.PairCandidate candidate = this.findBestPairCandidate();
      if (candidate == null) {
         this.initialized = true;
         this.phase = AutoFarm.Phase.Idle;
         this.releaseMovement();
         this.disable("No pumpkin pair found.");
      } else {
         this.rowDirection = candidate.row();
         this.sideDirection = candidate.side();
         this.currentPair = this.pairKey(candidate.base());
         if (this.firstPair == null) {
            this.firstPair = this.currentPair;
            this.firstRowDirection = this.rowDirection;
            this.firstSideDirection = this.sideDirection;
         }

         this.initialized = true;
         this.phase = AutoFarm.Phase.Farming;
         this.phaseTicks = 0;
         this.noPumpkinAheadTicks = 0;
      }
   }

   private void handleFarming() {
      if (this.isFarmFlagPauseActive()) {
         this.setFarmPauseRotation();
         this.updateFlagUseHold();
         this.noPumpkinAheadTicks = 0;
         this.pendingBreak = null;
         this.resetStuckState();
         this.releaseMovement();
      } else {
         this.setFarmRotation();
         this.applyFarmMovement();
         this.pendingBreak = this.findFarmBreakTarget();
         if (this.hasPumpkinsAhead()) {
            this.noPumpkinAheadTicks = 0;
         } else {
            this.noPumpkinAheadTicks++;
         }

         if (this.noPumpkinAheadTicks >= 8 || this.noPumpkinAheadTicks > 2 && this.isStuck()) {
            this.syncCurrentPairFromPlayer();
            this.markCurrentPairCompleted();
            this.exitDirection = this.rowDirection;
            this.nextRowDirection = this.rowDirection.getOpposite();
            this.transitionSideDirection = this.sideDirection;
            this.phase = AutoFarm.Phase.ExitingRow;
            this.phaseTicks = 0;
            this.noPumpkinAheadTicks = 0;
            this.noEntryTargetTicks = 0;
            this.entryPatch = null;
            this.targetEntryPair = this.findNextEntryPairKey();
            Direction targetSide = this.targetEntrySideDirection();
            if (targetSide != null) {
               this.transitionSideDirection = targetSide;
            }

            this.resetStuckState();
            this.releaseMovement();
         }
      }
   }

   private void handleExitingRow() {
      this.setRotation(this.yawFor(this.exitDirection), 0.0F);
      this.pressForwardOnly();
      AutoFarm.EntryPatch next = this.findEntryPatch();
      if (!this.isUsableEntryPatch(this.entryPatch) && next != null) {
         this.entryPatch = next;
         this.transitionSideDirection = next.side();
      }

      if (this.entryPatch != null && this.isPlayerInsidePreparationArea(this.entryPatch)) {
         this.phase = AutoFarm.Phase.ClearingEntry;
         this.phaseTicks = 0;
         this.noEntryTargetTicks = 0;
         this.releaseMovement();
      } else {
         AutoFarm.EntryPatch ready = this.findContainingPreparationPatch();
         if (this.shouldAcceptContainingPreparationPatch(ready)) {
            this.entryPatch = ready;
            this.transitionSideDirection = ready.side();
            this.phase = AutoFarm.Phase.ClearingEntry;
            this.phaseTicks = 0;
            this.noEntryTargetTicks = 0;
            this.releaseMovement();
         } else {
            this.updateNoEntryTargetTicks(next);
            if (next != null && this.isPlayerInsidePreparationArea(next)) {
               this.entryPatch = next;
               this.transitionSideDirection = next.side();
               this.phase = AutoFarm.Phase.ClearingEntry;
               this.phaseTicks = 0;
               this.noEntryTargetTicks = 0;
               this.releaseMovement();
            } else if (this.shouldReturnHomeWithoutEntryTarget()) {
               this.startReturningHome();
            } else {
               if (this.isStuck() || this.phaseTicks >= 80) {
                  this.entryPatch = next;
                  if (this.entryPatch != null) {
                     this.transitionSideDirection = this.entryPatch.side();
                  } else {
                     this.transitionSideDirection = this.findEntrySideHint();
                  }

                  this.phase = AutoFarm.Phase.Turning;
                  this.phaseTicks = 0;
                  this.resetStuckState();
               }
            }
         }
      }
   }

   private void handleTurning() {
      if (this.entryPatch != null && this.isPlayerInsidePreparationArea(this.entryPatch)) {
         this.phase = AutoFarm.Phase.ClearingEntry;
         this.phaseTicks = 0;
         this.noEntryTargetTicks = 0;
         this.releaseMovement();
      } else {
         AutoFarm.EntryPatch ready = this.findContainingPreparationPatch();
         if (this.shouldAcceptContainingPreparationPatch(ready)) {
            this.entryPatch = ready;
            this.transitionSideDirection = ready.side();
            this.phase = AutoFarm.Phase.ClearingEntry;
            this.phaseTicks = 0;
            this.noEntryTargetTicks = 0;
            this.releaseMovement();
         } else {
            AutoFarm.EntryPatch next = this.findEntryPatch();
            this.updateNoEntryTargetTicks(next);
            if (next != null && this.isPlayerInsidePreparationArea(next)) {
               this.entryPatch = next;
               this.transitionSideDirection = next.side();
               this.phase = AutoFarm.Phase.ClearingEntry;
               this.phaseTicks = 0;
               this.noEntryTargetTicks = 0;
               this.releaseMovement();
            } else {
               if (!this.isUsableEntryPatch(this.entryPatch)) {
                  this.entryPatch = next;
                  if (this.entryPatch != null) {
                     this.transitionSideDirection = this.entryPatch.side();
                  }
               }

               if (this.entryPatch != null && this.isPlayerInsidePreparationArea(this.entryPatch)) {
                  this.phase = AutoFarm.Phase.ClearingEntry;
                  this.phaseTicks = 0;
                  this.releaseMovement();
               } else {
                  if (this.entryPatch == null) {
                     this.transitionSideDirection = this.findEntrySideHint();
                  }

                  if (this.entryPatch == null && this.shouldReturnHomeWithoutEntryTarget()) {
                     this.startReturningHome();
                  } else {
                     this.setRotation(this.entryPatch == null ? this.yawFor(this.entrySideDirection()) : this.yawToPreparationArea(this.entryPatch), 0.0F);
                     this.pressForwardOnly();
                     if (this.phaseTicks >= 140) {
                        this.startReturningHome();
                     }
                  }
               }
            }
         }
      }
   }

   private void handleClearingEntry() {
      this.releaseMovement();
      if (this.entryPatch == null) {
         this.startReturningHome();
      } else {
         BlockHitResult hit = this.findEntryBreakTarget(this.entryPatch);
         if (hit == null) {
            this.beginEnteringRow();
         } else {
            float[] angle = this.calculateAngle(hit.method_17784());
            this.setRotation(angle[0], angle[1]);
            this.pendingBreak = hit;
         }
      }
   }

   private void handleEnteringRow() {
      this.pendingBreak = null;
      if (!this.restartEntryClearingIfRegrown()) {
         this.setRotation(this.entryPatch == null ? this.yawFor(this.entryRowDirection()) : this.yawToEntryPatch(this.entryPatch), 0.0F);
         this.pressForwardOnly();
         if (this.entryPatch != null && !this.isPlayerInsideEntryPatch(this.entryPatch)) {
            if (this.phaseTicks >= 60 || this.isStuck()) {
               if (this.findEntryBreakTarget(this.entryPatch) != null) {
                  this.phase = AutoFarm.Phase.ClearingEntry;
                  this.phaseTicks = 0;
                  this.resetStuckState();
                  this.releaseMovement();
               } else if (this.enterRowRetries++ >= 3) {
                  this.beginAligningEdge();
               } else {
                  this.phaseTicks = 0;
                  this.resetStuckState();
                  this.releaseMovement();
               }
            }
         } else {
            this.beginAligningEdge();
         }
      }
   }

   private void handleAligningEdge() {
      this.pendingBreak = null;
      if (!this.restartEntryClearingIfRegrown()) {
         this.setRotation(this.yawFor(this.alignDirection), 0.0F);
         this.pressForwardOnly();
         if (this.isStuck() || this.phaseTicks >= 40) {
            if (this.restartEntryClearingIfRegrown()) {
               return;
            }

            this.phase = AutoFarm.Phase.Farming;
            this.phaseTicks = 0;
            this.noPumpkinAheadTicks = 0;
            this.resetStuckState();
            this.releaseMovement();
         }
      }
   }

   private boolean restartEntryClearingIfRegrown() {
      if (this.entryPatch != null && this.findEntryBreakTarget(this.entryPatch) != null) {
         this.phase = AutoFarm.Phase.ClearingEntry;
         this.phaseTicks = 0;
         this.noEntryTargetTicks = 0;
         this.enterRowRetries = 0;
         this.resetStuckState();
         this.releaseMovement();
         return true;
      } else {
         return false;
      }
   }

   private void beginEnteringRow() {
      this.phase = AutoFarm.Phase.EnteringRow;
      this.phaseTicks = 0;
      this.noEntryTargetTicks = 0;
      this.enterRowRetries = 0;
      this.resetStuckState();
      this.releaseMovement();
   }

   private void beginAligningEdge() {
      this.rowDirection = this.entryRowDirection();
      this.nextRowDirection = null;
      this.sideDirection = this.entrySideDirection();
      this.transitionSideDirection = null;
      this.targetEntryPair = null;
      if (this.entryPatch != null) {
         this.currentPair = this.pairKey(this.entryPatch.base());
      }

      this.alignDirection = this.sideDirection.getOpposite();
      this.phase = AutoFarm.Phase.AligningEdge;
      this.phaseTicks = 0;
      this.noEntryTargetTicks = 0;
      this.enterRowRetries = 0;
      this.resetStuckState();
      this.releaseMovement();
   }

   private void startReturningHome() {
      this.phase = AutoFarm.Phase.ReturningHome;
      this.phaseTicks = 0;
      this.noEntryTargetTicks = 0;
      this.enterRowRetries = 0;
      this.nextRowDirection = null;
      this.transitionSideDirection = null;
      this.targetEntryPair = null;
      this.entryPatch = null;
      this.pendingBreak = null;
      this.resetStuckState();
      this.releaseMovement();
   }

   private void handleReturningHome() {
      if (this.startPos == null) {
         this.finishRoute();
      } else {
         Vec3d current = mc.player.method_19538();
         if (current.squaredDistanceTo(this.startPos.x, current.y, this.startPos.z) <= 0.5625) {
            this.restartFarmCycle();
         } else {
            Direction direction = this.getReturnDirection(current, this.startPos);
            if (direction == null) {
               this.restartFarmCycle();
            } else {
               this.setRotation(this.yawFor(direction), 0.0F);
               this.pressForwardOnly();
            }
         }
      }
   }

   private void finishRoute() {
      this.phase = AutoFarm.Phase.Idle;
      this.pendingBreak = null;
      this.releaseMovement();
      this.disable();
   }

   private void restartFarmCycle() {
      this.completedPairs.clear();
      this.entryPatch = null;
      this.targetEntryPair = null;
      this.pendingBreak = null;
      this.noPumpkinAheadTicks = 0;
      this.noEntryTargetTicks = 0;
      this.enterRowRetries = 0;
      this.phaseTicks = 0;
      this.resetStuckState();
      this.releaseMovement();
      if (this.firstPair != null) {
         this.rowDirection = this.firstRowDirection;
         this.sideDirection = this.firstSideDirection;
         this.nextRowDirection = null;
         this.transitionSideDirection = null;
         this.targetEntryPair = null;
         this.currentPair = this.firstPair;
         this.initialized = true;
      } else {
         this.currentPair = null;
         this.initialized = false;
      }

      this.phase = AutoFarm.Phase.Farming;
   }

   private AutoFarm.PairCandidate findBestPairCandidate() {
      BlockPos center = BlockPos.ofFloored(mc.player.method_19538());
      Direction preferred = this.nearestHorizontal(mc.player.method_36454());
      List<AutoFarm.PairCandidate> candidates = new ArrayList<>();

      for (int x = -7; x <= 7; x++) {
         for (int y = -1; y <= 2; y++) {
            for (int z = -7; z <= 7; z++) {
               BlockPos pos = center.add(x, y, z);
               if (this.isPumpkin(pos)) {
                  this.addPairCandidate(candidates, pos, Direction.NORTH, preferred);
                  this.addPairCandidate(candidates, pos, Direction.SOUTH, preferred);
                  this.addPairCandidate(candidates, pos, Direction.EAST, preferred);
                  this.addPairCandidate(candidates, pos, Direction.WEST, preferred);
               }
            }
         }
      }

      return candidates.stream()
         .max(Comparator.comparingInt(AutoFarm.PairCandidate::score).thenComparingDouble(candidate -> -candidate.distanceSq()))
         .orElse(null);
   }

   private void addPairCandidate(List<AutoFarm.PairCandidate> candidates, BlockPos pos, Direction side, Direction preferred) {
      if (this.isPumpkin(pos.offset(side))) {
         Direction rowA = side.getAxis() == Axis.X ? Direction.NORTH : Direction.EAST;
         Direction rowB = rowA.getOpposite();
         Direction row = preferred.getAxis() == rowA.getAxis() ? preferred : this.bestRowDirection(pos, side, rowA, rowB);
         Direction insideSide = this.getInsideSide(pos, side);
         BlockPos base = insideSide == side ? pos : pos.offset(side);
         int score = this.countPairLength(base, insideSide, row) + this.countPairLength(base, insideSide, row.getOpposite());
         if (preferred.getAxis() == row.getAxis()) {
            score += 12;
         }

         candidates.add(
            new AutoFarm.PairCandidate(row, insideSide, base.toImmutable(), score, this.horizontalDistanceSq(mc.player.method_19538(), base.toCenterPos()))
         );
      }
   }

   private Direction bestRowDirection(BlockPos base, Direction side, Direction rowA, Direction rowB) {
      return this.countPairLength(base, side, rowA) >= this.countPairLength(base, side, rowB) ? rowA : rowB;
   }

   private Direction getInsideSide(BlockPos pos, Direction side) {
      double first = this.horizontalDistanceSq(mc.player.method_19538(), pos.toCenterPos());
      double second = this.horizontalDistanceSq(mc.player.method_19538(), pos.offset(side).toCenterPos());
      return first <= second ? side : side.getOpposite();
   }

   private int countPairLength(BlockPos base, Direction side, Direction row) {
      int score = 0;
      int missing = 0;

      for (int i = 0; i < 36; i++) {
         BlockPos a = base.offset(row, i);
         BlockPos b = a.offset(side);
         boolean hasPumpkin = this.isLoaded(a) && this.isLoaded(b) && (this.isPumpkin(a) || this.isPumpkin(b));
         if (hasPumpkin) {
            score += (this.isPumpkin(a) ? 1 : 0) + (this.isPumpkin(b) ? 1 : 0);
            missing = 0;
         } else if (++missing >= 2) {
            break;
         }
      }

      return score;
   }

   private boolean hasPumpkinsAhead() {
      BlockPos player = BlockPos.ofFloored(mc.player.method_19538());
      int y0 = player.method_10264();
      boolean checkedLoaded = false;

      for (int step = 1; step <= 7; step++) {
         for (int lateral = -1; lateral <= 3; lateral++) {
            for (int y = -1; y <= 2; y++) {
               BlockPos pos = player.offset(this.rowDirection, step).offset(this.sideDirection, lateral).withY(y0 + y);
               if (!this.isLoaded(pos)) {
                  return true;
               }

               checkedLoaded = true;
               if (this.isPumpkin(pos)) {
                  return true;
               }
            }
         }
      }

      return !checkedLoaded;
   }

   private AutoFarm.EntryPatch findEntryPatch() {
      Direction lockedSide = this.lockedEntrySideDirection();
      if (lockedSide != null) {
         return this.findEntryPatch(lockedSide);
      } else {
         Direction primarySide = this.entrySideDirection();
         AutoFarm.EntryPatch primary = this.findEntryPatch(primarySide);
         AutoFarm.EntryPatch opposite = this.findEntryPatch(primarySide.getOpposite());
         if (primary == null) {
            return opposite;
         } else {
            return opposite != null && !(primary.score() <= opposite.score()) ? opposite : primary;
         }
      }
   }

   private AutoFarm.EntryPatch findEntryPatch(Direction entrySide) {
      if (!this.matchesTargetEntrySide(entrySide)) {
         return null;
      }

      BlockPos center = BlockPos.ofFloored(mc.player.method_19538());
      Vec3d player = mc.player.method_19538();
      List<AutoFarm.EntryPatch> patches = new ArrayList<>();
      Direction entryRow = this.entryRowDirection();

      for (int x = -10; x <= 10; x++) {
         for (int y = -1; y <= 2; y++) {
            for (int z = -10; z <= 10; z++) {
               BlockPos base = center.add(x, y, z);
               if (this.isEntryPatchCandidate(base, entrySide, entryRow)) {
                  AutoFarm.PairKey key = this.pairKey(base, entrySide, entryRow);
                  if (!this.completedPairs.contains(key) && this.matchesTargetEntryPair(key) && this.matchesEntrySideForPair(entrySide, key)) {
                     Vec3d patchCenter = this.averageCenter(base, base.offset(entrySide), base.offset(entryRow), base.offset(entrySide).offset(entryRow));
                     Vec3d rel = patchCenter.subtract(player);
                     double side = this.dotHorizontal(rel, entrySide);
                     double row = this.dotHorizontal(rel, entryRow);
                     if (side >= 2.5 && side <= 8.5 && row >= -3.0 && row <= 5.5) {
                        double missingPenalty = 4 - this.countEntryPatchPumpkins(base, entrySide, entryRow);
                        double score = this.entryPairPriority(key) * 1000.0 + side * side + row * row + missingPenalty;
                        patches.add(new AutoFarm.EntryPatch(base.toImmutable(), entrySide, score));
                     }
                  }
               }
            }
         }
      }

      return patches.stream().min(Comparator.comparingDouble(AutoFarm.EntryPatch::score)).orElse(null);
   }

   private AutoFarm.EntryPatch findContainingPreparationPatch() {
      Direction lockedSide = this.lockedEntrySideDirection();
      if (lockedSide != null) {
         return this.findContainingPreparationPatch(lockedSide);
      } else {
         Direction primary = this.entrySideDirection();
         AutoFarm.EntryPatch primaryPatch = this.findContainingPreparationPatch(primary);
         AutoFarm.EntryPatch oppositePatch = this.findContainingPreparationPatch(primary.getOpposite());
         if (primaryPatch == null) {
            return oppositePatch;
         } else {
            return oppositePatch != null && !(primaryPatch.score() <= oppositePatch.score()) ? oppositePatch : primaryPatch;
         }
      }
   }

   private AutoFarm.EntryPatch findContainingPreparationPatch(Direction entrySide) {
      if (!this.matchesTargetEntrySide(entrySide)) {
         return null;
      }

      BlockPos center = BlockPos.ofFloored(mc.player.method_19538());
      Vec3d player = mc.player.method_19538();
      Direction entryRow = this.entryRowDirection();
      List<AutoFarm.EntryPatch> patches = new ArrayList<>();

      for (int x = -10; x <= 10; x++) {
         for (int y = -1; y <= 2; y++) {
            for (int z = -10; z <= 10; z++) {
               BlockPos base = center.add(x, y, z);
               if (this.isEntryPatchCandidate(base, entrySide, entryRow)) {
                  AutoFarm.PairKey key = this.pairKey(base, entrySide, entryRow);
                  if (!this.completedPairs.contains(key) && this.matchesTargetEntryPair(key) && this.matchesEntrySideForPair(entrySide, key)) {
                     double score = this.entryPairPriority(key) * 1000.0
                        + this.horizontalDistanceSq(player, this.getPreparationBounds(base, entrySide).center());
                     AutoFarm.EntryPatch patch = new AutoFarm.EntryPatch(base.toImmutable(), entrySide, score);
                     if (this.isPlayerInsidePreparationArea(patch)) {
                        patches.add(patch);
                     }
                  }
               }
            }
         }
      }

      return patches.stream().min(Comparator.comparingDouble(AutoFarm.EntryPatch::score)).orElse(null);
   }

   private Direction findEntrySideHint() {
      Direction lockedSide = this.lockedEntrySideDirection();
      if (lockedSide != null) {
         return lockedSide;
      } else {
         AutoFarm.EntryPatch patch = this.findEntryPatch();
         if (patch != null) {
            return patch.side();
         } else {
            Direction primary = this.entrySideDirection();
            Direction opposite = primary.getOpposite();
            double primaryScore = this.scoreEntrySide(primary);
            double oppositeScore = this.scoreEntrySide(opposite);
            if (Double.isInfinite(primaryScore) && Double.isInfinite(oppositeScore)) {
               return primary;
            } else {
               return primaryScore <= oppositeScore ? primary : opposite;
            }
         }
      }
   }

   private boolean shouldAcceptContainingPreparationPatch(AutoFarm.EntryPatch patch) {
      if (patch == null) {
         return false;
      } else {
         return !this.isUsableEntryPatch(this.entryPatch) ? true : this.isSameEntryPair(this.entryPatch, patch);
      }
   }

   private void updateNoEntryTargetTicks(AutoFarm.EntryPatch patch) {
      if (patch == null && !this.isUsableEntryPatch(this.entryPatch) && !this.hasEntryTargetHint()) {
         this.noEntryTargetTicks++;
      } else {
         this.noEntryTargetTicks = 0;
      }
   }

   private boolean shouldReturnHomeWithoutEntryTarget() {
      return this.noEntryTargetTicks >= 16;
   }

   private boolean hasEntryTargetHint() {
      Direction lockedSide = this.lockedEntrySideDirection();
      if (lockedSide != null) {
         return !Double.isInfinite(this.scoreEntrySide(lockedSide));
      }

      Direction primary = this.entrySideDirection();
      return !Double.isInfinite(this.scoreEntrySide(primary)) || !Double.isInfinite(this.scoreEntrySide(primary.getOpposite()));
   }

   private AutoFarm.PairKey findNextEntryPairKey() {
      return this.findNextEntryPairKey(this.entrySideDirection());
   }

   private AutoFarm.PairKey findNextEntryPairKey(Direction side) {
      BlockPos center = BlockPos.ofFloored(mc.player.method_19538());
      Direction entryRow = this.entryRowDirection();
      AutoFarm.PairKey best = null;
      double bestScore = Double.POSITIVE_INFINITY;

      for (int x = -10; x <= 10; x++) {
         for (int y = -1; y <= 2; y++) {
            for (int z = -10; z <= 10; z++) {
               BlockPos pos = center.add(x, y, z);
               if (this.isPumpkin(pos)) {
                  AutoFarm.PairKey key = this.entryPairKeyForPumpkin(pos, side, entryRow);
                  if (key != null
                     && !this.completedPairs.contains(key)
                     && !key.equals(this.currentPair)
                     && this.isComparableEntryPair(key)
                     && this.isAdjacentEntryPair(key)) {
                     double priority = this.entryPairPriority(key);
                     if (!(priority <= 0.0)) {
                        double score = priority * 1000.0 + this.horizontalDistanceSq(mc.player.method_19538(), pos.toCenterPos());
                        if (score < bestScore) {
                           bestScore = score;
                           best = key;
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private double scoreEntrySide(Direction side) {
      if (!this.matchesTargetEntrySide(side)) {
         return Double.POSITIVE_INFINITY;
      }

      BlockPos center = BlockPos.ofFloored(mc.player.method_19538());
      Vec3d player = mc.player.method_19538();
      Direction entryRow = this.entryRowDirection();
      double bestScore = Double.POSITIVE_INFINITY;

      for (int x = -10; x <= 10; x++) {
         for (int y = -1; y <= 2; y++) {
            for (int z = -10; z <= 10; z++) {
               BlockPos pos = center.add(x, y, z);
               if (this.isPumpkin(pos)) {
                  AutoFarm.PairKey key = this.entryPairKeyForPumpkin(pos, side, entryRow);
                  if (key != null && !this.completedPairs.contains(key) && this.matchesTargetEntryPair(key) && this.matchesEntrySideForPair(side, key)) {
                     Vec3d rel = pos.toCenterPos().subtract(player);
                     double lateral = this.dotHorizontal(rel, side);
                     double row = this.dotHorizontal(rel, entryRow);
                     if (!(lateral < 2.0) && !(lateral > 10.5) && !(row < -4.0) && !(row > 10.5)) {
                        double score = this.entryPairPriority(key) * 1000.0 + lateral * lateral + row * row;
                        if (score < bestScore) {
                           bestScore = score;
                        }
                     }
                  }
               }
            }
         }
      }

      return bestScore;
   }

   private AutoFarm.PairKey entryPairKeyForPumpkin(BlockPos pos, Direction side, Direction row) {
      if (this.isPumpkin(pos.offset(side))) {
         return this.pairKey(pos, side, row);
      }

      BlockPos base = pos.offset(side.getOpposite());
      return this.isPumpkin(base) ? this.pairKey(base, side, row) : null;
   }

   private double entryPairPriority(AutoFarm.PairKey key) {
      if (this.currentPair == null || key == null) {
         return 0.0;
      } else {
         return !this.isComparableEntryPair(key) ? 1000.0 : Math.abs(key.lateral() - this.currentPair.lateral());
      }
   }

   private boolean isComparableEntryPair(AutoFarm.PairKey key) {
      return key != null && (this.currentPair == null || key.rowAxis() == this.currentPair.rowAxis() && key.y() == this.currentPair.y());
   }

   private boolean matchesTargetEntryPair(AutoFarm.PairKey key) {
      if (key == null) {
         return false;
      } else {
         return this.targetEntryPair == null ? this.isAdjacentEntryPair(key) : this.targetEntryPair.equals(key);
      }
   }

   private boolean isAdjacentEntryPair(AutoFarm.PairKey key) {
      if (this.currentPair == null) {
         return key != null;
      }

      if (!this.isComparableEntryPair(key)) {
         return false;
      }

      int distance = Math.abs(key.lateral() - this.currentPair.lateral());
      return distance >= 3 && distance <= 7;
   }

   private Direction targetEntrySideDirection() {
      return this.entrySideDirectionForPair(this.targetEntryPair);
   }

   private Direction entrySideDirectionForPair(AutoFarm.PairKey key) {
      if (this.currentPair != null && key != null && this.isComparableEntryPair(key)) {
         int delta = key.lateral() - this.currentPair.lateral();
         if (delta == 0) {
            return null;
         }

         Axis sideAxis = this.currentPair.rowAxis() == Axis.X ? Axis.Z : Axis.X;
         Direction positive = this.positiveDirection(sideAxis);
         return delta > 0 ? positive : positive.getOpposite();
      } else {
         return null;
      }
   }

   private boolean matchesTargetEntrySide(Direction side) {
      Direction targetSide = this.targetEntrySideDirection();
      return targetSide == null || side == targetSide;
   }

   private Direction lockedEntrySideDirection() {
      Direction targetSide = this.targetEntrySideDirection();
      return targetSide != null ? targetSide : this.transitionSideDirection;
   }

   private boolean matchesEntrySideForPair(Direction side, AutoFarm.PairKey key) {
      Direction pairSide = this.entrySideDirectionForPair(key);
      return pairSide == null || side == pairSide;
   }

   private boolean isSameEntryPair(AutoFarm.EntryPatch first, AutoFarm.EntryPatch second) {
      if (first != null && second != null) {
         Direction entryRow = this.entryRowDirection();
         return this.pairKey(first.base(), first.side(), entryRow).equals(this.pairKey(second.base(), second.side(), entryRow));
      } else {
         return false;
      }
   }

   private boolean isEntryPatchCandidate(BlockPos base, Direction entrySide, Direction entryRow) {
      return this.countEntryPatchPumpkins(base, entrySide, entryRow) >= 1 ? true : this.hasEntryPatchSupport(base, entrySide, entryRow);
   }

   private int countEntryPatchPumpkins(BlockPos base, Direction entrySide, Direction entryRow) {
      int pumpkins = 0;
      if (this.isPumpkin(base)) {
         pumpkins++;
      }

      if (this.isPumpkin(base.offset(entrySide))) {
         pumpkins++;
      }

      if (this.isPumpkin(base.offset(entryRow))) {
         pumpkins++;
      }

      if (this.isPumpkin(base.offset(entrySide).offset(entryRow))) {
         pumpkins++;
      }

      return pumpkins;
   }

   private boolean hasEntryPatchSupport(BlockPos base, Direction entrySide, Direction entryRow) {
      for (int rowOffset = 2; rowOffset <= 4; rowOffset++) {
         BlockPos firstLane = base.offset(entryRow, rowOffset);
         BlockPos secondLane = firstLane.offset(entrySide);
         if (this.isPumpkin(firstLane) || this.isPumpkin(secondLane)) {
            return true;
         }
      }

      return false;
   }

   private void markCurrentPairCompleted() {
      if (this.currentPair != null) {
         this.completedPairs.add(this.currentPair);
      }
   }

   private void syncCurrentPairFromPlayer() {
      if (mc.player != null) {
         BlockPos playerPos = BlockPos.ofFloored(mc.player.method_19538());
         if (this.currentPair != null) {
            playerPos = playerPos.withY(this.currentPair.y());
         }

         this.currentPair = this.pairKey(playerPos, this.sideDirection, this.rowDirection);
      }
   }

   private AutoFarm.PairKey pairKey(BlockPos base) {
      return this.pairKey(base, this.sideDirection, this.rowDirection);
   }

   private AutoFarm.PairKey pairKey(BlockPos base, Direction side, Direction row) {
      BlockPos other = base.offset(side);
      Axis sideAxis = side.getAxis();
      int lateral = Math.min(this.axisValue(base, sideAxis), this.axisValue(other, sideAxis));
      return new AutoFarm.PairKey(row.getAxis(), base.method_10264(), lateral);
   }

   private boolean isPlayerInsidePreparationArea(AutoFarm.EntryPatch patch) {
      if (mc.player == null) {
         return false;
      }

      AutoFarm.AreaBounds bounds = this.getPreparationBounds(patch);
      Box playerBox = mc.player.method_5829();
      return playerBox.minX >= bounds.minX() - 0.05
         && playerBox.maxX <= bounds.maxX() + 0.05
         && playerBox.minZ >= bounds.minZ() - 0.05
         && playerBox.maxZ <= bounds.maxZ() + 0.05;
   }

   private boolean isPlayerInsideEntryPatch(AutoFarm.EntryPatch patch) {
      if (mc.player == null) {
         return false;
      }

      AutoFarm.AreaBounds bounds = this.getEntryPatchBounds(patch);
      Box playerBox = mc.player.method_5829();
      return playerBox.minX >= bounds.minX() - 0.05
         && playerBox.maxX <= bounds.maxX() + 0.05
         && playerBox.minZ >= bounds.minZ() - 0.05
         && playerBox.maxZ <= bounds.maxZ() + 0.05;
   }

   private boolean isUsableEntryPatch(AutoFarm.EntryPatch patch) {
      if (patch != null && mc.player != null) {
         Direction entryRow = this.entryRowDirection();
         AutoFarm.PairKey key = this.pairKey(patch.base(), patch.side(), entryRow);
         if (this.completedPairs.contains(key)) {
            return false;
         } else if (this.matchesTargetEntryPair(key) && this.matchesEntrySideForPair(patch.side(), key)) {
            return !this.isEntryPatchCandidate(patch.base(), patch.side(), entryRow)
               ? false
               : this.horizontalDistanceSq(mc.player.method_19538(), this.getPreparationBounds(patch).center()) <= 256.0;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private float yawToPreparationArea(AutoFarm.EntryPatch patch) {
      Vec3d center = this.getPreparationBounds(patch).center();
      double dx = center.x - mc.player.method_23317();
      double dz = center.z - mc.player.method_23321();
      return (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
   }

   private float yawToEntryPatch(AutoFarm.EntryPatch patch) {
      Vec3d center = this.getEntryPatchBounds(patch).center();
      double dx = center.x - mc.player.method_23317();
      double dz = center.z - mc.player.method_23321();
      return (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
   }

   private AutoFarm.AreaBounds getPreparationBounds(AutoFarm.EntryPatch patch) {
      return this.getPreparationBounds(patch.base(), patch.side());
   }

   private AutoFarm.AreaBounds getPreparationBounds(BlockPos patchBase, Direction patchSide) {
      List<BlockPos> positions = new ArrayList<>();
      Direction entryRow = this.entryRowDirection();

      for (int depth = 1; depth <= 2; depth++) {
         BlockPos base = patchBase.offset(entryRow.getOpposite(), depth);
         positions.add(base);
         positions.add(base.offset(patchSide));
      }

      return this.boundsFor(positions);
   }

   private AutoFarm.AreaBounds getEntryPatchBounds(AutoFarm.EntryPatch patch) {
      return this.boundsFor(patch.targets(this.entryRowDirection()));
   }

   private AutoFarm.AreaBounds boundsFor(List<BlockPos> positions) {
      double minX = Double.POSITIVE_INFINITY;
      double minZ = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxZ = Double.NEGATIVE_INFINITY;

      for (BlockPos pos : positions) {
         minX = Math.min(minX, pos.method_10263());
         minZ = Math.min(minZ, pos.method_10260());
         maxX = Math.max(maxX, pos.method_10263() + 1.0);
         maxZ = Math.max(maxZ, pos.method_10260() + 1.0);
      }

      return new AutoFarm.AreaBounds(minX, minZ, maxX, maxZ);
   }

   private BlockHitResult findFarmBreakTarget() {
      BlockHitResult directHit = this.raycastFromRotation(this.rotationYaw, this.rotationPitch);
      if (directHit != null && this.isPumpkin(directHit.getBlockPos()) && this.isFarmTargetPosition(directHit.getBlockPos())) {
         return directHit;
      }

      Vec3d eyes = mc.player.method_33571();
      Vec3d look = this.rotationVector(this.rotationYaw, this.rotationPitch);
      BlockPos center = BlockPos.ofFloored(mc.player.method_19538());
      List<AutoFarm.BreakCandidate> candidates = new ArrayList<>();
      int range = MathHelper.ceil(5.0F) + 1;

      for (int x = -range; x <= range; x++) {
         for (int y = -1; y <= 2; y++) {
            for (int z = -range; z <= range; z++) {
               BlockPos pos = center.add(x, y, z);
               if (this.isPumpkin(pos) && !(eyes.squaredDistanceTo(pos.toCenterPos()) > 25.0) && this.isFarmTargetPosition(pos)) {
                  BlockHitResult hit = this.findBlockHit(pos, eyes);
                  if (hit != null) {
                     Vec3d target = hit.method_17784().subtract(eyes).normalize();
                     double alignment = look.dotProduct(target);
                     if (!(alignment < 0.82)) {
                        double score = 1.0 - look.dotProduct(target) + eyes.squaredDistanceTo(hit.method_17784()) * 0.03;
                        candidates.add(new AutoFarm.BreakCandidate(hit, score));
                     }
                  }
               }
            }
         }
      }

      return candidates.stream().min(Comparator.comparingDouble(AutoFarm.BreakCandidate::score)).map(AutoFarm.BreakCandidate::hit).orElse(null);
   }

   private boolean isFarmTargetPosition(BlockPos pos) {
      Vec3d rel = pos.toCenterPos().subtract(mc.player.method_19538());
      double row = this.dotHorizontal(rel, this.rowDirection);
      double side = this.dotHorizontal(rel, this.sideDirection);
      return row >= -1.25 && row <= 5.25 && side >= -1.75 && side <= 3.25;
   }

   private BlockHitResult findEntryBreakTarget(AutoFarm.EntryPatch patch) {
      Vec3d eyes = mc.player.method_33571();

      for (BlockPos pos : patch.targets(this.entryRowDirection())) {
         if (this.isPumpkin(pos)) {
            BlockHitResult hit = this.findBlockHit(pos, eyes);
            if (hit != null) {
               return hit;
            }
         }
      }

      return null;
   }

   private BlockHitResult findBlockHit(BlockPos pos, Vec3d eyes) {
      for (float[] sample : SAMPLES) {
         Vec3d point = new Vec3d(pos.method_10263() + sample[0], pos.method_10264() + sample[1], pos.method_10260() + sample[2]);
         if (!(eyes.squaredDistanceTo(point) > 25.0)) {
            BlockHitResult result = mc.world.method_17742(new RaycastContext(eyes, point, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
            if (result != null && result.method_17783() == Type.BLOCK && result.getBlockPos().equals(pos)) {
               return result;
            }
         }
      }

      return null;
   }

   private void breakBlock(BlockHitResult hit, float yaw, float pitch) {
      if (mc.player != null && mc.interactionManager != null && mc.world != null && this.isPumpkin(hit.getBlockPos())) {
         BlockHitResult currentHit = this.raycastFromRotation(yaw, pitch);
         if (currentHit != null && currentHit.getBlockPos().equals(hit.getBlockPos()) && this.isPumpkin(currentHit.getBlockPos())) {
            if (mc.player.method_7337()) {
               mc.interactionManager.attackBlock(currentHit.getBlockPos(), currentHit.getSide());
            } else {
               mc.interactionManager.updateBlockBreakingProgress(currentHit.getBlockPos(), currentHit.getSide());
            }

            mc.player.method_6104(Hand.MAIN_HAND);
         }
      }
   }

   private BlockHitResult raycastFromRotation(float yaw, float pitch) {
      if (mc.player != null && mc.world != null) {
         Vec3d eyes = mc.player.method_33571();
         Vec3d end = eyes.add(this.rotationVector(yaw, pitch).multiply(5.0));
         BlockHitResult hit = mc.world.method_17742(new RaycastContext(eyes, end, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         return hit != null && hit.method_17783() == Type.BLOCK && !(eyes.squaredDistanceTo(hit.method_17784()) > 25.0) ? hit : null;
      } else {
         return null;
      }
   }

   private boolean handleAutoFeed() {
      AutoFarm.AutoFeed feedMode = this.autoFeed.getValue();
      long now = System.currentTimeMillis();
      if (feedMode == AutoFarm.AutoFeed.None) {
         this.stopEating();
         this.clearEatingDelays();
         return false;
      }

      if (feedMode == AutoFarm.AutoFeed.Command) {
         this.stopEating();
         this.clearEatingDelays();
         if (mc.player.method_7344().getFoodLevel() <= 6 && now - this.lastFeedCommandMs >= 1500L) {
            this.sendChatCommand("feed");
            this.lastFeedCommandMs = now;
         }

         return false;
      } else {
         if (this.eatResumeDelayUntilMs > 0L) {
            if (now < this.eatResumeDelayUntilMs) {
               this.releaseMovement();
               return true;
            }

            this.eatResumeDelayUntilMs = 0L;
         }

         if (mc.player.method_7344().getFoodLevel() <= 6) {
            this.releaseMovement();
            if (!this.eating && this.eatStartDelayUntilMs == 0L) {
               this.eatStartDelayUntilMs = now + 200L;
            }

            if (now < this.eatStartDelayUntilMs) {
               return true;
            } else {
               this.eatStartDelayUntilMs = 0L;
               if (!this.prepareFood()) {
                  this.stopEating();
                  this.clearEatingDelays();
                  return false;
               } else {
                  this.eating = true;
                  this.startEatingWithPreparedFood();
                  return true;
               }
            }
         } else {
            boolean wasEating = this.eating || this.switchedFoodSlot || this.eatStartDelayUntilMs > 0L || this.isConsumingItem();
            this.stopEating();
            this.eatStartDelayUntilMs = 0L;
            if (wasEating) {
               this.eatResumeDelayUntilMs = now + 200L;
               this.releaseMovement();
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private boolean shouldAutoFeedInterruptFlagUse() {
      return (this.flagUsePending || this.flagUseKeyHeld)
         && this.autoFeed.getValue() == AutoFarm.AutoFeed.Eating
         && mc.player != null
         && mc.player.method_7344().getFoodLevel() <= 6;
   }

   private void startEatingWithPreparedFood() {
      if (!mc.player.method_6115() && this.isFood(mc.player.method_6079()) && mc.interactionManager != null) {
         mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
         mc.player.method_6019(Hand.OFF_HAND);
      } else if (mc.currentScreen != null && !mc.player.method_6115()) {
         ((IMinecraftClient)mc).idoItemUse();
      }

      mc.options.useKey.setPressed(true);
   }

   private boolean prepareFood() {
      if (!this.isFood(mc.player.method_6079()) && !this.isFood(mc.player.method_6047())) {
         int foodSlot = this.findHotbarFood();
         if (foodSlot == -1) {
            return false;
         }

         if (!this.switchedFoodSlot) {
            this.previousFoodSlot = mc.player.method_31548().selectedSlot;
            this.switchedFoodSlot = true;
         }

         if (mc.player.method_31548().selectedSlot != foodSlot) {
            InventoryUtility.switchTo(foodSlot);
         }

         return true;
      } else {
         return true;
      }
   }

   private int findHotbarFood() {
      for (int i = 0; i < 9; i++) {
         if (this.isFood(mc.player.method_31548().method_5438(i))) {
            return i;
         }
      }

      return -1;
   }

   private void stopEating() {
      if (this.eating || this.switchedFoodSlot) {
         this.eating = false;
         if (mc.options != null) {
            mc.options.useKey.setPressed(false);
         }

         if (!this.switchedFoodSlot || this.previousFoodSlot < 0 || this.previousFoodSlot >= 9 || !this.isConsumingItem()) {
            if (this.switchedFoodSlot && this.previousFoodSlot >= 0 && this.previousFoodSlot < 9) {
               InventoryUtility.switchTo(this.previousFoodSlot);
            }

            this.previousFoodSlot = -1;
            this.switchedFoodSlot = false;
         }
      }
   }

   private void clearEatingDelays() {
      this.eatStartDelayUntilMs = 0L;
      this.eatResumeDelayUntilMs = 0L;
   }

   private boolean isFood(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.method_57353().contains(DataComponentTypes.FOOD);
   }

   private boolean isConsumingItem() {
      if (mc.player != null && mc.player.method_6115()) {
         UseAction useAction = mc.player.method_6030().getUseAction();
         return useAction == UseAction.EAT || useAction == UseAction.DRINK;
      } else {
         return false;
      }
   }

   private void handleAutoFix() {
      if (this.autoFix.getValue() && mc.player != null) {
         ItemStack stack = mc.player.method_6047();
         if (!stack.isEmpty() && stack.getMaxDamage() > 0) {
            int durability = (int)((stack.getMaxDamage() - stack.getDamage()) / Math.max(0.1, stack.getMaxDamage()) * 100.0);
            if (durability <= 20) {
               long now = System.currentTimeMillis();
               if (now - this.lastFixCommandMs >= 3000L) {
                  this.sendChatCommand("fix");
                  this.lastFixCommandMs = now;
               }
            }
         }
      }
   }

   private void setFarmRotation() {
      this.setRotation(this.farmYaw(), 35.5F);
   }

   private void setFarmPauseRotation() {
      this.setRotation(this.farmYaw(), 90.0F);
   }

   private float farmYaw() {
      return this.yawFor(this.rowDirection) + (this.isRightOf(this.rowDirection, this.sideDirection) ? 45.0F : -45.0F);
   }

   private void setRotation(float yaw, float pitch) {
      this.rotationYaw = MathHelper.wrapDegrees(yaw);
      this.rotationPitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
      this.moveFixYaw = this.rotationYaw;
   }

   private void applyFarmMovement() {
      boolean movementPulseActive = this.shouldApplyFarmMovementInput();
      this.controllingMovement = movementPulseActive;
      mc.options.forwardKey.setPressed(movementPulseActive);
      mc.options.backKey.setPressed(false);
      boolean sideIsRight = this.isRightOf(this.rowDirection, this.sideDirection);
      mc.options.leftKey.setPressed(movementPulseActive && sideIsRight);
      mc.options.rightKey.setPressed(movementPulseActive && !sideIsRight);
      mc.options.jumpKey.setPressed(false);
      mc.options.sprintKey.setPressed(false);
      mc.player.method_5728(false);
   }

   private boolean isFarmFlagPauseActive() {
      long now = System.currentTimeMillis();
      if (this.pauseWhileFlag.getValue() && this.phase == AutoFarm.Phase.Farming) {
         this.expireFlagWindow(now);
         if (this.flagPauseUntilMs <= 0L) {
            return false;
         } else if (now >= this.flagPauseUntilMs) {
            this.flagPauseUntilMs = 0L;
            this.clearFlagWindow();
            this.releaseFlagUseHold();
            this.resetStuckState();
            this.resetFarmBpsLimiter();
            return false;
         } else {
            return true;
         }
      } else {
         this.flagPauseUntilMs = 0L;
         this.clearFlagWindow();
         this.releaseFlagUseHold();
         return false;
      }
   }

   private boolean shouldStartFlagPause(long now) {
      this.expireFlagWindow(now);
      if (this.flagWindowStartMs == 0L) {
         this.flagWindowStartMs = now;
         this.flagWindowCount = 1;
         return false;
      } else {
         this.flagWindowCount++;
         return this.flagWindowCount >= 2;
      }
   }

   private void expireFlagWindow(long now) {
      if (this.flagWindowStartMs != 0L && now - this.flagWindowStartMs > 200L) {
         this.clearFlagWindow();
      }
   }

   private void clearFlagWindow() {
      this.flagWindowStartMs = 0L;
      this.flagWindowCount = 0;
   }

   private void updateFlagUseHold() {
      long now = System.currentTimeMillis();
      if ((this.flagUsePending || this.flagUseKeyHeld) && this.flagPauseUntilMs > 0L && this.phase == AutoFarm.Phase.Farming) {
         if (mc.options == null || mc.player == null) {
            this.releaseFlagUseHold();
         } else if (!this.flagUseKeyHeld) {
            if (mc.currentScreen != null && !mc.player.method_6115()) {
               ((IMinecraftClient)mc).idoItemUse();
            }

            mc.options.useKey.setPressed(true);
            this.flagUseStartMs = now;
            this.flagUsePending = false;
            this.flagUseKeyHeld = true;
         } else if (now - this.flagUseStartMs >= 250L) {
            this.releaseFlagUseHold();
         } else {
            mc.options.useKey.setPressed(true);
         }
      } else {
         this.releaseFlagUseHold();
      }
   }

   private void releaseFlagUseHold() {
      boolean wasHeld = this.flagUseKeyHeld;
      this.flagUseStartMs = 0L;
      this.flagUsePending = false;
      this.flagUseKeyHeld = false;
      if (wasHeld && mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      if (wasHeld && mc.player != null && mc.interactionManager != null && mc.player.method_6115()) {
         mc.interactionManager.stopUsingItem(mc.player);
      }
   }

   private boolean shouldApplyFarmMovementInput() {
      if (this.limitBps.getValue() && mc.player != null && this.phase == AutoFarm.Phase.Farming) {
         long now = System.currentTimeMillis();
         Vec3d current = mc.player.method_19538();
         if (this.farmLimitLastPos != null && this.farmLimitLastMs != 0L) {
            long elapsedMs = now - this.farmLimitLastMs;
            if (elapsedMs <= 0L) {
               return this.farmLimitCredit >= 0.0;
            }

            double moved = Math.sqrt(this.horizontalDistanceSq(current, this.farmLimitLastPos));
            double targetBps = Math.max(0.1, this.limit.getValue().floatValue());
            double allowed = targetBps * elapsedMs / 1000.0;
            this.farmLimitCredit += allowed - moved;
            double maxCredit = Math.max(0.05, targetBps * 0.025);
            double maxDebt = Math.max(0.25, targetBps * 0.2);
            this.farmLimitCredit = Math.max(-maxDebt, Math.min(maxCredit, this.farmLimitCredit));
            this.farmLimitLastPos = current;
            this.farmLimitLastMs = now;
            return this.farmLimitCredit >= 0.0;
         } else {
            this.farmLimitLastPos = current;
            this.farmLimitLastMs = now;
            this.farmLimitCredit = 0.0;
            return true;
         }
      } else {
         this.resetFarmBpsLimiter();
         return true;
      }
   }

   private void resetFarmBpsLimiter() {
      this.farmLimitLastPos = null;
      this.farmLimitLastMs = 0L;
      this.farmLimitCredit = 0.0;
   }

   private void pressForwardOnly() {
      this.controllingMovement = true;
      mc.options.forwardKey.setPressed(true);
      mc.options.backKey.setPressed(false);
      mc.options.leftKey.setPressed(false);
      mc.options.rightKey.setPressed(false);
      mc.options.jumpKey.setPressed(false);
      mc.options.sprintKey.setPressed(false);
      mc.player.method_5728(false);
   }

   private void releaseMovement() {
      this.controllingMovement = false;
      this.moveFixYaw = Float.NaN;
      this.resetFarmBpsLimiter();
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         if (mc.player != null) {
            mc.player.method_5728(false);
         }
      }
   }

   private boolean isStuck() {
      if (!this.controllingMovement || mc.player == null) {
         this.resetStuckState();
         return false;
      }

      if (mc.player.field_5976) {
         return true;
      }

      Vec3d current = mc.player.method_19538();
      long now = System.currentTimeMillis();
      if (this.lastPosition == null) {
         this.lastPosition = current;
         this.lastMoveCheckMs = now;
         return false;
      }

      if (now - this.lastMoveCheckMs < 250L) {
         return this.stuckTicks >= 2;
      }

      double dx = Math.abs(current.x - this.lastPosition.x);
      double dz = Math.abs(current.z - this.lastPosition.z);
      this.stuckTicks = dx < 0.035 && dz < 0.035 ? this.stuckTicks + 1 : 0;
      this.lastPosition = current;
      this.lastMoveCheckMs = now;
      return this.stuckTicks >= 2;
   }

   private void resetStuckState() {
      this.lastPosition = null;
      this.lastMoveCheckMs = 0L;
      this.stuckTicks = 0;
   }

   private Direction getReturnDirection(Vec3d current, Vec3d target) {
      Direction rowPositive = this.positiveDirection(this.rowDirection.getAxis());
      double rowDiff = this.axisValue(target, this.rowDirection.getAxis()) - this.axisValue(current, this.rowDirection.getAxis());
      if (Math.abs(rowDiff) > 0.75) {
         return rowDiff > 0.0 ? rowPositive : rowPositive.getOpposite();
      } else {
         Direction sidePositive = this.positiveDirection(this.sideDirection.getAxis());
         double sideDiff = this.axisValue(target, this.sideDirection.getAxis()) - this.axisValue(current, this.sideDirection.getAxis());
         if (Math.abs(sideDiff) > 0.75) {
            return sideDiff > 0.0 ? sidePositive : sidePositive.getOpposite();
         } else {
            return null;
         }
      }
   }

   private Direction positiveDirection(Axis axis) {
      return axis == Axis.X ? Direction.EAST : Direction.SOUTH;
   }

   private double axisValue(Vec3d vec, Axis axis) {
      return axis == Axis.X ? vec.x : vec.z;
   }

   private int axisValue(BlockPos pos, Axis axis) {
      return axis == Axis.X ? pos.method_10263() : pos.method_10260();
   }

   private boolean isPumpkin(BlockPos pos) {
      if (!this.isLoaded(pos)) {
         return false;
      }

      Block block = mc.world.method_8320(pos).method_26204();
      return block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN || block == Blocks.JACK_O_LANTERN;
   }

   private boolean isLoaded(BlockPos pos) {
      return mc.world != null && mc.world.method_8393(pos.method_10263() >> 4, pos.method_10260() >> 4);
   }

   private float[] calculateAngle(Vec3d to) {
      Vec3d from = mc.player.method_33571();
      double difX = to.x - from.x;
      double difY = (to.y - from.y) * -1.0;
      double difZ = to.z - from.z;
      double dist = MathHelper.sqrt((float)(difX * difX + difZ * difZ));
      return new float[]{
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0),
         (float)MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))), -90.0, 90.0)
      };
   }

   private Vec3d rotationVector(float yaw, float pitch) {
      float yawRad = yaw * (float) (Math.PI / 180.0);
      float pitchRad = pitch * (float) (Math.PI / 180.0);
      float cosPitch = MathHelper.cos(pitchRad);
      return new Vec3d(-MathHelper.sin(yawRad) * cosPitch, -MathHelper.sin(pitchRad), MathHelper.cos(yawRad) * cosPitch).normalize();
   }

   private Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
      double lengthSq = movementInput.lengthSquared();
      if (lengthSq < 1.0E-7) {
         return Vec3d.ZERO;
      }

      Vec3d movement = (lengthSq > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
      float sin = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
      float cos = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
      return new Vec3d(movement.x * cos - movement.z * sin, movement.y, movement.z * cos + movement.x * sin);
   }

   private double dotHorizontal(Vec3d vec, Direction direction) {
      return vec.x * direction.getOffsetX() + vec.z * direction.getOffsetZ();
   }

   private double horizontalDistanceSq(Vec3d a, Vec3d b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return dx * dx + dz * dz;
   }

   private Vec3d averageCenter(BlockPos... positions) {
      double x = 0.0;
      double y = 0.0;
      double z = 0.0;

      for (BlockPos pos : positions) {
         Vec3d center = pos.toCenterPos();
         x += center.x;
         y += center.y;
         z += center.z;
      }

      return new Vec3d(x / positions.length, y / positions.length, z / positions.length);
   }

   private Direction nearestHorizontal(float yaw) {
      int index = MathHelper.floor(yaw / 90.0 + 0.5) & 3;

      return switch (index) {
         case 0 -> Direction.SOUTH;
         case 1 -> Direction.WEST;
         case 2 -> Direction.NORTH;
         default -> Direction.EAST;
      };
   }

   private float yawFor(Direction direction) {
      return switch (direction) {
         case SOUTH -> 0.0F;
         case WEST -> 90.0F;
         case NORTH -> 180.0F;
         case EAST -> -90.0F;
         default -> mc.player != null ? mc.player.method_36454() : 0.0F;
      };
   }

   private Direction rotateRight(Direction direction) {
      return switch (direction) {
         case SOUTH -> Direction.WEST;
         case WEST -> Direction.NORTH;
         case NORTH -> Direction.EAST;
         case EAST -> Direction.SOUTH;
         default -> Direction.EAST;
      };
   }

   private boolean isRightOf(Direction forward, Direction side) {
      return this.rotateRight(forward) == side;
   }

   private Direction entryRowDirection() {
      return this.nextRowDirection == null ? this.rowDirection : this.nextRowDirection;
   }

   private Direction entrySideDirection() {
      return this.transitionSideDirection == null ? this.sideDirection : this.transitionSideDirection;
   }

   private void resetState(boolean releaseControls) {
      this.phase = AutoFarm.Phase.Farming;
      this.startPos = null;
      this.rowDirection = Direction.NORTH;
      this.sideDirection = Direction.EAST;
      this.exitDirection = Direction.NORTH;
      this.nextRowDirection = null;
      this.transitionSideDirection = null;
      this.alignDirection = Direction.WEST;
      this.entryPatch = null;
      this.currentPair = null;
      this.firstPair = null;
      this.targetEntryPair = null;
      this.firstRowDirection = Direction.NORTH;
      this.firstSideDirection = Direction.EAST;
      this.completedPairs.clear();
      this.pendingBreak = null;
      this.moveFixYaw = Float.NaN;
      this.initialized = false;
      this.eating = false;
      this.switchedFoodSlot = false;
      this.previousFoodSlot = -1;
      this.noPumpkinAheadTicks = 0;
      this.noEntryTargetTicks = 0;
      this.enterRowRetries = 0;
      this.phaseTicks = 0;
      this.lastFeedCommandMs = 0L;
      this.lastFixCommandMs = 0L;
      this.eatStartDelayUntilMs = 0L;
      this.eatResumeDelayUntilMs = 0L;
      this.flagPauseUntilMs = 0L;
      this.clearFlagWindow();
      if (!releaseControls) {
         this.flagUseStartMs = 0L;
         this.flagUsePending = false;
         this.flagUseKeyHeld = false;
      }

      this.resetFarmBpsLimiter();
      this.resetStuckState();
      if (releaseControls) {
         this.resetRuntimeControls();
      }
   }

   private void resetRuntimeControls() {
      this.pendingBreak = null;
      this.flagPauseUntilMs = 0L;
      this.clearFlagWindow();
      this.releaseFlagUseHold();
      this.stopEating();
      this.clearEatingDelays();
      this.releaseMovement();
      this.moveFixYaw = Float.NaN;
      this.releaseAutoFarmFreeLook();
   }

   private void updateAutoFarmFreeLook() {
      if (this.mode.getValue() != AutoFarm.Mode.Pumpkin || mc.player == null) {
         this.releaseAutoFarmFreeLook();
      } else if (!this.autoFarmFreeLookActive) {
         this.autoFarmFreeLookActive = true;
         FreeLookState.setAutoFarmActive(true);
         if (mc.player instanceof CameraOverriddenEntity camera) {
            camera.setCameraYaw(mc.player.method_36454());
            camera.setCameraPitch(mc.player.method_36455());
         }
      }
   }

   private void releaseAutoFarmFreeLook() {
      if (this.autoFarmFreeLookActive) {
         this.restorePlayerLookFromAutoFarmCamera();
         this.autoFarmFreeLookActive = false;
         FreeLookState.setAutoFarmActive(false);
      }
   }

   private void restorePlayerLookFromAutoFarmCamera() {
      if (mc.player != null && !FreeLookState.isManualActive() && mc.player instanceof CameraOverriddenEntity camera) {
         float var4 = MathHelper.wrapDegrees(camera.getCameraYaw());
         float pitch = MathHelper.clamp(camera.getCameraPitch(), -90.0F, 90.0F);
         mc.player.method_36456(var4);
         mc.player.method_36457(pitch);
         mc.player.field_5982 = var4;
         mc.player.field_6004 = pitch;
         mc.player.field_6241 = var4;
         mc.player.field_6283 = var4;
         this.rotationYaw = var4;
         this.rotationPitch = pitch;
      }
   }

   public boolean isAutoFarmFreeLookActive() {
      return this.autoFarmFreeLookActive;
   }

   public float getSilentRotationYaw() {
      return this.rotationYaw;
   }

   public float getSilentRotationPitch() {
      return this.rotationPitch;
   }

   private record AreaBounds(double minX, double minZ, double maxX, double maxZ) {
      private Vec3d center() {
         return new Vec3d((this.minX + this.maxX) * 0.5, 0.0, (this.minZ + this.maxZ) * 0.5);
      }
   }

   private enum AutoFeed {
      Command,
      Eating,
      None;
   }

   private record BreakCandidate(BlockHitResult hit, double score) {
   }

   private record EntryPatch(BlockPos base, Direction side, double score) {
      private List<BlockPos> targets(Direction row) {
         return List.of(this.base, this.base.offset(this.side), this.base.offset(row), this.base.offset(this.side).offset(row));
      }
   }

   private enum Mode {
      Pumpkin;
   }

   private record PairCandidate(Direction row, Direction side, BlockPos base, int score, double distanceSq) {
   }

   private record PairKey(Axis rowAxis, int y, int lateral) {
   }

   private enum Phase {
      Farming,
      ExitingRow,
      Turning,
      ClearingEntry,
      EnteringRow,
      AligningEdge,
      ReturningHome,
      Idle;
   }
}
