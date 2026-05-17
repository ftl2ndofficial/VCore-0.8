package vcore.injection;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
   @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
   private void renderLabelIfPresent(
      T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo info
   ) {
      if (entity instanceof ArmorStandEntity && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noArmorStands.getValue()) {
         info.cancel();
      }

      if (entity instanceof LivingEntity living && ModuleManager.esp.isEnabled() && ModuleManager.esp.shouldHideVanillaNameTag(living)) {
         info.cancel();
      }
   }
}
