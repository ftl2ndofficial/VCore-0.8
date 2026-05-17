package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.jetbrains.annotations.NotNull;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.utility.math.MathUtility;

public final class AntiServerRP extends Module {
   private boolean confirm;
   private boolean accepted;
   private int delay;

   public AntiServerRP() {
      super("AntiServerRP", "Disables server resource packs.", Module.Category.MISC);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.@NotNull Receive e) {
      if (e.getPacket() instanceof ResourcePackSendS2CPacket) {
         this.confirm = true;
         this.accepted = false;
         this.delay = 0;
         e.cancel();
      }
   }

   @Override
   public void onUpdate() {
      if (this.confirm) {
         this.delay++;
         if (this.delay > MathUtility.random(15.0F, 30.0F) && !this.accepted) {
            this.sendPacket(new ResourcePackStatusC2SPacket(mc.player.method_5667(), Status.ACCEPTED));
            this.accepted = true;
         }

         if (this.delay > MathUtility.random(40.0F, 60.0F) && this.accepted) {
            this.sendPacket(new ResourcePackStatusC2SPacket(mc.player.method_5667(), Status.SUCCESSFULLY_LOADED));
            this.confirm = false;
         }
      }
   }
}
