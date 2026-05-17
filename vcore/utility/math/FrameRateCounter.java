package vcore.utility.math;

import java.util.ArrayList;
import java.util.List;

public class FrameRateCounter {
   public static final FrameRateCounter INSTANCE = new FrameRateCounter();
   final List<Long> records = new ArrayList<>();
   int fps = 5;

   public void recordFrame() {
      long c = System.currentTimeMillis();
      this.records.add(c);
      this.records.removeIf(aLong -> aLong + 1000L < System.currentTimeMillis());
      this.fps = Math.max(this.records.size(), 4);
   }

   public int getFps() {
      return this.fps;
   }
}
