package vcore.setting.impl;

import java.awt.Color;
import org.jetbrains.annotations.NotNull;
import vcore.utility.render.Render2DEngine;

public final class ColorSetting {
   private int color;
   private final int defaultColor;
   private boolean rainbow;
   private boolean allowAlpha = true;

   public ColorSetting(@NotNull Color color) {
      this(color.getRGB());
   }

   public ColorSetting(int color) {
      this.color = color;
      this.defaultColor = color;
   }

   @NotNull
   public ColorSetting withAlpha(int alpha) {
      int rawColor = this.getRawColor();
      int red = rawColor >> 16 & 0xFF;
      int green = rawColor >> 8 & 0xFF;
      int blue = rawColor & 0xFF;
      ColorSetting setting = new ColorSetting((alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
      setting.allowAlpha = this.allowAlpha;
      if (!this.allowAlpha) {
         setting.withoutAlpha();
      }

      return setting;
   }

   @NotNull
   public ColorSetting withoutAlpha() {
      this.allowAlpha = false;
      this.color = 0xFF000000 | this.color & 16777215;
      return this;
   }

   public int getColor() {
      return this.rainbow ? Render2DEngine.rainbow(10, 1, 1.0F, 1.0F, 1.0F).getRGB() : this.color;
   }

   public void setColor(int color) {
      this.color = this.allowAlpha ? color : 0xFF000000 | color & 16777215;
   }

   public int getRed() {
      return this.rainbow ? Render2DEngine.rainbow(10, 1, 1.0F, 1.0F, 1.0F).getRed() : this.color >> 16 & 0xFF;
   }

   public int getGreen() {
      return this.rainbow ? Render2DEngine.rainbow(10, 1, 1.0F, 1.0F, 1.0F).getGreen() : this.color >> 8 & 0xFF;
   }

   public int getBlue() {
      return this.rainbow ? Render2DEngine.rainbow(10, 1, 1.0F, 1.0F, 1.0F).getBlue() : this.color & 0xFF;
   }

   public float getGlRed() {
      return this.getRed() / 255.0F;
   }

   public float getGlBlue() {
      return this.getBlue() / 255.0F;
   }

   public float getGlGreen() {
      return this.getGreen() / 255.0F;
   }

   public float getGlAlpha() {
      return this.getAlpha() / 255.0F;
   }

   public int getAlpha() {
      return this.allowAlpha ? this.color >> 24 & 0xFF : 255;
   }

   @NotNull
   public Color getColorObject() {
      return new Color(this.getRawColor(), true);
   }

   public int getRawColor() {
      int rawColor = this.rainbow ? Render2DEngine.rainbow(10, 1, 1.0F, 1.0F, 1.0F).getRGB() : this.color;
      return this.allowAlpha ? rawColor : 0xFF000000 | rawColor & 16777215;
   }

   public boolean isRainbow() {
      return this.rainbow;
   }

   public void setRainbow(boolean rainbow) {
      this.rainbow = rainbow;
   }

   public void setDefault() {
      this.setColor(this.defaultColor);
   }

   public boolean allowsAlpha() {
      return this.allowAlpha;
   }
}
