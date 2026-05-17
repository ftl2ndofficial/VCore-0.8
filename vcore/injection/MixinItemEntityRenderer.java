package vcore.injection;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.ItemPhysics;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRender(ItemEntity entity, float f, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      ItemPhysics itemPhysics = ModuleManager.itemPhysics;
      if (itemPhysics != null && itemPhysics.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ItemStack stack = entity.getStack();
         BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, null, entity.method_5628());
         List<BakedQuad> quads = model.getQuads(null, null, mc.world.field_9229);
         itemPhysics.onRenderItemEntity(entity, matrices, vertexConsumers, light, tickDelta, quads);
         ci.cancel();
      }
   }
}
