package vcore.features.modules.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import vcore.core.Managers;
import vcore.events.impl.EventTick;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.SettingGroup;
import vcore.utility.Timer;
import vcore.utility.player.InteractionUtility;
import vcore.utility.player.InventoryUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class AutoWeb extends Module {
   private final Setting<Integer> range = new Setting<>("Range", 5, 1, 7);
   private final Setting<Integer> placeWallRange = new Setting<>("WallRange", 5, 1, 7);
   private final Setting<AutoWeb.PlaceTiming> placeTiming = new Setting<>("PlaceTiming", AutoWeb.PlaceTiming.Default);
   private final Setting<Integer> blocksPerTick = new Setting<>("Block/Tick", 8, 1, 12, v -> this.placeTiming.getValue() == AutoWeb.PlaceTiming.Default);
   private final Setting<Integer> placeDelay = new Setting<>("Delay/Place", 3, 0, 10);
   private final Setting<InteractionUtility.Interact> interact = new Setting<>("Interact", InteractionUtility.Interact.Strict);
   private final Setting<InteractionUtility.PlaceMode> placeMode = new Setting<>("PlaceMode", InteractionUtility.PlaceMode.Normal);
   private final Setting<InteractionUtility.Rotate> rotate = new Setting<>("Rotate", InteractionUtility.Rotate.None);
   private final Setting<SettingGroup> selection = new Setting<>("Selection", new SettingGroup(false, 0));
   private final Setting<Boolean> head = new Setting<>("Head", true).addToGroup(this.selection);
   private final Setting<Boolean> leggs = new Setting<>("Leggs", true).addToGroup(this.selection);
   private final Setting<Boolean> surround = new Setting<>("Surround", true).addToGroup(this.selection);
   private final Setting<Boolean> upperSurround = new Setting<>("UpperSurround", false).addToGroup(this.selection);
   private final Setting<SettingGroup> renderCategory = new Setting<>("Render", new SettingGroup(false, 0));
   private final Setting<AutoWeb.RenderMode> renderMode = new Setting<>("Render Mode", AutoWeb.RenderMode.Fade).addToGroup(this.renderCategory);
   private final Setting<ColorSetting> renderFillColor = new Setting<>("Render Fill Color", new ColorSetting(HudEditor.getColor(0)))
      .addToGroup(this.renderCategory);
   private final Setting<ColorSetting> renderLineColor = new Setting<>("Render Line Color", new ColorSetting(HudEditor.getColor(0)))
      .addToGroup(this.renderCategory);
   private final Setting<Integer> renderLineWidth = new Setting<>("Render Line Width", 2, 1, 5).addToGroup(this.renderCategory);
   private final Setting<Integer> effectDurationMs = new Setting<>("Effect Duration (MS)", 500, 0, 10000).addToGroup(this.renderCategory);
   private final ArrayList<BlockPos> sequentialBlocks = new ArrayList<>();
   public static Timer inactivityTimer = new Timer();
   private final Map<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();
   private int delay = 0;

   public AutoWeb() {
      super("AutoWeb", "Places webs at target's feet.", Module.Category.COMBAT);
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      this.renderPoses
         .forEach(
            (pos, time) -> {
               if (System.currentTimeMillis() - time > this.effectDurationMs.getValue().intValue()) {
                  this.renderPoses.remove(pos);
               } else {
                  switch ((AutoWeb.RenderMode)this.renderMode.getValue()) {
                     case Fade:
                        Render3DEngine.drawFilledBox(
                           stack,
                           new Box(pos),
                           Render2DEngine.injectAlpha(
                              this.renderFillColor.getValue().getColorObject(), (int)(100.0F * (1.0F - (float)(System.currentTimeMillis() - time) / 500.0F))
                           )
                        );
                        Render3DEngine.drawBoxOutline(
                           new Box(pos),
                           Render2DEngine.injectAlpha(
                              this.renderLineColor.getValue().getColorObject(), (int)(100.0F * (1.0F - (float)(System.currentTimeMillis() - time) / 500.0F))
                           ),
                           this.renderLineWidth.getValue().intValue()
                        );
                        break;
                     case Decrease:
                        float scale = 1.0F - (float)(System.currentTimeMillis() - time) / 500.0F;
                        Box box = new Box(
                           pos.method_10263(), pos.method_10264(), pos.method_10260(), pos.method_10263(), pos.method_10264(), pos.method_10260()
                        );
                        Render3DEngine.drawFilledBox(
                           stack,
                           box.shrink(scale, scale, scale).offset(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                           Render2DEngine.injectAlpha(
                              this.renderFillColor.getValue().getColorObject(), (int)(100.0F * (1.0F - (float)(System.currentTimeMillis() - time) / 500.0F))
                           )
                        );
                        Render3DEngine.drawBoxOutline(
                           box.shrink(scale, scale, scale).offset(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5),
                           this.renderLineColor.getValue().getColorObject(),
                           this.renderLineWidth.getValue().intValue()
                        );
                  }
               }
            }
         );
   }

   @Override
   public void onEnable() {
      this.sequentialBlocks.clear();
      this.renderPoses.clear();
   }

   @EventHandler
   public void onTick(EventTick e) {
      BlockPos targetBlock1 = this.getSequentialPos();
      if (targetBlock1 != null) {
         if (this.delay > 0) {
            this.delay--;
         } else {
            InventoryUtility.saveSlot();
            if (this.placeTiming.getValue() == AutoWeb.PlaceTiming.Default) {
               int placed = 0;

               while (placed < this.blocksPerTick.getValue()) {
                  BlockPos targetBlock = this.getSequentialPos();
                  if (targetBlock == null
                     || !InteractionUtility.placeBlock(
                        targetBlock, this.rotate.getValue(), this.interact.getValue(), this.placeMode.getValue(), this.getSlot(), false, true
                     )) {
                     break;
                  }

                  placed++;
                  this.renderPoses.put(targetBlock, System.currentTimeMillis());
                  this.delay = this.placeDelay.getValue();
                  inactivityTimer.reset();
               }
            } else if (this.placeTiming.getValue() == AutoWeb.PlaceTiming.Vanilla) {
               BlockPos targetBlock = this.getSequentialPos();
               if (targetBlock == null) {
                  return;
               }

               if (InteractionUtility.placeBlock(
                  targetBlock, this.rotate.getValue(), this.interact.getValue(), this.placeMode.getValue(), this.getSlot(), false, true
               )) {
                  this.sequentialBlocks.add(targetBlock);
                  this.renderPoses.put(targetBlock, System.currentTimeMillis());
                  this.delay = this.placeDelay.getValue();
                  inactivityTimer.reset();
               }
            }

            InventoryUtility.returnSlot();
         }
      }
   }

   private BlockPos getSequentialPos() {
      PlayerEntity target = Managers.COMBAT.getNearestTarget(this.range.getValue().intValue());
      if (target != null) {
         BlockPos targetBp = BlockPos.ofFloored(target.method_19538());
         ArrayList<BlockPos> positions = new ArrayList<>();
         if (this.leggs.getValue()) {
            positions.add(targetBp);
         }

         if (this.head.getValue()) {
            positions.add(targetBp.up());
         }

         if (this.surround.getValue()) {
            positions.add(targetBp.east());
            positions.add(targetBp.west());
            positions.add(targetBp.south());
            positions.add(targetBp.north());
         }

         if (this.upperSurround.getValue()) {
            positions.add(targetBp.east().up());
            positions.add(targetBp.west().up());
            positions.add(targetBp.south().up());
            positions.add(targetBp.north().up());
         }

         for (BlockPos bp : positions) {
            BlockHitResult wallCheck = mc.world
               .method_17742(
                  new RaycastContext(
                     InteractionUtility.getEyesPos(mc.player), bp.toCenterPos().offset(Direction.UP, 0.5), ShapeType.COLLIDER, FluidHandling.NONE, mc.player
                  )
               );
            if ((
                  wallCheck == null
                     || wallCheck.method_17783() != Type.BLOCK
                     || wallCheck.getBlockPos() == bp
                     || !(InteractionUtility.squaredDistanceFromEyes(bp.toCenterPos()) > this.placeWallRange.getPow2Value())
               )
               && InteractionUtility.canPlaceBlock(bp, this.interact.getValue(), true)
               && mc.world.method_8320(bp).method_45474()) {
               return bp;
            }
         }
      }

      return null;
   }

   private int getSlot() {
      List<Block> canUseBlocks = new ArrayList<>();
      canUseBlocks.add(Blocks.COBWEB);
      int slot = -1;
      ItemStack mainhandStack = mc.player.method_6047();
      if (mainhandStack != ItemStack.EMPTY && mainhandStack.getItem() instanceof BlockItem) {
         Block blockFromMainhandItem = ((BlockItem)mainhandStack.getItem()).getBlock();
         if (canUseBlocks.contains(blockFromMainhandItem)) {
            slot = mc.player.method_31548().selectedSlot;
         }
      }

      if (slot == -1) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.method_31548().method_5438(i);
            if (stack != ItemStack.EMPTY && stack.getItem() instanceof BlockItem) {
               Block blockFromItem = ((BlockItem)stack.getItem()).getBlock();
               if (canUseBlocks.contains(blockFromItem)) {
                  slot = i;
                  break;
               }
            }
         }
      }

      return slot;
   }

   private enum PlaceTiming {
      Default,
      Vanilla;
   }

   private enum RenderMode {
      Fade,
      Decrease;
   }
}
