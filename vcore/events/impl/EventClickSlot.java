package vcore.events.impl;

import net.minecraft.screen.slot.SlotActionType;
import vcore.events.Event;

public class EventClickSlot extends Event {
   private final SlotActionType slotActionType;
   private final int slot;
   private final int button;
   private final int id;

   public EventClickSlot(SlotActionType slotActionType, int slot, int button, int id) {
      this.slot = slot;
      this.button = button;
      this.id = id;
      this.slotActionType = slotActionType;
   }

   public SlotActionType getSlotActionType() {
      return this.slotActionType;
   }

   public int getSlot() {
      return this.slot;
   }

   public int getButton() {
      return this.button;
   }

   public int getId() {
      return this.id;
   }
}
