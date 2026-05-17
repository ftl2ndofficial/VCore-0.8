package vcore.features.modules.player;

import vcore.features.modules.Module;
import vcore.setting.Setting;

public class NoInteract extends Module {
   public static Setting<Boolean> onlyAura = new Setting<>("OnlyAura", false);

   public NoInteract() {
      super("NoInteract", "Prevents opening containers.", Module.Category.PLAYER);
   }
}
