package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventItemUse;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.SettingGroup;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public final class AutoCrystal extends Module {
   private final Setting<Bind> activateKey = new Setting<>("Activate Key", new Bind(-1, false, false));
   private final Setting<Integer> placeDelay = new Setting<>("Place Delay", 0, 0, 10);
   private final Setting<Integer> breakDelay = new Setting<>("Break Delay", 0, 0, 10);
   private final Setting<Integer> baseSwitchDelay = new Setting<>("Switch Delay", 0, 0, 10, this::isCrystalBaseTimingVisible);
   private final Setting<Integer> baseDelay = new Setting<>("Base Delay", 0, 0, 10, this::isCrystalBaseTimingVisible);
   private final Setting<Boolean> crystalBase = new Setting<>("Crystal Base", false);
   private final Setting<Boolean> stopOnKill = new Setting<>("Stop on Kill", false);
   private final Setting<Boolean> damageTick = new Setting<>("Damage Tick", false);
   private final Setting<Boolean> antiWeakness = new Setting<>("Anti-Weakness", false);
   private final Setting<SettingGroup> baseSetting = new Setting<>("Base Setting", new SettingGroup(false, 0), v -> this.crystalBase.getValue());
   private final Setting<Boolean> baseCheckPlace = new Setting<>("Check Place", false).addToGroup(this.baseSetting);
   private final Setting<Boolean> baseWorkWithTotem = new Setting<>("Work With Totem", false).addToGroup(this.baseSetting);
   private final Setting<Boolean> baseWorkWithCrystal = new Setting<>("Work With Crystal", false).addToGroup(this.baseSetting);
   private final Setting<Boolean> baseSwordSwap = new Setting<>("Sword Swap", true).addToGroup(this.baseSetting);
   private int placeClock;
   private int breakClock;
   private int basePlaceClock;
   private int baseSwitchClock;
   private int lastCrystalTick = -1;
   private boolean baseActive;
   private boolean baseCrystalling;
   private boolean baseCrystalSelected;
   public boolean crystalling;

   public AutoCrystal() {
      super("AutoCrystal", "Automatically places and breaks end crystals.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   @Override
   public void onUpdate() {
      this.runCrystalTick(false);
   }

   private void resetState() {
      this.placeClock = 0;
      this.breakClock = 0;
      this.lastCrystalTick = -1;
      this.crystalling = false;
      this.resetCrystalBaseState();
   }

   private void resetCrystalBaseState() {
      this.basePlaceClock = this.baseDelay.getValue();
      this.baseSwitchClock = this.baseSwitchDelay.getValue();
      this.baseActive = false;
      this.baseCrystalling = false;
      this.baseCrystalSelected = false;
   }

   @EventHandler
   private void onItemUse(@NotNull EventItemUse event) {
      if (!fullNullCheck()) {
         if (this.isCrystalBaseEnabled()) {
            Item mainHand = mc.player.method_6047().getItem();
            if ((mainHand == Items.END_CRYSTAL || mainHand == Items.OBSIDIAN) && !this.isMouseDown(1)) {
               event.cancel();
               return;
            }
         }

         if (this.isHolding(Items.END_CRYSTAL)) {
            if (this.getLookedBlock() != null && this.isCrystalBase(this.getLookedBlock().getBlockPos())) {
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   private void onAttack(@NotNull EventAttack event) {
      if (!fullNullCheck()) {
         if (event.isPre() && this.isCrystalBaseEnabled()) {
            if (mc.player.method_6047().isOf(Items.END_CRYSTAL) && !this.isMouseDown(0)) {
               event.cancel();
            }
         }
      }
   }

   public void runCrystalTick(boolean ignoreActivationKey) {
      if (!fullNullCheck() && mc.currentScreen == null) {
         boolean dontPlace = this.placeClock > 0;
         boolean dontBreak = this.breakClock > 0;
         if (dontPlace) {
            this.placeClock--;
         }

         if (dontBreak) {
            this.breakClock--;
         }

         if (!this.stopOnKill.getValue() || !this.isDeadPlayerNearby()) {
            if (!mc.player.method_6115()) {
               if (!this.damageTick.getValue() || !this.shouldWaitDamageTick()) {
                  if (!ignoreActivationKey && !this.isActivationDown()) {
                     this.resetState();
                  } else {
                     if (this.isCrystalBaseEnabled()) {
                        this.handleCrystalBase();
                     } else {
                        this.resetCrystalBaseState();
                     }

                     this.crystalling = true;
                     if (this.isHolding(Items.END_CRYSTAL)) {
                        if (this.lastCrystalTick != mc.player.field_6012) {
                           this.lastCrystalTick = mc.player.field_6012;
                           this.handleEntityTarget(dontBreak);
                           this.handleBlockTarget(dontPlace);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handleCrystalBase() {
      BlockHitResult hit = this.getLookedBlock();
      if (hit != null) {
         if (this.baseActive || !this.baseCheckPlace.getValue() || this.canPlaceAgainst(hit)) {
            if (this.baseActive || this.isValidBaseStartItem(mc.player.method_6047())) {
               if (!this.baseActive && this.baseSwordSwap.getValue() && this.isCrystalBase(hit.getBlockPos())) {
                  this.baseCrystalling = true;
               }

               this.baseActive = true;
               if (!this.baseCrystalling) {
                  this.handleBasePlacement(hit);
               }

               if (this.baseCrystalling) {
                  this.handleBaseCrystalSelection();
               }
            }
         }
      }
   }

   private void handleBasePlacement(@NotNull BlockHitResult hit) {
      if (!this.isCrystalBase(hit.getBlockPos()) && !this.isChargedAnchor(hit.getBlockPos())) {
         mc.options.useKey.setPressed(false);
         if (!this.isHolding(Items.OBSIDIAN)) {
            if (!this.tickBaseSwitchClock()) {
               return;
            }

            this.baseSwitchClock = this.baseSwitchDelay.getValue();
            this.selectHotbarItem(Items.OBSIDIAN);
         }

         if (this.isHolding(Items.OBSIDIAN) && this.tickBasePlaceClock()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.method_6104(Hand.MAIN_HAND);
            this.basePlaceClock = this.baseDelay.getValue();
            this.baseCrystalling = true;
         }
      }
   }

   private void handleBaseCrystalSelection() {
      if (!this.isHolding(Items.END_CRYSTAL) && !this.baseCrystalSelected) {
         if (!this.tickBaseSwitchClock()) {
            return;
         }

         this.baseCrystalSelected = this.selectHotbarItem(Items.END_CRYSTAL);
         this.baseSwitchClock = this.baseSwitchDelay.getValue();
      }
   }

   private void handleBlockTarget(boolean dontPlace) {
      BlockHitResult hit = this.getLookedBlock();
      if (hit != null) {
         if (!dontPlace && this.canPlaceCrystal(hit.getBlockPos())) {
            this.placeCrystal(hit);
            this.placeClock = this.placeDelay.getValue();
         }
      }
   }

   private void handleEntityTarget(boolean dontBreak) {
      EntityHitResult hit = this.getLookedEntity();
      if (hit != null && !dontBreak) {
         Entity entity = hit.getEntity();
         if (entity instanceof EndCrystalEntity || entity instanceof SlimeEntity) {
            if (!ModuleManager.crystalOptimizer.isAttackInhibited(entity)) {
               int previousSlot = mc.player.method_31548().selectedSlot;
               if ((entity instanceof EndCrystalEntity || entity instanceof SlimeEntity) && this.antiWeakness.getValue() && this.cantBreakCrystal()) {
                  SearchInvResult weapon = InventoryUtility.getAntiWeaknessItem();
                  if (!weapon.found()) {
                     return;
                  }

                  weapon.switchTo();
               }

               this.attackEntity(entity);
               this.breakClock = this.breakDelay.getValue();
               if (this.antiWeakness.getValue()) {
                  InventoryUtility.switchTo(previousSlot);
               }
            }
         }
      }
   }

   private void placeCrystal(@NotNull BlockHitResult hit) {
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      mc.player.method_6104(Hand.MAIN_HAND);
   }

   private void attackEntity(@NotNull Entity entity) {
      mc.interactionManager.attackEntity(mc.player, entity);
      mc.player.method_6104(Hand.MAIN_HAND);
   }

   private boolean tickBaseSwitchClock() {
      if (this.baseSwitchClock > 0) {
         this.baseSwitchClock--;
         return false;
      } else {
         return true;
      }
   }

   private boolean tickBasePlaceClock() {
      if (this.basePlaceClock > 0) {
         this.basePlaceClock--;
         return false;
      } else {
         return true;
      }
   }

   private boolean isActivationDown() {
      return this.activateKey.getValue().getKey() == -1 || this.isBindDown(this.activateKey);
   }

   private boolean isCrystalBaseEnabled() {
      return this.crystalBase.getValue();
   }

   private boolean isCrystalBaseTimingVisible(Integer ignored) {
      return this.crystalBase.getValue();
   }

   private boolean isMouseDown(int button) {
      return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), button) == 1;
   }

   private boolean isHolding(@NotNull Item item) {
      return mc.player.method_6047().isOf(item);
   }

   private boolean isDeadPlayerNearby() {
      return mc.world
         .method_18456()
         .stream()
         .filter(player -> player != mc.player)
         .anyMatch(player -> player.method_5858(mc.player) <= 36.0 && (player.method_29504() || player.method_6032() <= 0.0F));
   }

   private boolean shouldWaitDamageTick() {
      boolean targetDamageTick = mc.world
         .method_18456()
         .stream()
         .filter(player -> player != mc.player)
         .filter(player -> player.method_5858(mc.player) < 36.0)
         .filter(player -> player.method_5854() == null)
         .filter(player -> !player.method_24828())
         .anyMatch(player -> player.field_6235 >= 2);
      return targetDamageTick && !(mc.player.method_6065() instanceof PlayerEntity);
   }

   private boolean cantBreakCrystal() {
      StatusEffectInstance weakness = mc.player.method_6112(StatusEffects.WEAKNESS);
      StatusEffectInstance strength = mc.player.method_6112(StatusEffects.STRENGTH);
      return weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier()) && !this.isTool(mc.player.method_6047());
   }

   private boolean isTool(@NotNull ItemStack stack) {
      return stack.getItem() instanceof SwordItem
         || stack.getItem() instanceof PickaxeItem
         || stack.getItem() instanceof AxeItem
         || stack.getItem() instanceof ShovelItem;
   }

   private boolean isValidBaseStartItem(@NotNull ItemStack stack) {
      Item item = stack.getItem();
      if (item instanceof SwordItem) {
         return true;
      } else {
         return this.baseWorkWithTotem.getValue() && item == Items.TOTEM_OF_UNDYING ? true : this.baseWorkWithCrystal.getValue() && item == Items.END_CRYSTAL;
      }
   }

   private boolean selectHotbarItem(@NotNull Item item) {
      SearchInvResult result = InventoryUtility.findItemInHotBar(item);
      if (!result.found()) {
         return false;
      }

      result.switchTo();
      return true;
   }

   private boolean canPlaceAgainst(@NotNull BlockHitResult hit) {
      BlockPos placePos = hit.getBlockPos().offset(hit.getSide());
      return mc.world.method_8320(placePos).method_45474();
   }

   private boolean canPlaceCrystal(@NotNull BlockPos basePos) {
      if (!this.isCrystalBase(basePos)) {
         return false;
      }

      BlockPos crystalPos = basePos.up();
      if (!mc.world.method_8320(crystalPos).method_26215()) {
         return false;
      }

      Box crystalBox = new Box(
         crystalPos.method_10263(),
         crystalPos.method_10264(),
         crystalPos.method_10260(),
         crystalPos.method_10263() + 1.0,
         crystalPos.method_10264() + 2.0,
         crystalPos.method_10260() + 1.0
      );
      return mc.world.method_8333(null, crystalBox, Entity::isAlive).stream().noneMatch(this::blocksCrystalPlacement);
   }

   private boolean blocksCrystalPlacement(@NotNull Entity entity) {
      return !ModuleManager.crystalOptimizer.shouldIgnoreForPlacement(entity);
   }

   private boolean isCrystalBase(@NotNull BlockPos pos) {
      BlockState state = mc.world.method_8320(pos);
      return state.method_27852(Blocks.OBSIDIAN) || state.method_27852(Blocks.BEDROCK);
   }

   private boolean isChargedAnchor(@NotNull BlockPos pos) {
      BlockState state = mc.world.method_8320(pos);
      return state.method_27852(Blocks.RESPAWN_ANCHOR) && (Integer)state.method_11654(RespawnAnchorBlock.CHARGES) > 0;
   }

   private BlockHitResult getLookedBlock() {
      if (mc.crosshairTarget == null) {
         return null;
      }

      if (mc.crosshairTarget.getType() == Type.BLOCK) {
         return (BlockHitResult)mc.crosshairTarget;
      }

      if (mc.crosshairTarget instanceof EntityHitResult entityHit && ModuleManager.crystalOptimizer.shouldIgnoreForPlacement(entityHit.getEntity())) {
         HitResult result = mc.player.method_5745(5.0, 1.0F, false);
         if (result.getType() == Type.BLOCK) {
            return (BlockHitResult)result;
         }
      }

      return null;
   }

   private EntityHitResult getLookedEntity() {
      return mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY ? (EntityHitResult)mc.crosshairTarget : null;
   }
}
