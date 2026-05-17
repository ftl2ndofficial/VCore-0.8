package vcore.utility.render.animation;

import net.minecraft.util.math.MathHelper;
import vcore.utility.render.Render3DEngine;

public class EaseOutCirc {
   private final int maxTicks;
   private double value;
   private double dstValue;
   private int prevStep;
   private int step;

   public EaseOutCirc(int maxTicks) {
      this.maxTicks = maxTicks;
   }

   public EaseOutCirc() {
      this(5);
   }

   public void update() {
      this.prevStep = this.step;
      this.step = MathHelper.clamp(this.step + 1, 0, this.maxTicks);
   }

   public static double createAnimation(double value) {
      return Math.sqrt(1.0 - Math.pow(value - 1.0, 2.0));
   }

   public void setValue(double value) {
      if (value != this.dstValue) {
         this.prevStep = 0;
         this.step = 0;
         this.value = this.dstValue;
         this.dstValue = value;
      }
   }

   public double getAnimationD() {
      double delta = this.dstValue - this.value;
      double animation = createAnimation((double)(this.prevStep + (this.step - this.prevStep) * Render3DEngine.getTickDelta()) / this.maxTicks);
      return this.value + delta * animation;
   }
}
