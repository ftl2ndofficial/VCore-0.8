package vcore.features.modules.player;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import vcore.features.modules.Module;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;

public class TapeMouse extends Module {
   private final Setting<Integer> delay = new Setting<>("Delay", 600, 0, 10000);
   private final Setting<BooleanSettingGroup> randomize = new Setting<>("Randomize", new BooleanSettingGroup(false));
   private final Setting<Integer> randomizeValue = new Setting<>("Value", 600, 0, 10000).addToGroup(this.randomize);
   private final Setting<TapeMouse.Mode> mode = new Setting<>("Mode", TapeMouse.Mode.Left);
   private final Setting<Boolean> legit = new Setting<>("Legit", false, v -> this.mode.getValue() == TapeMouse.Mode.Left);
   private final Timer timer = new Timer();

   public TapeMouse() {
      super("TapeMouse", "Auto clicker.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (this.timer
         .every(
            (long)(
               this.delay.getValue().intValue()
                  + (this.randomize.getValue().isEnabled() ? MathUtility.random(0.0F, this.randomizeValue.getValue().intValue()) : 0.0F)
            )
         )) {
         if (this.mode.getValue() == TapeMouse.Mode.Left) {
            if (!this.legit.getValue()) {
               HitResult hr = mc.crosshairTarget;
               if (hr != null) {
                  if (hr instanceof EntityHitResult ehr && ehr.getEntity() != null) {
                     mc.interactionManager.attackEntity(mc.player, ehr.getEntity());
                     mc.player.method_6104(Hand.MAIN_HAND);
                  } else if (hr instanceof BlockHitResult bhr
                     && bhr.getBlockPos() != null
                     && bhr.getSide() != null
                     && !mc.world.method_22347(bhr.getBlockPos())) {
                     mc.interactionManager.attackBlock(bhr.getBlockPos(), bhr.getSide());
                     mc.player.method_6104(Hand.MAIN_HAND);
                  }
               }
            } else {
               ((IMinecraftClient)mc).idoAttack();
            }
         } else {
            ((IMinecraftClient)mc).idoItemUse();
         }
      }
   }

   private enum Mode {
      Right,
      Left;
   }
}
