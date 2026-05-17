package vcore.features.modules.render;

import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.render.chunk.ChunkAnimations;

public class ChunkAnimation extends Module {
   public final Setting<Integer> time = new Setting<>("Time", 800, 50, 1500);
   public final Setting<ChunkAnimation.Mode> modes = new Setting<>("Modes", ChunkAnimation.Mode.Linear);

   public ChunkAnimation() {
      super("ChunkAnimation", "Animates chunk loading.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      ChunkAnimations.INSTANCE.clear();
   }

   public enum Mode {
      Linear("Linear"),
      Quad("Quad"),
      Cube("Cube"),
      Quarta("Quarta"),
      Expo("Expo");

      private final String displayName;

      Mode(String displayName) {
         this.displayName = displayName;
      }

      public int getIndex() {
         return this.ordinal();
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }
}
