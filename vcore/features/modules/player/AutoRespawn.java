package vcore.features.modules.player;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.Formatting;
import vcore.core.Managers;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;

public class AutoRespawn extends Module {
   private final Setting<Boolean> deathcoords = new Setting<>("deathcoords", true);
   private final Setting<Boolean> autokit = new Setting<>("Auto Kit", false);
   private final Setting<String> kit = new Setting<>("kit name", "kitname", v -> this.autokit.getValue());
   private final Setting<Boolean> autohome = new Setting<>("Auto Home", false);
   private boolean flag;
   private final Timer timer = new Timer();

   public AutoRespawn() {
      super("AutoRespawn", "Auto clicks respawn button.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (!fullNullCheck()) {
         if (this.timer.passedMs(2100L)) {
            this.timer.reset();
         }

         if (mc.currentScreen instanceof DeathScreen) {
            if (this.flag) {
               if (this.deathcoords.getValue()) {
                  this.sendMessage(
                     Formatting.GOLD
                        + "[PlayerDeath] "
                        + Formatting.YELLOW
                        + (int)mc.player.method_23317()
                        + " "
                        + (int)mc.player.method_23318()
                        + " "
                        + (int)mc.player.method_23321()
                  );
               }

               mc.player.method_7331();
               mc.setScreen(null);
               Managers.ASYNC.run(() -> {
                  if (this.autokit.getValue() && mc.player != null) {
                     mc.player.networkHandler.sendChatCommand("kit " + this.kit.getValue());
                  }

                  if (this.autohome.getValue() && mc.player != null) {
                     mc.player.networkHandler.sendChatCommand("home");
                  }
               }, 1000L);
               this.flag = false;
            }
         } else {
            this.flag = true;
         }
      }
   }
}
