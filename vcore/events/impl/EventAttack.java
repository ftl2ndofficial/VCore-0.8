package vcore.events.impl;

import net.minecraft.entity.Entity;
import vcore.events.Event;

public class EventAttack extends Event {
   private Entity entity;
   boolean pre;

   public EventAttack(Entity entity, boolean pre) {
      this.entity = entity;
      this.pre = pre;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public boolean isPre() {
      return this.pre;
   }
}
