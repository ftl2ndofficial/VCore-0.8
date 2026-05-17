package vcore.features.modules.misc;

import vcore.features.modules.Module;
import vcore.setting.Setting;

public final class Notifications extends Module {
   public final Setting<Notifications.Mode> mode = new Setting<>("Mode", Notifications.Mode.Default);

   public Notifications() {
      super("Notifications", "Client notifications.", Module.Category.MISC);
   }

   public enum Mode {
      Default,
      CrossHair;
   }
}
