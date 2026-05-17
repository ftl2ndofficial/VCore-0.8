package vcore.injection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.injection.accesors.IPostProcessShader;
import vcore.utility.interfaces.IShaderEffect;

@Mixin(PostEffectProcessor.class)
public class MixinShaderEffect implements IShaderEffect {
   @Unique
   private final List<String> fakedBufferNames = new ArrayList<>();
   @Shadow
   @Final
   private Map<String, Framebuffer> field_1495;
   @Shadow
   @Final
   private List<PostEffectPass> field_1497;

   @Override
   public void addFakeTargetHook(String name, Framebuffer buffer) {
      Framebuffer previousFramebuffer = this.field_1495.get(name);
      if (previousFramebuffer != buffer) {
         if (previousFramebuffer != null) {
            for (PostEffectPass pass : this.field_1497) {
               if (pass.input == previousFramebuffer) {
                  ((IPostProcessShader)pass).setInput(buffer);
               }

               if (pass.output == previousFramebuffer) {
                  ((IPostProcessShader)pass).setOutput(buffer);
               }
            }

            this.field_1495.remove(name);
            this.fakedBufferNames.remove(name);
         }

         this.field_1495.put(name, buffer);
         this.fakedBufferNames.add(name);
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   void deleteFakeBuffersHook(CallbackInfo ci) {
      for (String fakedBufferName : this.fakedBufferNames) {
         this.field_1495.remove(fakedBufferName);
      }
   }
}
