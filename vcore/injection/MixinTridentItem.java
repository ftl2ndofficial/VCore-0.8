package vcore.injection;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.UseTridentEvent;

@Mixin(TridentItem.class)
public abstract class MixinTridentItem {
   @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
   public void onStoppedUsingHook(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
      if (user == Vcore.mc.player && EnchantmentHelper.getTridentSpinAttackStrength(stack, Vcore.mc.player) > 0.0F) {
         UseTridentEvent e = new UseTridentEvent();
         Vcore.EVENT_BUS.post(e);
         if (e.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "use", at = @At("HEAD"), cancellable = true)
   public void useHook(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
      ItemStack itemStack = user.method_5998(hand);
      if (EnchantmentHelper.getTridentSpinAttackStrength(itemStack, user) > 0.0F
         && !user.method_5721()
         && ModuleManager.tridentBoost.isEnabled()
         && ModuleManager.tridentBoost.anyWeather.getValue()) {
         user.method_6019(hand);
         cir.setReturnValue(TypedActionResult.consume(itemStack));
      }
   }
}
