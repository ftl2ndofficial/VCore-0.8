package vcore.injection;

import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.utility.render.chunk.ChunkAnimations;

@Mixin(BuiltChunk.class)
public class MixinChunkBuilderBuiltChunk {
   @Inject(method = "setOrigin", at = @At("TAIL"))
   private void onSetOrigin(int x, int y, int z, CallbackInfo ci) {
      ChunkAnimations.INSTANCE.setPosition((BuiltChunk)this, new BlockPos(x, y, z));
   }
}
