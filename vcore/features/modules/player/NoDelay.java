package vcore.features.modules.player;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import vcore.features.modules.Module;
import vcore.injection.accesors.ILivingEntity;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;

public class NoDelay extends Module {
   public Setting<Boolean> blocks = new Setting<>("Blocks", false);
   public Setting<Boolean> xp = new Setting<>("XP", false);
   public Setting<Boolean> jump = new Setting<>("Jump", false);

   public NoDelay() {
      super("NoDelay", "Removes delay.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (this.jump.getValue()) {
         ((ILivingEntity)mc.player).setLastJumpCooldown(0);
      }

      if (this.blocks.getValue() && mc.player.method_6047().getItem() instanceof BlockItem) {
         ((IMinecraftClient)mc).setUseCooldown(0);
      }

      if (this.xp.getValue() && mc.player.method_6047().getItem() == Items.EXPERIENCE_BOTTLE) {
         ((IMinecraftClient)mc).setUseCooldown(0);
      }
   }
}
