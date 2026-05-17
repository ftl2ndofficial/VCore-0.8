package vcore.injection.accesors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface IClientPlayerEntity {
   @Invoker("sendMovementPackets")
   void iSendMovementPackets();

   @Accessor("lastYaw")
   float getLastYaw();

   @Accessor("lastPitch")
   float getLastPitch();

   @Accessor("lastYaw")
   void setLastYaw(float var1);

   @Accessor("lastPitch")
   void setLastPitch(float var1);

   @Accessor("mountJumpStrength")
   void setMountJumpStrength(float var1);
}
