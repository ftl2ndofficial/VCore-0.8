package vcore.features.modules.render;

import com.google.common.collect.Lists;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AntiBot;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.animation.AnimationUtility;

public class Arrows extends Module {
   private static final boolean GLOW = false;
   private static final float HEIGHT = 1.2F;
   private static final boolean DOWN = true;
   private static final float DOWN_HEIGHT = 3.63F;
   private static final float TRACER_WIDTH = 0.0F;
   private static final int TRACER_RADIUS = 80;
   private static final int PITCH_LOCK = 42;
   private static final Color FRIEND_COLOR = new Color(59392);
   private static final Color TRACER_COLOR = new Color(16711680);
   private float smoothYaw = 0.0F;

   public Arrows() {
      super("Arrows", "Directional arrows to other players.", Module.Category.RENDER);
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (!fullNullCheck()) {
         float middleW = ModuleManager.crosshair.getAnimatedPosX();
         float middleH = ModuleManager.crosshair.getAnimatedPosY();
         int color = 0;
         context.getMatrices().push();
         context.getMatrices().translate(middleW, middleH, 0.0F);
         context.getMatrices()
            .multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F / Math.abs(90.0F / MathUtility.clamp(mc.player.method_36455(), 42.0F, 90.0F)) - 102.0F));
         context.getMatrices().translate(-middleW, -middleH, 0.0F);
         this.smoothYaw = AnimationUtility.fast(this.smoothYaw, mc.player.method_36454(), 13.0F);

         for (PlayerEntity e : Lists.newArrayList(mc.world.method_18456())) {
            if (e != mc.player && (!ModuleManager.antiBot.isEnabled() || ModuleManager.antiBot.mode.getValue() != AntiBot.Mode.Matrix || !AntiBot.isBot(e))) {
               context.getMatrices().push();
               float yaw = this.getRotations(e) - this.smoothYaw;
               context.getMatrices().translate(middleW, middleH, 0.0F);
               context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
               context.getMatrices().translate(-middleW, -middleH, 0.0F);
               if (Managers.FRIEND.isFriend(e)) {
                  color = FRIEND_COLOR.getRGB();
               } else {
                  color = TRACER_COLOR.getRGB();
               }

               Render2DEngine.drawTracerPointer(context.getMatrices(), middleW, middleH - 80.0F, 6.0F, 0.0F, 3.63F, true, false, color);
               context.getMatrices().translate(middleW, middleH, 0.0F);
               context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-yaw));
               context.getMatrices().translate(-middleW, -middleH, 0.0F);
               context.getMatrices().pop();
            }
         }

         context.getMatrices().pop();
      }
   }

   private float getRotations(PlayerEntity entity) {
      if (mc.player == null) {
         return 0.0F;
      }

      double x = this.interp(entity.method_19538().x, entity.field_6014) - this.interp(mc.player.method_19538().x, mc.player.field_6014);
      double z = this.interp(entity.method_19538().z, entity.field_5969) - this.interp(mc.player.method_19538().z, mc.player.field_5969);
      return (float)(-(Math.atan2(x, z) * (180.0 / Math.PI)));
   }

   private double interp(double current, double previous) {
      return previous + (current - previous) * Render3DEngine.getTickDelta();
   }
}
