package vcore.utility.render.animation.advanced;

public class AnimationTimer {
   private long baseTime;

   public AnimationTimer() {
      this.reset();
   }

   public boolean finished(long duration) {
      return this.getElapsedTime() >= duration;
   }

   public void reset() {
      this.baseTime = System.currentTimeMillis();
   }

   public void setMillis(long millis) {
      this.baseTime = millis;
   }

   public long getElapsedTime() {
      return System.currentTimeMillis() - this.baseTime;
   }
}
