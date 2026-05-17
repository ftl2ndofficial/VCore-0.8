package vcore.injection;

import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.utility.interfaces.ICrystal;

@Mixin(EndCrystalEntity.class)
public class MixinEndCrystal implements ICrystal {
   @Unique
   int attacks;
   @Unique
   int cooldown;

   @Override
   public boolean canAttack() {
      return this.cooldown == 0;
   }

   @Override
   public void attack() {
      if (this.attacks++ >= 5) {
         this.cooldown = 20;
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   public void tickHook(CallbackInfo ci) {
      if (this.cooldown > 0) {
         this.cooldown--;
      }
   }
}
