package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;
import vcore.core.InputBlocker;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.features.modules.movement.Blink;
import vcore.setting.Setting;
import vcore.setting.impl.SettingGroup;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public final class AutoTotem extends Module {
   private final Setting<AutoTotem.OffHand> offhand = new Setting<>("Item", AutoTotem.OffHand.Totem);
   private final Setting<Float> healthF = new Setting<>("HP", 3.5F, 0.0F, 20.0F, v -> this.offhand.is(AutoTotem.OffHand.Smart));
   private final Setting<Float> elytraHP = new Setting<>("ElytraHP", 16.0F, 0.0F, 20.0F, v -> this.offhand.is(AutoTotem.OffHand.Smart));
   private final Setting<Integer> ticksDelay = new Setting<>("TicksDelay", 2, 1, 5, v -> this.isDelaySwapEnabled());
   private final Setting<Boolean> goldenHearts = new Setting<>("GoldenHearts", false, v -> this.offhand.is(AutoTotem.OffHand.Smart));
   private final Setting<Boolean> delaySwap = new Setting<>("Delay", false);
   private final Setting<Boolean> grimStop = new Setting<>("GrimStop", false);
   private final Setting<Boolean> resetAttackCooldown = new Setting<>("ResetAttackCooldown", false);
   private final Setting<SettingGroup> safety = new Setting<>("Safety", new SettingGroup(false, 0), v -> this.offhand.is(AutoTotem.OffHand.Smart));
   private final Setting<Boolean> maceEntity = new Setting<>("MaceEntity", false, v -> this.offhand.is(AutoTotem.OffHand.Smart)).addToGroup(this.safety);
   private final Setting<Boolean> onCreeper = new Setting<>("OnCreeper", false, v -> this.offhand.is(AutoTotem.OffHand.Smart)).addToGroup(this.safety);
   private final Setting<Boolean> onElytra = new Setting<>("OnElytra", false, v -> this.offhand.is(AutoTotem.OffHand.Smart)).addToGroup(this.safety);
   private final Setting<Boolean> onFall = new Setting<>("OnFall", false, v -> this.offhand.is(AutoTotem.OffHand.Smart)).addToGroup(this.safety);
   private final Setting<Boolean> onMinecartTnt = new Setting<>("OnMinecartTNT", false, v -> this.offhand.is(AutoTotem.OffHand.Smart)).addToGroup(this.safety);
   private final Setting<Boolean> onTnt = new Setting<>("OnTNT", false, v -> this.offhand.is(AutoTotem.OffHand.Smart)).addToGroup(this.safety);
   private int pendingSwapSlot = -1;
   private int pendingSwapTicks = -1;
   private Item prevItem;
   private boolean grimInProgress = false;
   private int grimSlot = -1;
   private long grimSwapAt = -1L;
   private long grimUnblockAt = -1L;

   public AutoTotem() {
      super("AutoTotem", "Auto places totems in offhand.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (e != null) {
         if (this.grimInProgress && !this.grimStop.getValue()) {
            this.resetGrimStop();
         }

         this.handleGrimStop();
         if (this.pendingSwapTicks > 0) {
            this.pendingSwapTicks--;
         }

         int slot = this.getItemSlot();
         if (slot == -1) {
            this.resetPendingDelay();
         }

         this.swapTo(slot);
      }
   }

   private void handleGrimStop() {
      if (this.grimInProgress) {
         if (mc.player == null) {
            this.resetGrimStop();
         } else {
            long now = System.currentTimeMillis();
            if (this.grimSwapAt > 0L && now >= this.grimSwapAt) {
               if (this.grimSlot != -1) {
                  try {
                     this.performSwap(this.grimSlot);
                  } catch (Throwable var4) {
                  }
               }

               this.grimSwapAt = -1L;
               this.grimUnblockAt = now + 10L;
            } else {
               if (this.grimUnblockAt > 0L && now >= this.grimUnblockAt) {
                  this.resetGrimStop();
               }
            }
         }
      }
   }

   private float getTriggerHealth() {
      return mc.player == null ? 0.0F : mc.player.method_6032() + (this.shouldCalcAbsorption() ? mc.player.method_6067() : 0.0F);
   }

   public void swapTo(int slot) {
      if (slot != -1) {
         if (mc.currentScreen instanceof GenericContainerScreen) {
            return;
         }

         if (mc.player == null || mc.player.method_31548() == null) {
            return;
         }

         if (this.delaySwap.getValue()) {
            if (this.pendingSwapSlot != slot) {
               this.pendingSwapSlot = slot;
               this.pendingSwapTicks = this.ticksDelay.getValue();
               if (this.pendingSwapTicks > 0) {
                  return;
               }
            }

            if (this.pendingSwapTicks > 0) {
               return;
            }
         }

         if (this.grimStop.getValue()) {
            this.startGrimStop(slot);
         } else {
            this.performSwap(slot);
         }

         this.resetPendingDelay();
      }
   }

   private void performSwap(int slot) {
      mc.interactionManager.clickSlot(mc.player.field_7512.syncId, convertSlotIndex(slot), 40, SlotActionType.SWAP, mc.player);
      if (this.resetAttackCooldown.getValue()) {
         mc.player.method_7350();
      }
   }

   public int getItemSlot() {
      if (mc.player != null && mc.world != null) {
         Item offHandItem = mc.player.method_6079().getItem();
         int itemSlot = -1;
         Item item = null;
         switch ((AutoTotem.OffHand)this.offhand.getValue()) {
            case Totem:
               item = Items.TOTEM_OF_UNDYING;
               break;
            case Smart:
               if (offHandItem != Items.TOTEM_OF_UNDYING && !mc.player.method_6079().isEmpty()) {
                  this.prevItem = offHandItem;
               }

               if (this.getTriggerHealth() <= this.getHealthThreshold()) {
                  item = Items.TOTEM_OF_UNDYING;
               } else {
                  item = this.prevItem != null ? this.prevItem : offHandItem;
               }
         }

         if (this.offhand.getValue() == AutoTotem.OffHand.Smart) {
            if (this.getTriggerHealth() <= this.getHealthThreshold()
               && (InventoryUtility.findItemInInventory(Items.TOTEM_OF_UNDYING).found() || offHandItem == Items.TOTEM_OF_UNDYING)) {
               item = Items.TOTEM_OF_UNDYING;
            }

            boolean safetyEnabled = this.safety.getValue().isExtended();
            if (safetyEnabled && this.onFall.getValue() && this.getTriggerHealth() - ((mc.player.field_6017 - 3.0F) / 2.0F + 3.5F) < 0.5) {
               item = Items.TOTEM_OF_UNDYING;
            }

            if (safetyEnabled && this.onElytra.getValue() && mc.player.method_6128()) {
               item = Items.TOTEM_OF_UNDYING;
            }
         }

         if (this.offhand.getValue() == AutoTotem.OffHand.Smart && this.safety.getValue().isExtended() && this.maceEntity.getValue()) {
            for (PlayerEntity pl : Managers.ASYNC.getAsyncPlayers()) {
               if (pl != null && pl != mc.player && !Managers.FRIEND.isFriend(pl)) {
                  boolean hasMace = pl.method_6047().getItem() == Items.MACE || pl.method_6079().getItem() == Items.MACE;
                  if (hasMace) {
                     double dx = Math.abs(pl.method_23317() - mc.player.method_23317());
                     double dz = Math.abs(pl.method_23321() - mc.player.method_23321());
                     double dy = pl.method_23318() - mc.player.method_23318();
                     if (dx <= 8.0 && dz <= 8.0 && dy >= 5.0) {
                        item = Items.TOTEM_OF_UNDYING;
                        break;
                     }
                  }
               }
            }
         }

         if (this.offhand.getValue() == AutoTotem.OffHand.Smart && this.safety.getValue().isExtended()) {
            for (Entity entity : mc.world.getEntities()) {
               if (entity != null && entity.isAlive() && !(this.getPlayerPos().squaredDistanceTo(entity.getPos()) > 36.0)) {
                  if (this.onTnt.getValue() && entity instanceof TntEntity) {
                     item = Items.TOTEM_OF_UNDYING;
                     break;
                  }

                  if (this.onMinecartTnt.getValue() && entity instanceof TntMinecartEntity) {
                     item = Items.TOTEM_OF_UNDYING;
                     break;
                  }

                  if (this.onCreeper.getValue() && entity instanceof CreeperEntity) {
                     item = Items.TOTEM_OF_UNDYING;
                     break;
                  }
               }
            }
         }

         if (item != null && mc.player.method_6079().getItem() != item) {
            SearchInvResult itemResult = InventoryUtility.findItemInInventory(item);
            if (itemResult.found()) {
               itemSlot = itemResult.slot();
            }

            return item == mc.player.method_6047().getItem() && mc.options.useKey.isPressed() ? -1 : itemSlot;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private float getHealthThreshold() {
      if (mc.player == null) {
         return 0.0F;
      } else {
         return mc.player.method_31548().getArmorStack(2).getItem() == Items.ELYTRA ? this.elytraHP.getValue() : this.healthF.getValue();
      }
   }

   private boolean shouldCalcAbsorption() {
      return this.offhand.getValue() == AutoTotem.OffHand.Smart && this.goldenHearts.getValue();
   }

   private void resetPendingDelay() {
      this.pendingSwapSlot = -1;
      this.pendingSwapTicks = -1;
   }

   private void startGrimStop(int slot) {
      if (!this.grimInProgress) {
         try {
            InputBlocker.block();
         } catch (Throwable var3) {
         }

         this.grimInProgress = true;
         this.grimSlot = slot;
         this.grimSwapAt = System.currentTimeMillis() + 55L;
         this.grimUnblockAt = -1L;
      }
   }

   private void resetGrimStop() {
      try {
         InputBlocker.unblock();
      } catch (Throwable var2) {
      }

      this.grimInProgress = false;
      this.grimSlot = -1;
      this.grimSwapAt = -1L;
      this.grimUnblockAt = -1L;
   }

   private boolean isDelaySwapEnabled() {
      return this.delaySwap.getValue();
   }

   private static int convertSlotIndex(int slotIndex) {
      return slotIndex < 9 ? 36 + slotIndex : slotIndex;
   }

   private Vec3d getPlayerPos() {
      return ModuleManager.blink.isEnabled() ? Blink.lastPos : mc.player.method_19538();
   }

   @Override
   public void onDisable() {
      this.resetGrimStop();
      this.resetPendingDelay();
      super.onDisable();
   }

   private enum OffHand {
      Totem,
      Smart;
   }
}
