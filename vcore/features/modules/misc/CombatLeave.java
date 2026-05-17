package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import vcore.events.impl.EventScreen;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;

public class CombatLeave extends Module {
   private static final String LEAVE_COMMAND = "/KT Leave";
   private static final int COMMANDS_PER_BURST = 100;
   private static final long BURST_DELAY_MS = 50L;
   private final Setting<CombatLeave.Mode> mode = new Setting<>("Mode", CombatLeave.Mode.Spam);
   private final Timer spamTimer = new Timer();

   public CombatLeave() {
      super("CombatLeave", "Safe way to leave combat.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.spamTimer.setMs(50L);
   }

   @Override
   public void onDisable() {
      this.spamTimer.reset();
   }

   @Override
   public void onLogout() {
      this.disableAfterLeave();
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.currentEnumName();
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (!fullNullCheck() && this.spamTimer.passedMs(50L)) {
         if (this.mode.is(CombatLeave.Mode.Spam)) {
            this.spamLeaveCommand();
         }

         this.spamTimer.reset();
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof PlayerPositionLookS2CPacket) {
         this.disableAfterTeleport();
      } else {
         if (this.isDisconnectPacket(packet)) {
            this.disableAfterLeave();
         }
      }
   }

   @EventHandler
   public void onScreen(EventScreen event) {
      if (this.isDisconnectScreen(event.getScreen())) {
         this.disableAfterLeave();
      }
   }

   private void spamLeaveCommand() {
      String command = "/KT Leave".startsWith("/") ? "/KT Leave".substring(1) : "/KT Leave";

      for (int i = 0; i < 100; i++) {
         this.sendChatCommand(command);
      }
   }

   private void disableAfterTeleport() {
      if (this.isEnabled()) {
         this.disable("Disabled after teleport.");
      }
   }

   private void disableAfterLeave() {
      if (this.isEnabled()) {
         this.disableSilently();
      }
   }

   private boolean isDisconnectPacket(Packet<?> packet) {
      return packet != null && packet.getClass().getSimpleName().contains("Disconnect");
   }

   private boolean isDisconnectScreen(Screen screen) {
      return screen != null && "DisconnectedScreen".equals(screen.getClass().getSimpleName());
   }

   private enum Mode {
      Spam;
   }
}
