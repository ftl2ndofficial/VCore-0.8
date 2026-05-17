package vcore.utility.render.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import vcore.features.modules.Module;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.ShaderEffectManager;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform4f;

public class CheckboxShader {
   private Uniform2f uSize;
   private Uniform2f uLocation;
   private Uniform1f radius;
   private Uniform1f thickness;
   private Uniform1f progress;
   private Uniform4f color;
   public static final ManagedCoreShader CHECKBOX_SHADER = ShaderEffectManager.getInstance()
      .manageCoreShader(Identifier.of("vcore", "checkbox"), VertexFormats.POSITION);

   public CheckboxShader() {
      this.setup();
   }

   public void setParameters(float x, float y, float width, float height, float radius, float thickness, float progress, Color color) {
      float scale = (float)Module.mc.getWindow().getScaleFactor();
      this.radius.set(radius * scale);
      this.thickness.set(thickness * scale);
      this.progress.set(progress);
      this.uLocation.set(x * scale, -y * scale + Module.mc.getWindow().getScaledHeight() * scale - height * scale);
      this.uSize.set(width * scale, height * scale);
      this.color.set(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
   }

   public void use() {
      RenderSystem.setShader(CHECKBOX_SHADER::getProgram);
   }

   protected void setup() {
      this.uSize = CHECKBOX_SHADER.findUniform2f("uSize");
      this.uLocation = CHECKBOX_SHADER.findUniform2f("uLocation");
      this.radius = CHECKBOX_SHADER.findUniform1f("radius");
      this.thickness = CHECKBOX_SHADER.findUniform1f("thickness");
      this.progress = CHECKBOX_SHADER.findUniform1f("progress");
      this.color = CHECKBOX_SHADER.findUniform4f("color");
   }
}
