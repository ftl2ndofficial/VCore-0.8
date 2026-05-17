package vcore.features.modules.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import vcore.features.modules.Module;

public class ItemPhysics extends Module {
   private static final float PIXEL_SIZE = 0.0625F;
   private static final float ROTATE_SPEED_Y = 0.5F;
   private static final float ROTATE_SPEED_X = 0.5F;
   private static final float STACK_GROUND_OFFSET = 0.015F;
   private static final float STACK_GROUND_XZ_OFFSET = 0.015F;
   private final Random random = new Random();
   private static final Map<UUID, Float> rotationMapY = new HashMap<>();
   private static final Map<UUID, Float> rotationMapX = new HashMap<>();
   private static final Map<UUID, Float> groundYawMap = new HashMap<>();

   public ItemPhysics() {
      super("ItemPhysics", "Realistic item physics.", Module.Category.RENDER);
   }

   public void onRenderItemEntity(
      ItemEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, List<BakedQuad> quads
   ) {
      if (quads != null) {
         ItemPhysics.ModelInfo info = this.getInfo(quads);
         matrices.push();
         this.offsetInFluidOrBlock(matrices, entity, info);
         boolean isFlat = info.flat;
         boolean isBlock = !isFlat;
         MinecraftClient mc = MinecraftClient.getInstance();
         ItemStack stack = entity.getStack();
         BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, null, entity.method_5628());
         ItemPhysics.RotationPair rotation = this.getRotationPair(entity, tickDelta, isBlock);
         double centerOffsetX = (entity.method_5829().maxX + entity.method_5829().minX) / 2.0 - entity.method_23317();
         double centerOffsetY = (entity.method_5829().maxY + entity.method_5829().minY) / 2.0 - entity.method_23318();
         double centerOffsetZ = (entity.method_5829().maxZ + entity.method_5829().minZ) / 2.0 - entity.method_23321();
         matrices.translate(centerOffsetX, centerOffsetY, centerOffsetZ);
         if (entity.method_24828() && !entity.method_5799() && !entity.method_5771()) {
            if (isFlat) {
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
               float groundYaw = groundYawMap.getOrDefault(entity.method_5667(), 0.0F);
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(groundYaw));
            } else {
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.0F));
            }
         } else if (isFlat) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            if (!entity.method_5799() && !entity.method_5771()) {
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.rotZ));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.rotY));
            }
         } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation.rotX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.rotY));
         }

         matrices.translate(-centerOffsetX, -centerOffsetY, -centerOffsetZ);
         int count = stack.getCount();
         int renderedAmount = count > 48 ? 5 : (count > 32 ? 4 : (count > 16 ? 3 : (count > 1 ? 2 : 1)));

         for (int j = 0; j < renderedAmount; j++) {
            matrices.push();
            this.random.setSeed(entity.method_5667().getMostSignificantBits() + j);
            if ((entity.method_24828() && !entity.method_5799() && !entity.method_5771() || entity.method_5799() || entity.method_5771()) && j > 0) {
               float x = (this.random.nextFloat() * 2.0F - 1.0F) * 0.015F;
               float z = (this.random.nextFloat() * 2.0F - 1.0F) * 0.015F;
               float y = j * 0.015F;
               matrices.translate(x, y, z);
            }

            mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, false, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV, model);
            matrices.pop();
         }

         matrices.pop();
      }
   }

   private ItemPhysics.RotationPair getRotationPair(ItemEntity entity, float tickDelta, boolean isBlock) {
      UUID id = entity.method_5667();
      float rotY = rotationMapY.getOrDefault(id, 0.0F);
      float rotX = rotationMapX.getOrDefault(id, 0.0F);
      float rotZ = rotY;
      boolean inWater = entity.method_5799() || entity.method_5771();
      boolean onGround = entity.method_24828();
      if (!inWater && onGround) {
         groundYawMap.computeIfAbsent(id, uuid -> {
            Random r = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
            return r.nextFloat() * 360.0F;
         });
         float groundYaw = groundYawMap.get(id);
         rotationMapY.put(id, 0.0F);
         rotationMapX.put(id, 0.0F);
         return new ItemPhysics.RotationPair(groundYaw, 0.0F, groundYaw);
      } else if (inWater && !isBlock) {
         rotationMapY.put(id, 0.0F);
         rotationMapX.put(id, 0.0F);
         return new ItemPhysics.RotationPair(0.0F, 0.0F, 0.0F);
      } else {
         float speedY = this.getRotateByY(tickDelta);
         float speedX = this.getRotateByX(tickDelta);
         rotY += speedY * 2.0F;
         rotX += speedX * 2.3F;
         rotZ += speedY * 2.0F;
         rotationMapY.put(id, rotY);
         rotationMapX.put(id, rotX);
         return new ItemPhysics.RotationPair(rotY, rotX, rotZ);
      }
   }

   private float getRotateByY(float tickDelta) {
      return tickDelta * 3.0F * 0.5F;
   }

   private float getRotateByX(float tickDelta) {
      return tickDelta * 3.0F * 0.5F;
   }

   private void offsetInFluidOrBlock(MatrixStack matrices, ItemEntity entity, ItemPhysics.ModelInfo info) {
      if (entity.method_5799()) {
         if (!info.flat) {
            matrices.translate(0.0F, 0.025F, 0.0F);
         }
      } else if (info.flat && !entity.method_24828()) {
         matrices.translate(0.0F, info.offsetY, 0.0F);
      }
   }

   private ItemPhysics.ModelInfo getInfo(List<BakedQuad> quads) {
      float minX = Float.MAX_VALUE;
      float maxX = Float.MIN_VALUE;
      float minY = Float.MAX_VALUE;
      float maxY = Float.MIN_VALUE;
      float minZ = Float.MAX_VALUE;
      float maxZ = Float.MIN_VALUE;

      for (BakedQuad _quad : quads) {
         for (int i = 0; i < 4; i++) {
            Direction face = _quad.getFace();
            float vx = Float.intBitsToFloat(_quad.getVertexData()[i * 8]);
            float vy = Float.intBitsToFloat(_quad.getVertexData()[i * 8 + 1]);
            float vz = Float.intBitsToFloat(_quad.getVertexData()[i * 8 + 2]);
            switch (face) {
               case DOWN:
                  minY = Math.min(minY, vy);
                  break;
               case UP:
                  maxY = Math.max(maxY, vy);
                  break;
               case NORTH:
                  minZ = Math.min(minZ, vz);
                  break;
               case SOUTH:
                  maxZ = Math.max(maxZ, vz);
                  break;
               case WEST:
                  minX = Math.min(minX, vx);
                  break;
               case EAST:
                  maxX = Math.max(maxX, vx);
            }
         }
      }

      if (minX == Float.MAX_VALUE) {
         minX = 0.0F;
      }

      if (minY == Float.MAX_VALUE) {
         minY = 0.0F;
      }

      if (minZ == Float.MAX_VALUE) {
         minZ = 0.0F;
      }

      if (maxX == Float.MIN_VALUE) {
         maxX = 1.0F;
      }

      if (maxY == Float.MIN_VALUE) {
         maxY = 1.0F;
      }

      if (maxZ == Float.MIN_VALUE) {
         maxZ = 1.0F;
      }

      float x = maxX - minX;
      float y = maxY - minY;
      float z = maxZ - minZ;
      boolean flat = x > 0.0625F && y > 0.0625F && z <= 0.0625F;
      float offsetY = flat ? 0.5F - minY : 0.0F;
      return new ItemPhysics.ModelInfo(flat, offsetY, minZ - minY);
   }

   private record ModelInfo(boolean flat, float offsetY, float offsetZ) {
   }

   private record RotationPair(float rotY, float rotX, float rotZ) {
   }
}
