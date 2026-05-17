package vcore.features.modules.base;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.InteractionUtility;
import vcore.utility.world.HoleUtility;

public abstract class TrapModule extends PlaceModule {
   protected final Setting<TrapModule.PlaceTiming> placeTiming = new Setting<>("Place Timing", TrapModule.PlaceTiming.Vanilla);
   protected final Setting<Integer> blocksPerTick = new Setting<>("Block/Tick", 8, 1, 12, v -> this.placeTiming.getValue() == TrapModule.PlaceTiming.Default);
   protected final Setting<Integer> placeDelay = new Setting<>("Delay/Place", 0, 0, 10);
   protected final Setting<TrapModule.TrapMode> trapMode = new Setting<>("Trap Mode", TrapModule.TrapMode.Full);
   private int delay;
   protected PlayerEntity target;
   private final ArrayList<BlockPos> sequentialBlocks = new ArrayList<>();

   public static void handleRotateSync(
      BlockPos targetBlock,
      Setting<InteractionUtility.Rotate> rotateSetting,
      Setting<TrapModule.PlaceTiming> placeTiming,
      Setting<InteractionUtility.Interact> interact
   ) {
      if (placeTiming.getValue() == TrapModule.PlaceTiming.Vanilla && !rotateSetting.getValue().equals(InteractionUtility.Rotate.None) && mc.player != null) {
         BlockHitResult result = InteractionUtility.getPlaceResult(targetBlock, interact.getValue(), false);
         if (result != null) {
            float[] angle = InteractionUtility.calculateAngle(result.method_17784());
            mc.player.method_36456(angle[0]);
            mc.player.method_36457(angle[1]);
         }
      }
   }

   public TrapModule(@NotNull String name, @NotNull Module.Category category) {
      super(name, category);
   }

   public TrapModule(@NotNull String name, @NotNull String description, @NotNull Module.Category category) {
      super(name, description, category);
   }

   protected abstract boolean needNewTarget();

   @Nullable
   protected abstract PlayerEntity getTarget();

   @Override
   public void onDisable() {
      this.target = null;
      this.delay = 0;
      this.sequentialBlocks.clear();
   }

   @EventHandler
   private void onSync(EventSync event) {
      if (this.needNewTarget()) {
         this.target = this.getTarget();
      } else {
         if (this.placeTiming.getValue() == TrapModule.PlaceTiming.Vanilla && !this.rotate.is(InteractionUtility.Rotate.None)) {
            BlockPos targetBlock = this.getBlockToPlace();
            if (targetBlock != null && mc.player != null) {
               BlockHitResult result = InteractionUtility.getPlaceResult(targetBlock, this.interact.getValue(), false);
               if (result != null) {
                  float[] angle = InteractionUtility.calculateAngle(result.method_17784());
                  mc.player.method_36456(angle[0]);
                  mc.player.method_36457(angle[1]);
               }
            }
         }
      }
   }

   @EventHandler
   private void onPostSync(EventPostSync event) {
      if (this.delay > 0) {
         this.delay--;
      } else {
         InteractionUtility.Rotate rotateMod = this.placeTiming.is(TrapModule.PlaceTiming.Vanilla) && !this.rotate.is(InteractionUtility.Rotate.None)
            ? InteractionUtility.Rotate.None
            : this.rotate.getValue();
         if (this.placeTiming.getValue() == TrapModule.PlaceTiming.Default) {
            int placed = 0;

            while (placed < this.blocksPerTick.getValue()) {
               BlockPos targetBlock = this.getBlockToPlace();
               if (targetBlock == null || !this.placeBlock(targetBlock, rotateMod)) {
                  break;
               }

               placed++;
               this.delay = this.placeDelay.getValue();
               this.inactivityTimer.reset();
            }
         } else if (this.placeTiming.getValue() == TrapModule.PlaceTiming.Vanilla) {
            BlockPos targetBlock = this.getBlockToPlace();
            if (targetBlock != null && this.placeBlock(targetBlock, rotateMod)) {
               this.sequentialBlocks.add(targetBlock);
               this.delay = this.placeDelay.getValue();
               this.inactivityTimer.reset();
            }
         }
      }
   }

