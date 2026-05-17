package vcore.features.modules.combat;

import vcore.features.modules.Module;
import vcore.setting.Setting;

public class Reach extends Module {
   public final Setting<Float> blocksRange = new Setting<>("BlocksRange", 3.0F, 0.1F, 10.0F);
   public final Setting<Float> entityRange = new Setting<>("EntityRange", 3.0F, 0.1F, 10.0F);
   public final Setting<Boolean> Creative = new Setting<>("Creative", false);
   public final Setting<Float> creativeBlocksRange = new Setting<>("CBlocksRange", 5.0F, 0.1F, 10.0F, v -> this.Creative.getValue());
   public final Setting<Float> creativeEntityRange = new Setting<>("CEntityRange", 5.0F, 0.1F, 10.0F, v -> this.Creative.getValue());

   public Reach() {
      super("Reach", "Extends reach distance.", Module.Category.COMBAT);
   }

   @Override
   public String getDisplayInfo() {
      return "B: " + this.blocksRange.getValue() + " E:" + this.entityRange.getValue();
   }
}
