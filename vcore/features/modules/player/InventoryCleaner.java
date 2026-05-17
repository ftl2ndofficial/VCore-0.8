package vcore.features.modules.player;

import java.util.ArrayList;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.Timer;

public class InventoryCleaner extends Module {
   public final Setting<ItemSelectSetting> items = new Setting<>("Items", new ItemSelectSetting(new ArrayList<>()));
   private final Setting<InventoryCleaner.DropWhen> dropWhen = new Setting<>("DropWhen", InventoryCleaner.DropWhen.NotInInventory);
   private final Setting<Integer> delay = new Setting<>("Delay", 50, 0, 500);
   private final Setting<Boolean> cleanChests = new Setting<>("CleanChests", false);
   private final Timer delayTimer = new Timer();
   private boolean dirty;

   public InventoryCleaner() {
      super("InventoryCleaner", "Cleans unwanted items.", Module.Category.PLAYER);
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      boolean inInv = mc.currentScreen instanceof InventoryScreen;
      if (mc.player.field_7512 instanceof GenericContainerScreenHandler chest && this.cleanChests.getValue()) {
         for (int i = 0; i < chest.getInventory().size(); i++) {
            Slot slot = chest.method_7611(i);
            if (slot.hasStack()
               && this.dropThisShit(slot.getStack())
               && !mc.currentScreen.getTitle().getString().contains("Ð\u0090ÑƒÐºÑ†Ð¸Ð¾Ð½")
               && !mc.currentScreen.getTitle().getString().contains("Ð¿Ð¾ÐºÑƒÐ¿ÐºÐ¸")
               && this.delayTimer.every(this.delay.getValue().intValue())) {
               mc.interactionManager.clickSlot(mc.player.field_7512.syncId, i, 1, SlotActionType.THROW, mc.player);
               this.dirty = true;
            }
         }
      }

      if (this.dropWhen.getValue() != InventoryCleaner.DropWhen.Inventory || inInv) {
         if (this.dropWhen.getValue() != InventoryCleaner.DropWhen.NotInInventory || !inInv) {
            for (int slot = 0; slot < 36; slot++) {
               ItemStack itemFromslot = mc.player.method_31548().method_5438(slot);
               if (this.dropThisShit(itemFromslot)) {
                  this.drop(slot);
               }
            }

            if (this.dirty && this.delayTimer.passedMs(this.delay.getValue() + 100)) {
               this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
               this.debug("after click cleaning...");
               this.dirty = false;
            }
         }
      }
   }

   private void drop(int slot) {
      if (this.delayTimer.every(this.delay.getValue().intValue())) {
         mc.interactionManager.clickSlot(mc.player.field_7512.syncId, slot < 9 ? slot + 36 : slot, 1, SlotActionType.THROW, mc.player);
         this.dirty = true;
      }
   }

   private boolean dropThisShit(ItemStack stack) {
      return this.items.getValue().getItemsById().contains(stack.getItem().getTranslationKey().replace("block.minecraft.", "").replace("item.minecraft.", ""));
   }

   public enum DropWhen {
      Inventory,
      Always,
      NotInInventory;
   }
}
