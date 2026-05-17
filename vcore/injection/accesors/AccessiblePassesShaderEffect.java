package vcore.injection.accesors;

import java.util.List;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostEffectProcessor.class)
public interface AccessiblePassesShaderEffect {
   @Accessor
   List<PostEffectPass> getPasses();
}
