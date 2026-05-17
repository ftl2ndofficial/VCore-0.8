package vcore.features.modules.combat;

import vcore.features.modules.Module;
import vcore.setting.Setting;

public class WallsBypass extends Module {
   public final Setting<WallsBypass.Mode> mode = new Setting<>("Mode", WallsBypass.Mode.YawOffset);

   public WallsBypass() {
      super("WallsBypass", "Bypass wall rotations for Aura.", Module.Category.COMBAT);
   }

   public boolean isYawOffset() {
      return this.mode.getValue() == WallsBypass.Mode.YawOffset;
   }

   public boolean isPeekHigh() {
      return this.mode.getValue() == WallsBypass.Mode.PeekHigh;
   }

   public enum Mode {
      YawOffset,
      PeekHigh;
   }
}
