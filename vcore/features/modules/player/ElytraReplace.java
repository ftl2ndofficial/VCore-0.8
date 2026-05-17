package vcore.features.modules.player;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import vcore.core.Managers;
import vcore.features.modules.Module;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class ElytraReplace extends Module {
   private final Setting<Integer> durability = new Setting<>("Durability", 5, 0, 100);

   public ElytraReplace() {
      super("ElytraReplace", "Replaces broken elytras.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      ItemStack is = mc.player.method_6118(EquipmentSlot.CHEST);
      if (is.isOf(Items.ELYTRA) && 100.0F - (float)is.getDamage() / is.getMaxDamage() * 100.0F <= this.durability.getValue().intValue()) {
         SearchInvResult result = InventoryUtility.findInInventory(
            stack -> stack.getItem() instanceof ElytraItem
               ? 100.0F - (float)stack.getDamage() / stack.getMaxDamage() * 100.0F > this.durability.getValue().intValue()
               : false
         );
         if (result.found()) {
            clickSlot(result.slot());
            clickSlot(6);
            clickSlot(result.slot());
            this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
            Managers.NOTIFICATION.publicity("ElytraReplace", "Swapping the old elytra for a new one!", 2, Notification.Type.SUCCESS);
            this.sendMessage("Swapping the old elytra for a new one!");
         }
      }
   }
}
