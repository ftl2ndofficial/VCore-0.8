package vcore.features.modules.render;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.block.AirBlock;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.Timer;
import vcore.utility.player.PlayerUtility;
import vcore.utility.render.Render3DEngine;

public class BlockESP extends Module {
   public final Setting<ItemSelectSetting> selectedBlocks = new Setting<>("SelectedBlocks", new ItemSelectSetting(new ArrayList<>()));
   public static ArrayList<BlockESP.BlockVec> blocks = new ArrayList<>();
   private final Setting<Integer> range = new Setting<>("Range", 100, 1, 128);
   private final Setting<BooleanSettingGroup> limit = new Setting<>("Limit", new BooleanSettingGroup(true));
   private final Setting<Integer> limitCount = new Setting<>("LimitCount", 50, 1, 2048).addToGroup(this.limit);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(-16711681));
   private final Setting<Boolean> illegals = new Setting<>("Illegals", true);
   private final Setting<Boolean> tracers = new Setting<>("Tracers", false);
   private final Setting<Boolean> fill = new Setting<>("Fill", true);
   private final Setting<Boolean> outline = new Setting<>("Outline", true);
   private final ExecutorService searchThread = Executors.newSingleThreadExecutor();
   private final Timer searchTimer = new Timer();
   private long lastFrameTime;
   private boolean canContinue;

   public BlockESP() {
      super("BlockESP", "Highlights selected blocks.", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      blocks.clear();
      this.lastFrameTime = System.currentTimeMillis();
      this.canContinue = true;
   }

   @Override
   public void onUpdate() {
      if (this.searchTimer.every(1000L) && this.canContinue) {
         CompletableFuture.supplyAsync(this::scan, this.searchThread).thenAcceptAsync(this::sync, Util.getMainWorkerExecutor());
         this.canContinue = false;
      }
   }

   private ArrayList<BlockESP.BlockVec> scan() {
      ArrayList<BlockESP.BlockVec> blocks = new ArrayList<>();
      int startX = (int)Math.floor(mc.player.method_23317() - this.range.getValue().intValue());
      int endX = (int)Math.ceil(mc.player.method_23317() + this.range.getValue().intValue());
      int startY = mc.world.method_31607() + 1;
      int endY = mc.world.method_31600();
      int startZ = (int)Math.floor(mc.player.method_23321() - this.range.getValue().intValue());
      int endZ = (int)Math.ceil(mc.player.method_23321() + this.range.getValue().intValue());

      for (int x = startX; x <= endX; x++) {
         for (int y = startY; y <= endY; y++) {
            for (int z = startZ; z <= endZ; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               BlockState bs = mc.world.method_8320(pos);
               if (this.shouldAdd(bs.method_26204(), pos)) {
                  blocks.add(new BlockESP.BlockVec(pos.method_10263(), pos.method_10264(), pos.method_10260()));
               }
            }
         }
      }

      return blocks;
   }

   private void sync(ArrayList<BlockESP.BlockVec> b) {
      blocks = b;
      this.canContinue = true;
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (!fullNullCheck() && !blocks.isEmpty()) {
         int count = 0;
         if (mc.getCurrentFps() < 8 && mc.player.field_6012 > 100) {
            this.disable("Saving ur pc :)");
         } else {
            if (this.fill.getValue() || this.outline.getValue()) {
               for (BlockESP.BlockVec vec : Lists.newArrayList(blocks)) {
                  if (count <= this.limitCount.getValue() || !this.limit.getValue().isEnabled()) {
                     if (vec.getDistance(mc.player.method_19538()) > this.range.getPow2Value()) {
                        blocks.remove(vec);
                     } else {
                        Box b = new Box(vec.x, vec.y, vec.z, vec.x + 1.0, vec.y + 1.0, vec.z + 1.0);
                        if (this.fill.getValue()) {
                           Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(b, this.color.getValue().getColorObject()));
                        }

                        if (this.outline.getValue()) {
                           Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(b, this.color.getValue().getColorObject(), 2.0F));
                        }

                        if (this.tracers.getValue()) {
                           Vec3d vec2 = new Vec3d(0.0, 0.0, 75.0)
                              .rotateX(-((float)Math.toRadians(mc.gameRenderer.getCamera().getPitch())))
                              .rotateY(-((float)Math.toRadians(mc.gameRenderer.getCamera().getYaw())))
                              .add(mc.cameraEntity.getEyePos());
                           Render3DEngine.drawLineDebug(vec2, vec.getVector(), this.color.getValue().getColorObject());
                        }

                        count++;
                     }
                  }
               }
            }

            this.lastFrameTime = System.currentTimeMillis();
         }
      }
   }

   private boolean shouldAdd(Block block, BlockPos pos) {
      if (block instanceof AirBlock) {
         return false;
      } else if (this.selectedBlocks.getValue().contains(block)) {
         return true;
      } else {
         return this.illegals.getValue() ? this.isIllegal(block, pos) : false;
      }
   }

   private boolean isIllegal(Block block, BlockPos pos) {
      if (block instanceof CommandBlock || block instanceof BarrierBlock) {
         return true;
      } else if (block != Blocks.BEDROCK) {
         return false;
      } else {
         return !PlayerUtility.isInHell() ? pos.method_10264() > 4 : pos.method_10264() > 127 || pos.method_10264() < 123 && pos.method_10264() > 4;
      }
   }

   public record BlockVec(double x, double y, double z) {
      public double getDistance(@NotNull Vec3d v) {
         double dx = this.x - v.x;
         double dy = this.y - v.y;
         double dz = this.z - v.z;
         return dx * dx + dy * dy + dz * dz;
      }

      public Vec3d getVector() {
         return new Vec3d(this.x + 0.5, this.y + 0.5, this.z + 0.5);
      }
   }
}
