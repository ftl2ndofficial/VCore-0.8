package vcore.utility.render.shaders.satin.impl;

import java.util.function.IntSupplier;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.JsonEffectShaderProgram;
import net.minecraft.client.texture.AbstractTexture;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniformV2;

public final class ManagedSamplerUniformV2 extends ManagedSamplerUniformBase implements SamplerUniformV2 {
   public ManagedSamplerUniformV2(String name) {
      super(name);
   }

   @Override
   public void set(AbstractTexture texture) {
      this.set(texture::getGlId);
   }

   @Override
   public void set(Framebuffer textureFbo) {
      this.set(textureFbo::getColorAttachment);
   }

   @Override
   public void set(int textureName) {
      this.set(() -> textureName);
   }

   @Override
   protected void set(Object value) {
      this.set((IntSupplier)value);
   }

   @Override
   public void set(IntSupplier value) {
      SamplerAccess[] targets = this.targets;
      if (targets.length > 0 && this.cachedValue != value) {
         for (SamplerAccess target : targets) {
            ((JsonEffectShaderProgram)target).bindSampler(this.name, value);
         }

         this.cachedValue = value;
      }
   }
}
