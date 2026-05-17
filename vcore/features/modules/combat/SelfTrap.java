package vcore.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import vcore.features.modules.Module;
import vcore.features.modules.base.TrapModule;

public final class SelfTrap extends TrapModule {
   public SelfTrap() {
      super("SelfTrap", "Auto traps yourself.", Module.Category.COMBAT);
   }

   @Override
   protected boolean needNewTarget() {
      return this.target == null;
   }

   @Nullable
   @Override
   protected PlayerEntity getTarget() {
      return mc.player;
   }
}
