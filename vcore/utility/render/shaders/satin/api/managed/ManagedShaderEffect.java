package vcore.utility.render.shaders.satin.api.managed;

import net.minecraft.client.gl.PostEffectProcessor;
import vcore.utility.render.shaders.satin.api.managed.uniform.SamplerUniformV2;
import vcore.utility.render.shaders.satin.api.managed.uniform.UniformFinder;

public interface ManagedShaderEffect extends UniformFinder {
   PostEffectProcessor getShaderEffect();

   void release();

   void render(float var1);

   ManagedFramebuffer getTarget(String var1);

   void setUniformValue(String var1, int var2);

   void setUniformValue(String var1, float var2);

   void setUniformValue(String var1, float var2, float var3);

   void setUniformValue(String var1, float var2, float var3, float var4);

   void setUniformValue(String var1, float var2, float var3, float var4, float var5);

   SamplerUniformV2 findSampler(String var1);
}
