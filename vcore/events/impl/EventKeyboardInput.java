package vcore.events.impl;

import net.minecraft.client.input.Input;
import vcore.events.Event;

public class EventKeyboardInput extends Event {
   private final Input input;
   private boolean clearMovementInput;

   public EventKeyboardInput(Input input) {
      this.input = input;
   }

   public Input getInput() {
      return this.input;
   }

   public void clearMovementInput() {
      this.clearMovementInput = true;
   }

   public boolean shouldClearMovementInput() {
      return this.clearMovementInput;
   }

   public static void clearMovementInput(Input input) {
      input.pressingForward = false;
      input.pressingBack = false;
      input.pressingLeft = false;
      input.pressingRight = false;
      input.movementForward = 0.0F;
      input.movementSideways = 0.0F;
      input.jumping = false;
      input.sneaking = false;
   }
}
