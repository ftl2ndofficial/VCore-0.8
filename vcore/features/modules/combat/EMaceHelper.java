package vcore.features.modules.combat;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.player.ElytraSwap;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class EMaceHelper extends Module {
   public final Setting<Boolean> autoTarget = new Setting<>("Auto Target", false);
   public final Setting<Boolean> peakAssist = new Setting<>("PeakAssist", false);
   public final Setting<Integer> height = new Setting<>("Height", 100, 25, 1000, v -> this.peakAssist.getValue());
   public final Setting<Boolean> armorSwap = new Setting<>("Armor Swap", false);
   public final Setting<Boolean> autoFireWork = new Setting<>("FireWork Vector", false);
   public final Setting<Boolean> extraAimRange = new Setting<>("Extra AimRange", false);
   public final Setting<Integer> swapDistance = new Setting<>("Swap Distance", 10, 5, 16, v -> this.armorSwap.getValue());
   public final Setting<Integer> fireWorkDistance = new Setting<>("FireWork Distance", 30, 16, 64, v -> this.autoFireWork.getValue());
   public final Setting<Integer> aimDistance = new Setting<>("Aim Distance", 900, 800, 1000, v -> this.extraAimRange.getValue());
   private boolean swapping;
   private boolean fireworkTriggered;
   private boolean peakOffsetActive = true;
   private boolean peakOffsetDisabledPendingFirework;
   private static volatile boolean auraPostAttackTriggered;
   private static volatile boolean peakAssistPostAttackTriggered;
   private final Timer peakAssistFireworkTimer = new Timer();

   public EMaceHelper() {
      super("EMaceHelper", "Elytra mace helper automation.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.swapping = false;
      this.resetFireworkFlags();
      this.resetPeakAssistState();
   }

   @Override
   public void onDisable() {
      this.swapping = false;
      this.resetFireworkFlags();
      this.resetPeakAssistState();
   }

   @Override
   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         if (this.peakAssist.getValue()) {
            this.handlePeakAssist();
         } else {
            this.resetPeakAssistState();
         }

         if (this.armorSwap.getValue()) {
            this.handleArmorSwap();
         }

         if (this.autoFireWork.getValue()) {
            this.handleAutoFireWork();
         } else {
            this.resetFireworkFlags();
         }
      }
   }

   public boolean shouldSkipAuraTarget(Entity target) {
      if (target == null || target.isRemoved()) {
         return false;
      }

      if (!this.canUseAutoTarget()) {
         return false;
      }

      if (target instanceof LivingEntity livingTarget) {
         if (livingTarget.isFallFlying()) {
            return true;
         } else if (target instanceof PlayerEntity player && player.method_6096() == 0) {
            return true;
         } else {
            return livingTarget.method_5681() && this.isEyeInWater(livingTarget) ? true : this.isTargetBlockedBySolidBlock(target);
         }
      } else {
         return false;
      }
   }

   private boolean canUseAutoTarget() {
      if (mc.player == null || mc.world == null) {
         return false;
      } else if (!this.isEnabled()) {
         return false;
      } else {
         return !this.autoTarget.getValue() ? false : this.canUseMaceAuraSupport();
      }
   }

   private boolean isEyeInWater(LivingEntity livingTarget) {
      return mc.world.method_8316(BlockPos.ofFloored(livingTarget.method_33571())).isIn(FluidTags.WATER);
   }

   private boolean isTargetBlockedBySolidBlock(Entity target) {
      Vec3d start = mc.player.method_33571();
      Vec3d end = this.getClosestVisiblePoint(target);
      Vec3d direction = end.subtract(start);
      if (direction.lengthSquared() < 1.0E-6) {
         return false;
      }

      Vec3d step = direction.normalize().multiply(0.05);

      for (int attempts = 0; attempts < 8; attempts++) {
         BlockHitResult hit = mc.world.method_17742(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (hit.method_17783() != Type.BLOCK) {
            return false;
         }

         if (!this.isGrassBlock(hit)) {
            return true;
         }

         start = hit.method_17784().add(step);
      }

      return false;
   }

   private Vec3d getClosestVisiblePoint(Entity target) {
      Vec3d eyePos = mc.player.method_33571();
      return new Vec3d(
         MathHelper.clamp(eyePos.x, target.method_5829().minX, target.method_5829().maxX),
         MathHelper.clamp(eyePos.y, target.method_5829().minY, target.method_5829().maxY),
         MathHelper.clamp(eyePos.z, target.method_5829().minZ, target.method_5829().maxZ)
      );
   }

   private boolean isGrassBlock(BlockHitResult hit) {
      BlockState state = mc.world.method_8320(hit.getBlockPos());
      return state.method_27852(Blocks.SHORT_GRASS) || state.method_27852(Blocks.TALL_GRASS);
   }

   public Vec3d applyPeakAssistAimOffset(Entity target, Vec3d aimPos) {
      if (aimPos == null || target == null || target.isRemoved()) {
         return aimPos;
      } else if (!this.canUsePeakAssist()) {
         return aimPos;
      } else if (!this.peakOffsetActive) {
         return aimPos;
      } else if (Aura.target != target) {
         return aimPos;
      } else {
         Vec3d offsetPos = aimPos.add(0.0, this.height.getValue().intValue(), 0.0);
         if (this.shouldDisablePeakOffset(offsetPos)) {
            this.peakOffsetActive = false;
            this.peakOffsetDisabledPendingFirework = true;
            return aimPos;
         } else {
            return offsetPos;
         }
      }
   }

   private void handlePeakAssist() {
      if (!this.canUsePeakAssist()) {
         this.resetPeakAssistState();
      } else {
         if (peakAssistPostAttackTriggered) {
            peakAssistPostAttackTriggered = false;
            this.peakOffsetActive = true;
            this.peakAssistFireworkTimer.reset();
         }

         Entity target = Aura.target;
         if (target != null && !target.isRemoved()) {
            Vec3d offsetPos = this.getPeakAssistOffsetPos(target);
            if (offsetPos != null) {
               if (this.peakOffsetActive && this.shouldDisablePeakOffset(offsetPos)) {
                  this.peakOffsetActive = false;
                  this.peakOffsetDisabledPendingFirework = true;
               }

               if (this.peakOffsetActive) {
                  if (this.peakAssistFireworkTimer.every(500L)) {
                     this.triggerPeakAssistFirework();
                  }
               } else {
                  if (this.peakOffsetDisabledPendingFirework) {
                     this.triggerPeakAssistFirework();
                     this.peakOffsetDisabledPendingFirework = false;
                  }
               }
            }
         } else {
            this.peakOffsetDisabledPendingFirework = false;
         }
      }
   }

   private boolean canUsePeakAssist() {
      if (mc.player == null || mc.world == null) {
         return false;
      } else if (!this.isEnabled()) {
         return false;
      } else if (!this.peakAssist.getValue()) {
         return false;
      } else {
         return !mc.player.method_6128() ? false : this.canUseMaceAuraSupport();
      }
   }

   private Vec3d getPeakAssistOffsetPos(Entity target) {
      Vec3d baseAimPos;
      if (target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
         baseAimPos = ModuleManager.aura.getElytraTargetVec(livingTarget, true);
      } else {
         baseAimPos = target.getEyePos();
      }

      return baseAimPos == null ? null : baseAimPos.add(0.0, this.height.getValue().intValue(), 0.0);
   }

   private boolean shouldDisablePeakOffset(Vec3d offsetPos) {
      return mc.player.method_23318() > offsetPos.y ? true : mc.player.method_19538().squaredDistanceTo(offsetPos) < 1.0;
   }

   private void triggerPeakAssistFirework() {
      if (this.hasFirework()) {
         if (!mc.player.method_7357().isCoolingDown(Items.FIREWORK_ROCKET)) {
            ModuleManager.elytraSwap.useFireWork();
         }
      }
   }

   private void resetPeakAssistState() {
      this.peakOffsetActive = true;
      this.peakOffsetDisabledPendingFirework = false;
      peakAssistPostAttackTriggered = false;
      this.peakAssistFireworkTimer.reset();
   }

   private boolean isPeakOffsetRunning() {
      return this.canUsePeakAssist() && this.peakOffsetActive;
   }

   public boolean isPeakAssistActive() {
      return this.isPeakOffsetRunning();
   }

   private void handleArmorSwap() {
      if (!this.isPeakOffsetRunning()) {
         if (mc.currentScreen == null && !this.swapping) {
            if (this.canUseMaceAuraSupport()) {
               if (ModuleManager.maceSwap.isEnabled()) {
                  if (ModuleManager.maceSwap.hasMaceInHotbar()) {
                     Entity target = Aura.target;
                     if (target != null && !target.isRemoved()) {
                        if (this.isWearingElytra()) {
                           if (mc.player.method_6128()) {
                              if (!(mc.player.method_23318() <= target.getY() + 5.0)) {
                                 double distance = this.getArmorSwapDistance(target);
                                 if (!(distance > this.swapDistance.getValue().intValue())) {
                                    this.swapToChestplate();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void swapToChestplate() {
      int slot = ElytraSwap.getChestPlateSlot();
      if (slot != -1) {
         this.swapping = true;

         try {
            clickSlot(slot);
            clickSlot(6);
            clickSlot(slot);
            this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
         } finally {
            this.swapping = false;
         }
      }
   }

   private double getArmorSwapDistance(Entity target) {
      if (target instanceof LivingEntity livingTarget && ModuleManager.elytraTarget.shouldTarget(livingTarget)) {
         Vec3d predictedPos = ModuleManager.aura.getElytraTargetVec(livingTarget, true);
         if (predictedPos != null) {
            return mc.player.method_19538().distanceTo(predictedPos);
         }
      }

      return mc.player.method_19538().distanceTo(target.getPos());
   }

   private void handleAutoFireWork() {
      Entity target = Aura.target;
      if (target != null && !target.isRemoved()) {
         boolean wearingElytra = mc.player.method_31548().method_5438(38).getItem() == Items.ELYTRA;
         boolean isFallFlying = mc.player.method_6128();
         double distance = mc.player.method_19538().distanceTo(target.getPos());
         if (this.shouldResetFirework(wearingElytra, isFallFlying, distance)) {
            this.resetFireworkFlags();
         } else if (this.canUseMaceAuraSupport()) {
            if (wearingElytra) {
               if (isFallFlying) {
                  if (this.hasFirework()) {
                     boolean holdingMace = this.isMace(mc.player.method_6047());
                     if (!holdingMace) {
                        if (!ModuleManager.maceSwap.isEnabled()) {
                           return;
                        }

                        if (!ModuleManager.maceSwap.hasMaceInHotbar()) {
                           return;
                        }
                     }

                     if (!(mc.player.method_23318() <= target.getY() + 10.0)) {
                        if (!(distance > this.fireWorkDistance.getValue().intValue())) {
                           if (!this.fireworkTriggered && this.useFireWorkSilent()) {
                              this.fireworkTriggered = true;
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.resetFireworkFlags();
      }
   }

   private boolean shouldResetFirework(boolean wearingElytra, boolean isFallFlying, double distance) {
      return !wearingElytra || !isFallFlying || auraPostAttackTriggered || distance > this.fireWorkDistance.getValue().intValue();
   }

   private void resetFireworkFlags() {
      this.fireworkTriggered = false;
      auraPostAttackTriggered = false;
   }

   public float getElytraAimRangeOverride(float defaultRange) {
      return !this.shouldUseExtraAimRange() ? defaultRange : this.aimDistance.getValue().floatValue();
   }

   private boolean shouldUseExtraAimRange() {
      if (mc.player == null || mc.world == null) {
         return false;
      } else if (!this.isEnabled()) {
         return false;
      } else {
         return !this.extraAimRange.getValue() ? false : this.canUseMaceAuraSupport();
      }
   }

   private boolean canUseMaceAuraSupport() {
      if (mc.player == null || mc.world == null) {
         return false;
      } else {
         return !ModuleManager.aura.isEnabled()
            ? false
            : this.isMace(mc.player.method_6047()) || ModuleManager.maceSwap.isEnabled() && ModuleManager.maceSwap.hasMaceInHotbar();
      }
   }

   private boolean hasFirework() {
      return mc.player.method_6079().getItem() == Items.FIREWORK_ROCKET
         ? true
         : InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).found() || InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).found();
   }

   private boolean useFireWorkSilent() {
      if (mc.player.method_6079().getItem() == Items.FIREWORK_ROCKET) {
         this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, ModuleManager.aura.rotationYaw, ModuleManager.aura.rotationPitch));
         this.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
         return true;
      }

      SearchInvResult hotbarFireWorkResult = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET);
      SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);
      if (!hotbarFireWorkResult.found() && !fireWorkResult.found()) {
         return false;
      }

      InventoryUtility.saveSlot();
      boolean swappedFromInventory = false;
      if (hotbarFireWorkResult.found()) {
         hotbarFireWorkResult.switchTo();
      } else {
         if (mc.player.field_7512 != mc.player.field_7498) {
            InventoryUtility.returnSlot();
            return false;
         }

         swappedFromInventory = true;
         mc.interactionManager
            .clickSlot(mc.player.field_7512.syncId, fireWorkResult.slot(), mc.player.method_31548().selectedSlot, SlotActionType.SWAP, mc.player);
         this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
      }

      this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, ModuleManager.aura.rotationYaw, ModuleManager.aura.rotationPitch));
      this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      InventoryUtility.returnSlot();
      if (swappedFromInventory) {
         mc.interactionManager
            .clickSlot(mc.player.field_7512.syncId, fireWorkResult.slot(), mc.player.method_31548().selectedSlot, SlotActionType.SWAP, mc.player);
         this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
      }

      return true;
   }

   private boolean isMace(ItemStack stack) {
      if (stack != null && stack.getItem() != null) {
         String key = stack.getItem().getTranslationKey();
         return key != null && key.toLowerCase().contains("mace") ? true : stack.getItem().toString().toLowerCase().contains("mace");
      } else {
         return false;
      }
   }

   private boolean isWearingElytra() {
      return mc.player.method_31548().method_5438(38).getItem() == Items.ELYTRA;
   }

   public static void markAuraPostAttack() {
      auraPostAttackTriggered = true;
      peakAssistPostAttackTriggered = true;
   }
}
