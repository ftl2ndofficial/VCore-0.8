package vcore.gui.clickui.impl;

import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.MathHelper;
import vcore.Vcore;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;

public class SliderElement extends AbstractElement {
   private static final float TRACK_SIDE_PADDING = 6.0F;
   private static final float TRACK_HEIGHT = 2.0F;
   private static final float THUMB_SIZE = 5.5F;
   private static final float THUMB_HIT_PADDING = 2.0F;
   private static final float VALUE_HIT_PADDING_X = 2.0F;
   private static final float VALUE_HIT_PADDING_Y = 2.0F;
   private final float min;
   private final float max;
   private float animation;
   private boolean dragging;
   private boolean listening;
   public String Stringnumber = "";

   public SliderElement(Setting setting) {
      super(setting);
      this.min = ((Number)setting.getMin()).floatValue();
      this.max = ((Number)setting.getMax()).floatValue();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.animation = Render2DEngine.scrollAnimate(this.animation, (((Number)this.setting.getValue()).floatValue() - this.min) / (this.max - this.min), 0.4F);
      MatrixStack matrixStack = context.getMatrices();
      this.drawGroupGuide(context, this.height);
      String inputValue = this.getInputValue();
      if (!this.dragging) {
         String displayValue = this.setting.getDisplayValue();
         FontRenderers.sf_medium_mini.drawString(matrixStack, this.setting.getName(), this.getSettingNameX(), this.y + 4.0F, new Color(-1).getRGB());
         FontRenderers.sf_medium_mini.drawString(matrixStack, inputValue, (int)this.getValueTextX(inputValue), this.y + 5.0F, new Color(-1).getRGB());
      } else {
         if (this.animation > 0.2F) {
            FontRenderers.sf_medium_mini.drawString(matrixStack, this.setting.getMinDisplay(), this.x + 6.0F, this.y + 4.0F, new Color(-1).getRGB());
         }

         if (this.animation < 0.8F) {
            FontRenderers.sf_medium_mini
               .drawString(
                  matrixStack,
                  this.setting.getMaxDisplay(),
                  this.x + this.width - FontRenderers.sf_medium_mini.getStringWidth(this.setting.getMaxDisplay()) - 6.0F,
                  this.y + 4.0F,
                  new Color(-1).getRGB()
               );
         }

         String displayValue = this.setting.getDisplayValue();
         FontRenderers.sf_medium_mini
            .drawString(
               matrixStack,
               inputValue,
               this.animation > 0.2F
                  ? (
                     this.animation < 0.8F
                        ? this.x + 6.0F + (this.width - 14.0F) * this.animation - FontRenderers.sf_medium_mini.getStringWidth(displayValue) / 2.0F
                        : this.x + this.width - FontRenderers.sf_medium_mini.getStringWidth(this.setting.getMaxDisplay()) - 6.0F
                  )
                  : this.x + 6.0F,
               this.y + 4.0F,
               new Color(-1).getRGB()
            );
      }

      float trackX = this.getTrackX();
      float trackY = this.getTrackY();
      float trackWidth = this.getTrackWidth();
      float fillWidth = trackWidth * this.animation;
      float thumbX = this.getThumbX();
      float thumbY = this.getThumbY();
      Render2DEngine.drawRect(matrixStack, trackX, trackY, trackWidth, 2.0F, new Color(687865855, true));
      Render2DEngine.draw2DGradientRect(
         matrixStack,
         trackX,
         trackY,
         trackX + fillWidth,
         trackY + 2.0F,
         HudEditor.getColor(180),
         HudEditor.getColor(180),
         HudEditor.getColor(0),
         HudEditor.getColor(0)
      );
      Render2DEngine.drawBlurredShadow(matrixStack, thumbX, thumbY, 5.5F, 5.5F, 5, new Color(570425344, true));
      Render2DEngine.drawRound(matrixStack, thumbX, thumbY, 5.5F, 5.5F, 2.75F, new Color(-1250068));
      this.animation = MathUtility.clamp(this.animation, 0.0F, 1.0F);
      if (this.dragging) {
         this.setValue(mouseX);
      }
   }

   private float getTrackX() {
      return this.x + 6.0F + this.getGroupIndent();
   }

   private float getTrackY() {
      return this.y + this.height - 6.0F;
   }

