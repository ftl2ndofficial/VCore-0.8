package vcore.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.misc.FakePlayer;
import vcore.utility.interfaces.IEntityLiving;
import vcore.utility.interfaces.IOtherClientPlayerEntity;

@Mixin(OtherClientPlayerEntity.class)
public class MixinOtherClientPlayerEntity extends AbstractClientPlayerEntity implements IOtherClientPlayerEntity {
   @Unique
   private double backUpX;
   @Unique
   private double backUpY;
   @Unique
   private double backUpZ;

   public MixinOtherClientPlayerEntity(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Override
   public void resolve(Aura.Resolver mode) {
      if (this == FakePlayer.fakePlayer) {
         this.backUpY = -999.0;
      } else {
         this.backUpX = this.method_23317();
         this.backUpY = this.method_23318();
         this.backUpZ = this.method_23321();
         if (mode == Aura.Resolver.BackTrack) {
            double minDst = 999.0;
            Aura.Position bestPos = null;

            for (Aura.Position p : ((IEntityLiving)this).getPositionHistory()) {
               double dst = Module.mc.player.method_5649(p.getX(), p.getY(), p.getZ());
               if (dst < minDst) {
                  minDst = dst;
                  bestPos = p;
               }
            }

            if (bestPos != null) {
               this.method_5814(bestPos.getX(), bestPos.getY(), bestPos.getZ());
               if (Aura.target == this) {
                  ModuleManager.aura.resolvedBox = this.method_5829();
               }
            }
         } else {
            Vec3d from = new Vec3d(((IEntityLiving)this).getPrevServerX(), ((IEntityLiving)this).getPrevServerY(), ((IEntityLiving)this).getPrevServerZ());
            Vec3d to = new Vec3d(this.field_6224, this.field_6245, this.field_6263);
            if (mode == Aura.Resolver.Advantage) {
               if (Module.mc.player.method_5707(from) > Module.mc.player.method_5707(to)) {
                  this.method_5814(to.x, to.y, to.z);
               } else {
                  this.method_5814(from.x, from.y, from.z);
               }
            } else {
               this.method_5814(to.x, to.y, to.z);
            }

            if (Aura.target == this) {
               ModuleManager.aura.resolvedBox = this.method_5829();
            }
         }
      }
   }

   @Override
   public void releaseResolver() {
      if (this.backUpY != -999.0) {
         this.method_5814(this.backUpX, this.backUpY, this.backUpZ);
         this.backUpY = -999.0;
      }
   }
}
