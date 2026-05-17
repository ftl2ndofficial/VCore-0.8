package vcore.injection;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.Vcore;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
   @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
   private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         if (packet instanceof BundleS2CPacket packs) {
            packs.method_48324().forEach(p -> {
               PacketEvent.Receive eventx = new PacketEvent.Receive((Packet<?>)p);
               Vcore.EVENT_BUS.post(eventx);
               if (eventx.isCancelled()) {
                  info.cancel();
               }
            });
         } else {
            PacketEvent.Receive event = new PacketEvent.Receive(packet);
            Vcore.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               info.cancel();
            }
         }
      }
   }

   @Inject(method = "handlePacket", at = @At("TAIL"), cancellable = true)
   private static <T extends PacketListener> void onHandlePacketPost(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         if (packet instanceof BundleS2CPacket packs) {
            packs.method_48324().forEach(p -> {
               PacketEvent.ReceivePost eventx = new PacketEvent.ReceivePost((Packet<?>)p);
               Vcore.EVENT_BUS.post(eventx);
               if (eventx.isCancelled()) {
                  info.cancel();
               }
            });
         } else {
            PacketEvent.ReceivePost event = new PacketEvent.ReceivePost(packet);
            Vcore.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               info.cancel();
            }
         }
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void onSendPacketPre(Packet<?> packet, CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         if (Vcore.core.silentPackets.contains(packet)) {
            Vcore.core.silentPackets.remove(packet);
         } else {
            PacketEvent.Send event = new PacketEvent.Send(packet);
            Vcore.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               info.cancel();
            }
         }
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("RETURN"), cancellable = true)
   private void onSendPacketPost(Packet<?> packet, CallbackInfo info) {
      if (!Module.fullNullCheck()) {
         PacketEvent.SendPost event = new PacketEvent.SendPost(packet);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }
}
