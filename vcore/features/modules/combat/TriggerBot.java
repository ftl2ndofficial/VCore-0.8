package vcore.features.modules.combat;

import java.util.Random;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;

public final class TriggerBot extends Module {
   public final Setting<Float> attackRange = new Setting<>("Range", 3.0F, 1.0F, 7.0F);
   public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
   public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(this.smartCrit);
   public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(this.smartCrit);
   public final Setting<Boolean> ignoreWalls = new Setting<>("IgnoreWalls", false);
   public final Setting<Boolean> pauseEating = new Setting<>("PauseWhileEating", false);
   public final Setting<Integer> minDelay = new Setting<>("RandomDelayMin", 2, 0, 20);
   public final Setting<Integer> maxDelay = new Setting<>("RandomDelayMax", 13, 0, 20);
   private int delay;
   private final Random random = new Random();

   public TriggerBot() {
      super("TriggerBot", "Auto attacks what you look at.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onAttack(PlayerUpdateEvent e) {
      if (!mc.player.method_6115() || !this.pauseEating.getValue()) {
         if (!mc.options.jumpKey.isPressed() && mc.player.method_24828() && this.autoJump.getValue()) {
            mc.player.method_6043();
         }

         if (!this.autoCrit() && this.delay > 0) {
            this.delay--;
         } else {
            Entity ent = Managers.PLAYER
               .getRtxTarget(mc.player.method_36454(), mc.player.method_36455(), this.attackRange.getValue(), this.ignoreWalls.getValue());
            if (ent != null && !Managers.FRIEND.isFriend(ent.method_5477().getString())) {
               mc.interactionManager.attackEntity(mc.player, ent);
               mc.player.method_6104(Hand.MAIN_HAND);
               this.delay = this.random.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
            }
         }
      }
   }

   private boolean autoCrit() {
      boolean reasonForSkipCrit = !this.smartCrit.getValue().isEnabled()
         || mc.player.method_31549().flying
         || mc.player.method_6128()
         || ModuleManager.elytraPlus.isEnabled()
         || mc.player.method_6059(StatusEffects.BLINDNESS)
         || mc.player.method_21754()
         || mc.world.method_8320(BlockPos.ofFloored(mc.player.method_19538())).method_26204() == Blocks.COBWEB;
      if (mc.player.field_6017 > 1.0F && mc.player.field_6017 < 1.14) {
         return false;
      } else if (ModuleManager.aura.getAttackCooldown() < (mc.player.method_24828() ? 1.0F : 0.9F)) {
         return false;
      } else {
         boolean mergeWithTargetStrafe = !ModuleManager.targetStrafe.isEnabled() || !ModuleManager.targetStrafe.jump.getValue();
         boolean mergeWithSpeed = !ModuleManager.speed.isEnabled() || mc.player.method_24828();
         if (!mc.options.jumpKey.isPressed() && mergeWithTargetStrafe && mergeWithSpeed && !this.onlySpace.getValue() && !this.autoJump.getValue()) {
            return true;
         } else if (mc.player.method_5771()) {
            return true;
         } else if (!mc.options.jumpKey.isPressed() && ModuleManager.aura.isAboveWater()) {
            return true;
         } else {
            return reasonForSkipCrit ? true : !mc.player.method_24828() && mc.player.field_6017 > 0.0F;
         }
      }
   }
}
