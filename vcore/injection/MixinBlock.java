package vcore.injection;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.XRay;

@Mixin(Block.class)
public abstract class MixinBlock {
   @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
   private static void shouldDrawSideHook(
      BlockState state, BlockView world, BlockPos pos, Direction side, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir
   ) {
      if (ModuleManager.xray.isEnabled() && ModuleManager.xray.wallHack.getValue()) {
         cir.setReturnValue(XRay.isCheckableOre(state.method_26204()));
      }
   }

   @Inject(method = "getVelocityMultiplier", at = @At("HEAD"), cancellable = true)
   public void getVelocityMultiplierHook(CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.noSlow.isEnabled()) {
         if (ModuleManager.noSlow.soulSand.getValue() && this == Blocks.SOUL_SAND) {
            cir.setReturnValue(Blocks.DIRT.getVelocityMultiplier());
         }

         if (ModuleManager.noSlow.honey.getValue() && this == Blocks.HONEY_BLOCK) {
            cir.setReturnValue(Blocks.DIRT.getVelocityMultiplier());
         }
      }
   }

   @Inject(method = "getSlipperiness", at = @At("HEAD"), cancellable = true)
   public void getSlipperinessHook(CallbackInfoReturnable<Float> cir) {
      if (ModuleManager.noSlow.isEnabled()) {
         if (ModuleManager.noSlow.slime.getValue() && this == Blocks.SLIME_BLOCK) {
            cir.setReturnValue(Blocks.DIRT.getSlipperiness());
         }

         if (ModuleManager.noSlow.ice.getValue()
            && (this == Blocks.ICE || this == Blocks.PACKED_ICE || this == Blocks.BLUE_ICE || this == Blocks.FROSTED_ICE)
            && !IManager.mc.options.jumpKey.isPressed()) {
            cir.setReturnValue(Blocks.DIRT.getSlipperiness());
         }
      }
   }
}
