package vcore.gui.clickui.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.AbstractButton;
import vcore.gui.clickui.ClickGUI;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

public class ThemeSelector extends AbstractButton {
   private boolean paletteOpen = false;
   private float barX;
   private float barWidth;
   private float paletteY;
   private float swatchHeight;
   private float themeMenuAnim = 0.0F;
   private float themeAlphaAnim = 0.0F;
   private static final float THEME_ANIM_SPEED = 0.2F;

   public boolean isPaletteOpen() {
      return this.paletteOpen;
   }

   public void setLayout(float buttonX, float buttonY, float size, float barX, float barWidth, float paletteY, float paletteHeight) {
      this.setX(buttonX);
      this.setY(buttonY);
      this.setWidth(size);
      this.setHeight(size);
      this.barX = barX;
      this.barWidth = barWidth;
      this.paletteY = paletteY;
      this.swatchHeight = paletteHeight;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      boolean hovered = Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      int hudAlpha = Math.round(255.0F * HudEditor.getAlpha());
      Color buttonBg = Render2DEngine.injectAlpha(new Color(25, 25, 28), hudAlpha);
      Render2DEngine.drawClickGuiRound(context.getMatrices(), this.x, this.y, this.width, this.height, this.width / 4.0F, buttonBg);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, HudEditor.getAlpha());
      context.drawTexture(
         TextureStorage.themeHudIcon, (int)(this.x + this.width / 2.0F - 6.0F), (int)(this.y + this.height / 2.0F - 6.0F), 0.0F, 0.0F, 12, 12, 12, 12
      );
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (hovered) {
         ClickGUI.anyHovered = true;
      }
   }

   public boolean shouldRenderPalette() {
      return this.paletteOpen || this.themeAlphaAnim > 0.01F;
   }

   public void renderPalette(DrawContext context, int mouseX, int mouseY, float delta, float px, float width, float py, float height) {
      float target = this.paletteOpen ? 1.0F : 0.0F;
      this.themeAlphaAnim = this.lerp(this.themeAlphaAnim, target, 0.15F);
      this.themeMenuAnim = this.lerp(this.themeMenuAnim, target, 0.2F);
      if (!(this.themeAlphaAnim < 0.01F) || this.paletteOpen) {
         float offsetY = (1.0F - this.themeMenuAnim) * 10.0F;
         float actualY = py + offsetY;
         int paletteAlpha = Math.round(255.0F * HudEditor.getAlpha() * this.themeAlphaAnim);
         Color bg = Render2DEngine.injectAlpha(new Color(20, 20, 22), paletteAlpha);
         Render2DEngine.drawClickGuiRound(context.getMatrices(), px, actualY, width, height, height / 2.0F, bg);
         List<HudEditor.Theme> palette = HudEditor.advancecolor;
         float swatchSize = Math.max(10.5F, height - 5.5F);
         float spacing = 5.0F;
         float totalWidth = palette.size() * (swatchSize + spacing) - spacing;
         float startX = px + (width - totalWidth) / 2.0F;
         float swatchY = actualY + (height - swatchSize) / 2.0F;
         float speed = 10.0F;

         for (int i = 0; i < palette.size(); i++) {
            float sx = startX + i * (swatchSize + spacing);
            HudEditor.Theme theme = palette.get(i);
            boolean swatchHovered = Render2DEngine.isHovered(mouseX, mouseY, sx, swatchY, swatchSize, swatchSize);
            if (swatchHovered) {
               Render2DEngine.drawRound(
                  context.getMatrices(),
                  sx - 1.0F,
                  swatchY - 1.0F,
                  swatchSize + 2.0F,
                  swatchSize + 2.0F,
                  (swatchSize + 2.0F) / 2.0F,
                  Render2DEngine.injectAlpha(Color.BLACK, Math.round(70.0F * HudEditor.getAlpha() * this.themeAlphaAnim))
               );
               ClickGUI.anyHovered = true;
            }

            Color tl = Render2DEngine.injectAlpha(this.animatedBlend(theme, speed, 0.0), paletteAlpha);
            Color tr = Render2DEngine.injectAlpha(this.animatedBlend(theme, speed, 90.0), paletteAlpha);
            Color br = Render2DEngine.injectAlpha(this.animatedBlend(theme, speed, 180.0), paletteAlpha);
            Color bl = Render2DEngine.injectAlpha(this.animatedBlend(theme, speed, 270.0), paletteAlpha);
            this.drawGradientCircle(context, sx, swatchY, swatchSize / 2.0F, tl, tr, bl, br);
         }
      }
   }

   private float lerp(float start, float end, float step) {
      return start + (end - start) * step;
   }

   public void drawGradientCircle(DrawContext context, float x, float y, float radius, Color tl, Color tr, Color bl, Color br) {
      MatrixStack matrices = context.getMatrices();
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      float cx = x + radius;
      float cy = y + radius;
      int cr = (tl.getRed() + tr.getRed() + bl.getRed() + br.getRed()) / 4;
      int cg = (tl.getGreen() + tr.getGreen() + bl.getGreen() + br.getGreen()) / 4;
      int cb = (tl.getBlue() + tr.getBlue() + bl.getBlue() + br.getBlue()) / 4;
      int ca = (tl.getAlpha() + tr.getAlpha() + bl.getAlpha() + br.getAlpha()) / 4;
      bufferBuilder.method_22918(matrix, cx, cy, 0.0F).color(cr, cg, cb, ca);

      for (int i = 0; i <= 360; i += 10) {
         double rad = Math.toRadians(i);
         float px = (float)(cx + Math.cos(rad) * radius);
         float py = (float)(cy + Math.sin(rad) * radius);
         float u = (float)(Math.cos(rad) + 1.0) / 2.0F;
         float v = (float)(Math.sin(rad) + 1.0) / 2.0F;
         Color cTop = this.interpolate(tl, tr, u);
         Color cBot = this.interpolate(bl, br, u);
         Color cFinal = this.interpolate(cTop, cBot, v);
         bufferBuilder.method_22918(matrix, px, py, 0.0F).color(cFinal.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   private Color interpolate(Color c1, Color c2, float ratio) {
      int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
      int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
      int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
      int a = (int)(c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * ratio);
      return new Color(r, g, b, a);
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      boolean buttonHover = Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      if (buttonHover) {
         this.paletteOpen = !this.paletteOpen;
      } else {
         List<HudEditor.Theme> palette = HudEditor.advancecolor;
         float swatchSize = Math.max(10.5F, this.swatchHeight - 5.5F);
         float spacing = 5.0F;
         float totalWidth = palette.size() * (swatchSize + spacing) - spacing;
         float startX = this.barX + (this.barWidth - totalWidth) / 2.0F;
         float swatchY = this.paletteY + (this.swatchHeight - swatchSize) / 2.0F;

         for (int i = 0; i < palette.size(); i++) {
            float sx = startX + i * (swatchSize + spacing);
            if (Render2DEngine.isHovered(mouseX, mouseY, sx, swatchY, swatchSize, swatchSize)) {
               this.applyTheme(i);
               return;
            }
         }
      }
   }

   private void applyTheme(int index) {
      HudEditor.setThemeIndex(index);
   }

   private Color animatedBlend(HudEditor.Theme theme, float speed, double phase) {
      Color blend = Render2DEngine.TwoColoreffect(theme.color1, theme.color2, speed, phase);
      return Render2DEngine.injectAlpha(blend, 255);
   }
}
