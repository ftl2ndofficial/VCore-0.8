package vcore.setting.impl;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ItemBindSetting {
   private String itemId;
   private Bind bind;

   public ItemBindSetting() {
      this("", new Bind(-1, false, false));
   }

   public ItemBindSetting(String itemId, Bind bind) {
      this.itemId = itemId == null ? "" : itemId;
      this.bind = bind == null ? new Bind(-1, false, false) : bind;
   }

   public String getItemId() {
      return this.itemId;
   }

   public void setItemId(String itemId) {
      this.itemId = itemId == null ? "" : itemId;
   }

   public boolean hasItem() {
      return this.itemId != null && !this.itemId.isBlank();
   }

   public Bind getBind() {
      return this.bind;
   }

   public void setBind(Bind bind) {
      this.bind = bind == null ? new Bind(-1, false, false) : bind;
   }

   public void clearItem() {
      this.itemId = "";
   }

   public void setItem(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         this.itemId = Registries.ITEM.method_10221(stack.getItem()).toString();
      } else {
         this.clearItem();
      }
   }

   public Item getItem() {
      if (!this.hasItem()) {
         return null;
      }

      Identifier id = Identifier.tryParse(this.itemId);
      return id != null && Registries.ITEM.method_10250(id) ? (Item)Registries.ITEM.method_10223(id) : null;
   }

   public String getDisplayName(String fallback) {
      Item item = this.getItem();
      return item == null ? fallback : item.getName().getString();
   }
}
