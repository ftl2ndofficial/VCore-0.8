package vcore.features.modules.misc;

import java.util.Objects;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class MessageAppend extends Module {
   private final Setting<String> word = new Setting<>("word", " | NextGen 0.8");
   private String skip;

   public MessageAppend() {
      super("MessageAppend", "Appends text to chat messages.", Module.Category.MISC);
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (!fullNullCheck()) {
         if (e.getPacket() instanceof ChatMessageC2SPacket pac) {
            if (Objects.equals(pac.chatMessage(), this.skip)) {
               return;
            }

            if (mc.player.method_6047().getItem() == Items.FILLED_MAP || mc.player.method_6079().getItem() == Items.FILLED_MAP) {
               return;
            }

            if (pac.chatMessage().startsWith("/") || pac.chatMessage().startsWith(Managers.COMMAND.getPrefix())) {
               return;
            }

            this.skip = pac.chatMessage() + this.word.getValue();
            mc.player.networkHandler.sendChatMessage(pac.chatMessage() + this.word.getValue());
            e.cancel();
         }
      }
   }
}
