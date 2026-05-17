package vcore.injection;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.features.modules.Module;
import vcore.utility.world.ExplosionUtility;

@Mixin(World.class)
public abstract class MixinWorld {
   @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
   public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
      if (ExplosionUtility.terrainIgnore && Module.mc.world != null && !Module.mc.world.method_24794(pos)) {
         WorldChunk worldChunk = Module.mc.world.method_8497(pos.method_10263() >> 4, pos.method_10260() >> 4);
         BlockState tempState = worldChunk.method_8320(pos);
         if (tempState.method_26204() == Blocks.OBSIDIAN
            || tempState.method_26204() == Blocks.BEDROCK
            || tempState.method_26204() == Blocks.ENDER_CHEST
            || tempState.method_26204() == Blocks.RESPAWN_ANCHOR) {
            return;
         }

         cir.setReturnValue(Blocks.AIR.getDefaultState());
      }
   }
}
