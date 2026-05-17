package vcore.utility.render.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL30;
import vcore.features.modules.Module;
import vcore.utility.render.WindowResizeCallback;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.ShaderEffectManager;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniform;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform4f;

public class BlurProgram {
   private Uniform2f uSize;
   private Uniform2f uLocation;
   private Uniform1f radius;
   private Uniform2f inputResolution;
   private Uniform1f opacity;
   private Uniform1f quality;
   private Uniform4f color1;
   private SamplerUniform sampler;
   private Framebuffer input;
   public static final ManagedCoreShader BLUR = ShaderEffectManager.getInstance().manageCoreShader(Identifier.of("vcore", "blur"), VertexFormats.POSITION);

   public BlurProgram() {
      this.setup();
   }

   public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
      if (this.input == null) {
         this.input = new SimpleFramebuffer(
            Module.mc.getWindow().getScaledWidth(), Module.mc.getWindow().getScaledHeight(), false, MinecraftClient.IS_SYSTEM_MAC
         );
      }

      float i = (float)Module.mc.getWindow().getScaleFactor();
      this.radius.set(r * i);
      this.uLocation.set(x * i, -y * i + Module.mc.getWindow().getScaledHeight() * i - height * i);
      this.uSize.set(width * i, height * i);
      this.opacity.set(blurOpacity);
      this.quality.set(blurStrenth);
      this.color1.set(c1.getRed() / 255.0F, c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, 1.0F);
      this.sampler.set(this.input.getColorAttachment());
   }

   public void use() {
      Framebuffer buffer = MinecraftClient.getInstance().getFramebuffer();
      this.input.beginWrite(false);
      GL30.glBindFramebuffer(36008, buffer.fbo);
      GL30.glBlitFramebuffer(0, 0, buffer.textureWidth, buffer.textureHeight, 0, 0, buffer.textureWidth, buffer.textureHeight, 16384, 9729);
      buffer.beginWrite(false);
      if (this.input != null
         && (this.input.textureWidth != Module.mc.getWindow().getFramebufferWidth() || this.input.textureHeight != Module.mc.getWindow().getFramebufferHeight())
         )
       {
         this.input.resize(Module.mc.getWindow().getFramebufferWidth(), Module.mc.getWindow().getFramebufferHeight(), MinecraftClient.IS_SYSTEM_MAC);
      }

      this.inputResolution.set(buffer.textureWidth, buffer.textureHeight);
      this.sampler.set(this.input.getColorAttachment());
      RenderSystem.setShader(BLUR::getProgram);
   }

   protected void setup() {
      this.inputResolution = BLUR.findUniform2f("InputResolution");
      this.opacity = BLUR.findUniform1f("Opacity");
      this.quality = BLUR.findUniform1f("Quality");
      this.color1 = BLUR.findUniform4f("color1");
      this.uSize = BLUR.findUniform2f("uSize");
      this.uLocation = BLUR.findUniform2f("uLocation");
      this.radius = BLUR.findUniform1f("radius");
      this.sampler = BLUR.findSampler("InputSampler");
      WindowResizeCallback.EVENT.register((WindowResizeCallback)(client, window) -> {
         if (this.input != null) {
            this.input.resize(window.getFramebufferWidth(), window.getFramebufferHeight(), MinecraftClient.IS_SYSTEM_MAC);
         }
      });
   }
}
