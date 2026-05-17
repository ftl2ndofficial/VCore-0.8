package vcore.utility.render.shaders.satin.impl;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniform;

public final class ResettableManagedCoreShader extends ResettableManagedShaderBase<ShaderProgram> implements ManagedCoreShader {
   private final Consumer<ManagedCoreShader> initCallback;
   private final VertexFormat vertexFormat;
   private final Map<String, ManagedSamplerUniformV1> managedSamplers = new HashMap<>();

   public ResettableManagedCoreShader(Identifier location, VertexFormat vertexFormat, Consumer<ManagedCoreShader> initCallback) {
      super(location);
      this.vertexFormat = vertexFormat;
      this.initCallback = initCallback;
   }

   protected ShaderProgram parseShader(ResourceFactory resourceManager, MinecraftClient mc, Identifier location) throws IOException {
      return new FabricShaderProgram(resourceManager, this.getLocation(), this.vertexFormat);
   }

   @Override
   public void setup(int newWidth, int newHeight) {
      Preconditions.checkNotNull(this.shader);

      for (ManagedUniformBase uniform : this.getManagedUniforms()) {
         this.setupUniform(uniform, this.shader);
      }

      this.initCallback.accept(this);
   }

   @Override
   public ShaderProgram getProgram() {
      return this.shader;
   }

   protected boolean setupUniform(ManagedUniformBase uniform, ShaderProgram shader) {
      return uniform.findUniformTarget(shader);
   }

   @Override
   public SamplerUniform findSampler(String samplerName) {
      return this.manageUniform(this.managedSamplers, ManagedSamplerUniformV1::new, samplerName, "sampler");
   }

   @Override
   protected void logInitError(IOException e) {
      LogUtils.getLogger().error("Could not create shader program {}", this.getLocation(), e);
   }
}
