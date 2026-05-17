package vcore.injection;

import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.ElderGuardianAppearanceParticle;
import net.minecraft.client.particle.ExplosionLargeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.FireworksSparkParticle.FireworkParticle;
import net.minecraft.client.particle.FireworksSparkParticle.Flash;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.render.NoRender;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
   @Inject(at = @At("HEAD"), method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", cancellable = true)
   public void addParticleHook(Particle p, CallbackInfo e) {
      NoRender nR = ModuleManager.noRender;
      if (nR.isEnabled()) {
         if (nR.noWeather.getValue()) {
            String particleName = p.getClass().getSimpleName();
            if (particleName.contains("Rain") || particleName.contains("Splash")) {
               e.cancel();
            }
         }

         if (nR.elderGuardian.getValue() && p instanceof ElderGuardianAppearanceParticle) {
            e.cancel();
         }

         if (nR.explosions.getValue() && p instanceof ExplosionLargeParticle) {
            e.cancel();
         }

         if (nR.campFire.getValue() && p instanceof CampfireSmokeParticle) {
            e.cancel();
         }

         if (nR.breakParticles.getValue() && p instanceof BlockDustParticle) {
            e.cancel();
         }

         if (nR.fireworks.getValue() && (p instanceof FireworkParticle || p instanceof Flash)) {
            e.cancel();
         }
      }
   }
}
