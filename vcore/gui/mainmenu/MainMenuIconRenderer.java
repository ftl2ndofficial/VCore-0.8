package vcore.gui.mainmenu;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import vcore.utility.render.Render2DEngine;

final class MainMenuIconRenderer {
   private MainMenuIconRenderer() {
   }

   public static void render(@NotNull DrawContext context, @NotNull MainMenuButton.IconType iconType, float centerX, float centerY, float size, boolean hovered) {
      Color primary = new Color(255, 255, 255, hovered ? 255 : 215);
      Color secondary = new Color(255, 255, 255, hovered ? 210 : 170);
      Color cutout = new Color(12, 17, 24, hovered ? 240 : 220);
      switch (iconType) {
         case SINGLEPLAYER:
            renderSingleplayer(context.getMatrices(), centerX, centerY, size, primary);
            break;
         case MULTIPLAYER:
            renderMultiplayer(context.getMatrices(), centerX, centerY, size, primary, secondary);
            break;
         case ALT:
            renderAlt(context.getMatrices(), centerX, centerY, size, primary, secondary);
            break;
         case SETTING:
            renderSetting(context.getMatrices(), centerX, centerY, size, primary, secondary);
            break;
         case LEAVE:
            renderLeave(context.getMatrices(), centerX, centerY, size, primary, cutout);
      }
   }

   private static void renderSingleplayer(MatrixStack matrices, float centerX, float centerY, float size, Color color) {
      float headSize = size * 0.4F;
      float bodyWidth = size * 0.92F;
      float bodyHeight = size * 0.34F;
      drawCircle(matrices, centerX, centerY - size * 0.22F, headSize, color);
      Render2DEngine.drawRound(matrices, centerX - bodyWidth / 2.0F, centerY + size * 0.05F, bodyWidth, bodyHeight, bodyHeight / 2.0F, color);
   }

   private static void renderMultiplayer(MatrixStack matrices, float centerX, float centerY, float size, Color primary, Color secondary) {
      float frontHeadSize = size * 0.38F;
      float frontBodyWidth = size * 0.74F;
      float frontBodyHeight = size * 0.3F;
      float backHeadSize = size * 0.3F;
      float backBodyWidth = size * 0.56F;
      float backBodyHeight = size * 0.24F;
      drawCircle(matrices, centerX + size * 0.23F, centerY - size * 0.2F, backHeadSize, secondary);
      Render2DEngine.drawRound(
         matrices, centerX - backBodyWidth / 2.0F + size * 0.23F, centerY + size * 0.03F, backBodyWidth, backBodyHeight, backBodyHeight / 2.0F, secondary
      );
      drawCircle(matrices, centerX - size * 0.1F, centerY - size * 0.18F, frontHeadSize, primary);
      Render2DEngine.drawRound(
         matrices, centerX - frontBodyWidth / 2.0F - size * 0.1F, centerY + size * 0.05F, frontBodyWidth, frontBodyHeight, frontBodyHeight / 2.0F, primary
      );
   }

   private static void renderAlt(MatrixStack matrices, float centerX, float centerY, float size, Color primary, Color secondary) {
      float cardWidth = size * 0.74F;
      float cardHeight = size * 0.52F;
      float cardRadius = size * 0.1F;
      Render2DEngine.drawRound(
         matrices, centerX - cardWidth / 2.0F + size * 0.14F, centerY - cardHeight / 2.0F - size * 0.14F, cardWidth, cardHeight, cardRadius, secondary
      );
      Render2DEngine.drawRound(
         matrices, centerX - cardWidth / 2.0F - size * 0.12F, centerY - cardHeight / 2.0F + size * 0.05F, cardWidth, cardHeight, cardRadius, primary
      );
      float frontCardX = centerX - cardWidth / 2.0F - size * 0.12F;
      float frontCardY = centerY - cardHeight / 2.0F + size * 0.05F;
      float headSize = size * 0.15F;
      float lineWidth = size * 0.2F;
      float lineHeight = size * 0.07F;
      drawCircle(matrices, frontCardX + size * 0.22F, frontCardY + size * 0.18F, headSize, new Color(12, 17, 24, 230));
      Render2DEngine.drawRound(
         matrices, frontCardX + size * 0.14F, frontCardY + size * 0.29F, size * 0.16F, size * 0.09F, size * 0.04F, new Color(12, 17, 24, 230)
      );
      Render2DEngine.drawRound(
         matrices, frontCardX + size * 0.36F, frontCardY + size * 0.14F, lineWidth, lineHeight, lineHeight / 2.0F, new Color(12, 17, 24, 230)
      );
      Render2DEngine.drawRound(
         matrices, frontCardX + size * 0.36F, frontCardY + size * 0.28F, lineWidth * 0.82F, lineHeight, lineHeight / 2.0F, new Color(12, 17, 24, 230)
      );
   }

   private static void renderSetting(MatrixStack matrices, float centerX, float centerY, float size, Color primary, Color secondary) {
      float trackWidth = size * 0.12F;
      float trackHeight = size * 0.78F;
      float knobSize = size * 0.22F;
      float leftX = centerX - size * 0.28F;
      float middleX = centerX;
      float rightX = centerX + size * 0.28F;
      drawSliderTrack(matrices, leftX, centerY, trackWidth, trackHeight, secondary);
      drawSliderTrack(matrices, middleX, centerY, trackWidth, trackHeight, secondary);
      drawSliderTrack(matrices, rightX, centerY, trackWidth, trackHeight, secondary);
      drawCircle(matrices, leftX, centerY - size * 0.16F, knobSize, primary);
      drawCircle(matrices, middleX, centerY + size * 0.06F, knobSize, primary);
      drawCircle(matrices, rightX, centerY - size * 0.02F, knobSize, primary);
   }

   private static void renderLeave(MatrixStack matrices, float centerX, float centerY, float size, Color primary, Color cutout) {
      float ringDiameter = size * 0.78F;
      float innerDiameter = size * 0.48F;
      float breakWidth = size * 0.24F;
      float breakHeight = size * 0.24F;
      float stemWidth = size * 0.14F;
      float stemHeight = size * 0.38F;
      drawCircle(matrices, centerX, centerY, ringDiameter, primary);
      drawCircle(matrices, centerX, centerY, innerDiameter, cutout);
      Render2DEngine.drawRound(
         matrices, centerX - breakWidth / 2.0F, centerY - ringDiameter / 2.0F - size * 0.02F, breakWidth, breakHeight, breakHeight / 2.0F, cutout
      );
      Render2DEngine.drawRound(
         matrices, centerX - stemWidth / 2.0F, centerY - ringDiameter / 2.0F - size * 0.02F, stemWidth, stemHeight, stemWidth / 2.0F, primary
      );
   }

   private static void drawCircle(MatrixStack matrices, float centerX, float centerY, float diameter, Color color) {
      float radius = diameter / 2.0F;
      Render2DEngine.drawRound(matrices, centerX - radius, centerY - radius, diameter, diameter, radius, color);
   }

   private static void drawRoundedSquare(MatrixStack matrices, float centerX, float centerY, float size, Color color) {
      float radius = size * 0.32F;
      Render2DEngine.drawRound(matrices, centerX - size / 2.0F, centerY - size / 2.0F, size, size, radius, color);
   }

   private static void drawSliderTrack(MatrixStack matrices, float centerX, float centerY, float width, float height, Color color) {
      Render2DEngine.drawRound(matrices, centerX - width / 2.0F, centerY - height / 2.0F, width, height, width / 2.0F, color);
   }
}
