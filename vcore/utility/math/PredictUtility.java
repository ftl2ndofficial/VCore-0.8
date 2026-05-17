package vcore.utility.math;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import vcore.features.modules.Module;

public class PredictUtility {
   public static PlayerEntity movePlayer(PlayerEntity entity, Vec3d newPos) {
      return entity != null && newPos != null ? equipAndReturn(entity, newPos) : null;
   }

   public static PlayerEntity predictPlayer(PlayerEntity entity, int ticks) {
      Vec3d posVec = predictPosition(entity, ticks);
      return posVec == null ? null : equipAndReturn(entity, posVec);
   }

   public static Vec3d predictPosition(PlayerEntity entity, int ticks) {
      if (entity == null) {
         return null;
      }

      Vec3d posVec = new Vec3d(entity.method_23317(), entity.method_23318(), entity.method_23321());
      double motionX = entity.method_18798().method_10216();
      double motionZ = entity.method_18798().method_10215();

      for (int i = 0; i < ticks; i++) {
         float hbDeltaX = motionX > 0.0 ? 0.3F : -0.3F;
         float hbDeltaZ = motionZ > 0.0 ? 0.3F : -0.3F;
         if (!Module.mc.world.method_22347(BlockPos.ofFloored(posVec.add(motionX + hbDeltaX, 0.1, motionZ + hbDeltaZ)))
            || !Module.mc.world.method_22347(BlockPos.ofFloored(posVec.add(motionX + hbDeltaX, 1.0, motionZ + hbDeltaZ)))) {
            motionX = 0.0;
            motionZ = 0.0;
         }

         posVec = posVec.add(motionX, 0.0, motionZ);
      }

      return posVec;
   }

   public static Box predictBox(PlayerEntity entity, int ticks) {
      Vec3d posVec = predictPosition(entity, ticks);
      return posVec == null ? null : createBox(posVec, entity);
   }

   public static PlayerEntity equipAndReturn(PlayerEntity original, Vec3d posVec) {
      PlayerEntity copyEntity = new PlayerEntity(
         Module.mc.world,
         original.method_24515(),
         original.method_36454(),
         new GameProfile(UUID.fromString("66123666-1234-5432-6666-667563866600"), "PredictEntity339")
      ) {
         public boolean method_7325() {
            return false;
         }

         public boolean method_7337() {
            return false;
         }
      };
      copyEntity.method_33574(posVec);
      copyEntity.method_6033(original.method_6032());
      copyEntity.field_6014 = original.field_6014;
      copyEntity.field_5969 = original.field_5969;
      copyEntity.field_6036 = original.field_6036;
      copyEntity.getInventory().clone(original.getInventory());

      for (StatusEffectInstance se : original.method_6026()) {
         copyEntity.method_6092(se);
      }

      return copyEntity;
   }

   public static Box createBox(Vec3d vec, Entity entity) {
      return entity.method_5829().offset(entity.getPos().relativize(vec));
   }
}
