package vcore.features.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import vcore.events.impl.EventMove;
import vcore.events.impl.EventSetting;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.math.FrameRateCounter;
import vcore.utility.math.MathUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class XRay extends Module {
   private final Setting<XRay.Plugin> plugin = new Setting<>("Plugin", XRay.Plugin.New);
   public final Setting<Boolean> wallHack = new Setting<>("WallHack", false);
   private final Setting<Boolean> brutForce = new Setting<>("OreDeobf", false);
   private final Setting<Boolean> fast = new Setting<>("Fast", false, v -> this.brutForce.getValue());
   private final Setting<Integer> delay = new Setting<>("Delay", 25, 1, 100, v -> this.brutForce.getValue());
   private final Setting<Integer> radius = new Setting<>("Radius", 5, 1, 64, v -> this.brutForce.getValue());
   private final Setting<Integer> up = new Setting<>("Up", 5, 1, 32, v -> this.brutForce.getValue());
   private final Setting<Integer> down = new Setting<>("Down", 5, 1, 32, v -> this.brutForce.getValue());
   private static final Setting<Boolean> netherite = new Setting<>("Netherite", false);
   private static final Setting<Boolean> diamond = new Setting<>("Diamond ", false);
   private static final Setting<Boolean> gold = new Setting<>("Gold", false);
   private static final Setting<Boolean> iron = new Setting<>("Iron", false);
   private static final Setting<Boolean> emerald = new Setting<>("Emerald", false);
   private static final Setting<Boolean> redstone = new Setting<>("Redstone", false);
   private static final Setting<Boolean> lapis = new Setting<>("Lapis", false);
   private static final Setting<Boolean> coal = new Setting<>("Coal", false);
   private static final Setting<Boolean> quartz = new Setting<>("Quartz", false);
   private static final Setting<Boolean> water = new Setting<>("Water", false);
   private static final Setting<Boolean> lava = new Setting<>("Lava", false);
   private final Timer delayTimer = new Timer();
   private final ArrayList<BlockPos> ores = new ArrayList<>();
   private final ArrayList<BlockPos> toCheck = new ArrayList<>();
   private final ArrayList<XRay.BlockMemory> checked = new ArrayList<>();
   private BlockPos displayBlock;
   private int done;
   private int all;
   private Box area = new Box(BlockPos.ORIGIN);

   public XRay() {
      super("XRay", "See ores through blocks.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.ores.clear();
      this.toCheck.clear();
      this.checked.clear();
      this.toCheck.addAll(this.getBlocks());
      this.all = this.toCheck.size();
      this.done = 0;
      mc.chunkCullingEnabled = false;
      mc.worldRenderer.reload();
      this.area = this.getArea();
   }

   @Override
   public void onDisable() {
      mc.worldRenderer.reload();
      mc.chunkCullingEnabled = true;
   }

   @EventHandler
   public void onReceivePacket(PacketEvent.Receive e) {
      if (e.getPacket() instanceof BlockUpdateS2CPacket pac) {
         this.debug(((BlockUpdateS2CPacket)e.getPacket()).getState().method_26204().getName().getString() + " " + pac.getPos().toString());
         if (isCheckableOre(pac.getState().method_26204()) && !this.ores.contains(pac.getPos())) {
            this.ores.add(pac.getPos());
         }
      }
   }

   @EventHandler
   public void onSettingChange(EventSetting e) {
      if (e.getSetting() == this.wallHack) {
         mc.worldRenderer.reload();
      }
   }

   @EventHandler
   public void onMove(EventMove e) {
      if (this.brutForce.getValue()) {
         if (this.all != this.done) {
            e.setZ(0.0);
            e.setX(0.0);
            e.cancel();
            if (mc.player.field_6012 % 8 == 0 && MovementUtility.isMoving()) {
               this.sendMessage("Don't move while deobf!");
            }
         } else {
            Box newArea = this.getArea();
            if (!newArea.intersects(this.area)) {
               this.area = newArea;
               this.toCheck.clear();
               this.toCheck.addAll(this.getBlocks());
               this.checked.clear();
               this.all = this.toCheck.size();
               this.done = 0;
            }
         }
      }
   }

   @NotNull
   private Box getArea() {
      int radius_ = this.plugin.is(XRay.Plugin.New) ? Math.min(4, this.radius.getValue()) : this.radius.getValue();
      int down_ = this.plugin.is(XRay.Plugin.New) ? Math.min(3, this.down.getValue()) : this.down.getValue();
      int up_ = this.plugin.is(XRay.Plugin.New) ? Math.min(4, this.up.getValue()) : this.up.getValue();
      return new Box(
         mc.player.method_23317() - radius_,
         mc.player.method_23318() - down_,
         mc.player.method_23321() - radius_,
         mc.player.method_23317() + radius_,
         mc.player.method_23318() + up_,
         mc.player.method_23321() + radius_
      );
   }

   @Override
   public void onUpdate() {
      if (this.plugin.is(XRay.Plugin.New)) {
         this.checked.forEach(blockMemory -> {
            if (blockMemory.isDelayed() && !this.ores.contains(blockMemory.bp)) {
               this.ores.add(blockMemory.bp);
            }
         });
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      for (BlockPos pos : this.ores) {
         Block block = mc.world.method_8320(pos).method_26204();
         if ((block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) && diamond.getValue()) {
            this.draw(pos, 0, 255, 255);
         }

         if ((block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) && gold.getValue()) {
            this.draw(pos, 255, 215, 0);
         }

         if (block == Blocks.NETHER_GOLD_ORE && gold.getValue()) {
            this.draw(pos, 255, 215, 0);
         }

         if ((block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) && iron.getValue()) {
            this.draw(pos, 213, 213, 213);
         }

         if ((block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) && emerald.getValue()) {
            this.draw(pos, 0, 255, 77);
         }

         if ((block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) && redstone.getValue()) {
            this.draw(pos, 255, 0, 0);
         }

         if (block == Blocks.COAL_ORE && coal.getValue()) {
            this.draw(pos, 0, 0, 0);
         }

         if ((block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) && lapis.getValue()) {
            this.draw(pos, 38, 97, 156);
         }

         if (block == Blocks.ANCIENT_DEBRIS && netherite.getValue()) {
            this.draw(pos, 255, 255, 255);
         }

         if (block == Blocks.NETHER_QUARTZ_ORE && quartz.getValue()) {
            this.draw(pos, 170, 170, 170);
         }
      }

      if (this.displayBlock != null && this.done != this.all) {
         this.draw(this.displayBlock, 255, 0, 60);
      }

      if (this.brutForce.getValue()) {
         Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(this.area, HudEditor.getColor(1), 2.0F));
      }

      if (!this.toCheck.isEmpty() && this.brutForce.getValue()) {
         if (mc.isInSingleplayer()) {
            this.disable("Bro, you're in singleplayer");
         } else if (mc.player.method_6047().getItem() instanceof PickaxeItem) {
            if (mc.player.field_6012 % 8 == 0) {
               this.disable("Remove pickaxe from ur hand!");
            }
         } else {
            if (this.delayTimer.every(this.delay.getValue().intValue())) {
               BlockPos pos = this.toCheck.remove(this.toCheck.size() - 1 <= 1 ? 0 : ThreadLocalRandom.current().nextInt(0, this.toCheck.size() - 1));
               mc.interactionManager.attackBlock(this.displayBlock = pos, mc.player.method_5735());
               mc.interactionManager.cancelBlockBreaking();
               this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               this.checked.add(new XRay.BlockMemory(pos));
               this.done++;
            }
         }
      }
   }

   private void draw(BlockPos pos, int r, int g, int b) {
      Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new Box(pos), new Color(r, g, b, 100)));
      Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(new Box(pos), new Color(r, g, b, 200), 2.0F));
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (this.brutForce.getValue()) {
         float posX = mc.getWindow().getScaledWidth() / 2.0F - 68.0F;
         float posY = mc.getWindow().getScaledHeight() / 2.0F + 68.0F;
         Render2DEngine.drawGradientBlurredShadow(
            context.getMatrices(),
            posX + 2.0F,
            posY + 2.0F,
            133.0F,
            44.0F,
            14,
            HudEditor.getColor(270),
            HudEditor.getColor(0),
            HudEditor.getColor(180),
            HudEditor.getColor(90)
         );
         Render2DEngine.renderRoundedGradientRect(
            context.getMatrices(),
            HudEditor.getColor(270),
            HudEditor.getColor(0),
            HudEditor.getColor(180),
            HudEditor.getColor(90),
            posX,
            posY,
            137.0F,
            47.5F,
            9.0F
         );
         Render2DEngine.drawRound(context.getMatrices(), posX + 0.5F, posY + 0.5F, 136.0F, 46.0F, 9.0F, Render2DEngine.injectAlpha(Color.BLACK, 220));
         Render2DEngine.drawGradientRound(
            context.getMatrices(),
            posX + 4.0F,
            posY + 32.0F,
            129.0F,
            11.0F,
            4.0F,
            HudEditor.getColor(0).darker().darker(),
            HudEditor.getColor(0).darker().darker().darker().darker(),
            HudEditor.getColor(0).darker().darker().darker().darker(),
            HudEditor.getColor(0).darker().darker().darker().darker()
         );
         Render2DEngine.renderRoundedGradientRect(
            context.getMatrices(),
            HudEditor.getColor(270),
            HudEditor.getColor(0),
            HudEditor.getColor(0),
            HudEditor.getColor(270),
            posX + 4.0F,
            posY + 32.0F,
            (int)MathHelper.clamp(129.0F * (this.done / Math.max(this.all, 1.0F)), 8.0F, 129.0F),
            11.0F,
            4.0F
         );
         FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), (int)((float)this.done / this.all * 100.0F) + "%", posX + 68.0F, posY + 35.0F, -1);
         FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), "XRay", posX + 68.0F, posY + 7.0F, -1);
         double time = 0.0;

         try {
            time = MathUtility.round((this.all - this.done) * ((1000.0 / FrameRateCounter.INSTANCE.getFps() + this.delay.getValue().intValue()) / 1000.0), 1);
         } catch (NumberFormatException var7) {
         }

         FontRenderers.sf_bold
            .drawCenteredString(context.getMatrices(), this.done + " / " + this.all + " Estimated time: " + time + "s", posX + 68.0F, posY + 18.0F, -1);
      }
   }

   public static boolean isCheckableOre(Block block) {
      if (!diamond.getValue() || block != Blocks.DIAMOND_ORE && block != Blocks.DEEPSLATE_DIAMOND_ORE) {
         if (!gold.getValue() || block != Blocks.GOLD_ORE && block != Blocks.DEEPSLATE_GOLD_ORE && block != Blocks.NETHER_GOLD_ORE) {
            if (!iron.getValue() || block != Blocks.IRON_ORE && block != Blocks.DEEPSLATE_IRON_ORE) {
               if (!emerald.getValue() || block != Blocks.EMERALD_ORE && block != Blocks.DEEPSLATE_EMERALD_ORE) {
                  if (!redstone.getValue() || block != Blocks.REDSTONE_ORE && block != Blocks.DEEPSLATE_REDSTONE_ORE) {
                     if (!coal.getValue() || block != Blocks.COAL_ORE && block != Blocks.DEEPSLATE_COAL_ORE) {
                        if (netherite.getValue() && block == Blocks.ANCIENT_DEBRIS) {
                           return true;
                        } else if (water.getValue() && block == Blocks.WATER) {
                           return true;
                        } else if (lava.getValue() && block == Blocks.LAVA) {
                           return true;
                        } else if (quartz.getValue() && block == Blocks.NETHER_QUARTZ_ORE) {
                           return true;
                        } else {
                           return !lapis.getValue() || block != Blocks.LAPIS_ORE && block != Blocks.DEEPSLATE_LAPIS_ORE
                              ? lapis.getValue() && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)
                              : true;
                        }
                     } else {
                        return true;
                     }
                  } else {
                     return true;
                  }
               } else {
                  return true;
               }
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private ArrayList<BlockPos> getBlocks() {
      int radius_ = this.plugin.is(XRay.Plugin.New) ? Math.min(4, this.radius.getValue()) : this.radius.getValue();
      int down_ = this.plugin.is(XRay.Plugin.New) ? Math.min(3, this.down.getValue()) : this.down.getValue();
      int up_ = this.plugin.is(XRay.Plugin.New) ? Math.min(4, this.up.getValue()) : this.up.getValue();
      ArrayList<BlockPos> positions = new ArrayList<>();

      for (int x = (int)(mc.player.method_23317() - radius_); x < mc.player.method_23317() + radius_; x++) {
         for (int y = (int)(mc.player.method_23318() - down_); y < mc.player.method_23318() + up_; y++) {
            for (int z = (int)(mc.player.method_23321() - radius_); z < mc.player.method_23321() + radius_; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               if (!mc.world.method_22347(pos) && (!this.fast.getValue() || !this.plugin.is(XRay.Plugin.Old) || x % 2 != 0 && y % 2 != 0 && z % 2 != 0)) {
                  positions.add(pos);
               }
            }
         }
      }

      return positions;
   }

   public class BlockMemory {
      private final BlockPos bp;
      private long time = 0L;

      public BlockMemory(BlockPos bp) {
         this.bp = bp;
      }

      private boolean isDelayed() {
         return this.time++ > 10L;
      }
   }

   private enum Plugin {
      Old,
      New;
   }
}
