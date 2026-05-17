package vcore.injection;

import java.util.List;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.render.Hat;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   private LivingEntity lastEntity;
   private float originalHeadYaw;
   private float originalPrevHeadYaw;
   private float originalPrevHeadPitch;
   private float originalHeadPitch;
   private float originalBodyYaw;
   private float originalPrevBodyYaw;
   @Shadow
   protected M field_4737;
   @Shadow
   @Final
   protected List<FeatureRenderer<T, M>> field_4738;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         boolean applyRenderRotations = this.shouldApplyRenderRotations(livingEntity);
         if (applyRenderRotations) {
            PlayerEntity player = Module.mc.player;
            IClientPlayerEntity playerAccessor = (IClientPlayerEntity)player;
            float tickDelta = Render3DEngine.getTickDelta();
            float interpolatedBodyYaw = Render2DEngine.interpolateFloat(Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, tickDelta);
            this.originalHeadYaw = livingEntity.headYaw;
            this.originalPrevHeadYaw = livingEntity.prevHeadYaw;
            this.originalPrevHeadPitch = livingEntity.field_6004;
            this.originalHeadPitch = livingEntity.method_36455();
            this.originalBodyYaw = livingEntity.bodyYaw;
            this.originalPrevBodyYaw = livingEntity.prevBodyYaw;
            livingEntity.method_36457(playerAccessor.getLastPitch());
            livingEntity.field_6004 = Managers.PLAYER.lastPitch;
            livingEntity.headYaw = playerAccessor.getLastYaw();
            livingEntity.bodyYaw = interpolatedBodyYaw;
            livingEntity.prevHeadYaw = Managers.PLAYER.lastYaw;
            livingEntity.prevBodyYaw = interpolatedBodyYaw;
         }

         if (livingEntity != Module.mc.player
            && ModuleManager.freeCam.isEnabled()
            && ModuleManager.freeCam.track.getValue()
            && ModuleManager.freeCam.trackEntity != null
            && ModuleManager.freeCam.trackEntity == livingEntity) {
            ci.cancel();
         } else {
            this.lastEntity = livingEntity;
         }
      }
   }

   @Unique
   public void postRender(T livingEntity) {
      if (!Module.fullNullCheck()) {
         if (this.shouldApplyRenderRotations(livingEntity)) {
            livingEntity.field_6004 = this.originalPrevHeadPitch;
            livingEntity.method_36457(this.originalHeadPitch);
            livingEntity.headYaw = this.originalHeadYaw;
            livingEntity.prevHeadYaw = this.originalPrevHeadYaw;
            livingEntity.bodyYaw = this.originalBodyYaw;
            livingEntity.prevBodyYaw = this.originalPrevBodyYaw;
         }
      }
   }

   @Unique
   private boolean shouldApplyRenderRotations(LivingEntity livingEntity) {
      if (Module.mc.player == null || livingEntity != Module.mc.player) {
         return false;
      } else if (!ClientSettings.renderRotations.getValue() || Vcore.isFuturePresent()) {
         return false;
      } else {
         return Module.mc.player.method_5765() ? false : this.isClientRotationSystemActive();
      }
   }

   @Unique
   private boolean isClientRotationSystemActive() {
      if (ModuleManager.aura.isEnabled() && Aura.target != null && ModuleManager.aura.rotationMode.not(Aura.Mode.None)) {
         return true;
      }

      IClientPlayerEntity playerAccessor = (IClientPlayerEntity)Module.mc.player;
      float yawDelta = Math.abs(MathHelper.wrapDegrees(playerAccessor.getLastYaw() - Managers.PLAYER.yaw));
      float pitchDelta = Math.abs(playerAccessor.getLastPitch() - Managers.PLAYER.pitch);
      return yawDelta > 0.5F || pitchDelta > 0.5F;
   }

   @Inject(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
         shift = Shift.AFTER
      )
   )
   public void onRenderModelAfter(
      T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci
   ) {
      if (!Module.fullNullCheck()) {
         if (livingEntity == Module.mc.player && ModuleManager.hat.isEnabled() && this.field_4737 instanceof PlayerEntityModel<?> playerModel) {
            matrixStack.push();
            playerModel.field_3398.rotate(matrixStack);
            float yOffset = Hat.getYOffset(livingEntity);
            matrixStack.translate(0.0F, yOffset, 0.0F);
            matrixStack.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180.0F));
            ModuleManager.hat.renderHat(matrixStack);
            matrixStack.pop();
         }
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         this.postRender(livingEntity);
      }
   }

   @ModifyArgs(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void renderHook(Args args) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.antiPlayerCollision.getValue()) {
            if (this.lastEntity != Module.mc.player && this.lastEntity instanceof PlayerEntity pl && !pl.method_5767()) {
               float alpha = MathUtility.clamp((float)(Module.mc.player.method_5707(this.lastEntity.method_19538()) / 3.0) + 0.2F, 0.0F, 1.0F);
               args.set(4, Render2DEngine.applyOpacity(654311423, alpha));
            }
         }
      }
   }
}
