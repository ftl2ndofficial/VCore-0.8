package vcore.injection;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.manager.IManager;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.player.AutoTool;

@Mixin(AbstractBlock.class)
public abstract class MixinAbstractBlock {
   @Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
   public void calcBlockBreakingDeltaHook(BlockState state, PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> ci) {
      if (ModuleManager.autoTool.isEnabled() && AutoTool.silent.getValue()) {
         float f = state.method_26214(world, pos);
         if (f < 0.0F) {
            ci.setReturnValue(0.0F);
         } else {
            float dig_speed = this.getDigSpeed(state, player.getInventory().method_5438(AutoTool.itemIndex)) / f;
            ci.setReturnValue(player.getInventory().method_5438(AutoTool.itemIndex).isSuitableFor(state) ? dig_speed / 30.0F : dig_speed / 100.0F);
         }
      }
   }

   public float getDigSpeed(BlockState state, ItemStack stack) {
      double str = stack.getMiningSpeedMultiplier(state);
      int effect = EnchantmentHelper.getLevel(
         (RegistryEntry)IManager.mc.world.method_30349().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), stack
      );
      return (float)Math.max(str + (str > 1.0 ? effect * effect + 1.0 : 0.0), 0.0);
   }
}
