package vcore.injection.accesors;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityVelocityUpdateS2CPacket.class)
public interface ISPacketEntityVelocity {
   @Mutable
   @Accessor("velocityX")
   void setMotionX(int var1);

   @Mutable
   @Accessor("velocityY")
   void setMotionY(int var1);

   @Mutable
   @Accessor("velocityZ")
   void setMotionZ(int var1);
}
