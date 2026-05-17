package vcore.features.modules.render;

import vcore.features.modules.Module;
import vcore.setting.Setting;

public class AspectRatio extends Module {
   public Setting<AspectRatio.Mode> mode = new Setting<>("Mode", AspectRatio.Mode.Custom);
   public Setting<Float> ratio = new Setting<>("Ratio", 1.45F, 0.1F, 5.0F, v -> this.mode.is(AspectRatio.Mode.Custom));

   public AspectRatio() {
      super("AspectRatio", "Changes screen aspect ratio.", Module.Category.RENDER);
   }

   public float getRatioValue() {
      return switch ((AspectRatio.Mode)this.mode.getValue()) {
         case FourThree -> 1.3333334F;
         case OneOne -> 1.0F;
         case Custom -> this.ratio.getValue();
      };
   }

   public enum Mode {
      FourThree("4:3"),
      OneOne("1:1"),
      Custom("Custom");

      private final String displayName;

      Mode(String displayName) {
         this.displayName = displayName;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }
}
