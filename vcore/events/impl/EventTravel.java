package vcore.events.impl;

import net.minecraft.util.math.Vec3d;
import vcore.events.Event;

public class EventTravel extends Event {
   private Vec3d mVec;
   private boolean pre;

   public EventTravel(Vec3d mVec, boolean pre) {
      this.mVec = mVec;
      this.pre = pre;
   }

   public Vec3d getmVec() {
      return this.mVec;
   }

   public boolean isPre() {
      return this.pre;
   }
}
