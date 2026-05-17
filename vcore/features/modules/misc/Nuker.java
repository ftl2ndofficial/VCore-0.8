package vcore.features.modules.misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventSync;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.player.InteractionUtility;
import vcore.utility.render.Render3DEngine;

public class Nuker extends Module {
   private static final long TARGET_RENDER_TIME_MS = 500L;
   private static final float TARGET_RENDER_END_SCALE = 0.8F;
   private final Setting<Nuker.Mode> mode = new Setting<>("Mode", Nuker.Mode.Legit);
   private final Setting<Nuker.Filter> filter = new Setting<>("Filter", Nuker.Filter.BlackList);
   public final Setting<ItemSelectSetting> selectedBlocks = new Setting<>("SelectedBlocks", new ItemSelectSetting(new ArrayList<>()));
   private final Setting<Float> range = new Setting<>("Range", 5.0F, 1.0F, 6.0F, v -> this.mode.getValue() == Nuker.Mode.Legit);
   private final Setting<Float> wallRange = new Setting<>("WallRange", 0.0F, 0.0F, 6.0F, v -> this.mode.getValue() == Nuker.Mode.Legit);
   private final Setting<Integer> switchDelay = new Setting<>("SwitchDelay", 0, 0, 20, v -> this.mode.getValue() == Nuker.Mode.Legit);
   private final Setting<Boolean> forceImmediateBreak = new Setting<>("ForceImmediateBreak", false, v -> this.mode.getValue() == Nuker.Mode.Legit);
   private final Setting<Float> instantRange = new Setting<>("InstantRange", 5.0F, 1.0F, 50.0F, v -> this.mode.getValue() == Nuker.Mode.Instant);
   private final Setting<Integer> bpsMin = new Setting<>("BPSMin", 40, 1, 200, v -> this.mode.getValue() == Nuker.Mode.Instant);
   private final Setting<Integer> bpsMax = new Setting<>("BPSMax", 50, 1, 200, v -> this.mode.getValue() == Nuker.Mode.Instant);
   private final Setting<Boolean> doNotStop = new Setting<>("DoNotStop", false, v -> this.mode.getValue() == Nuker.Mode.Instant);
   private final Setting<Boolean> ignoreOpenInventory = new Setting<>("IgnoreOpenInventory", true);
   private final Setting<Boolean> render = new Setting<>("Render", true);
   private final Setting<Nuker.ColorMode> colorMode = new Setting<>("ColorMode", Nuker.ColorMode.Sync, v -> this.render.getValue());
   public final Setting<ColorSetting> color = new Setting<>(
      "Color", new ColorSetting(575714484), v -> this.render.getValue() && this.colorMode.getValue() == Nuker.ColorMode.Custom
   );
   private final Setting<Float> outlineWidth = new Setting<>("OutlineWidth", 1.0F, 0.5F, 5.0F, v -> this.render.getValue());
   private final Setting<Integer> fillAlpha = new Setting<>("FillAlpha", 100, 0, 255, v -> this.render.getValue());
   private BlockPos currentTarget;
   private BlockPos wasTarget;
   private BlockHitResult cachedHitResult;
   private int switchTickCounter;
   private float rotationYaw = -999.0F;
   private float rotationPitch = -999.0F;
   private final Map<Long, Nuker.TargetRenderEntry> renderTargets = new LinkedHashMap<>();
   private static final float[][] SAMPLE_POINTS = new float[][]{
      {0.5F, 0.5F, 0.5F},
      {0.5F, 0.5F, 0.1F},
      {0.5F, 0.5F, 0.9F},
      {0.1F, 0.5F, 0.5F},
      {0.9F, 0.5F, 0.5F},
      {0.5F, 0.1F, 0.5F},
      {0.5F, 0.9F, 0.5F},
      {0.1F, 0.1F, 0.1F},
      {0.9F, 0.1F, 0.1F},
      {0.1F, 0.9F, 0.1F},
      {0.9F, 0.9F, 0.1F},
      {0.1F, 0.1F, 0.9F},
      {0.9F, 0.1F, 0.9F},
      {0.1F, 0.9F, 0.9F},
      {0.9F, 0.9F, 0.9F}
   };

