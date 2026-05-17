package vcore.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import vcore.core.Managers;
import vcore.core.manager.player.CombatManager;
import vcore.features.modules.Module;
import vcore.features.modules.base.TrapModule;
import vcore.setting.Setting;

public final class AutoTrap extends TrapModule {
   private final Setting<CombatManager.TargetBy> targetBy = new Setting<>("Target By", CombatManager.TargetBy.Distance);
   private final Setting<Boolean> targetMovingPlayers = new Setting<>("MovingPlayers", false);

   public AutoTrap() {
      super("AutoTrap", "Auto traps players.", Module.Category.COMBAT);
   }

   @Override
   protected boolean needNewTarget() {
      return this.target == null
         || this.target.method_5739(mc.player) > this.range.getValue()
         || this.target.method_6032() + this.target.method_6067() <= 0.0F
         || this.target.method_29504();
   }

   @Nullable
   @Override
   protected PlayerEntity getTarget() {
      return Managers.COMBAT
         .getTarget(this.range.getValue(), this.targetBy.getValue(), p -> p.method_18798().lengthSquared() < 0.08 || this.targetMovingPlayers.getValue());
   }
}
