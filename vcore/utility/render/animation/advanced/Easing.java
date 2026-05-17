package vcore.utility.render.animation.advanced;

import java.util.function.DoubleUnaryOperator;

public enum Easing {
   LINEAR(x -> x),
   BOTH_SINE(x -> -(Math.cos(Math.PI * x) - 1.0) / 2.0),
   SINE_IN_OUT(x -> -(Math.cos(Math.PI * x) - 1.0) / 2.0),
   BOTH_CIRC(x -> x < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * x, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * x + 2.0, 2.0)) + 1.0) / 2.0),
   BOTH_CUBIC(x -> x < 0.5 ? 4.0 * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 3.0) / 2.0),
   EASE_IN_OUT_QUART(x -> x < 0.5 ? 8.0 * Math.pow(x, 4.0) : 1.0 - Math.pow(-2.0 * x + 2.0, 4.0) / 2.0),
   EASE_OUT_QUAD(x -> 1.0 - Math.pow(1.0 - x, 2.0)),
   EASE_OUT_BACK(x -> 1.0 + 2.70158 * Math.pow(x - 1.0, 3.0) + 1.70158 * Math.pow(x - 1.0, 2.0)),
   TARGETESP_EASE_OUT_BACK(x -> 1.0 + 3.70158 * Math.pow(x - 1.0, 3.0) + 2.70158 * Math.pow(x - 1.0, 2.0)),
   EASE_OUT_CIRC(x -> Math.sqrt(1.0 - Math.pow(x - 1.0, 2.0))),
   SMOOTH_STEP(x -> -2.0 * Math.pow(x, 3.0) + 3.0 * Math.pow(x, 2.0));

   private final DoubleUnaryOperator function;

   Easing(DoubleUnaryOperator function) {
      this.function = function;
   }

   public double apply(double arg) {
      return this.function.applyAsDouble(arg);
   }
}
