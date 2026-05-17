package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;

public class AirStuck extends Module {
   private static final boolean CANCEL_PACKETS = true;
   private Vec3d freezePos = Vec3d.ZERO;

   public AirStuck() {
      super("AirStuck", "Freezes the player in mid-air, preventing movement.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.freezePos = mc.player.method_19538();
      }
   }

   @Override
   public void onDisable() {
      this.freezePos = Vec3d.ZERO;
   }

   @EventHandler
   public void onTick(EventTick e) {
      if (!fullNullCheck() && this.freezePos != Vec3d.ZERO) {
         ClientPlayerEntity player = mc.player;
         if (player != null) {
            player.method_18800(0.0, 0.0, 0.0);
            if (player.input != null) {
               player.input.movementForward = 0.0F;
               player.input.movementSideways = 0.0F;
            }

            player.method_23327(player.method_23317(), this.freezePos.y, player.method_23321());
         }
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (!fullNullCheck()) {
         if (e.getPacket() instanceof PlayerMoveC2SPacket) {
            e.cancel();
         }
      }
   }
}
