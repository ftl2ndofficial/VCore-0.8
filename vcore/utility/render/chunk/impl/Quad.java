package vcore.utility.render.chunk.impl;

public class Quad {
   public static float easeIn(float t, float b, float c, float d) {
      float var4;
      return c * (var4 = t / d) * var4 + b;
   }

   public static float easeOut(float t, float b, float c, float d) {
      float var4;
      return -c * (var4 = t / d) * (var4 - 2.0F) + b;
   }

   public static float easeInOut(float t, float b, float c, float d) {
      t /= d / 2.0F;
      if (t < 1.0F) {
         return c / 2.0F * t * t + b;
      }

      t--;
      return -c / 2.0F * (t * (t - 2.0F) - 1.0F) + b;
   }
}