   private float getTrackWidth() {
      return this.width - 12.0F - this.getGroupIndent();
   }

   private float getThumbX() {
      return this.getTrackX() + (this.getTrackWidth() - 5.5F) * this.animation;
   }

   private float getThumbY() {
      return this.getTrackY() - 1.75F;
   }

   private boolean isSliderHovered(int mouseX, int mouseY) {
      return Render2DEngine.isHovered(mouseX, mouseY, this.getTrackX(), this.getThumbY() - 2.0F, this.getTrackWidth(), 9.5);
   }

   private String getInputValue() {
      return this.listening ? (Objects.equals(this.Stringnumber, "") ? "..." : this.Stringnumber) : this.setting.getDisplayValue();
   }

   private float getValueTextX(String valueText) {
      return this.x + this.width - 6.0F - FontRenderers.sf_medium_mini.getStringWidth(valueText);
   }

   private boolean isValueHovered(int mouseX, int mouseY) {
      String valueText = this.getInputValue();
      float valueX = this.getValueTextX(valueText);
      float valueWidth = FontRenderers.sf_medium_mini.getStringWidth(valueText);
      float valueHeight = FontRenderers.sf_medium_mini.getFontHeight(valueText);
      return Render2DEngine.isHovered(mouseX, mouseY, valueX - 2.0F, this.y + 3.0F - 2.0F, valueWidth + 4.0F, valueHeight + 4.0F);
   }

   private void setValue(int mouseX) {
      float value = Render2DEngine.interpolateFloat(
         ((Number)this.setting.getMin()).floatValue(),
         ((Number)this.setting.getMax()).floatValue(),
         MathHelper.clamp((mouseX - (this.getTrackX() + 2.75F)) / (this.getTrackWidth() - 5.5F), 0.0, 1.0)
      );
      if (this.setting.getValue() instanceof Float) {
         this.setting.setValue(this.setting.hasStep() ? value : MathUtility.round2(value));
      } else if (this.setting.getValue() instanceof Integer) {
         this.setting.setValue((int)value);
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (button == 0) {
         if (this.isSliderHovered(mouseX, mouseY)) {
            this.listening = false;
            this.dragging = true;
            this.setValue(mouseX);
         }
      } else if (button == 1 && this.isValueHovered(mouseX, mouseY)) {
         this.Stringnumber = "";
         this.listening = true;
      }

      if (this.listening) {
         Vcore.currentKeyListener = Vcore.KeyListening.Sliders;
      }

      super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int button) {
      this.dragging = false;
   }

   @Override
   public void keyTyped(int keyCode) {
      if (Vcore.currentKeyListener == Vcore.KeyListening.Sliders) {
         if (this.listening) {
            switch (keyCode) {
               case 256:
                  this.listening = false;
                  this.Stringnumber = "";
                  return;
               case 257:
                  try {
                     this.searchNumber();
                  } catch (Exception e) {
                     this.Stringnumber = "";
                     this.listening = false;
                  }

                  return;
               case 258:
               case 260:
               default:
                  break;
               case 259:
                  this.Stringnumber = removeLastChar(this.Stringnumber);
                  return;
               case 261:
                  this.Stringnumber = "";
                  this.listening = false;
                  return;
            }
         }
      }
   }

   @Override
   public void charTyped(char key, int keyCode) {
      if (this.listening && Vcore.currentKeyListener == Vcore.KeyListening.Sliders) {
         if (StringHelper.isValidChar(key)) {
            String k = key == '-' ? "-" : ".";

            try {
               k = String.valueOf(Integer.parseInt(String.valueOf(key)));
            } catch (Exception var5) {
            }

            this.Stringnumber = this.Stringnumber + k;
         }
      }
   }

   public static String removeLastChar(String str) {
      String output = "";
      if (str != null && !str.isEmpty()) {
         output = str.substring(0, str.length() - 1);
      }

      return output;
   }

   private void searchNumber() {
      if (this.setting.getValue() instanceof Float) {
         this.setting.setValue(Float.valueOf(this.Stringnumber));
         this.Stringnumber = "";
         this.listening = false;
      } else if (this.setting.getValue() instanceof Integer) {
         this.setting.setValue(Integer.valueOf(this.Stringnumber));
         this.Stringnumber = "";
         this.listening = false;
      }
   }
}
