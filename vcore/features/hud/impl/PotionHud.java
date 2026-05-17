package vcore.features.hud.impl;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Formatting;
import vcore.features.hud.HudElement;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.animation.AnimationUtility;

public class PotionHud extends HudElement {
   private static final float MIN_WIDTH = 50.0F;
   private static final float ICON_X = 2.0F;
   private static final float ICON_SIZE = 8.0F;
   private static final float TEXT_X = 12.0F;
   private static final float SEPARATOR_LEFT_GAP = 4.0F;
   private static final float SEPARATOR_RIGHT_GAP = 4.0F;
   private static final float RIGHT_PADDING = 3.0F;
   private static final int WARNING_DURATION_TICKS = 200;
   private static final long WARNING_BLINK_PERIOD_MS = 500L;
   private static final Color WARNING_DURATION_COLOR = new Color(255, 70, 70);
   private float vAnimation;
   private float hAnimation;

   public PotionHud(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("Potions", 100, 100, position, activeElementsSupplier);
   }

   public static String getDuration(StatusEffectInstance pe) {
      if (pe.isInfinite()) {
         return "*:*";
      }

      int var1 = pe.getDuration();
      int mins = var1 / 1200;
      String sec = String.format("%02d", var1 % 1200 / 20);
      return mins + ":" + sec;
   }

   private int getDurationColor(StatusEffectInstance potionEffect) {
      Color baseColor = HudEditor.getTextColor();
      if (!potionEffect.isInfinite() && potionEffect.getDuration() <= 200) {
         double blink = Math.sin(System.currentTimeMillis() * Math.PI * 2.0 / 500.0);
         float progress = (float)((blink + 1.0) * 0.5);
         return Render2DEngine.interpolateColorC(baseColor, WARNING_DURATION_COLOR, progress).getRGB();
      } else {
         return baseColor.getRGB();
      }
   }

   @Override
   public void render(DrawContext context) {
      super.render(context);
      int y_offset1 = 0;
      float max_width = 50.0F;
      float maxNameWidth = 0.0F;
      float maxDurationWidth = 0.0F;

      for (StatusEffectInstance potionEffect : mc.player.method_6026()) {
         StatusEffect potion = (StatusEffect)potionEffect.getEffectType().value();
         if (y_offset1 == 0) {
            y_offset1 += 4;
         }

         y_offset1 += 9;
         float nameWidth = FontRenderers.sf_bold_mini.getStringWidth(potion.getName().getString() + " " + (potionEffect.getAmplifier() + 1));
         float timeWidth = FontRenderers.sf_bold_mini.getStringWidth(getDuration(potionEffect));
         maxNameWidth = Math.max(maxNameWidth, nameWidth);
         maxDurationWidth = Math.max(maxDurationWidth, timeWidth);
      }

      if (y_offset1 > 0) {
         max_width = Math.max(max_width, 12.0F + maxNameWidth + 4.0F + 4.0F + maxDurationWidth + 3.0F);
      }

      this.vAnimation = AnimationUtility.fast(this.vAnimation, 14 + y_offset1, 15.0F);
      this.hAnimation = AnimationUtility.fast(this.hAnimation, max_width, 15.0F);
      Render2DEngine.drawHudBase(context.getMatrices(), this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation, 4.0F);
      if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
         FontRenderers.sf_bold
            .drawCenteredString(context.getMatrices(), "Potions", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4.0F, HudEditor.getTextColor());
      } else {
         FontRenderers.sf_bold.drawGradientCenteredString(context.getMatrices(), "Potions", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4.0F, 10);
      }

      if (y_offset1 > 0) {
         if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRectDumbWay(
               context.getMatrices(),
               this.getPosX() + 4.0F,
               this.getPosY() + 13.0F,
               this.getPosX() + this.getWidth() - 4.0F,
               this.getPosY() + 13.5F,
               new Color(1426063359, true)
            );
         } else {
            Render2DEngine.horizontalGradient(
               context.getMatrices(),
               this.getPosX() + 2.0F,
               this.getPosY() + 13.7F,
               this.getPosX() + 2.0F + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 14.0F,
               Render2DEngine.injectAlpha(HudEditor.getTextColor(), 0),
               HudEditor.getTextColor()
            );
            Render2DEngine.horizontalGradient(
               context.getMatrices(),
               this.getPosX() + 2.0F + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 13.7F,
               this.getPosX() + 2.0F + this.hAnimation - 4.0F,
               this.getPosY() + 14.0F,
               HudEditor.getTextColor(),
               Render2DEngine.injectAlpha(HudEditor.getTextColor(), 0)
            );
         }
      }

      Render2DEngine.addWindow(context.getMatrices(), this.getPosX(), this.getPosY(), this.getPosX() + this.hAnimation, this.getPosY() + this.vAnimation, 1.0);
      int y_offset = 0;

      for (StatusEffectInstance potionEffect : mc.player.method_6026()) {
         StatusEffect potion = (StatusEffect)potionEffect.getEffectType().value();
         String duration = getDuration(potionEffect);
         float durationRightX = this.getPosX() + this.hAnimation - 3.0F;
         float durationX = durationRightX - FontRenderers.sf_bold_mini.getStringWidth(duration);
         float separatorX = durationRightX - maxDurationWidth - 4.0F;
         context.getMatrices().push();
         context.getMatrices().translate(this.getPosX() + 2.0F, this.getPosY() + 16.0F + y_offset, 0.0F);
         context.drawSprite(0, 0, 0, 8, 8, mc.getStatusEffectSpriteManager().getSprite(potionEffect.getEffectType()));
         context.getMatrices().pop();
         FontRenderers.sf_bold_mini
            .drawString(
               context.getMatrices(),
               potion.getName().getString() + " " + Formatting.RED + (potionEffect.getAmplifier() + 1),
               this.getPosX() + 12.0F,
               this.getPosY() + 19.0F + y_offset,
               HudEditor.getTextColor().getRGB()
            );
         FontRenderers.sf_bold_mini
            .drawString(context.getMatrices(), duration, durationX, this.getPosY() + 19.0F + y_offset, this.getDurationColor(potionEffect));
         Render2DEngine.drawRect(context.getMatrices(), separatorX, this.getPosY() + 17.0F + y_offset, 0.5F, 8.0F, new Color(1157627903, true));
         y_offset += 9;
      }

      Render2DEngine.popWindow();
      this.setBounds(this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation);
   }
}
