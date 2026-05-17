package vcore.utility.render.animation;

import vcore.utility.math.FrameRateCounter;
import vcore.utility.math.MathUtility;

public class AnimationUtility {
   public static float deltaTime() {
      return FrameRateCounter.INSTANCE.getFps() > 5 ? 1.0F / FrameRateCounter.INSTANCE.getFps() : 0.016F;
   }

   public static float fast(float end, float start, float multiple) {
      float clampedDelta = MathUtility.clamp(deltaTime() * multiple, 0.0F, 1.0F);
      return (1.0F - clampedDelta) * end + clampedDelta * start;
   }
}
