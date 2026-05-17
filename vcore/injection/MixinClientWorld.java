package vcore.injection;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventEntityRemoved;
import vcore.events.impl.EventEntitySpawn;
import vcore.events.impl.EventEntitySpawnPost;
import vcore.features.modules.Module;
import vcore.features.modules.render.WorldTweaks;
import vcore.setting.impl.ColorSetting;

@Mixin(ClientWorld.class)
public class MixinClientWorld {
   @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
   public void addEntityHook(Entity entity, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventEntitySpawn ees = new EventEntitySpawn(entity);
         Vcore.EVENT_BUS.post(ees);
         if (ees.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "addEntity", at = @At("RETURN"), cancellable = true)
   public void addEntityHookPost(Entity entity, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventEntitySpawnPost ees = new EventEntitySpawnPost(entity);
         Vcore.EVENT_BUS.post(ees);
         if (ees.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "removeEntity", at = @At("HEAD"))
   public void removeEntityHook(int entityId, RemovalReason removalReason, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventEntityRemoved eer = new EventEntityRemoved(Module.mc.world.method_8469(entityId));
         Vcore.EVENT_BUS.post(eer);
      }
   }

   @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
   private void getSkyColorHook(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
      if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
         ColorSetting c = WorldTweaks.fogColor.getValue();
         cir.setReturnValue(new Vec3d(c.getGlRed(), c.getGlGreen(), c.getGlBlue()));
      }
   }
}
