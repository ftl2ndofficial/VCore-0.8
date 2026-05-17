package vcore.setting.impl;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class ItemSelectSetting {
   private List<String> itemsById;

   public ItemSelectSetting(List<String> itemsById) {
      this.itemsById = itemsById;
   }

   public List<String> getItemsById() {
      return this.itemsById;
   }

   public void add(String s) {
      this.itemsById.add(s);
   }

   public void remove(String s) {
      this.itemsById.remove(s);
   }

   public boolean contains(String s) {
      return this.itemsById.contains(s);
   }

   public void add(Block b) {
      this.add(b.getTranslationKey().replace("block.minecraft.", ""));
   }

   public void add(Item i) {
      this.add(i.getTranslationKey().replace("item.minecraft.", ""));
   }

   public void remove(Block b) {
      this.remove(b.getTranslationKey().replace("block.minecraft.", ""));
   }

   public void remove(Item i) {
      this.remove(i.getTranslationKey().replace("item.minecraft.", ""));
   }

   public boolean contains(Block b) {
      return this.contains(b.getTranslationKey().replace("block.minecraft.", ""));
   }

   public boolean contains(Item i) {
      return this.contains(i.getTranslationKey().replace("item.minecraft.", ""));
   }

   public void clear() {
      this.itemsById.clear();
   }
}