   @Nullable
   protected BlockPos getBlockToPlace() {
      return this.target != null && mc.player != null
         ? this.getBlocks(this.target)
            .stream()
            .filter(pos -> pos.method_19770(mc.player.method_19538()) < this.range.getPow2Value())
            .filter(pos -> InteractionUtility.canPlaceBlock(pos, this.interact.getValue(), true))
            .max(Comparator.comparing(pos -> mc.player.method_5707(pos.toCenterPos())))
            .orElse(null)
         : null;
   }

   protected List<BlockPos> getBlocks(@NotNull PlayerEntity player) {
      Vec3d playerPos = player.method_19538();
      List<BlockPos> offsets = new ArrayList<>();
      List<BlockPos> holePoses = HoleUtility.getHolePoses(playerPos);
      List<BlockPos> surroundPoses = HoleUtility.getSurroundPoses(playerPos);
      if (mc.player != null && mc.world != null) {
         switch ((TrapModule.TrapMode)this.trapMode.getValue()) {
            case Full:
               offsets.addAll(holePoses.stream().map(BlockPos::down).toList());
               if (this.interact.getValue() != InteractionUtility.Interact.AirPlace) {
                  offsets.addAll(this.addHelpOffsets(surroundPoses));
               }

               offsets.addAll(surroundPoses);
               offsets.addAll(surroundPoses.stream().map(BlockPos::up).toList());
               if (this.interact.getValue() != InteractionUtility.Interact.AirPlace) {
                  surroundPoses.stream()
                     .map(pos -> pos.up(2))
                     .filter(pos -> pos.method_19770(mc.player.method_19538()) < this.range.getPow2Value())
                     .max(Comparator.comparing(pos -> mc.player.method_5707(pos.toCenterPos())))
                     .ifPresent(pos -> {
                        offsets.add(pos);
                        offsets.add(pos.down());
                     });
               }

               offsets.addAll(holePoses.stream().map(pos -> pos.up(2)).toList());
               break;
            case Legs:
               offsets.addAll(holePoses.stream().map(BlockPos::down).toList());
               if (this.interact.getValue() != InteractionUtility.Interact.AirPlace) {
                  surroundPoses.stream()
                     .filter(pos -> pos.method_19770(mc.player.method_19538()) < this.range.getPow2Value())
                     .max(Comparator.comparing(pos -> player.method_5707(pos.toCenterPos())))
                     .ifPresent(pos -> {
                        offsets.add(pos);
                        offsets.add(pos.up());
                        offsets.add(pos.up(2));
                     });
                  offsets.addAll(this.addHelpOffsets(surroundPoses));
               }

               offsets.addAll(surroundPoses);
               offsets.addAll(holePoses.stream().map(pos -> pos.up(2)).toList());
               break;
            case Head:
               offsets.addAll(holePoses.stream().map(BlockPos::down).toList());
               if (this.interact.getValue() != InteractionUtility.Interact.AirPlace) {
                  surroundPoses.stream()
                     .<BlockPos>map(BlockPos::down)
                     .filter(pos -> pos.method_19770(mc.player.method_19538()) < this.range.getPow2Value())
                     .max(Comparator.comparing(pos -> player.method_5707(pos.toCenterPos())))
                     .ifPresent(pos -> {
                        offsets.add(pos);
                        offsets.add(pos.up());
                        offsets.add(pos.up(3));
                     });
               }

               offsets.addAll(surroundPoses.stream().map(BlockPos::up).toList());
               offsets.addAll(holePoses.stream().map(pos -> pos.up(2)).toList());
         }

         return offsets;
      } else {
         return offsets;
      }
   }

   @NotNull
   private List<BlockPos> addHelpOffsets(@NotNull List<BlockPos> surroundPoses) {
      List<BlockPos> helpOffsets = new ArrayList<>();
      if (mc.world != null && mc.player != null) {
         surroundPoses.stream()
            .<BlockPos>map(BlockPos::down)
            .filter(pos -> pos.method_19770(mc.player.method_19538()) < this.range.getPow2Value())
            .filter(pos -> {
               for (Direction dir : Direction.values()) {
                  if (!mc.world.method_8320(pos.add(dir.getVector().up())).method_45474()) {
                     return false;
                  }
               }

               return true;
            })
            .forEach(helpOffsets::add);
         return helpOffsets;
      } else {
         return helpOffsets;
      }
   }

   protected enum PlaceTiming {
      Default,
      Vanilla;
   }

   protected enum TrapMode {
      Full,
      Legs,
      Head;
   }
}
