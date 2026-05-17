package vcore.injection;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.utility.render.chunk.ChunkAnimations;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public class MixinSodiumRenderSectionManagerChunkAnimations {
   @Inject(method = "onSectionAdded(III)V", at = @At("TAIL"), remap = false)
   private void onSectionAdded(int x, int y, int z, CallbackInfo ci) {
      ChunkAnimations.INSTANCE.setPosition(new BlockPos(x << 4, y << 4, z << 4));
   }
}
