package vcore.injection;

import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import vcore.utility.render.shaders.satin.impl.SamplerAccess;

@Mixin(ShaderProgram.class)
public abstract class MixinCoreShader implements SamplerAccess {
   @Shadow
   @Final
   private Map<String, Object> field_29487;

   @Override
   public boolean hasSampler(String name) {
      return this.field_29487.containsKey(name);
   }

   @Accessor("samplerNames")
   @Override
   public abstract List<String> getSamplerNames();

   @Accessor("loadedSamplerIds")
   @Override
   public abstract List<Integer> getSamplerShaderLocs();
}
