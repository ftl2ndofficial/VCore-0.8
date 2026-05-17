package vcore.gui.clickui.impl;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import vcore.Vcore;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;

public class ColorPickerElement extends AbstractElement {
   private float hue;
   private float saturation;
   private float brightness;
   private int alpha;
   private boolean afocused;
   private boolean hfocused;
   private boolean sbfocused;
   private float spos;
   private float bpos;
   private float hpos;
   private float apos;
   private Color prevColor;
   private boolean firstInit;
   private boolean extended;
   private final Setting<?> colorSetting;

   public ColorSetting getColorSetting() {
      return (ColorSetting)this.colorSetting.getValue();
   }

   public ColorPickerElement(Setting<?> setting) {
      super(setting);
      this.colorSetting = setting;
      this.prevColor = this.getColorSetting().getColorObject();
      this.updatePos();
      this.firstInit = true;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrixStack = context.getMatrices();
      this.drawGroupGuide(context, this.height);
      float swatchX = this.x + this.width - 22.0F;
      float swatchY = this.y + 5.0F;
      float swatchW = 14.0F;
      float swatchH = 7.0F;
      boolean colorHovered = Render2DEngine.isHovered(mouseX, mouseY, swatchX, swatchY, swatchW, swatchH);
      FontRenderers.sf_medium_mini.drawString(matrixStack, this.setting.getName(), this.getSettingNameX(), this.y + 8.0F, new Color(-1).getRGB());
      Render2DEngine.drawBlurredShadow(matrixStack, swatchX, swatchY, swatchW, swatchH, colorHovered ? 6 : 10, this.getColorSetting().getColorObject());
      if (colorHovered) {
         Render2DEngine.drawRound(matrixStack, swatchX - 0.5F, swatchY - 0.5F, swatchW + 1.0F, swatchH + 1.0F, 1.0F, this.getColorSetting().getColorObject());
      } else {
         Render2DEngine.drawRound(matrixStack, swatchX, swatchY, swatchW, swatchH, 1.0F, this.getColorSetting().getColorObject());
      }

      if (this.extended) {
         boolean rainbowHovered = Render2DEngine.isHovered(mouseX, mouseY, this.x + 36.0F, this.y + 54.0F, 24.0, 7.0);
         boolean copyHovered = Render2DEngine.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 54.0F, 24.0, 7.0);
         boolean pasteHovered = Render2DEngine.isHovered(mouseX, mouseY, this.x + 63.0F, this.y + 54.0F, 24.0, 7.0);
         boolean dark = Render2DEngine.isDark(Vcore.copy_color);
         boolean dark2 = Render2DEngine.isDark(this.getColorSetting().getColorObject());
         Render2DEngine.drawRect(matrixStack, this.x + 9.0F, this.y + 54.0F, 24.0F, 7.0F, new Color(4342338));
         FontRenderers.sf_medium_mini
            .drawString(matrixStack, "Copy", this.x + 13.0F, this.y + 56.5F, copyHovered ? new Color(-1543503873, true).getRGB() : Color.WHITE.getRGB());
         Render2DEngine.drawRect(
            matrixStack,
            this.x + 36.0F,
            this.y + 54.0F,
            24.0F,
            7.0F,
            this.getColorSetting().isRainbow() ? this.getColorSetting().getColorObject() : new Color(4342338)
         );
         FontRenderers.sf_medium_mini
            .drawString(
               matrixStack,
               "RB",
               this.x + 44.0F,
               this.y + 56.5F,
               rainbowHovered ? new Color(-1543503873, true).getRGB() : (dark2 ? Color.WHITE.getRGB() : Color.BLACK.getRGB())
            );
         Render2DEngine.drawRect(matrixStack, this.x + 63.0F, this.y + 54.0F, 24.0F, 7.0F, Vcore.copy_color);
         FontRenderers.sf_medium_mini
            .drawString(
               matrixStack,
               "Paste",
               this.x + 67.0F,
               this.y + 56.5F,
               pasteHovered ? new Color(-1543503873, true).getRGB() : (dark ? Color.WHITE.getRGB() : Color.BLACK.getRGB())
            );
         this.renderPicker(matrixStack, mouseX, mouseY, this.getColorSetting().getColorObject());
      }
   }

   @Override
   public float getHeight() {
      return this.extended ? 66.0F : 15.0F;
   }

   private void renderPicker(MatrixStack matrixStack, int mouseX, int mouseY, Color color) {
      boolean hasAlpha = this.getColorSetting().allowsAlpha();
      double cx = this.x + 6.0F;
      double cy = this.y + 16.0F;
      double cw = this.width - (hasAlpha ? 38 : 25);
      double ch = this.height - 30.0F;
      if (this.prevColor != this.getColorSetting().getColorObject()) {
         this.updatePos();
         this.prevColor = this.getColorSetting().getColorObject();
      }

      if (this.firstInit) {
         this.spos = (float)(cx + cw - (cw - cw * this.saturation));
         this.bpos = (float)(cy + (ch - ch * this.brightness));
         this.hpos = (float)(cy + (ch - 3.0 + (ch - 3.0) * this.hue));
         if (hasAlpha) {
            this.apos = (float)(cy + (ch - 3.0 - (ch - 3.0) * (this.alpha / 255.0F)));
         }

         this.firstInit = false;
      }

      this.spos = Render2DEngine.scrollAnimate(this.spos, (float)(cx + cw - (cw - cw * this.saturation)), 0.6F);
      this.bpos = Render2DEngine.scrollAnimate(this.bpos, (float)(cy + (ch - ch * this.brightness)), 0.6F);
      this.hpos = Render2DEngine.scrollAnimate(this.hpos, (float)(cy + (ch - 3.0 + (ch - 3.0) * this.hue)), 0.6F);
      if (hasAlpha) {
         this.apos = Render2DEngine.scrollAnimate(this.apos, (float)(cy + (ch - 3.0 - (ch - 3.0) * (this.alpha / 255.0F))), 0.6F);
      }

      Color colorA = Color.getHSBColor(this.hue, 0.0F, 1.0F);
      Color colorB = Color.getHSBColor(this.hue, 1.0F, 1.0F);
      Color colorC = new Color(0, 0, 0, 0);
      Color colorD = new Color(0, 0, 0);
      Render2DEngine.horizontalGradient(matrixStack, (float)cx + 2.0F, (float)cy, (float)(cx + cw), (float)(cy + ch), colorA, colorB);
      Render2DEngine.verticalGradient(matrixStack, (float)(cx + 2.0), (float)cy, (float)(cx + cw), (float)(cy + ch), colorC, colorD);

      for (float i = 1.0F; i < ch - 2.0; i++) {
         float curHue = (float)(1.0 / (ch / i));
         Render2DEngine.drawRect(matrixStack, (float)(cx + cw + 4.0), (float)(cy + i), 8.0F, 1.0F, Color.getHSBColor(curHue, 1.0F, 1.0F));
      }

      if (hasAlpha) {
         Render2DEngine.drawRect(matrixStack, (float)(cx + cw + 17.0), (float)(cy + 1.0), 8.0F, (float)(ch - 3.0), new Color(-1));
         Render2DEngine.verticalGradient(
            matrixStack,
            (float)(cx + cw + 17.0),
            (float)(cy + 0.8F),
            (float)(cx + cw + 25.0),
            (float)(cy + ch - 2.0),
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 255),
            new Color(0, 0, 0, 0)
         );
      }

      Render2DEngine.drawRect(matrixStack, (float)(cx + cw + 3.0), this.hpos + 0.5F, 10.0F, 1.0F, Color.WHITE);
      if (hasAlpha) {
         Render2DEngine.drawRect(matrixStack, (float)(cx + cw + 16.0), this.apos + 0.5F, 10.0F, 1.0F, Color.WHITE);
      }

      Render2DEngine.drawRound(matrixStack, this.spos - 1.5F, this.bpos - 1.5F, 3.0F, 3.0F, 1.5F, new Color(-1));
      Color value = Color.getHSBColor(this.hue, this.saturation, this.brightness);
      if (this.sbfocused) {
         this.saturation = (float)(MathUtility.clamp((float)(mouseX - cx), 0.0F, (float)cw) / cw);
         this.brightness = (float)((ch - MathUtility.clamp((float)(mouseY - cy), 0.0F, (float)ch)) / ch);
         value = Color.getHSBColor(this.hue, this.saturation, this.brightness);
         this.setColor(new Color(value.getRed(), value.getGreen(), value.getBlue(), this.alpha));
      }

      if (this.hfocused) {
         this.hue = (float)(-((ch - MathUtility.clamp((float)(mouseY - cy), 0.0F, (float)ch)) / ch));
         value = Color.getHSBColor(this.hue, this.saturation, this.brightness);
         this.setColor(new Color(value.getRed(), value.getGreen(), value.getBlue(), this.alpha));
      }

      if (hasAlpha && this.afocused) {
         this.alpha = (int)((ch - MathUtility.clamp((float)(mouseY - cy), 0.0F, (float)ch)) / ch * 255.0);
         this.setColor(new Color(value.getRed(), value.getGreen(), value.getBlue(), this.alpha));
      }
   }

   private void updatePos() {
      float[] hsb = Color.RGBtoHSB(
         this.getColorSetting().getColorObject().getRed(),
         this.getColorSetting().getColorObject().getGreen(),
         this.getColorSetting().getColorObject().getBlue(),
         null
      );
      this.hue = -1.0F + hsb[0];
      this.saturation = hsb[1];
      this.brightness = hsb[2];
      this.alpha = this.getColorSetting().getAlpha();
   }

   private void setColor(Color color) {
      this.getColorSetting().setColor(color.getRGB());
      this.prevColor = color;
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      boolean hasAlpha = this.getColorSetting().allowsAlpha();
      double cx = this.x + 6.0F;
      double cy = this.y + 16.0F;
      double cw = this.width - (hasAlpha ? 38 : 25);
      double ch = this.height - 30.0F;
      boolean rainbowHovered = Render2DEngine.isHovered(mouseX, mouseY, this.x + 36.0F, this.y + 54.0F, 24.0, 7.0);
      boolean copyHovered = Render2DEngine.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 54.0F, 24.0, 7.0);
      boolean pasteHovered = Render2DEngine.isHovered(mouseX, mouseY, this.x + 63.0F, this.y + 54.0F, 24.0, 7.0);
      float swatchX = this.x + this.width - 22.0F;
      float swatchY = this.y + 5.0F;
      float swatchW = 14.0F;
      float swatchH = 7.0F;
      boolean colorHovered = Render2DEngine.isHovered(mouseX, mouseY, swatchX, swatchY, swatchW, swatchH);
      if (colorHovered) {
         this.extended = !this.extended;
      }

      if (this.extended) {
         if (hasAlpha && Render2DEngine.isHovered(mouseX, mouseY, cx + cw + 17.0, cy, 8.0, ch) && button == 0) {
            this.afocused = true;
         } else if (Render2DEngine.isHovered(mouseX, mouseY, cx + cw + 4.0, cy, 8.0, ch) && button == 0) {
            this.hfocused = true;
         } else if (Render2DEngine.isHovered(mouseX, mouseY, cx, cy, cw, ch) && button == 0) {
            this.sbfocused = true;
         } else if (rainbowHovered && button == 0) {
            this.getColorSetting().setRainbow(!this.getColorSetting().isRainbow());
         } else if (copyHovered) {
            Vcore.copy_color = this.getColorSetting().getColorObject();
         } else if (pasteHovered) {
            this.getColorSetting().setColor(Vcore.copy_color.getRGB());
         }

         super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int button) {
      this.hfocused = false;
      this.afocused = false;
      this.sbfocused = false;
   }

   @Override
   public void onClose() {
      this.hfocused = false;
      this.afocused = false;
      this.sbfocused = false;
   }
}
