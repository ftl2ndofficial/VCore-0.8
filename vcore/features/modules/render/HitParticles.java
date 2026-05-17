package vcore.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import vcore.events.impl.EventAttack;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.animation.AnimationUtility;

public class HitParticles extends Module {
   private final Setting<HitParticles.Mode> mode = new Setting<>("Mode", HitParticles.Mode.Stars);
   private final Setting<HitParticles.Physics> physics = new Setting<>("Physics", HitParticles.Physics.Fly);
   private final Setting<Integer> amount = new Setting<>("Amount", 15, 5, 50);
   private final Setting<Integer> lifeTime = new Setting<>("LifeTime", 1, 1, 10);
   private final Setting<Integer> speed = new Setting<>("Speed", 12, 5, 20);
   private final Setting<Float> starsScale = new Setting<>("Scale", 3.5F, 1.0F, 10.0F, v -> this.mode.getValue() != HitParticles.Mode.Orbiz);
   private final Setting<Boolean> syncColor = new Setting<>("SyncColor", false);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 255).getRGB()), v -> !this.syncColor.getValue());
   private final Setting<ColorSetting> colorH = new Setting<>(
      "HealColor", new ColorSetting(new Color(0, 255, 0, 255).getRGB()), v -> this.mode.is(HitParticles.Mode.Text)
   );
   private final Setting<ColorSetting> colorD = new Setting<>(
      "DamageColor", new ColorSetting(new Color(255, 0, 0, 255).getRGB()), v -> this.mode.is(HitParticles.Mode.Text)
   );
   private final CopyOnWriteArrayList<HitParticles.Particle> particles = new CopyOnWriteArrayList<>();

   public HitParticles() {
      super("HitParticles", "Particles on hit.", Module.Category.RENDER);
   }

   @Override
   public void onUpdate() {
      this.particles.removeIf(HitParticles.Particle::update);
   }

   @EventHandler
   public void onAttack(EventAttack e) {
      if (mc.player != null && e != null && !e.isPre()) {
         Entity target = e.getEntity();
         if (target != null) {
            if (target instanceof LivingEntity living) {
               if (living.getHealth() <= 0.0F) {
                  return;
               }

               if (living.hurtTime > 1) {
                  return;
               }
            }

            Vec3d point = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2.0, target.getZ());
            Color c = this.syncColor.getValue() ? HudEditor.getColor((int)MathUtility.random(1.0F, 228.0F)) : this.color.getValue().getColorObject();

            for (int i = 0; i < this.amount.getValue(); i++) {
               this.particles
                  .add(
                     new HitParticles.Particle(
                        (float)point.x, (float)point.y, (float)point.z, c, MathUtility.random(0.0F, 180.0F), MathUtility.random(10.0F, 60.0F), 0.0F
                     )
                  );
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      RenderSystem.disableDepthTest();
      if (mc.player != null && mc.world != null) {
         for (HitParticles.Particle particle : this.particles) {
            particle.render(stack);
         }
      }

      RenderSystem.enableDepthTest();
   }

   public enum Mode {
      Orbiz,
      Stars,
      Hearts,
      Bloom,
      Text;
   }

   public class Particle {
      float x;
      float y;
      float z;
      float px;
      float py;
      float pz;
      float motionX;
      float motionY;
      float motionZ;
      float rotationAngle;
      float rotationSpeed;
      float health;
      long time;
      Color color;

      public Particle(float x, float y, float z, Color color, float rotationAngle, float rotationSpeed, float health) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.px = x;
         this.py = y;
         this.pz = z;
         this.motionX = MathUtility.random(-HitParticles.this.speed.getValue().intValue() / 50.0F, HitParticles.this.speed.getValue().intValue() / 50.0F);
         this.motionY = MathUtility.random(-HitParticles.this.speed.getValue().intValue() / 50.0F, HitParticles.this.speed.getValue().intValue() / 50.0F);
         this.motionZ = MathUtility.random(-HitParticles.this.speed.getValue().intValue() / 50.0F, HitParticles.this.speed.getValue().intValue() / 50.0F);
         this.time = System.currentTimeMillis();
         this.color = color;
         this.rotationAngle = rotationAngle;
         this.rotationSpeed = rotationSpeed;
         this.health = health;
      }

      public long getTime() {
         return this.time;
      }

      public boolean update() {
         double sp = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         this.x = this.x + this.motionX;
         this.y = this.y + this.motionY;
         this.z = this.z + this.motionZ;
         if (this.posBlock(this.x, this.y - HitParticles.this.starsScale.getValue() / 10.0F, this.z)) {
            this.motionY = -this.motionY / 1.1F;
            this.motionX /= 1.1F;
            this.motionZ /= 1.1F;
         } else if (this.posBlock(this.x - sp, this.y, this.z - sp)
            || this.posBlock(this.x + sp, this.y, this.z + sp)
            || this.posBlock(this.x + sp, this.y, this.z - sp)
            || this.posBlock(this.x - sp, this.y, this.z + sp)
            || this.posBlock(this.x + sp, this.y, this.z)
            || this.posBlock(this.x - sp, this.y, this.z)
            || this.posBlock(this.x, this.y, this.z + sp)
            || this.posBlock(this.x, this.y, this.z - sp)) {
            this.motionX = -this.motionX;
            this.motionZ = -this.motionZ;
         }

         if (HitParticles.this.physics.getValue() == HitParticles.Physics.Fall) {
            this.motionY -= 0.035F;
         }

         this.motionX /= 1.005F;
         this.motionZ /= 1.005F;
         this.motionY /= 1.005F;
         float lifeTimeMs = HitParticles.this.lifeTime.getValue().intValue() * 1000.0F;
         float lived = (float)(System.currentTimeMillis() - this.getTime());
         float alpha = 1.0F - lived / lifeTimeMs;
         return alpha <= 0.0F;
      }

      public void render(MatrixStack matrixStack) {
         float size = HitParticles.this.starsScale.getValue();
         float scale = HitParticles.this.mode.is(HitParticles.Mode.Text) ? 0.025F * size : 0.07F;
         double posX = Render2DEngine.interpolate(this.px, this.x, Render3DEngine.getTickDelta())
            - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216();
         double posY = Render2DEngine.interpolate(this.py, this.y, Render3DEngine.getTickDelta())
            + 0.1
            - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214();
         double posZ = Render2DEngine.interpolate(this.pz, this.z, Render3DEngine.getTickDelta())
            - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215();
         matrixStack.push();
         matrixStack.translate(posX, posY, posZ);
         matrixStack.scale(scale, scale, scale);
         matrixStack.translate(size / 2.0F, size / 2.0F, size / 2.0F);
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-Module.mc.gameRenderer.getCamera().getYaw()));
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Module.mc.gameRenderer.getCamera().getPitch()));
         if (HitParticles.this.mode.is(HitParticles.Mode.Text)) {
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
         } else {
            matrixStack.multiply(
               RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationAngle = this.rotationAngle + AnimationUtility.deltaTime() * this.rotationSpeed)
            );
         }

         matrixStack.translate(-size / 2.0F, -size / 2.0F, -size / 2.0F);
         float lifeTimeMs = HitParticles.this.lifeTime.getValue().intValue() * 1000.0F;
         float lived = (float)(System.currentTimeMillis() - this.getTime());
         float alpha = 1.0F - lived / lifeTimeMs;
         if (alpha < 0.0F) {
            alpha = 0.0F;
         }

         int renderAlpha = (int)(this.color.getAlpha() * alpha);
         Color renderColor = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), renderAlpha);
         switch ((HitParticles.Mode)HitParticles.this.mode.getValue()) {
            case Orbiz:
               Render2DEngine.drawOrbiz(matrixStack, 0.0F, 0.3, renderColor);
               Render2DEngine.drawOrbiz(matrixStack, -0.1F, 0.5, renderColor);
               Render2DEngine.drawOrbiz(matrixStack, -0.2F, 0.7, renderColor);
               break;
            case Stars:
               Render2DEngine.drawStar(matrixStack, renderColor, size);
               break;
            case Hearts:
               Render2DEngine.drawHeart(matrixStack, renderColor, size);
               break;
            case Bloom:
               Render2DEngine.drawBloom(matrixStack, renderColor, size);
               break;
            case Text:
               FontRenderers.sf_medium
                  .drawCenteredString(
                     matrixStack,
                     MathUtility.round2(this.health) + " ",
                     0.0,
                     0.0,
                     (this.health > 0.0F ? HitParticles.this.colorH.getValue() : HitParticles.this.colorD.getValue()).getColorObject()
                  );
         }

         matrixStack.scale(0.8F, 0.8F, 0.8F);
         matrixStack.pop();
      }

      private boolean posBlock(double x, double y, double z) {
         if (Module.mc.world == null) {
            return false;
         }

         Block b = Module.mc.world.method_8320(BlockPos.ofFloored(x, y, z)).method_26204();
         return !(b instanceof AirBlock) && b != Blocks.WATER && b != Blocks.LAVA;
      }
   }

   public enum Physics {
      Fall,
      Fly;
   }
}
