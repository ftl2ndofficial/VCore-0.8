package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AntiBot;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;

public class TotemPopCounter extends Module {
   public Setting<Boolean> notification = new Setting<>("Notification", true);

   public TotemPopCounter() {
      super("TotemPopCounter", "Counts players' totem pops.", Module.Category.MISC);
   }

   @EventHandler
   public void onTotemPop(@NotNull TotemPopEvent event) {
      if (event.getEntity() != mc.player) {
         String s = Formatting.GREEN
            + event.getEntity().method_5477().getString()
            + Formatting.WHITE
            + " popped "
            + Formatting.AQUA
            + (event.getPops() > 1 ? "" + event.getPops() + Formatting.WHITE + " totems!" : Formatting.WHITE + " a totem!");
         Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
      }
   }

   @Override
   public void onUpdate() {
      for (PlayerEntity player : mc.world.method_18456()) {
         if (player != mc.player
            && (!ModuleManager.antiBot.isEnabled() || ModuleManager.antiBot.mode.getValue() != AntiBot.Mode.Matrix || !AntiBot.isBot(player))
            && !(player.method_6032() > 0.0F)
            && Managers.COMBAT.popList.containsKey(player.method_5477().getString())) {
            String s = Formatting.GREEN
               + player.method_5477().getString()
               + Formatting.WHITE
               + " popped "
               + (
                  Managers.COMBAT.popList.get(player.method_5477().getString()) > 1
                     ? "" + Managers.COMBAT.popList.get(player.method_5477().getString()) + Formatting.WHITE + " totems and died EZ LMAO!"
                     : Formatting.WHITE + " totem and died EZ LMAO!"
               );
            Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
         }
      }
   }
}
