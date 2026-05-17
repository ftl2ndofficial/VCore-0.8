package vcore.utility.player;

import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.utility.world.ExplosionUtility;

public final class PlayerUtility {
   public static boolean isInHell() {
      return Module.mc.world == null ? false : Objects.equals(Module.mc.world.method_27983().getValue().getPath(), "the_nether");
   }

   public static boolean isInEnd() {
      return Module.mc.world == null ? false : Objects.equals(Module.mc.world.method_27983().getValue().getPath(), "the_end");
   }

   public static boolean isInOver() {
      return Module.mc.world == null ? false : Objects.equals(Module.mc.world.method_27983().getValue().getPath(), "overworld");
   }

   public static boolean isEating() {
      return Module.mc.player == null
         ? false
         : (
               Module.mc.player.method_6047().method_57353().contains(DataComponentTypes.FOOD)
                  || Module.mc.player.method_6079().method_57353().contains(DataComponentTypes.FOOD)
            )
            && Module.mc.player.method_6115();
   }

   public static boolean isMining() {
      return Module.mc.interactionManager == null ? false : Module.mc.interactionManager.isBreakingBlock();
   }

   public static float squaredDistanceFromEyes(@NotNull Vec3d targetPos) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double dx = targetPos.x - Module.mc.player.method_23317();
      double dy = targetPos.y - (Module.mc.player.method_23318() + Module.mc.player.method_18381(Module.mc.player.method_18376()));
      double dz = targetPos.z - Module.mc.player.method_23321();
      return (float)(dx * dx + dy * dy + dz * dz);
   }

   public static float squaredDistance2d(@NotNull Vec2f point) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double d = Module.mc.player.method_23317() - point.x;
      double f = Module.mc.player.method_23321() - point.y;
      return (float)(d * d + f * f);
   }

   public static ClientPlayerEntity getPlayer() {
      return Module.mc.player;
   }

   public static float calculatePercentage(@NotNull ItemStack stack) {
      float durability = stack.getMaxDamage() - stack.getDamage();
      return durability / stack.getMaxDamage() * 100.0F;
   }

   public static float fixAngle(float angle) {
      return Math.round(angle / (float)(getGCD() * 0.15)) * (float)(getGCD() * 0.15);
   }

   public static float getGCD() {
      double sensitivity = (Double)Module.mc.options.getMouseSensitivity().getValue();
      double value = sensitivity * 0.6 + 0.2;
      double result = Math.pow(value, 3.0) * 8.0;
      return (float)result;
   }

   public static float squaredDistance2d(double x, double z) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double d = Module.mc.player.method_23317() - x;
      double f = Module.mc.player.method_23321() - z;
      return (float)(d * d + f * f);
   }

   public static float getSquaredDistance2D(Vec3d vec) {
      double d0 = Module.mc.player.method_23317() - vec.method_10216();
      double d2 = Module.mc.player.method_23321() - vec.method_10215();
      return (float)(d0 * d0 + d2 * d2);
   }

   public static boolean canSee(Vec3d pos) {
      Vec3d vec3d = new Vec3d(Module.mc.player.method_23317(), Module.mc.player.method_23320(), Module.mc.player.method_23321());
      return pos.distanceTo(vec3d) > 128.0 ? false : ExplosionUtility.raycast(vec3d, pos, false) == Type.MISS;
   }

   public static boolean isFalling() {
      return Module.mc.player == null ? false : !Module.mc.player.method_24828() && !Module.mc.player.method_7337() && Module.mc.player.method_18798().y < 0.0;
   }
}
