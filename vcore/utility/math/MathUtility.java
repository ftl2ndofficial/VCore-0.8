package vcore.utility.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.features.modules.Module;

public final class MathUtility {
   public static double random(double min, double max) {
      return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
   }

   public static float random(float min, float max) {
      return (float)(Math.random() * (max - min) + min);
   }

   public static double getDistanceSq(double x, double y, double z) {
      double d0 = Module.mc.player.method_23317() - x;
      double d1 = Module.mc.player.method_23318() - y;
      double d2 = Module.mc.player.method_23321() - z;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double d0 = x1 - x2;
      double d1 = y1 - y2;
      double d2 = z1 - z2;
      return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
   }

   public static double getSqrDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double d0 = x1 - x2;
      double d1 = y1 - y2;
      double d2 = z1 - z2;
      return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
   }

   public static float round(float value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return bd.floatValue();
   }

   public static double getDistanceSq(Entity ent) {
      return getDistanceSq(ent.getX(), ent.getY(), ent.getZ());
   }

   public static double roundToDecimal(double n, int point) {
      if (point == 0) {
         return Math.floor(n);
      }

      double factor = Math.pow(10.0, point);
      return Math.round(n * factor) / factor;
   }

   public static double angle(Vec3d vec3d, Vec3d other) {
      double lengthSq = vec3d.length() * other.length();
      if (lengthSq < 1.0E-4) {
         return 0.0;
      } else {
         double dot = vec3d.dotProduct(other);
         double arg = dot / lengthSq;
         if (arg > 1.0) {
            return 0.0;
         } else {
            return arg < -1.0 ? 180.0 : Math.acos(arg) * 180.0 / Math.PI;
         }
      }
   }

   public static Vec3d fromTo(Vec3d from, double x, double y, double z) {
      return fromTo(from.x, from.y, from.z, x, y, z);
   }

   public static float lerp(float f, float st, float en) {
      return st + f * (en - st);
   }

   public static Vec3d fromTo(double x, double y, double z, double x2, double y2, double z2) {
      return new Vec3d(x2 - x, y2 - y, z2 - z);
   }

   public static float rad(float angle) {
      return (float)(angle * Math.PI / 180.0);
   }

   public static int clamp(int num, int min, int max) {
      return num < min ? min : Math.min(num, max);
   }

   public static float clamp(float num, float min, float max) {
      return num < min ? min : Math.min(num, max);
   }

   public static double clamp(double num, double min, double max) {
      return num < min ? min : Math.min(num, max);
   }

   public static float sin(float value) {
      return MathHelper.sin(value);
   }

   public static float cos(float value) {
      return MathHelper.cos(value);
   }

   public static float wrapDegrees(float value) {
      return MathHelper.wrapDegrees(value);
   }

   public static double wrapDegrees(double value) {
      return MathHelper.wrapDegrees(value);
   }

   public static double square(double input) {
      return input * input;
   }

   public static double round(double value, int places) {
      BigDecimal bd = BigDecimal.valueOf(value);
      bd = bd.setScale(places, RoundingMode.HALF_UP);
      return bd.doubleValue();
   }

   public static float wrap(float angle) {
      float wrappedAngle = angle % 360.0F;
      if (wrappedAngle >= 180.0F) {
         wrappedAngle -= 360.0F;
      }

      if (wrappedAngle < -180.0F) {
         wrappedAngle += 360.0F;
      }

      return wrappedAngle;
   }

   public static Vec3d direction(float yaw) {
      return new Vec3d(Math.cos(degToRad(yaw + 90.0F)), 0.0, Math.sin(degToRad(yaw + 90.0F)));
   }

   public static float round(float value, int places) {
      if (places < 0) {
         throw new IllegalArgumentException();
      }

      BigDecimal bd = BigDecimal.valueOf(value);
      bd = bd.setScale(places, RoundingMode.FLOOR);
      return bd.floatValue();
   }

   public static float round2(double value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return bd.floatValue();
   }

   public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map, boolean descending) {
      List<Entry<K, V>> list = new LinkedList<>(map.entrySet());
      if (descending) {
         list.sort(Entry.comparingByValue(Comparator.reverseOrder()));
      } else {
         list.sort(Entry.comparingByValue());
      }

      LinkedHashMap<K, V> result = new LinkedHashMap<>();

      for (Entry<K, V> entry : list) {
         result.put(entry.getKey(), entry.getValue());
      }

      return result;
   }

   public static double degToRad(double deg) {
      return deg * (float) (Math.PI / 180.0);
   }
}
