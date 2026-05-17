package vcore.features.modules.misc;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.discord.DiscordEventHandlers;
import vcore.utility.discord.DiscordRPC;
import vcore.utility.discord.DiscordRichPresence;

public final class RPC extends Module {
   private static final DiscordRPC rpc = DiscordRPC.INSTANCE;
   public static Setting<Boolean> showIP = new Setting<>("ShowIP", true);
   public static Setting<Boolean> nickname = new Setting<>("Nickname", true);
   public static DiscordRichPresence presence = new DiscordRichPresence();
   public static boolean started;
   private final Timer timer_delay = new Timer();
   private static Thread thread;
   String slov;
   String[] rpc_perebor_en = new String[]{"Đang Lọ", "Đang xem Bún Bò Huế"};
   int randomInt;

   public RPC() {
      super("DiscordRPC", "Discord rich presence integration.", Module.Category.MISC);
   }

   @Override
   public void onDisable() {
      started = false;
      if (thread != null && !thread.isInterrupted()) {
         thread.interrupt();
      }

      rpc.Discord_Shutdown();
   }

   @Override
   public void onUpdate() {
      this.startRpc();
   }

   public void startRpc() {
      if (!this.isDisabled()) {
         if (!started) {
            started = true;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1455655905856327975", handlers, true, "");
            presence.startTimestamp = System.currentTimeMillis() / 1000L;
            presence.largeImageKey = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExdzdvbmNocjZoN3lxaDVkbW0zYnA0eHNqODF1bjRtdmM4cm1zNnprcyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/DyvuwYcDPGgI86JGWw/giphy.gif";
            rpc.Discord_UpdatePresence(presence);
            thread = new Thread(() -> {
               while (!Thread.currentThread().isInterrupted()) {
                  rpc.Discord_RunCallbacks();
                  presence.details = this.getDetails();
                  presence.state = "v0.8 for mc 1.21";
                  if (nickname.getValue()) {
                     presence.smallImageText = "logged as - " + mc.getSession().getUsername();
                     presence.smallImageKey = "https://minotar.net/helm/" + mc.getSession().getUsername() + "/100.png";
                  } else {
                     presence.smallImageText = "";
                     presence.smallImageKey = "";
                  }

                  presence.button_label_1 = "Download";
                  presence.button_url_1 = "https://www.mediafire.com/view/8k7sa659gb8k97v/Donate.jpg/file";
                  rpc.Discord_UpdatePresence(presence);

                  try {
                     Thread.sleep(2000L);
                  } catch (InterruptedException var2) {
                  }
               }
            }, "RPC-Callback-Handler");
            thread.start();
         }
      }
   }

   private String getDetails() {
      String result = "";
      if (!(mc.currentScreen instanceof MultiplayerScreen) && !(mc.currentScreen instanceof AddServerScreen) && !(mc.currentScreen instanceof TitleScreen)) {
         if (mc.getCurrentServerEntry() != null) {
            result = showIP.getValue() ? "Playing on " + mc.getCurrentServerEntry().address : "Playing on server";
         } else if (mc.isInSingleplayer()) {
            result = "SinglePlayer";
         }
      } else {
         if (this.slov == null || this.timer_delay.passedMs(60000L)) {
            this.randomInt = ThreadLocalRandom.current().nextInt(this.rpc_perebor_en.length);
            this.slov = this.rpc_perebor_en[this.randomInt];
            this.timer_delay.reset();
         }

         result = this.slov;
      }

      return result;
   }
}
