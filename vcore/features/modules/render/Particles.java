package vcore.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.TextureStorage;

public class Particles extends Module {
   private final Setting<Particles.Mode> mode = new Setting<>("Mode", Particles.Mode.Stars);
   private final Setting<Particles.Physics> physics = new Setting<>("Physics", Particles.Physics.Fly);
   private final Setting<Integer> count = new Setting<>("Count", 200, 20, 800);
   private final Setting<Float> size = new Setting<>("Size", 0.8F, 0.1F, 6.0F);
   private final Setting<Boolean> syncColor = new Setting<>("SyncColor", false);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 255).getRGB()), v -> !this.syncColor.getValue());
   private final ArrayList<Particles.ParticleBase> particles = new ArrayList<>();

   public Particles() {
      super("Particles", "World particle effects.", Module.Category.RENDER);
   }

   @Override
   public void onUpdate() {
      this.particles.removeIf(Particles.ParticleBase::tick);
      if (mc.player != null && mc.world != null) {
         boolean drop = this.physics.getValue() == Particles.Physics.Drop;

         for (int j = this.particles.size(); j < this.count.getValue(); j++) {
            this.particles
               .add(
                  new Particles.ParticleBase(
                     (float)(mc.player.method_23317() + MathUtility.random(-48.0F, 48.0F)),
                     (float)(mc.player.method_23318() + MathUtility.random(2.0F, 48.0F)),
                     (float)(mc.player.method_23321() + MathUtility.random(-48.0F, 48.0F)),
                     drop ? 0.0F : MathUtility.random(-0.4F, 0.4F),
                     drop ? MathUtility.random(-0.2F, -0.05F) : MathUtility.random(-0.1F, 0.1F),
                     drop ? 0.0F : MathUtility.random(-0.4F, 0.4F)
                  )
               );
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (mc.player != null && mc.world != null) {
         stack.push();
         BufferBuilder bufferBuilder = startQuads();
         this.particles.forEach(p -> p.render(bufferBuilder));
         endQuads(bufferBuilder);
         stack.pop();
      }
   }

   private static BufferBuilder startQuads() {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      return Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
   }

   private static void endQuads(BufferBuilder bufferBuilder) {
      Render2DEngine.endBuilding(bufferBuilder);
      RenderSystem.depthMask(true);
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.disableDepthTest();
      RenderSystem.disableBlend();
   }

   private static Matrix4f buildBillboardMatrix(Camera camera, Vec3d pos) {
      MatrixStack matrices = new MatrixStack();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(pos.x, pos.y, pos.z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      return matrices.peek().getPositionMatrix();
   }

   private static void putTexturedQuad(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float w, float h, int color) {
      bufferBuilder.method_22918(matrix, x, y + h, 0.0F).texture(0.0F, 1.0F).color(color);
      bufferBuilder.method_22918(matrix, x + w, y + h, 0.0F).texture(1.0F, 1.0F).color(color);
      bufferBuilder.method_22918(matrix, x + w, y, 0.0F).texture(1.0F, 0.0F).color(color);
      bufferBuilder.method_22918(matrix, x, y, 0.0F).texture(0.0F, 0.0F).color(color);
   }

   public enum Mode {
      SnowFlake,
      Stars,
      Hearts,
      Dollars,
      Bloom;
   }

   public class ParticleBase {
      protected float prevposX;
      protected float prevposY;
      protected float prevposZ;
      protected float posX;
      protected float posY;
      protected float posZ;
      protected float motionX;
      protected float motionY;
      protected float motionZ;
      protected int age;
      protected int maxAge;

      public ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
         this.posX = posX;
         this.posY = posY;
         this.posZ = posZ;
         this.prevposX = posX;
         this.prevposY = posY;
         this.prevposZ = posZ;
         this.motionX = motionX;
         this.motionY = motionY;
         this.motionZ = motionZ;
         this.age = (int)MathUtility.random(100.0F, 300.0F);
         this.maxAge = this.age;
      }

      public boolean tick() {
         if (Module.mc == null) {
            return true;
         }

         ClientPlayerEntity player = Module.mc.player;
         if (player == null) {
            return true;
         }

         if (player.method_5649(this.posX, this.posY, this.posZ) > 4096.0) {
            this.age -= 8;
         } else {
            this.age--;
         }

         if (this.age < 0) {
            return true;
         }

         this.prevposX = this.posX;
         this.prevposY = this.posY;
         this.prevposZ = this.posZ;
         this.posX = this.posX + this.motionX;
         this.posY = this.posY + this.motionY;
         this.posZ = this.posZ + this.motionZ;
         this.motionX *= 0.9F;
         if (Particles.this.physics.getValue() == Particles.Physics.Fly) {
            this.motionY *= 0.9F;
         }

         this.motionZ *= 0.9F;
         this.motionY -= 0.001F;
         return false;
      }

      public void render(BufferBuilder bufferBuilder) {
         if (Module.mc != null && Module.mc.gameRenderer != null) {
            switch ((Particles.Mode)Particles.this.mode.getValue()) {
               case SnowFlake:
                  RenderSystem.setShaderTexture(0, TextureStorage.snowflake);
                  break;
               case Stars:
                  RenderSystem.setShaderTexture(0, TextureStorage.star);
                  break;
               case Hearts:
                  RenderSystem.setShaderTexture(0, TextureStorage.heart);
                  break;
               case Dollars:
                  RenderSystem.setShaderTexture(0, TextureStorage.dollar);
                  break;
               case Bloom:
                  RenderSystem.setShaderTexture(0, TextureStorage.firefly);
            }

            Camera camera = Module.mc.gameRenderer.getCamera();
            Color color1 = Particles.this.syncColor.getValue() ? HudEditor.getColor(this.age * 2) : Particles.this.color.getValue().getColorObject();
            Vec3d pos = Render3DEngine.interpolatePos(this.prevposX, this.prevposY, this.prevposZ, this.posX, this.posY, this.posZ);
            Matrix4f matrix1 = Particles.buildBillboardMatrix(camera, pos);
            if (Particles.this.mode.getValue() == Particles.Mode.Hearts) {
               matrix1.rotate((float)Math.toRadians(180.0), 0.0F, 0.0F, 1.0F);
            }

            int col = Render2DEngine.injectAlpha(color1, (int)(255.0F * ((float)this.age / this.maxAge))).getRGB();
            float s = Particles.this.size.getValue();
            Particles.putTexturedQuad(bufferBuilder, matrix1, -s, -s, s, s, col);
         }
      }
   }

   public enum Physics {
      Drop,
      Fly;
   }
}
