package vcore.utility;

public class Timer {
   private long time;

   public Timer() {
      this.reset();
   }

   public boolean passedS(double s) {
      return this.getMs(System.nanoTime() - this.time) >= (long)(s * 1000.0);
   }

   public boolean passedMs(long ms) {
      return this.getMs(System.nanoTime() - this.time) >= ms;
   }

   public boolean every(long ms) {
      boolean passed = this.getMs(System.nanoTime() - this.time) >= ms;
      if (passed) {
         this.reset();
      }

      return passed;
   }

   public void setMs(long ms) {
      this.time = System.nanoTime() - ms * 1000000L;
   }

   public long getPassedTimeMs() {
      return this.getMs(System.nanoTime() - this.time);
   }

   public void reset() {
      this.time = System.nanoTime();
   }

   public long getMs(long time) {
      return time / 1000000L;
   }

   public long getTimeMs() {
      return this.getMs(System.nanoTime() - this.time);
   }
}
