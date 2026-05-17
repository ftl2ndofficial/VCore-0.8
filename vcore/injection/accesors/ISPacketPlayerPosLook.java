package vcore.injection.accesors;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerPositionLookS2CPacket.class)
public interface ISPacketPlayerPosLook {
   @Accessor("yaw")
   void setYaw(float var1);

   @Accessor("pitch")
   void setPitch(float var1);
}
