package vcore.events.impl;

import vcore.events.Event;

public class EventKeyRelease extends Event {
   private final int key;
   private final int scanCode;

   public EventKeyRelease(int key, int scanCode) {
      this.key = key;
      this.scanCode = scanCode;
   }

   public int getKey() {
      return this.key;
   }

   public int getScanCode() {
      return this.scanCode;
   }
}
