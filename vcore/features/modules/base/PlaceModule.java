package vcore.features.modules.base;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.SettingGroup;
import vcore.utility.Timer;
import vcore.utility.player.InteractionUtility;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.PlayerUtility;
import vcore.utility.player.SearchInvResult;
import vcore.utility.render.BlockAnimationUtility;

public abstract class PlaceModule extends Module {
   protected final Setting<Float> range = new Setting<>("Range", 5.0F, 0.0F, 7.0F);
   protected final Setting<InteractionUtility.Interact> interact = new Setting<>("Interact", InteractionUtility.Interact.Legit);
   protected final Setting<PlaceModule.InteractMode> placeMode = new Setting<>("Place Mode", PlaceModule.InteractMode.Normal);
   protected final Setting<InteractionUtility.Rotate> rotate = new Setting<>("Rotate", InteractionUtility.Rotate.Default);
   protected final Setting<Boolean> swing = new Setting<>("Swing", false);
   protected final Setting<BooleanSettingGroup> crystalBreaker = new Setting<>("Crystal Breaker", new BooleanSettingGroup(false));
   protected final Setting<Integer> crystalAge = new Setting<>("CrystalAge", 0, 0, 20).addToGroup(this.crystalBreaker);
   protected final Setting<Integer> breakDelay = new Setting<>("Break Delay", 100, 1, 1000).addToGroup(this.crystalBreaker);
   protected final Setting<Boolean> remove = new Setting<>("Remove", false).addToGroup(this.crystalBreaker);
   protected final Setting<PlaceModule.InteractMode> breakCrystalMode = new Setting<>("Break Mode", PlaceModule.InteractMode.Normal)
      .addToGroup(this.crystalBreaker);
   protected final Setting<Boolean> antiWeakness = new Setting<>("Anti Weakness", false).addToGroup(this.crystalBreaker);
   protected final Setting<SettingGroup> blocks = new Setting<>("Blocks", new SettingGroup(false, 0));
   protected final Setting<Boolean> obsidian = new Setting<>("Obsidian", true).addToGroup(this.blocks);
   protected final Setting<Boolean> anchor = new Setting<>("Anchor", false).addToGroup(this.blocks);
   protected final Setting<Boolean> enderChest = new Setting<>("EnderChest", false).addToGroup(this.blocks);
   protected final Setting<Boolean> netherite = new Setting<>("Netherite", false).addToGroup(this.blocks);
   protected final Setting<Boolean> cryingObsidian = new Setting<>("Crying Obsidian", true).addToGroup(this.blocks);
   protected final Setting<Boolean> dirt = new Setting<>("Dirt", false).addToGroup(this.blocks);
   protected final Setting<Boolean> oakPlanks = new Setting<>("OakPlanks", false).addToGroup(this.blocks);
   protected final Setting<SettingGroup> pause = new Setting<>("Pause", new SettingGroup(false, 0));
   protected final Setting<Boolean> eatPause = new Setting<>("On Eat", false).addToGroup(this.pause);
   protected final Setting<Boolean> breakPause = new Setting<>("On Break", false).addToGroup(this.pause);
   protected final Setting<BooleanSettingGroup> render = new Setting<>("Render", new BooleanSettingGroup(true));
   protected final Setting<BlockAnimationUtility.BlockRenderMode> renderMode = new Setting<>("Render Mode", BlockAnimationUtility.BlockRenderMode.All)
      .addToGroup(this.render);
   protected final Setting<BlockAnimationUtility.BlockAnimationMode> animationMode = new Setting<>(
         "Animation Mode", BlockAnimationUtility.BlockAnimationMode.Fade
      )
      .addToGroup(this.render);
   protected final Setting<ColorSetting> renderFillColor = new Setting<>("Fill Color", new ColorSetting(HudEditor.getColor(0))).addToGroup(this.render);
   protected final Setting<ColorSetting> renderLineColor = new Setting<>("Line Color", new ColorSetting(HudEditor.getColor(0))).addToGroup(this.render);
   protected final Setting<Integer> renderLineWidth = new Setting<>("Line Width", 2, 1, 5).addToGroup(this.render);
   public final Timer inactivityTimer = new Timer();
   public final Timer pauseTimer = new Timer();
   protected final Timer attackTimer = new Timer();

   public PlaceModule(@NotNull String name, @NotNull Module.Category category) {
      super(name, category);
   }

   public PlaceModule(@NotNull String name, @NotNull String description, @NotNull Module.Category category) {
      super(name, description, category);
   }

   protected boolean shouldPause() {
      return this.eatPause.getValue() && PlayerUtility.isEating() || this.breakPause.getValue() && PlayerUtility.isMining() || !this.pauseTimer.passedMs(350L);
   }

   protected boolean placeBlock(BlockPos pos) {
      return this.placeBlock(pos, this.crystalBreaker.getValue().isEnabled(), this.placeMode.getValue(), this.rotate.getValue());
   }

   protected boolean placeBlock(BlockPos pos, boolean ignoreEntities) {
      return this.placeBlock(pos, ignoreEntities, this.placeMode.getValue(), this.rotate.getValue());
   }

   protected boolean placeBlock(BlockPos pos, InteractionUtility.Rotate rotate) {
      return this.placeBlock(pos, this.crystalBreaker.getValue().isEnabled(), this.placeMode.getValue(), rotate);
   }

