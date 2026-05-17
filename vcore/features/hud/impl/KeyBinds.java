package vcore.features.hud.impl;

import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.hud.HudElement;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.Bind;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.animation.AnimationUtility;

public class KeyBinds extends HudElement {
   private static final boolean ONLY_ENABLED = true;
   private static final int ON_COLOR = -1;
   private static final int OFF_COLOR = -1;
   private float vAnimation;
   private float hAnimation;

   public KeyBinds(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("KeyBinds", 100, 100, position, activeElementsSupplier);
   }

   @Override
   public void render(DrawContext context) {
      super.render(context);
      int y_offset1 = 0;
      float max_width = 50.0F;
      float maxBindWidth = 0.0F;
      float pointerX = 0.0F;

      for (Module feature : Managers.MODULE.modules) {
         if (!feature.isDisabled() && !Objects.equals(feature.getBind().getBind(), "None") && feature != ModuleManager.clickGui) {
            if (y_offset1 == 0) {
               y_offset1 += 4;
            }

            y_offset1 += 9;
            float nameWidth = FontRenderers.sf_bold_mini.getStringWidth(feature.getName());
            float bindWidth = FontRenderers.sf_bold_mini.getStringWidth(getShortKeyName(feature));
            if (bindWidth > maxBindWidth) {
               maxBindWidth = bindWidth;
            }

            if (nameWidth > pointerX) {
               pointerX = nameWidth;
            }
         }
      }

      float px = this.getPosX() + 10.0F + pointerX;
      max_width = Math.max(20.0F + pointerX + maxBindWidth, 50.0F);
      this.vAnimation = AnimationUtility.fast(this.vAnimation, 14 + y_offset1, 15.0F);
      this.hAnimation = AnimationUtility.fast(this.hAnimation, max_width, 15.0F);
      Render2DEngine.drawHudBase(context.getMatrices(), this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation, 4.0F);
      if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
         FontRenderers.sf_bold
            .drawCenteredString(context.getMatrices(), "KeyBinds", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4.0F, HudEditor.getTextColor());
      } else {
         FontRenderers.sf_bold
            .drawGradientCenteredString(context.getMatrices(), "KeyBinds", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4.0F, 10);
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

      for (Module feature : Managers.MODULE.modules) {
         if (!feature.isDisabled() && !Objects.equals(feature.getBind().getBind(), "None") && feature != ModuleManager.clickGui) {
            FontRenderers.sf_bold_mini
               .drawString(context.getMatrices(), feature.getName(), this.getPosX() + 5.0F, this.getPosY() + 19.0F + y_offset, feature.isOn() ? -1 : -1);
            FontRenderers.sf_bold_mini
               .drawCenteredString(
                  context.getMatrices(),
                  getShortKeyName(feature),
                  px + (this.getPosX() + max_width - px) / 2.0F,
                  this.getPosY() + 19.0F + y_offset,
                  feature.isOn() ? -1 : -1
               );
            Render2DEngine.drawRect(context.getMatrices(), px, this.getPosY() + 17.0F + y_offset, 0.5F, 8.0F, new Color(1157627903, true));
            y_offset += 9;
         }
      }

      Render2DEngine.popWindow();
      this.setBounds(this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation);
   }

   @NotNull
   public static String getShortKeyName(Module feature) {
      return getShortKeyName(feature.getBind());
   }

   @NotNull
   public static String getShortKeyName(Bind bind) {
      String sbind = bind.getBind();

      return switch (sbind) {
         case "LEFT_CONTROL" -> "LCtrl";
         case "RIGHT_CONTROL" -> "RCtrl";
         case "LEFT_SHIFT" -> "LShift";
         case "RIGHT_SHIFT" -> "RShift";
         case "LEFT_ALT" -> "LAlt";
         case "RIGHT_ALT" -> "RAlt";
         case "CAPS_LOCK" -> "Caps";
         case "BACKSPACE" -> "BackSpace";
         case "INSERT" -> "Ins";
         case "PAGE_UP" -> "PgUp";
         case "PAGE_DOWN" -> "PgDown";
         case "ENTER" -> "Enter";
         case "SPACE" -> "Space";
         case "UP" -> "Up";
         case "DOWN" -> "Down";
         case "LEFT" -> "Left";
         case "RIGHT" -> "Right";
         default -> sbind.toUpperCase();
      };
   }
}
