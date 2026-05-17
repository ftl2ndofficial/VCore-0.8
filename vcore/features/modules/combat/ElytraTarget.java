package vcore.features.modules.combat;

import java.awt.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.Timer;
import vcore.utility.render.Render3DEngine;

public final class ElytraTarget extends Module {
   private final Setting<Boolean> prediction = new Setting<>("Prediction", true);
   private final Setting<Boolean> visual = new Setting<>("Visual", true, v -> this.prediction.getValue());
   private final Setting<ColorSetting> color = new Setting<>(
      "Color", new ColorSetting(new Color(16752640)), v -> this.visual.getValue() && this.prediction.getValue()
   );
   public final Setting<ElytraTarget.Mode> mode = new Setting<>("Mode", ElytraTarget.Mode.Auto);
   private final Setting<Float> predictRange = new Setting<>("Predict Range", 2.6F, 1.5F, 6.0F, v -> this.mode.is(ElytraTarget.Mode.Default));
   private final Setting<Float> predictMax = new Setting<>("Predict Max", 10.0F, 1.5F, 10.0F, v -> this.mode.is(ElytraTarget.Mode.Auto));
   public boolean status = true;
   public boolean disableForward = false;
   private final Timer hurtTimer = new Timer();
   private double bps;
   public double scale;

   public ElytraTarget() {
      super("ElytraTarget", "Predicts elytra target movement for Aura.", Module.Category.COMBAT);
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity target = null;
         if (Aura.target instanceof LivingEntity livingTarget) {
            target = livingTarget;
         }

         this.status = this.prediction.getValue();
         if (target != null && this.shouldTarget(target)) {
            this.bps = this.getBps(target);
            this.scale = Math.sqrt(this.bps) / 2.0;
            this.scale = Math.min(this.scale, this.predictMax.getValue().floatValue());
         } else {
            this.bps = 0.0;
         }

         if (target == null) {
            this.disableForward = false;
         } else {
            if (mc.player.field_6235 > 0) {
               this.disableForward = true;
               this.hurtTimer.reset();
            }

            if (this.hurtTimer.passedMs(500L)) {
               this.disableForward = false;
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (mc.player != null && mc.world != null && this.visual.getValue()) {
         if (Aura.target instanceof LivingEntity livingTarget) {
            if (this.shouldTarget(livingTarget)) {
               Vec3d predicted = this.getPredictedVec(livingTarget);
               if (predicted != null) {
                  double size = 0.2;
                  Box box = new Box(predicted.x - size, predicted.y - size, predicted.z - size, predicted.x + size, predicted.y + size, predicted.z + size);
                  Color base = this.color.getValue().getColorObject();
                  Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), 120);
                  Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
                  Render3DEngine.drawFilledBox(stack, box, fill);
                  Render3DEngine.drawBoxOutline(box, outline, 1.0F);
               }
            }
         }
      }
   }

   public double getPrediction(LivingEntity target) {
      double scaleValue = this.predictRange.getValue().floatValue();
      if (this.mode.is(ElytraTarget.Mode.Auto)) {
         scaleValue = this.scale;
      }

      return scaleValue;
   }

   public boolean shouldTarget(LivingEntity livingEntity) {
      if (this.isEnabled() && livingEntity != null && !this.disableForward && mc.player != null) {
         boolean isTargetValid = livingEntity.isFallFlying();
         return this.status && mc.player.method_6128() && isTargetValid;
      } else {
         return false;
      }
   }

   public double getBps(LivingEntity entity) {
      if (entity == null) {
         return 0.0;
      }

      double dx = entity.method_18798().x;
      double dz = entity.method_18798().z;
      return Math.hypot(dx, dz) * 20.0;
   }

   private Vec3d getPredictedVec(LivingEntity target) {
      if (mc.player != null && target != null) {
         Vec3d playerEye = mc.player.method_33571();
         Box box = target.method_5829();
         Vec3d basePos = new Vec3d(
            MathHelper.clamp(playerEye.x, box.minX, box.maxX),
            MathHelper.clamp(playerEye.y, box.minY, box.maxY),
            MathHelper.clamp(playerEye.z, box.minZ, box.maxZ)
         );
         if (this.shouldTarget(target) && this.getBps(target) >= 20.0) {
            double scaleValue = this.getPrediction(target);
            basePos = basePos.add(target.method_18798().multiply(scaleValue));
         }

         return basePos;
      } else {
         return null;
      }
   }

   public enum Mode {
      Auto,
      Default;
   }
}
