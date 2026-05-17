package vcore.features.modules.player;

import vcore.features.modules.Module;
import vcore.setting.Setting;

public final class NoEntityTrace extends Module {
   public static final Setting<Boolean> cobweb = new Setting<>("Cobweb", true);
   public static final Setting<Boolean> pickaxe = new Setting<>("Pickaxe", true);
   public static final Setting<Boolean> axe = new Setting<>("Axe", true);

   public NoEntityTrace() {
      super("NoEntityTrace", "Ignores entity hitboxes.", Module.Category.PLAYER);
   }
}
