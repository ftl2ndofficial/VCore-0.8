package vcore.features.modules.misc;

import vcore.features.modules.Module;
import vcore.setting.Setting;

public final class ClientSound extends Module {
   public final Setting<Integer> volume = new Setting<>("Volume", 100, 20, 100);
   public final Setting<ClientSound.OnOffSound> onOffSound = new Setting<>("Mode", ClientSound.OnOffSound.Celestial);

   public ClientSound() {
      super("ClientSound", "Custom client sounds and notifications.", Module.Category.MISC);
   }

   public enum OnOffSound {
      Newcode,
      Catlavan,
      Celestial,
      Nursultan;
   }
}
