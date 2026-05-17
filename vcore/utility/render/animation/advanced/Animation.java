package vcore.utility.render.animation.advanced;

public class Animation {
   private final AnimationTimer timer = new AnimationTimer();
   private int speed;
   private double size = 1.0;
   private boolean forward;
   private Easing easing = Easing.LINEAR;

   public boolean finished(boolean expectedForward) {
      if (this.timer.finished(this.speed)) {
         return expectedForward ? this.forward : !this.forward;
      } else {
         return false;
      }
   }

   public boolean finished() {
      return this.timer.finished(this.speed) && this.forward;
   }

   public Animation setForward(boolean forward) {
      if (this.forward != forward) {
         this.forward = forward;
         double remaining = this.size - Math.min(this.size, this.timer.getElapsedTime());
         this.timer.setMillis((long)(System.currentTimeMillis() - remaining));
      }

      return this;
   }

   public Animation finish() {
      this.timer.setMillis(System.currentTimeMillis() - this.speed);
      return this;
   }

   public Animation setEasing(Easing easing) {
      this.easing = easing;
      return this;
   }

   public Animation setSpeed(int speed) {
      this.speed = speed;
      return this;
   }

   public Animation setSize(float size) {
      this.size = size;
      return this;
   }

   public float getLinear() {
      if (this.forward) {
         return this.timer.finished(this.speed) ? (float)this.size : (float)((double)this.timer.getElapsedTime() / this.speed * this.size);
      } else {
         return this.timer.finished(this.speed) ? 0.0F : (float)((1.0 - (double)this.timer.getElapsedTime() / this.speed) * this.size);
      }
   }

   public float get() {
      if (this.forward) {
         return this.timer.finished(this.speed) ? (float)this.size : (float)(this.easing.apply((double)this.timer.getElapsedTime() / this.speed) * this.size);
      } else {
         return this.timer.finished(this.speed) ? 0.0F : (float)((1.0 - this.easing.apply((double)this.timer.getElapsedTime() / this.speed)) * this.size);
      }
   }

   public float reversed() {
      return 1.0F - this.get();
   }

   public void reset() {
      this.timer.reset();
   }

   public AnimationTimer getTimer() {
      return this.timer;
   }

   public int getSpeed() {
      return this.speed;
   }

   public double getSize() {
      return this.size;
   }

   public Easing getEasing() {
      return this.easing;
   }

   public boolean isForward() {
      return this.forward;
   }
}
