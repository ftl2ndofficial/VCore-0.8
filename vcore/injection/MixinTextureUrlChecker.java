package vcore.injection;

import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureUrlChecker.class)
public class MixinTextureUrlChecker {
   @Inject(method = "isAllowedTextureDomain", at = @At("HEAD"), cancellable = true, remap = false)
   private static void onIsAllowedTextureDomain(String url, CallbackInfoReturnable<Boolean> cir) {
      if (url == null || url.isBlank()) {
         cir.setReturnValue(false);
      }
   }
}
