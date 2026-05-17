package vcore.events.impl;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;

public class ClientClickEvent extends ClickEvent {
   public ClientClickEvent(Action action, String value) {
      super(action, value);
   }
}
