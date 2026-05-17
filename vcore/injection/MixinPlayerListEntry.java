package vcore.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.features.modules.misc.UnHook;
import vcore.utility.render.TextureStorage;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry {
   @Unique
   private boolean loadedCapeTexture;
   @Unique
   private Identifier customCapeTexture;
   @Unique
   private String playerName;

   @Inject(method = "<init>(Lcom/mojang/authlib/GameProfile;Z)V", at = @At("TAIL"))
   private void initHook(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci) {
      this.playerName = profile.getName();
      this.getTexture(profile);
   }

   @Inject(method = "getSkinTextures", at = @At("TAIL"), cancellable = true)
   private void getCapeTexture(CallbackInfoReturnable<SkinTextures> cir) {
      if (UnHook.isActive()) {
         this.customCapeTexture = null;
      } else {
         this.ensureFriendCape();
         if (this.customCapeTexture != null) {
            SkinTextures prev = (SkinTextures)cir.getReturnValue();
            SkinTextures newTextures = new SkinTextures(
               prev.texture(), prev.textureUrl(), this.customCapeTexture, this.customCapeTexture, prev.model(), prev.secure()
            );
            cir.setReturnValue(newTextures);
         }
      }
   }

   @Unique
   private void getTexture(GameProfile profile) {
      if (!this.loadedCapeTexture) {
         this.loadedCapeTexture = true;
         Util.getMainWorkerExecutor().execute(() -> {
            String name = profile.getName();
            if (this.isStarcapeTarget(name)) {
               this.customCapeTexture = TextureStorage.starCape;
            }
         });
      }
   }

   @Unique
   private void ensureFriendCape() {
      if (this.playerName != null && !this.playerName.isEmpty()) {
         if (this.isStarcapeTarget(this.playerName)) {
            this.customCapeTexture = TextureStorage.starCape;
         } else {
            if (this.customCapeTexture == TextureStorage.starCape) {
               this.customCapeTexture = null;
            }
         }
      }
   }

   private boolean isStarcapeTarget(String name) {
      if (name != null && !name.isEmpty()) {
         if (Vcore.mc.player != null) {
            String selfName = Vcore.mc.player.method_5477().getString();
            if (selfName.equalsIgnoreCase(name)) {
               return true;
            }
         }

         return Managers.FRIEND.isFriend(name);
      } else {
         return false;
      }
   }
}
