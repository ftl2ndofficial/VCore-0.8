package vcore.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.combat.HitBox;
import vcore.setting.Setting;
import vcore.utility.render.GhostRenderer3D;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.TextureStorage;
import vcore.utility.render.animation.CaptureMark;
import vcore.utility.render.animation.advanced.Animation;
import vcore.utility.render.animation.advanced.Easing;
import vcore.utility.render.animation.advanced.InfinityAnimation;

public class TargerESP extends Module {
   private static final int PARTICLE_LIMIT = 3;
   private static final float PARTICLE_SIZE = 0.28F;
   private static final float GHOST_V1_SPEED = 0.62F;
   private final Setting<TargerESP.Mode> mode = new Setting<>("Mode", TargerESP.Mode.GhostV2);
   private final Animation ghostV1Anim = new Animation().setEasing(Easing.EASE_OUT_QUAD).setSpeed(400).setSize(1.0F).setForward(false);
   private LivingEntity ghostV1Target;
   private final TargerESP.TargetEspRenderer renderer = new TargerESP.TargetEspRenderer(() -> 3, () -> 0.28F, () -> TextureStorage.firefly);

   public TargerESP() {
      super("TargetESP", "Ghost trail ESP for the current target.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      this.renderer.reset();
      this.resetGhostV1();
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (fullNullCheck()) {
         this.renderer.reset();
         this.resetGhostV1();
      } else {
         Entity target = this.resolveTarget();
         if (this.mode.is(TargerESP.Mode.GhostV1)) {
            this.renderGhostV1(target);
         } else {
            this.resetGhostV1();
            switch ((TargerESP.Mode)this.mode.getValue()) {
               case Circle:
                  if (target == null) {
                     this.renderer.reset();
                     return;
                  }

                  Render3DEngine.drawOldTargetEsp(stack, target);
                  break;
               case Cube:
                  if (target == null) {
                     this.renderer.reset();
                     return;
                  }

                  CaptureMark.render(target);
               case GhostV1:
               default:
                  break;
               case GhostV2:
                  if (target instanceof LivingEntity living) {
                     this.renderer.render(living);
                  }
            }
         }

         if (!this.mode.is(TargerESP.Mode.GhostV2)) {
            this.renderer.reset();
         }
      }
   }

   private Entity resolveTarget() {
      Entity auraTarget = Aura.target;
      return auraTarget != null && !auraTarget.isRemoved() ? auraTarget : null;
   }

   private void renderGhostV1(Entity target) {
      LivingEntity livingTarget = target instanceof LivingEntity living && !living.method_31481() ? living : null;
      if (livingTarget != null) {
         this.ghostV1Target = livingTarget;
      }

      this.ghostV1Anim.setForward(livingTarget != null);
      if (this.ghostV1Target != null) {
         if (!this.ghostV1Target.method_31481() && !this.ghostV1Anim.finished(false)) {
            float anim = this.ghostV1Anim.get();
            float red = MathHelper.clamp((this.ghostV1Target.hurtTime - Render3DEngine.getTickDelta()) / 20.0F, 0.0F, 1.0F);
            Render3DEngine.renderGhosts(this.ghostV1Target, anim, red, 0.62F);
         } else {
            this.ghostV1Target = null;
         }
      }
   }

   private void resetGhostV1() {
      this.ghostV1Target = null;
      this.ghostV1Anim.setForward(false);
      this.ghostV1Anim.reset();
   }

   public enum Mode {
      Circle("Circle"),
      Cube("Cube"),
      GhostV1("Ghost V1"),
      GhostV2("Ghost V2");

      private final String displayName;

      Mode(String displayName) {
         this.displayName = displayName;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }

   public static class TargetEspRenderer {
      private final Supplier<Integer> particleLimit;
      private final Supplier<Float> particleSize;
      private final Supplier<Identifier> textureSupplier;
      private final InfinityAnimation moving = new InfinityAnimation();
      private final Animation targetEspAnim = new Animation().setEasing(Easing.TARGETESP_EASE_OUT_BACK).setSpeed(300).setSize(1.0F).setForward(false);
      private final List<GhostRenderer3D> particles = new ArrayList<>();

      public TargetEspRenderer(Supplier<Integer> particleLimit, Supplier<Float> particleSize, Supplier<Identifier> textureSupplier) {
         this.particleLimit = particleLimit;
         this.particleSize = particleSize;
         this.textureSupplier = textureSupplier;
      }

      public void render(LivingEntity target) {
         if (target != null && !target.method_31481()) {
            this.spawnParticleIfNeeded(target);
            float fpsFactor = 500.0F / Math.max(Module.mc.getCurrentFps(), 5);
            this.moving.animate(this.moving.get() + 20.0F, 55);
            this.targetEspAnim.setForward(target.hurtTime > 7);
            float movementValue = this.moving.get();
            float animationFactor = this.targetEspAnim.get();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, this.textureSupplier.get());
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Camera camera = Module.mc.gameRenderer.getCamera();

            for (int index = 0; index < this.particles.size(); index++) {
               GhostRenderer3D particle = this.particles.get(index);
               this.updateParticle(particle, index, fpsFactor, target, movementValue, animationFactor);
               particle.render(buffer, camera);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
         } else {
            this.reset();
         }
      }

      private void spawnParticleIfNeeded(LivingEntity target) {
         int desired = Math.max(1, this.particleLimit.get());
         if (this.particles.size() < desired) {
            this.particles.add(new GhostRenderer3D(target.method_19538(), Vec3d.ZERO, this.particleSize.get()));
         }

         while (this.particles.size() > desired) {
            this.particles.removeLast();
         }
      }

      private void updateParticle(GhostRenderer3D particle, int index, float fpsFactor, LivingEntity target, float movementValue, float animationFactor) {
         int segments = Math.max(1, this.particleLimit.get());
         float angleOffset = index * 360.0F / segments;
         float currentAngle = movementValue + angleOffset;
         double rad = Math.toRadians(currentAngle);
         double baseRadius = this.getOrbitRadius(target, this.particleSize.get());
         double dynamicRadius = baseRadius - baseRadius * animationFactor;
         double offsetX = Math.sin(rad) * dynamicRadius;
         double offsetZ = Math.cos(rad) * dynamicRadius;
         double verticalSwing = Math.sin(Math.toRadians(movementValue / (index + 1.0F))) * this.getVerticalAmplitude(target);
         Vec3d desiredPos = target.method_19538().add(offsetX, this.getVerticalCenter(target) + verticalSwing, offsetZ);
         double mul = 0.05F * fpsFactor;
         Vec3d motion = desiredPos.subtract(particle.getPosition()).multiply(mul, mul, mul);
         particle.setMotion(motion);
         particle.tick();
      }

      private double getOrbitRadius(LivingEntity target, float particleSize) {
         Box box = HitBox.getBaseBoundingBox(target);
         double hitboxWidth = Math.max(box.getLengthX(), box.getLengthZ());
         return hitboxWidth * 0.5 + particleSize * 0.25;
      }

      private double getVerticalCenter(LivingEntity target) {
         return this.getVerticalAmplitude(target) - 0.7;
      }

      private double getVerticalAmplitude(LivingEntity target) {
         return HitBox.getBaseBoundingBox(target).getLengthY() * 0.5;
      }

      public void reset() {
         this.particles.clear();
         this.moving.getAnimation().reset();
         this.targetEspAnim.reset();
      }
   }
}
