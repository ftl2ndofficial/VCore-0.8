package vcore.injection;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventHeldItemRenderer;
import vcore.features.modules.Module;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {
   @Inject(
      method = "renderFirstPersonItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )
   )
   private void onRenderItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      if (!Module.fullNullCheck()) {
         EventHeldItemRenderer event = new EventHeldItemRenderer(hand, item, equipProgress, matrices);
         Vcore.EVENT_BUS.post(event);
      }
   }

   @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
   private void onRenderItemPost(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
   }

   @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
   private void onRenderItemHook(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      if (Managers.MODULE != null && ModuleManager.animations.shouldAnimate(item) && !item.isEmpty() && !(item.getItem() instanceof FilledMapItem)) {
         ci.cancel();
         ModuleManager.animations
            .renderFirstPersonItemCustom(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
      }
   }

   private void applyEatOrDrinkTransformationCustom(MatrixStack matrices, float tickDelta, Arm arm, @NotNull ItemStack stack) {
      float f = Module.mc.player.method_6014() - tickDelta + 1.0F;
      float g = f / stack.getMaxUseTime(Module.mc.player);
      if (g < 0.8F) {
         float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.005F);
         matrices.translate(0.0F, h, 0.0F);
      }

      float h = 1.0F - (float)Math.pow(g, 27.0);
      int i = arm == Arm.RIGHT ? 1 : -1;
      matrices.translate(h * 0.6F * i * ModuleManager.viewModel.getEatXFactor(), h * -0.5F * ModuleManager.viewModel.eatY.getValue(), h * 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * h * 90.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * h * 30.0F));
   }

   @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true)
   private void applyEatOrDrinkTransformationHook(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
      if (ModuleManager.animations.isEnabled()) {
         this.applyEatOrDrinkTransformationCustom(matrices, tickDelta, arm, stack);
         ci.cancel();
      }
   }
}
