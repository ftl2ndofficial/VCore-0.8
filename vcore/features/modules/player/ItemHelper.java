package vcore.features.modules.player;

import java.util.Arrays;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import vcore.core.InputBlocker;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.ItemBindSetting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.player.SearchInvResult;

public class ItemHelper extends Module {
   public final Setting<ItemBindSetting> item1 = new Setting<>("Item 1", new ItemBindSetting());
   public final Setting<ItemBindSetting> item2 = new Setting<>("Item 2", new ItemBindSetting());
   public final Setting<ItemBindSetting> item3 = new Setting<>("Item 3", new ItemBindSetting());
   public final Setting<ItemBindSetting> item4 = new Setting<>("Item 4", new ItemBindSetting());
   public final Setting<ItemBindSetting> item5 = new Setting<>("Item 5", new ItemBindSetting());
   public final Setting<Boolean> grimStop = new Setting<>("GrimStop", false);
   private final List<Setting<ItemBindSetting>> itemSettings = Arrays.asList(this.item1, this.item2, this.item3, this.item4, this.item5);
   private final boolean[] pressed = new boolean[this.itemSettings.size()];
   private boolean scheduledUse;
   private int scheduledInventorySlot = -1;
   private int scheduledHotbarSlot = -1;
   private long scheduledUseAt = -1L;
   private static final long GRIM_STOP_DELAY_MS = 5L;

   public ItemHelper() {
      super("ItemHelper", "Quick swap and use saved items.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.resetScheduledUse();
   }

   @Override
   public void onDisable() {
      this.resetScheduledUse();
   }

   @Override
   public void onUpdate() {
      if (fullNullCheck()) {
         this.resetScheduledUse();
      } else {
         this.handleScheduledUse();
         if (mc.currentScreen == null) {
            for (int i = 0; i < this.itemSettings.size(); i++) {
               ItemBindSetting setting = this.itemSettings.get(i).getValue();
               Bind bind = setting.getBind();
               boolean down = bind != null && this.isKeyPressed(bind.getKey());
               if (down && !this.pressed[i] && !this.useItem(setting)) {
                  this.sendMessage("Item không tồn tại !");
               }

               this.pressed[i] = down;
            }
         }
      }
   }

   public List<Setting<ItemBindSetting>> getItemSettings() {
      return this.itemSettings;
   }

   public int findEmptyIndex() {
      for (int i = 0; i < this.itemSettings.size(); i++) {
         if (!this.itemSettings.get(i).getValue().hasItem()) {
            return i;
         }
      }

      return -1;
   }

   public boolean setItemAt(int index, Item item) {
      if (item != null && item != Items.AIR) {
         Setting<ItemBindSetting> setting = this.itemSettings.get(index);
         ItemBindSetting current = setting.getValue();
         setting.setValue(new ItemBindSetting(Registries.ITEM.method_10221(item).toString(), current.getBind()));
         return true;
      } else {
         return false;
      }
   }

   public void clearItemAt(int index) {
      Setting<ItemBindSetting> setting = this.itemSettings.get(index);
      ItemBindSetting current = setting.getValue();
      setting.setValue(new ItemBindSetting("", current.getBind()));
   }

   public void resetAll() {
      for (Setting<ItemBindSetting> setting : this.itemSettings) {
         setting.setValue(new ItemBindSetting());
      }
   }

   private boolean useItem(ItemBindSetting setting) {
      if (this.scheduledUse) {
         return true;
      }

      Item item = setting.getItem();
      if (item != null && item != Items.AIR) {
         SearchInvResult hotbar = InventoryUtility.findInHotBar(stack -> stack.getItem() == item);
         SearchInvResult inv = hotbar.found() ? SearchInvResult.notFound() : InventoryUtility.findInInventory(stack -> stack.getItem() == item);
         if (!hotbar.found() && !inv.found()) {
            return false;
         }

         this.use(hotbar, inv);
         return true;
      } else {
         return false;
      }
   }

   private void use(SearchInvResult result, SearchInvResult invResult) {
      float[] rotation = this.getUseRotation();
      if (result.found()) {
         InventoryUtility.saveAndSwitchTo(result.slot());
         this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, rotation[0], rotation[1]));
         InventoryUtility.returnSlot();
      } else if (invResult.found()) {
         int targetHotbarSlot = mc.player.method_31548().selectedSlot;
         if (this.grimStop.getValue() && MovementUtility.isMoving()) {
            this.scheduleInventoryUse(invResult.slot(), targetHotbarSlot, 5L);
            return;
         }

         this.useInventoryItem(invResult.slot(), targetHotbarSlot, rotation);
      }
   }

   private void scheduleInventoryUse(int inventorySlot, int hotbarSlot, long delayMs) {
      if (inventorySlot != -1) {
         try {
            InputBlocker.block();
         } catch (Throwable var6) {
         }

         this.scheduledUse = true;
         this.scheduledInventorySlot = inventorySlot;
         this.scheduledHotbarSlot = hotbarSlot;
         this.scheduledUseAt = System.currentTimeMillis() + delayMs;
      }
   }

   private void handleScheduledUse() {
      if (this.scheduledUse && System.currentTimeMillis() >= this.scheduledUseAt) {
         try {
            if (this.scheduledInventorySlot != -1) {
               float[] rotation = this.getUseRotation();
               this.useInventoryItem(this.scheduledInventorySlot, this.scheduledHotbarSlot, rotation);
            }
         } catch (Throwable var3) {
         }

         try {
            this.resetScheduledUse();
         } catch (Throwable var2) {
         }
      }
   }

   private void useInventoryItem(int inventorySlot, int hotbarSlot, float[] rotation) {
      if (inventorySlot != -1 && mc.player != null && mc.interactionManager != null) {
         clickSlot(inventorySlot, hotbarSlot, SlotActionType.SWAP);
         this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, rotation[0], rotation[1]));
         clickSlot(inventorySlot, hotbarSlot, SlotActionType.SWAP);
         this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
      }
   }

   private void resetScheduledUse() {
      this.scheduledUse = false;
      this.scheduledInventorySlot = -1;
      this.scheduledHotbarSlot = -1;
      this.scheduledUseAt = -1L;

      try {
         InputBlocker.unblock();
      } catch (Throwable var2) {
      }
   }

   private float[] getUseRotation() {
      float yaw = mc.player.method_36454();
      float pitch = mc.player.method_36455();
      if (ModuleManager.aura.isEnabled() && Aura.target != null) {
         yaw = ModuleManager.aura.rotationYaw;
         pitch = ModuleManager.aura.rotationPitch;
      }

      return new float[]{yaw, pitch};
   }
}
