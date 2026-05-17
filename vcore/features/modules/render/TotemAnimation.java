package vcore.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class TotemAnimation extends Module {
   private final Setting<TotemAnimation.Mode> mode = new Setting<>("Mode", TotemAnimation.Mode.FadeOut);
   private final Setting<Integer> speed = new Setting<>("Speed", 40, 1, 100);
   private ItemStack floatingItem = null;
   private int floatingItemTimeLeft;

   public TotemAnimation() {
      super("TotemAnimation", "Custom totem animations.", Module.Category.RENDER);
   }

   public void showFloatingItem(ItemStack floatingItem) {
      this.floatingItem = floatingItem;
      this.floatingItemTimeLeft = this.getTime();
   }

   @Override
   public void onUpdate() {
      if (this.floatingItemTimeLeft > 0) {
         this.floatingItemTimeLeft--;
         if (this.floatingItemTimeLeft == 0) {
            this.floatingItem = null;
         }
      }
   }

   public void renderFloatingItem(float tickDelta) {
      if (this.floatingItem != null && this.floatingItemTimeLeft > 0 && !this.mode.is(TotemAnimation.Mode.Off)) {
         int scaledWidth = mc.getWindow().getScaledWidth();
         int scaledHeight = mc.getWindow().getScaledHeight();
         int elapsedTime = this.getTime() - this.floatingItemTimeLeft;
         float animationProgress = (elapsedTime + tickDelta) / this.getTime();
         float progressSquared = animationProgress * animationProgress;
         float progressCubed = animationProgress * progressSquared;
         float oscillationFactor = 10.25F * progressCubed * progressSquared
            - 24.95F * progressSquared * progressSquared
            + 25.5F * progressCubed
            - 13.8F * progressSquared
            + 4.0F * animationProgress;
         float oscillationRadians = oscillationFactor * (float) Math.PI;
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.push();
         float adjustedProgress = elapsedTime + tickDelta;
         float scale = 50.0F + 175.0F * MathHelper.sin(oscillationRadians);
         switch ((TotemAnimation.Mode)this.mode.getValue()) {
            case FadeOut:
               float x2 = (float)(Math.sin(adjustedProgress * 112.0F / 180.0F) * 100.0);
               float y2 = (float)(Math.cos(adjustedProgress * 112.0F / 180.0F) * 50.0);
               matrixStack.translate(scaledWidth / 2 + x2, scaledHeight / 2 + y2, -50.0F);
               matrixStack.scale(scale, -scale, scale);
               break;
            case Size:
               matrixStack.translate(scaledWidth / 2, scaledHeight / 2, -50.0F);
               matrixStack.scale(scale, -scale, scale);
               break;
            case Otkisuli:
               matrixStack.translate(scaledWidth / 2, scaledHeight / 2, -50.0F);
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(adjustedProgress * 2.0F));
               matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(adjustedProgress * 2.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            case Insert:
               matrixStack.translate(scaledWidth / 2, scaledHeight / 2, -50.0F);
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(adjustedProgress * 3.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            case Fall: {
               float downFactor = (float)(Math.pow(adjustedProgress, 3.0) * 0.2F);
               matrixStack.translate(scaledWidth / 2, scaledHeight / 2 + downFactor, -50.0F);
               matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(adjustedProgress * 5.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            }
            case Rocket: {
               float downFactor = (float)(Math.pow(adjustedProgress, 3.0) * 0.2F) - 20.0F;
               matrixStack.translate(scaledWidth / 2, scaledHeight / 2 - downFactor, -50.0F);
               matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(adjustedProgress * this.floatingItemTimeLeft * 2.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
               break;
            }
            case Roll:
               float rightFactor = (float)(Math.pow(adjustedProgress, 2.0) * 4.5);
               matrixStack.translate(scaledWidth / 2 + rightFactor, scaledHeight / 2, -50.0F);
               matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(adjustedProgress * 40.0F));
               matrixStack.scale(200.0F - adjustedProgress * 1.5F, -200.0F + adjustedProgress * 1.5F, 200.0F - adjustedProgress * 1.5F);
         }

         Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F - animationProgress);
         mc.getItemRenderer()
            .renderItem(this.floatingItem, ModelTransformationMode.FIXED, 15728880, OverlayTexture.DEFAULT_UV, matrixStack, immediate, mc.world, 0);
         matrixStack.pop();
         immediate.draw();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.disableDepthTest();
      }
   }

   private int getTime() {
      int invertedSpeed = 101 - this.speed.getValue();
      if (this.mode.is(TotemAnimation.Mode.FadeOut)) {
         return invertedSpeed / 4;
      } else {
         return this.mode.is(TotemAnimation.Mode.Insert) ? invertedSpeed / 2 : invertedSpeed;
      }
   }

   private enum Mode {
      FadeOut,
      Size,
      Otkisuli,
      Insert,
      Fall,
      Rocket,
      Roll,
      Off;
   }
}
