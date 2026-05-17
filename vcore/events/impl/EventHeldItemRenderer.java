package vcore.events.impl;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import vcore.events.Event;

public class EventHeldItemRenderer extends Event {
   private final Hand hand;
   private final ItemStack item;
   private float ep;
   private final MatrixStack stack;

   public EventHeldItemRenderer(Hand hand, ItemStack item, float equipProgress, MatrixStack stack) {
      this.hand = hand;
      this.item = item;
      this.ep = equipProgress;
      this.stack = stack;
   }

   public Hand getHand() {
      return this.hand;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public float getEp() {
      return this.ep;
   }

   public MatrixStack getStack() {
      return this.stack;
   }
}
