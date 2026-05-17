package vcore.utility;

import vcore.core.manager.IManager;

public class TickTimer {
   private int time;

   public TickTimer() {
      this.reset();
   }

   public boolean passedTicks(long t) {
      if (this.getPassedTicks() < 0) {
         this.reset();
      }

      return this.getPassedTicks() >= t;
   }

   public boolean every(long ms) {
      if (this.getPassedTicks() < 0) {
         this.reset();
      }

      boolean passed = this.getPassedTicks() >= ms;
      if (passed) {
         this.reset();
      }

      return passed;
   }

   public void set(int t) {
      this.time = t;
   }

   public void reset() {
      this.time = IManager.mc.player == null ? 0 : IManager.mc.player.field_6012;
   }

   private int getPassedTicks() {
      return IManager.mc.player == null ? 0 : IManager.mc.player.field_6012 - this.time;
   }
}
