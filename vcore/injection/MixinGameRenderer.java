package vcore.injection;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.movement.freelook.CameraOverriddenEntity;
import vcore.features.modules.movement.freelook.FreeLookState;
import vcore.features.modules.player.NoEntityTrace;
import vcore.utility.math.FrameRateCounter;
import vcore.utility.render.BlockAnimationUtility;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.shaders.satin.impl.ReloadableShaderEffectManager;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
   @Shadow
   private float field_4005;
   @Shadow
   private float field_3988;
   @Shadow
   private float field_4004;
   @Shadow
   private float field_4025;
   @Unique
   private boolean vcore$renderingHand;

   @Shadow
   public abstract void method_3182();

   @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 1, shift = Shift.BEFORE), method = "render")
   void postHudRenderHook(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
      FrameRateCounter.INSTANCE.recordFrame();
   }

   @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = 180, ordinal = 0), method = "renderWorld")
   void render3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         Camera camera = Module.mc.gameRenderer.getCamera();
         MatrixStack matrixStack = new MatrixStack();
         RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         RenderSystem.applyModelViewMatrix();
         Render3DEngine.lastProjMat.set(RenderSystem.getProjectionMatrix());
         Render3DEngine.lastModMat.set(RenderSystem.getModelViewMatrix());
         Render3DEngine.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
         Managers.MODULE.onRender3D(matrixStack);
         BlockAnimationUtility.onRender(matrixStack);
         Render3DEngine.onRender3D(matrixStack);
         RenderSystem.getModelViewStack().popMatrix();
         RenderSystem.applyModelViewMatrix();
      }
   }

   @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
   private float renderWorldHook(float delta, float first, float second) {
      return ModuleManager.noRender.isEnabled() && ModuleManager.noRender.nausea.getValue() ? 0.0F : MathHelper.lerp(delta, first, second);
   }

   @Inject(method = "loadPrograms", at = @At("RETURN"))
   private void loadSatinPrograms(ResourceFactory factory, CallbackInfo ci) {
      ReloadableShaderEffectManager.INSTANCE.reload(factory);
   }

   @Inject(
      method = "updateCrosshairTarget",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"
      ),
      cancellable = true
   )
   private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.freeCam.isEnabled()) {
            Module.mc.getProfiler().pop();
            info.cancel();
            Module.mc.crosshairTarget = Managers.PLAYER
               .getRtxTarget(
                  ModuleManager.freeCam.getFakeYaw(),
                  ModuleManager.freeCam.getFakePitch(),
                  ModuleManager.freeCam.getFakeX(),
                  ModuleManager.freeCam.getFakeY(),
                  ModuleManager.freeCam.getFakeZ()
               );
         }
      }
   }

   @Inject(method = "findCrosshairTarget", at = @At("HEAD"), cancellable = true)
   private void findCrosshairTargetHook(
      Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta, CallbackInfoReturnable<HitResult> cir
   ) {
      boolean disableEntityTrace = this.shouldDisableEntityTrace();
      if (ModuleManager.autoFarm.isAutoFarmFreeLookActive()) {
         cir.setReturnValue(
            this.findRotationCrosshairTarget(
               camera,
               blockInteractionRange,
               entityInteractionRange,
               tickDelta,
               ModuleManager.autoFarm.getSilentRotationYaw(),
               ModuleManager.autoFarm.getSilentRotationPitch(),
               disableEntityTrace
            )
         );
      } else if (FreeLookState.active && camera instanceof CameraOverriddenEntity freeLookCamera) {
         cir.setReturnValue(
            this.findRotationCrosshairTarget(
               camera,
               blockInteractionRange,
               entityInteractionRange,
               tickDelta,
               freeLookCamera.getCameraYaw(),
               freeLookCamera.getCameraPitch(),
               disableEntityTrace
            )
         );
      } else if (disableEntityTrace) {
         double d = Math.max(blockInteractionRange, entityInteractionRange);
         Vec3d vec3d = camera.getCameraPosVec(tickDelta);
         HitResult hitResult = camera.raycast(d, tickDelta, false);
         cir.setReturnValue(this.ensureTargetInRangeCustom(hitResult, vec3d, blockInteractionRange));
      }
   }

   @Unique
   private boolean shouldDisableEntityTrace() {
      if (ModuleManager.noEntityTrace.isEnabled() && Module.mc.player != null) {
         Item mainItem = Module.mc.player.method_6047().getItem();
         return mainItem == Items.COBWEB && NoEntityTrace.cobweb.getValue()
            || mainItem instanceof PickaxeItem && NoEntityTrace.pickaxe.getValue()
            || mainItem instanceof AxeItem && NoEntityTrace.axe.getValue();
      } else {
         return false;
      }
   }

   @Unique
   private HitResult findRotationCrosshairTarget(
      Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta, float yaw, float pitch, boolean disableEntityTrace
   ) {
      double maxRange = Math.max(blockInteractionRange, entityInteractionRange);
      Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
      Vec3d rotation = this.rotationVector(yaw, pitch);
      Vec3d blockEnd = cameraPos.add(rotation.multiply(maxRange));
      HitResult blockHit = Module.mc.world.method_17742(new RaycastContext(cameraPos, blockEnd, ShapeType.OUTLINE, FluidHandling.NONE, camera));
      HitResult target = this.ensureTargetInRangeCustom(blockHit, cameraPos, blockInteractionRange);
      if (disableEntityTrace) {
         return target;
      }

      double entityDistanceSq = entityInteractionRange * entityInteractionRange;
      if (target != null && target.getType() != Type.MISS) {
         entityDistanceSq = Math.min(entityDistanceSq, cameraPos.squaredDistanceTo(target.getPos()));
      }

      Vec3d entityEnd = cameraPos.add(rotation.multiply(entityInteractionRange));
      Box box = camera.method_5829().stretch(rotation.multiply(entityInteractionRange)).expand(1.0, 1.0, 1.0);
      EntityHitResult entityHit = ProjectileUtil.raycast(
         camera, cameraPos, entityEnd, box, entity -> !entity.isSpectator() && entity.canHit(), entityDistanceSq
      );
      return (HitResult)(entityHit == null ? target : entityHit);
   }

   @Unique
   private Vec3d rotationVector(float yaw, float pitch) {
      float yawRad = yaw * (float) (Math.PI / 180.0);
      float pitchRad = pitch * (float) (Math.PI / 180.0);
      float cosPitch = MathHelper.cos(pitchRad);
      return new Vec3d(-MathHelper.sin(yawRad) * cosPitch, -MathHelper.sin(pitchRad), MathHelper.cos(yawRad) * cosPitch);
   }

   @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
   public void getBasicProjectionMatrixHook(double fov, CallbackInfoReturnable<Matrix4f> cir) {
      if (ModuleManager.aspectRatio.isEnabled() && !this.vcore$renderingHand) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.peek().getPositionMatrix().identity();
         if (this.field_4005 != 1.0F) {
            matrixStack.translate(this.field_3988, -this.field_4004, 0.0F);
            matrixStack.scale(this.field_4005, this.field_4005, 1.0F);
         }

         matrixStack.peek()
            .getPositionMatrix()
            .mul(
               new Matrix4f()
                  .setPerspective((float)(fov * (float) (Math.PI / 180.0)), ModuleManager.aspectRatio.getRatioValue(), 0.05F, this.field_4025 * 4.0F)
            );
         cir.setReturnValue(matrixStack.peek().getPositionMatrix());
      }
   }

   @Inject(method = "renderHand", at = @At("HEAD"))
   private void renderHandHead(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
      this.vcore$renderingHand = true;
   }

   @Inject(method = "renderHand", at = @At("RETURN"))
   private void renderHandReturn(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
      this.vcore$renderingHand = false;
   }

   @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
   private void bobViewHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noBob.getValue()) {
            ci.cancel();
         } else {
            Vcore.core.bobView(matrices, tickDelta);
            ci.cancel();
         }
      }
   }

   @Unique
   private HitResult ensureTargetInRangeCustom(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
      Vec3d vec3d = hitResult.getPos();
      if (!vec3d.isInRange(cameraPos, interactionRange)) {
         Vec3d vec3d2 = hitResult.getPos();
         Direction direction = Direction.getFacing(vec3d2.x - cameraPos.x, vec3d2.y - cameraPos.y, vec3d2.z - cameraPos.z);
         return BlockHitResult.createMissed(vec3d2, direction, BlockPos.ofFloored(vec3d2));
      } else {
         return hitResult;
      }
   }

   @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
   private void showFloatingItemHook(ItemStack floatingItem, CallbackInfo info) {
      if (ModuleManager.totemAnimation.isEnabled()) {
         ModuleManager.totemAnimation.showFloatingItem(floatingItem);
         info.cancel();
      }
   }

   @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
   private void renderFloatingItemHook(DrawContext context, float tickDelta, CallbackInfo ci) {
      if (ModuleManager.totemAnimation.isEnabled()) {
         ModuleManager.totemAnimation.renderFloatingItem(tickDelta);
         ci.cancel();
      }
   }

   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hurtCam.getValue()) {
         ci.cancel();
      }
   }
}
