package vcore.events.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import vcore.events.Event;

public class EventFireworkMotion extends Event {
   private LivingEntity entity;
   private FireworkRocketEntity fireworkRocketEntity;
   private Vec3d vector;

   public EventFireworkMotion(LivingEntity entity, FireworkRocketEntity fireworkRocketEntity, Vec3d vector) {
      this.entity = entity;
      this.fireworkRocketEntity = fireworkRocketEntity;
      this.vector = vector;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public FireworkRocketEntity getFireworkRocketEntity() {
      return this.fireworkRocketEntity;
   }

   public Vec3d getVector() {
      return this.vector;
   }

   public void setEntity(LivingEntity entity) {
      this.entity = entity;
   }

   public void setFireworkRocketEntity(FireworkRocketEntity fireworkRocketEntity) {
      this.fireworkRocketEntity = fireworkRocketEntity;
   }

   public void setVector(Vec3d vector) {
      this.vector = vector;
   }
}
