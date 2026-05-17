package vcore.injection;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.XRay;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public class MixinSodiumBlockOcclusionCache {
   @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
   void shouldDrawSideHook(BlockState state, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
      if (ModuleManager.xray.isEnabled() && ModuleManager.xray.wallHack.getValue()) {
         cir.setReturnValue(XRay.isCheckableOre(state.method_26204()));
      }
   }
}
