package vcore.utility.world;

import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.Explosion.DestructionType;
import org.apache.commons.lang3.mutable.MutableInt;
import vcore.features.modules.Module;
import vcore.injection.accesors.IExplosion;
import vcore.utility.math.PredictUtility;

public final class ExplosionUtility {
   public static boolean terrainIgnore = false;
   public static Explosion explosion;
   private static final boolean IGNORE_TERRAIN = false;
   private static final boolean ASSUME_BEST_ARMOR = false;

   public static float getAutoCrystalDamage(Vec3d crystalPos, PlayerEntity target, int predictTicks, boolean optimized) {
      return predictTicks == 0
         ? getExplosionDamage(crystalPos, target, optimized)
         : getExplosionDamageWPredict(crystalPos, target, PredictUtility.predictBox(target, predictTicks), optimized);
   }

   public static float getSelfExplosionDamage(Vec3d explosionPos, int predictTicks, boolean optimized) {
      return getAutoCrystalDamage(explosionPos, Module.mc.player, predictTicks, optimized);
   }

   public static float getExplosionDamage(Vec3d explosionPos, PlayerEntity target, boolean optimized) {
      if (Module.mc.world.method_8407() != Difficulty.PEACEFUL && target != null) {
         if (explosion == null) {
            explosion = new Explosion(Module.mc.world, Module.mc.player, 1.0, 33.0, 7.0, 6.0F, false, DestructionType.DESTROY);
         }

         ((IExplosion)explosion).setX(explosionPos.x);
         ((IExplosion)explosion).setY(explosionPos.y);
         ((IExplosion)explosion).setZ(explosionPos.z);
         if (((IExplosion)explosion).getWorld() != Module.mc.world) {
            ((IExplosion)explosion).setWorld(Module.mc.world);
         }

         if (!new Box(
               MathHelper.floor(explosionPos.x - 11.0),
               MathHelper.floor(explosionPos.y - 11.0),
               MathHelper.floor(explosionPos.z - 11.0),
               MathHelper.floor(explosionPos.x + 13.0),
               MathHelper.floor(explosionPos.y + 13.0),
               MathHelper.floor(explosionPos.z + 13.0)
            )
            .intersects(target.method_5829())) {
            return 0.0F;
         }

         if (!target.method_5659(explosion) && !target.method_5655()) {
            double distExposure = (float)target.method_5707(explosionPos) / 144.0;
            if (distExposure <= 1.0) {
               terrainIgnore = false;
               double exposure = getExposure(explosionPos, target.method_5829(), optimized);
               terrainIgnore = false;
               double finalExposure = (1.0 - distExposure) * exposure;
               float toDamage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0);
               if (Module.mc.world.method_8407() == Difficulty.EASY) {
                  toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
               } else if (Module.mc.world.method_8407() == Difficulty.HARD) {
                  toDamage = toDamage * 3.0F / 2.0F;
               }

               toDamage = DamageUtil.getDamageLeft(
                  target,
                  toDamage,
                  ((IExplosion)explosion).getDamageSource(),
                  target.method_6096(),
                  (float)target.method_5996(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue()
               );
               if (target.method_6059(StatusEffects.RESISTANCE)) {
                  int resistance = 25 - (target.method_6112(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
                  float resistance_1 = toDamage * resistance;
                  toDamage = Math.max(resistance_1 / 25.0F, 0.0F);
               }

               if (toDamage <= 0.0F) {
                  toDamage = 0.0F;
               } else {
                  float protAmount = getProtectionAmount(target.method_5661());
                  if (protAmount > 0.0F) {
                     toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
                  }
               }

               return toDamage;
            }
         }

         return 0.0F;
      } else {
         return 0.0F;
      }
   }

   public static float getExplosionDamageWPredict(Vec3d explosionPos, PlayerEntity target, Box predict, boolean optimized) {
      if (Module.mc.world.method_8407() == Difficulty.PEACEFUL) {
         return 0.0F;
      }

      if (target != null && predict != null) {
         if (explosion == null) {
            explosion = new Explosion(Module.mc.world, Module.mc.player, 1.0, 33.0, 7.0, 6.0F, false, DestructionType.DESTROY);
         }

         ((IExplosion)explosion).setX(explosionPos.x);
         ((IExplosion)explosion).setY(explosionPos.y);
         ((IExplosion)explosion).setZ(explosionPos.z);
         if (((IExplosion)explosion).getWorld() != Module.mc.world) {
            ((IExplosion)explosion).setWorld(Module.mc.world);
         }

         if (!new Box(
               MathHelper.floor(explosionPos.x - 11.0),
               MathHelper.floor(explosionPos.y - 11.0),
               MathHelper.floor(explosionPos.z - 11.0),
               MathHelper.floor(explosionPos.x + 13.0),
               MathHelper.floor(explosionPos.y + 13.0),
               MathHelper.floor(explosionPos.z + 13.0)
            )
            .intersects(predict)) {
            return 0.0F;
         }

         if (!target.method_5659(explosion) && !target.method_5655()) {
            double distExposure = predict.getCenter().add(0.0, -0.9, 0.0).squaredDistanceTo(explosionPos) / 144.0;
            if (distExposure <= 1.0) {
               terrainIgnore = false;
               double exposure = getExposure(explosionPos, predict, optimized);
               terrainIgnore = false;
               double finalExposure = (1.0 - distExposure) * exposure;
               float toDamage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0);
               if (Module.mc.world.method_8407() == Difficulty.EASY) {
                  toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
               } else if (Module.mc.world.method_8407() == Difficulty.HARD) {
                  toDamage = toDamage * 3.0F / 2.0F;
               }

               toDamage = DamageUtil.getDamageLeft(
                  target,
                  toDamage,
                  ((IExplosion)explosion).getDamageSource(),
                  target.method_6096(),
                  (float)Objects.requireNonNull(target.method_5996(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)).getValue()
               );
               if (target.method_6059(StatusEffects.RESISTANCE)) {
                  int resistance = 25 - (Objects.requireNonNull(target.method_6112(StatusEffects.RESISTANCE)).getAmplifier() + 1) * 5;
                  float resistance_1 = toDamage * resistance;
                  toDamage = Math.max(resistance_1 / 25.0F, 0.0F);
               }

               if (toDamage <= 0.0F) {
                  toDamage = 0.0F;
               } else {
                  float protAmount = getProtectionAmount(target.method_5661());
                  if (protAmount > 0.0F) {
                     toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
                  }
               }

               return toDamage;
            }
         }

         return 0.0F;
      } else {
         return 0.0F;
      }
   }

   public static BlockHitResult rayCastBlock(RaycastContext context, BlockPos block) {
      return (BlockHitResult)BlockView.raycast(
         context.getStart(),
         context.getEnd(),
         context,
         (raycastContext, blockPos) -> {
            BlockState blockState;
            if (!blockPos.equals(block)) {
               blockState = Blocks.AIR.getDefaultState();
            } else {
               blockState = Blocks.OBSIDIAN.getDefaultState();
            }

            Vec3d vec3d = raycastContext.getStart();
            Vec3d vec3d2 = raycastContext.getEnd();
            VoxelShape voxelShape = raycastContext.getBlockShape(blockState, Module.mc.world, blockPos);
            BlockHitResult blockHitResult = Module.mc.world.method_17745(vec3d, vec3d2, blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = VoxelShapes.empty();
            BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, blockPos);
            double d = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult.method_17784());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult2.method_17784());
            return d <= e ? blockHitResult : blockHitResult2;
         },
         raycastContext -> {
            Vec3d vec3d = raycastContext.getStart().subtract(raycastContext.getEnd());
            return BlockHitResult.createMissed(
               raycastContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(raycastContext.getEnd())
            );
         }
      );
   }

   public static float getDamageOfGhostBlock(Vec3d explosionPos, PlayerEntity target, BlockPos bp) {
      if (Module.mc.world.method_8407() == Difficulty.PEACEFUL) {
         return 0.0F;
      }

      if (explosion == null) {
         explosion = new Explosion(Module.mc.world, Module.mc.player, 1.0, 33.0, 7.0, 6.0F, false, DestructionType.DESTROY);
      }

      ((IExplosion)explosion).setX(explosionPos.x);
      ((IExplosion)explosion).setY(explosionPos.y);
      ((IExplosion)explosion).setZ(explosionPos.z);
      if (((IExplosion)explosion).getWorld() != Module.mc.world) {
         ((IExplosion)explosion).setWorld(Module.mc.world);
      }

      double maxDist = 12.0;
      if (!new Box(
            MathHelper.floor(explosionPos.x - maxDist - 1.0),
            MathHelper.floor(explosionPos.y - maxDist - 1.0),
            MathHelper.floor(explosionPos.z - maxDist - 1.0),
            MathHelper.floor(explosionPos.x + maxDist + 1.0),
            MathHelper.floor(explosionPos.y + maxDist + 1.0),
            MathHelper.floor(explosionPos.z + maxDist + 1.0)
         )
         .intersects(target.method_5829())) {
         return 0.0F;
      }

      if (!target.method_5659(explosion) && !target.method_5655()) {
         double distExposure = target.method_5707(explosionPos) / 144.0;
         if (distExposure <= 1.0) {
            terrainIgnore = false;
            double exposure = getExposureGhost(explosionPos, target, bp);
            terrainIgnore = false;
            double finalExposure = (1.0 - distExposure) * exposure;
            float toDamage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);
            if (Module.mc.world.method_8407() == Difficulty.EASY) {
               toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
            } else if (Module.mc.world.method_8407() == Difficulty.HARD) {
               toDamage = toDamage * 3.0F / 2.0F;
            }

            toDamage = DamageUtil.getDamageLeft(
               target,
               toDamage,
               ((IExplosion)explosion).getDamageSource(),
               target.method_6096(),
               (float)target.method_5996(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue()
            );
            if (target.method_6059(StatusEffects.RESISTANCE)) {
               int resistance = 25 - (target.method_6112(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
               float resistance_1 = toDamage * resistance;
               toDamage = Math.max(resistance_1 / 25.0F, 0.0F);
            }

            if (toDamage <= 0.0F) {
               toDamage = 0.0F;
            } else {
               float protAmount = getProtectionAmount(target.method_5661());
               if (protAmount > 0.0F) {
                  toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
               }
            }

            return toDamage;
         }
      }

      return 0.0F;
   }

   private static float getExposureGhost(Vec3d source, Entity entity, BlockPos pos) {
      Box box = entity.method_5829();
      double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
      double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
      double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
      double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
      double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;
      if (!(d < 0.0) && !(e < 0.0) && !(f < 0.0)) {
         int i = 0;
         int j = 0;

         for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
               for (double m = 0.0; m <= 1.0; m += f) {
                  double n = MathHelper.lerp(k, box.minX, box.maxX);
                  double o = MathHelper.lerp(l, box.minY, box.maxY);
                  double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                  Vec3d vec3d = new Vec3d(n + g, o, p + h);
                  if (raycastGhost(new RaycastContext(vec3d, source, ShapeType.COLLIDER, FluidHandling.NONE, entity), pos).method_17783() == Type.MISS) {
                     i++;
                  }

                  j++;
               }
            }
         }

         return (float)i / j;
      } else {
         return 0.0F;
      }
   }

   public static float getExposure(Vec3d source, Box box, boolean optimized) {
      if (!optimized) {
         return getExposure(source, box);
      }

      int miss = 0;
      int hit = 0;

      for (int k = 0; k <= 1; k++) {
         for (int l = 0; l <= 1; l++) {
            for (int m = 0; m <= 1; m++) {
               double n = MathHelper.lerp(k, box.minX, box.maxX);
               double o = MathHelper.lerp(l, box.minY, box.maxY);
               double p = MathHelper.lerp(m, box.minZ, box.maxZ);
               Vec3d vec3d = new Vec3d(n, o, p);
               if (raycast(vec3d, source, false) == Type.MISS) {
                  miss++;
               }

               hit++;
            }
         }
      }

      return (float)miss / hit;
   }

   public static float getExposure(Vec3d source, Box box) {
      double d = 0.4545454446934474;
      double e = 0.21739130885479366;
      double f = 0.4545454446934474;
      int i = 0;
      int j = 0;

      for (double k = 0.0; k <= 1.0; k += d) {
         for (double l = 0.0; l <= 1.0; l += e) {
            for (double m = 0.0; m <= 1.0; m += f) {
               double n = MathHelper.lerp(k, box.minX, box.maxX);
               double o = MathHelper.lerp(l, box.minY, box.maxY);
               double p = MathHelper.lerp(m, box.minZ, box.maxZ);
               Vec3d vec3d = new Vec3d(n + 0.045454555306552624, o, p + 0.045454555306552624);
               if (raycast(vec3d, source, false) == Type.MISS) {
                  i++;
               }

               j++;
            }
         }
      }

      return (float)i / j;
   }

   private static BlockHitResult raycastGhost(RaycastContext context, BlockPos bPos) {
      return (BlockHitResult)BlockView.raycast(context.getStart(), context.getEnd(), context, (innerContext, pos) -> {
         Vec3d vec3d = innerContext.getStart();
         Vec3d vec3d2 = innerContext.getEnd();
         BlockState blockState;
         if (!pos.equals(bPos)) {
            blockState = Module.mc.world.method_8320(bPos);
         } else {
            blockState = Blocks.OBSIDIAN.getDefaultState();
         }

         VoxelShape voxelShape = innerContext.getBlockShape(blockState, Module.mc.world, pos);
         BlockHitResult blockHitResult = Module.mc.world.method_17745(vec3d, vec3d2, pos, voxelShape, blockState);
         BlockHitResult blockHitResult2 = VoxelShapes.empty().raycast(vec3d, vec3d2, pos);
         double d = blockHitResult == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult.method_17784());
         double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult2.method_17784());
         return d <= e ? blockHitResult : blockHitResult2;
      }, innerContext -> {
         Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
         return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
      });
   }

   public static Type raycast(Vec3d start, Vec3d end, boolean ignoreTerrain) {
      return (Type)BlockView.raycast(start, end, null, (innerContext, blockPos) -> {
         BlockState blockState = Module.mc.world.method_8320(blockPos);
         if (blockState.method_26204().getBlastResistance() < 600.0F && ignoreTerrain) {
            return null;
         }

         BlockHitResult hitResult = blockState.method_26220(Module.mc.world, blockPos).raycast(start, end, blockPos);
         return hitResult == null ? null : hitResult.method_17783();
      }, innerContext -> Type.MISS);
   }

   public static int getProtectionAmount(Iterable<ItemStack> equipment) {
      MutableInt mutableInt = new MutableInt();
      equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
      return mutableInt.intValue();
   }

   public static int getProtectionAmount(ItemStack stack) {
      int modifierBlast = EnchantmentHelper.getLevel(
         (RegistryEntry)Module.mc.world.method_30349().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BLAST_PROTECTION).get(), stack
      );
      int modifier = EnchantmentHelper.getLevel(
         (RegistryEntry)Module.mc.world.method_30349().get(Enchantments.PROTECTION.getRegistryRef()).getEntry(Enchantments.PROTECTION).get(), stack
      );
      return modifierBlast * 2 + modifier;
   }
}