   public Nuker() {
      super("Nuker", "Auto mines blocks.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.currentTarget = null;
      this.wasTarget = null;
      this.cachedHitResult = null;
      this.switchTickCounter = 0;
      this.renderTargets.clear();
   }

   @Override
   public void onDisable() {
      this.currentTarget = null;
      this.wasTarget = null;
      this.cachedHitResult = null;
      this.rotationYaw = -999.0F;
      this.rotationPitch = -999.0F;
      this.renderTargets.clear();
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (this.rotationYaw != -999.0F) {
         mc.player.method_36456(this.rotationYaw);
         mc.player.method_36457(this.rotationPitch);
         this.rotationYaw = -999.0F;
      }
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent e) {
      if (!fullNullCheck()) {
         if (!this.ignoreOpenInventory.getValue() && mc.currentScreen instanceof HandledScreen) {
            this.currentTarget = null;
         } else {
            if (this.mode.getValue() == Nuker.Mode.Legit) {
               this.handleLegitMode();
            } else {
               this.handleInstantMode();
            }
         }
      }
   }

   private void handleLegitMode() {
      Vec3d eyesPos = InteractionUtility.getEyesPos(mc.player);
      float rangeVal = this.range.getValue();
      float wallRangeVal = this.wallRange.getValue();
      float searchRange = Math.max(rangeVal, wallRangeVal);
      if (this.currentTarget != null) {
         BlockState state = mc.world.method_8320(this.currentTarget);
         if (!this.isNotBreakable(state, this.currentTarget)
            && this.isValidBlock(state)
            && this.isInRange(eyesPos, this.currentTarget, state, rangeVal, wallRangeVal)) {
            BlockHitResult hit = this.findBlockHit(this.currentTarget, eyesPos, rangeVal, wallRangeVal);
            if (hit != null) {
               this.cachedHitResult = hit;
               this.setRotationFromHit(hit);
            } else {
               this.currentTarget = null;
               this.cachedHitResult = null;
            }
         } else {
            this.currentTarget = null;
            this.cachedHitResult = null;
         }
      }

      if (this.currentTarget == null) {
         for (Nuker.BlockPosPair pair : this.lookupTargets(searchRange)) {
            BlockHitResult hit = this.findBlockHit(pair.pos, eyesPos, rangeVal, wallRangeVal);
            if (hit != null) {
               this.currentTarget = pair.pos;
               this.cachedHitResult = hit;
               this.setRotationFromHit(hit);
               break;
            }
         }
      }

      if (this.currentTarget != null && this.cachedHitResult != null) {
         if (this.switchDelay.getValue() > 0 && this.wasTarget != null && !this.currentTarget.equals(this.wasTarget)) {
            if (this.switchTickCounter < this.switchDelay.getValue()) {
               this.switchTickCounter++;
               return;
            }

            this.switchTickCounter = 0;
         }

         this.doBreak(this.cachedHitResult);
         this.wasTarget = this.currentTarget;
      } else {
         this.wasTarget = null;
      }
   }

   private void setRotationFromHit(BlockHitResult hit) {
      float[] angle = InteractionUtility.calculateAngle(hit.method_17784());
      this.rotationYaw = angle[0];
      this.rotationPitch = angle[1];
      ModuleManager.moveFix.fixRotation = this.rotationYaw;
   }

   private BlockHitResult findBlockHit(BlockPos pos, Vec3d eyesPos, float rangeVal, float wallRangeVal) {
      double rangeSq = (double)rangeVal * rangeVal;

      for (float[] sample : SAMPLE_POINTS) {
         Vec3d point = new Vec3d(pos.method_10263() + sample[0], pos.method_10264() + sample[1], pos.method_10260() + sample[2]);
         if (!(eyesPos.squaredDistanceTo(point) > rangeSq)) {
            BlockHitResult bhr = mc.world.method_17742(new RaycastContext(eyesPos, point, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
            if (bhr != null && bhr.method_17783() == Type.BLOCK && bhr.getBlockPos().equals(pos)) {
               return bhr;
            }
         }
      }

      if (wallRangeVal > 0.0F) {
         double wallRangeSq = (double)wallRangeVal * wallRangeVal;
         Vec3d closest = this.getClosestPointOnBlock(pos, eyesPos);
         if (closest != null && eyesPos.squaredDistanceTo(closest) <= wallRangeSq) {
            Direction dir = Direction.getFacing(
               (float)(eyesPos.x - (pos.method_10263() + 0.5)),
               (float)(eyesPos.y - (pos.method_10264() + 0.5)),
               (float)(eyesPos.z - (pos.method_10260() + 0.5))
            );
            Vec3d hitPoint = new Vec3d(
               pos.method_10263() + 0.5 + dir.getOffsetX() * 0.5,
               pos.method_10264() + 0.5 + dir.getOffsetY() * 0.5,
               pos.method_10260() + 0.5 + dir.getOffsetZ() * 0.5
            );
            return new BlockHitResult(hitPoint, dir, pos, false);
         }
      }

      return null;
   }

   private Vec3d getClosestPointOnBlock(BlockPos pos, Vec3d eyesPos) {
      BlockState state = mc.world.method_8320(pos);
      VoxelShape shape = state.method_26218(mc.world, pos);
      if (shape.isEmpty()) {
         return null;
      }

      Box b = shape.getBoundingBox().offset(pos.method_10263(), pos.method_10264(), pos.method_10260());
      return new Vec3d(
         Math.max(b.minX, Math.min(eyesPos.x, b.maxX)), Math.max(b.minY, Math.min(eyesPos.y, b.maxY)), Math.max(b.minZ, Math.min(eyesPos.z, b.maxZ))
      );
   }

   private void doBreak(BlockHitResult hitResult) {
      BlockPos blockPos = hitResult.getBlockPos();
      Direction direction = hitResult.getSide();
      this.rememberRenderTarget(blockPos);
      if (mc.player.method_7337()) {
         mc.interactionManager.attackBlock(blockPos, direction);
         mc.player.method_6104(Hand.MAIN_HAND);
      } else if (this.forceImmediateBreak.getValue()) {
         this.sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, blockPos, direction, id));
         mc.player.method_6104(Hand.MAIN_HAND);
         this.sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, id));
      } else {
         mc.interactionManager.updateBlockBreakingProgress(blockPos, direction);
         mc.player.method_6104(Hand.MAIN_HAND);
      }
   }

   private void handleInstantMode() {
      this.currentTarget = null;
      float rangeVal = this.instantRange.getValue();
      int min = Math.min(this.bpsMin.getValue(), this.bpsMax.getValue());
      int max = Math.max(this.bpsMin.getValue(), this.bpsMax.getValue());
      int count = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
      List<Nuker.BlockPosPair> targets = this.lookupTargets(rangeVal);
      if (targets.isEmpty()) {
         this.wasTarget = null;
      } else {
         int processed = 0;

         for (Nuker.BlockPosPair pair : targets) {
            if (processed >= count) {
               break;
            }

            BlockPos pos = pair.pos;
            this.rememberRenderTarget(pos);
            this.sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, Direction.DOWN, id));
            mc.player.method_6104(Hand.MAIN_HAND);
            if (!this.doNotStop.getValue()) {
               this.sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN, id));
            }

            this.wasTarget = pos;
            processed++;
         }
      }
   }

   private List<Nuker.BlockPosPair> lookupTargets(float radius) {
      Vec3d eyesPos = InteractionUtility.getEyesPos(mc.player);
      double rangeSquared = radius * radius;
      int intRange = (int)Math.ceil(radius) + 1;
      BlockPos playerBlockPos = BlockPos.ofFloored(mc.player.method_19538());
      List<Nuker.BlockPosPair> positions = new ArrayList<>();

      for (BlockPos pos : BlockPos.iterateOutwards(playerBlockPos.up(), intRange, intRange, intRange)) {
         BlockState state = mc.world.method_8320(pos);
         if (this.isPositionAvailable(eyesPos, rangeSquared, pos, state)) {
            positions.add(new Nuker.BlockPosPair(pos.toImmutable(), state));
         }
      }

      BlockPos sortAnchor = this.wasTarget != null ? this.wasTarget : playerBlockPos;
      positions.sort(Comparator.comparingDouble(p -> p.pos.method_10262(sortAnchor)));
      Box playerFeet = mc.player.method_5829().offset(0.0, -1.0, 0.0);
      List<Nuker.BlockPosPair> nonStanding = new ArrayList<>();

      for (Nuker.BlockPosPair pair : positions) {
         if (!playerFeet.intersects(new Box(pair.pos))) {
            nonStanding.add(pair);
         }
      }

      return nonStanding.isEmpty() ? positions : nonStanding;
   }

   private boolean isPositionAvailable(Vec3d eyesPos, double rangeSquared, BlockPos pos, BlockState state) {
      if (state.method_26215()) {
         return false;
      }

      Block block = state.method_26204();
      if (block == Blocks.BEDROCK) {
         return false;
      }

      if (!this.isValidBlock(state)) {
         return false;
      }

      if (!mc.player.method_7337() && state.method_26214(mc.world, pos) < 0.0F) {
         return false;
      }

      double dx = eyesPos.x - (pos.method_10263() + 0.5);
      double dy = eyesPos.y - (pos.method_10264() + 0.5);
      double dz = eyesPos.z - (pos.method_10260() + 0.5);
      if (dx * dx + dy * dy + dz * dz > rangeSquared + 1.5 + 2.0 * Math.sqrt(rangeSquared)) {
         return false;
      }

      VoxelShape shape = state.method_26218(mc.world, pos);
      if (shape.isEmpty()) {
         return false;
      }

      Box b = shape.getBoundingBox().offset(pos.method_10263(), pos.method_10264(), pos.method_10260());
      double cx = Math.max(b.minX, Math.min(eyesPos.x, b.maxX));
      double cy = Math.max(b.minY, Math.min(eyesPos.y, b.maxY));
      double cz = Math.max(b.minZ, Math.min(eyesPos.z, b.maxZ));
      double ex = eyesPos.x - cx;
      double ey = eyesPos.y - cy;
      double ez = eyesPos.z - cz;
      return ex * ex + ey * ey + ez * ez <= rangeSquared;
   }

   private boolean isNotBreakable(BlockState state, BlockPos pos) {
      if (state.method_26215()) {
         return true;
      } else {
         return state.method_26204() == Blocks.BEDROCK ? true : !mc.player.method_7337() && state.method_26214(mc.world, pos) < 0.0F;
      }
   }

   private boolean isValidBlock(BlockState state) {
      Block block = state.method_26204();
      boolean inList = this.selectedBlocks.getValue().getItemsById().contains(block.getTranslationKey().replace("block.minecraft.", ""));

      return switch ((Nuker.Filter)this.filter.getValue()) {
         case BlackList -> !inList && block != Blocks.AIR && block != Blocks.CAVE_AIR && !(block instanceof FluidBlock);
         case WhiteList -> inList;
      };
   }

   private boolean isInRange(Vec3d eyesPos, BlockPos pos, BlockState state, float rangeVal, float wallRangeVal) {
      float maxRange = Math.max(rangeVal, wallRangeVal);
      return this.isPositionAvailable(eyesPos, (double)maxRange * maxRange, pos, state);
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.render.getValue()) {
         if (mc.world != null) {
            Color baseColor = this.colorMode.getValue() == Nuker.ColorMode.Sync ? HudEditor.getColor(1) : this.color.getValue().getColorObject();
            long now = System.currentTimeMillis();
            Iterator<Entry<Long, Nuker.TargetRenderEntry>> iterator = this.renderTargets.entrySet().iterator();

            while (iterator.hasNext()) {
               Nuker.TargetRenderEntry entry = iterator.next().getValue();
               long age = now - entry.timestamp();
               if (age >= 500L) {
                  iterator.remove();
               } else {
                  float progress = (float)age / 500.0F;
                  float scale = 1.0F + -0.19999999F * progress;
                  float alphaFactor = 1.0F - progress;
                  Box renderBox = scaleBox(entry.box(), scale);
                  Color outlineColor = new Color(
                     baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), Math.max(0, Math.min(255, Math.round(255.0F * alphaFactor)))
                  );
                  Color fillColor = new Color(
                     baseColor.getRed(),
                     baseColor.getGreen(),
                     baseColor.getBlue(),
                     Math.max(0, Math.min(255, Math.round(this.fillAlpha.getValue().intValue() * alphaFactor)))
                  );
                  Render3DEngine.drawBoxOutline(renderBox, outlineColor, this.outlineWidth.getValue());
                  if (fillColor.getAlpha() > 0) {
                     Render3DEngine.drawFilledBox(stack, renderBox, fillColor);
                  }
               }
            }
         }
      }
   }

   private void rememberRenderTarget(BlockPos pos) {
      if (this.render.getValue() && pos != null) {
         this.renderTargets.put(pos.asLong(), new Nuker.TargetRenderEntry(new Box(pos), System.currentTimeMillis()));
      }
   }

   private static Box scaleBox(Box box, float scale) {
      if (scale == 1.0F) {
         return box;
      }

      double expand = (scale - 1.0F) * 0.5F;
      return box.expand(expand, expand, expand);
   }

   private record BlockPosPair(BlockPos pos, BlockState state) {
   }

   private enum ColorMode {
      Custom,
      Sync;
   }

   private enum Filter {
      BlackList,
      WhiteList;
   }

   private enum Mode {
      Legit,
      Instant;
   }

   private record TargetRenderEntry(Box box, long timestamp) {
   }
}
