package vcore.injection;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.terraformersmc.modmenu.util.mod.fabric.FabricMod")
public abstract class MixinModMenuFabricMod {
   @Shadow(remap = false)
   public abstract String getId();

   @Inject(method = "getCredits", at = @At("HEAD"), cancellable = true, remap = false)
   private void hideVcoreCredits(CallbackInfoReturnable<SortedMap<String, Set<String>>> cir) {
      if ("vcore".equalsIgnoreCase(this.getId())) {
         cir.setReturnValue(new TreeMap());
      }
   }
}