   protected boolean placeBlock(BlockPos pos, PlaceModule.InteractMode mode) {
      return this.placeBlock(pos, this.crystalBreaker.getValue().isEnabled(), mode, this.rotate.getValue());
   }

   protected boolean placeBlock(BlockPos pos, boolean ignoreEntities, PlaceModule.InteractMode mode, InteractionUtility.Rotate rotate) {
      if (this.shouldPause()) {
         return false;
      }

      boolean validInteraction = false;
      SearchInvResult result = this.getBlockResult();
      if (!result.found()) {
         return false;
      }

      if (this.crystalBreaker.getValue().isEnabled() && mc.world != null && this.attackTimer.passedMs(this.breakDelay.getValue().intValue())) {
         mc.world.method_18467(EndCrystalEntity.class, new Box(pos)).stream().findFirst().ifPresent(this::breakCrystal);
      }

      if (mode == PlaceModule.InteractMode.Packet) {
         validInteraction = InteractionUtility.placeBlock(
            pos, rotate, this.interact.getValue(), InteractionUtility.PlaceMode.Packet, result.slot(), true, ignoreEntities
         );
      }

      if (mode == PlaceModule.InteractMode.Normal) {
         validInteraction = InteractionUtility.placeBlock(
            pos, rotate, this.interact.getValue(), InteractionUtility.PlaceMode.Normal, result.slot(), true, ignoreEntities
         );
      }

      if (validInteraction && mc.player != null) {
         if (this.render.getValue().isEnabled()) {
            this.renderBlock(pos);
         }

         if (this.swing.getValue()) {
            mc.player.method_6104(Hand.MAIN_HAND);
         }
      }

      return validInteraction;
   }

   protected void breakCrystal(EndCrystalEntity entity) {
      if (mc.player != null
         && mc.world != null
         && mc.interactionManager != null
         && !this.shouldPause()
         && this.attackTimer.passedMs(this.breakDelay.getValue().intValue())
         && !(mc.player.method_5858(entity) > this.range.getPow2Value())
         && entity.field_6012 >= this.crystalAge.getValue()) {
         int preSlot = mc.player.method_31548().selectedSlot;
         if (this.antiWeakness.getValue() && mc.player.method_6059(StatusEffects.WEAKNESS)) {
            SearchInvResult result = InventoryUtility.getAntiWeaknessItem();
            if (!result.found()) {
               return;
            }

            result.switchTo();
         }

         if (this.breakCrystalMode.getValue() == PlaceModule.InteractMode.Packet) {
            this.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.method_5715()));
         }

         if (this.breakCrystalMode.getValue() == PlaceModule.InteractMode.Normal) {
            mc.interactionManager.attackEntity(mc.player, entity);
         }

         this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         this.attackTimer.reset();
         if (this.remove.getValue()) {
            entity.method_5768();
            entity.method_31745(RemovalReason.KILLED);
            entity.method_36209();
         }

         if (this.antiWeakness.getValue() && mc.player.method_6059(StatusEffects.WEAKNESS)) {
            InventoryUtility.switchTo(preSlot);
         }
      }
   }

   protected boolean canPlaceBlock(BlockPos pos, boolean ignoreEntities) {
      return InteractionUtility.canPlaceBlock(pos, this.interact.getValue(), ignoreEntities);
   }

   protected void renderBlock(BlockPos pos) {
      BlockAnimationUtility.renderBlock(
         pos,
         this.renderLineColor.getValue().getColorObject(),
         this.renderLineWidth.getValue(),
         this.renderFillColor.getValue().getColorObject(),
         this.animationMode.getValue(),
         this.renderMode.getValue()
      );
   }

   protected SearchInvResult getBlockResult() {
      List<Block> canUseBlocks = new ArrayList<>();
      if (mc.player == null) {
         return SearchInvResult.notFound();
      }

      if (this.obsidian.getValue()) {
         canUseBlocks.add(Blocks.OBSIDIAN);
      }

      if (this.enderChest.getValue()) {
         canUseBlocks.add(Blocks.ENDER_CHEST);
      }

      if (this.cryingObsidian.getValue()) {
         canUseBlocks.add(Blocks.CRYING_OBSIDIAN);
      }

      if (this.netherite.getValue()) {
         canUseBlocks.add(Blocks.NETHERITE_BLOCK);
      }

      if (this.anchor.getValue()) {
         canUseBlocks.add(Blocks.RESPAWN_ANCHOR);
      }

      if (this.dirt.getValue()) {
         canUseBlocks.addAll(List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL));
      }

      if (this.oakPlanks.getValue()) {
         canUseBlocks.addAll(List.of(Blocks.OAK_PLANKS, Blocks.BIRCH_PLANKS, Blocks.DARK_OAK_PLANKS));
      }

      ItemStack mainHandStack = mc.player.method_6047();
      if (mainHandStack != ItemStack.EMPTY && mainHandStack.getItem() instanceof BlockItem) {
         Block blockFromMainHandItem = ((BlockItem)mainHandStack.getItem()).getBlock();
         if (canUseBlocks.contains(blockFromMainHandItem)) {
            return new SearchInvResult(mc.player.method_31548().selectedSlot, true, mainHandStack);
         }
      }

      return InventoryUtility.findBlockInHotBar(canUseBlocks);
   }

   public void pause() {
      this.pauseTimer.reset();
   }

   protected enum InteractMode {
      Packet,
      Normal;
   }
}
