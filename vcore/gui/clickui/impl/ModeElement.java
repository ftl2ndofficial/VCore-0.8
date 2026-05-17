package vcore.gui.clickui.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import vcore.core.Managers;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;
import vcore.utility.render.animation.AnimationUtility;

public class ModeElement extends AbstractElement {
   private static final float CHIP_TOP_MARGIN = 3.0F;
   private static final float CHIP_BOTTOM_MARGIN = 3.0F;
   private static final float CHIP_HORIZONTAL_PADDING = 1.75F;
   private static final float CHIP_VERTICAL_PADDING = 0.5F;
   private static final float CHIP_HORIZONTAL_GAP = 0.85F;
   private static final float CHIP_VERTICAL_GAP = 0.85F;
   private static final float CHIP_LEFT_PADDING = 6.0F;
   private static final float CHIP_RIGHT_PADDING = 8.0F;
   private static final float CHIP_MIN_WIDTH = 8.0F;
   private static final float CHIP_RADIUS = 2.0F;
   private static final int CHIP_CORNER_STEPS = 6;
   private static final float CHIP_OUTLINE_THICKNESS = 0.3F;
   public Setting setting2;
   private boolean open;
   private double wheight;
   private String prevMode;
   private float animation;
   private float animation2;

   public ModeElement(Setting setting) {
      super(setting);
      this.setting2 = setting;
      this.prevMode = setting.currentEnumName();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.animation = AnimationUtility.fast(this.animation, this.open ? 0.0F : 1.0F, 15.0F);
      this.animation2 = AnimationUtility.fast(this.animation2, 1.0F, 10.0F);
      ModeElement.ModeChipLayout chipLayout = this.buildChipLayout();
      float tx = this.x + this.width - 11.0F;
      float ty = this.y + 7.5F;
      MatrixStack matrixStack = context.getMatrices();
      float thetaRotation = -180.0F * this.animation;
      matrixStack.push();
      matrixStack.translate(tx, ty, 0.0F);
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(thetaRotation));
      matrixStack.translate(-tx, -ty, 0.0F);
      matrixStack.translate(this.x + this.width - 14.0F, this.y + 4.5F, 0.0F);
      context.drawTexture(TextureStorage.guiArrow, 0, 0, 0.0F, 0.0F, 6, 6, 6, 6);
      matrixStack.translate(-(this.x + this.width - 14.0F), -this.y - 4.5F, 0.0F);
      matrixStack.pop();
      if (this.setting.group != null) {
         this.drawGroupGuide(context, this.height);
      } else if (this.open) {
         this.drawGuideAt(context, this.x + 4.0F, this.height);
      }

      FontRenderers.sf_medium_mini
         .drawString(matrixStack, this.setting2.getName(), this.getSettingNameX(), this.y + this.wheight / 2.0 - 3.0 + 3.0, Color.WHITE.getRGB());
      if (this.animation2 < 0.99 && !Objects.equals(this.setting2.currentEnumName(), this.prevMode)) {
         FontRenderers.sf_medium_mini
            .drawString(
               matrixStack,
               this.prevMode,
               (int)(this.x + this.width - 18.0F - FontRenderers.sf_medium_mini.getStringWidth(this.prevMode)),
               3.0 + (this.y + this.wheight / 2.0 - 3.0) - this.animation2 * 5.0F,
               Render2DEngine.applyOpacity(new Color(-1), this.animation2)
            );
         FontRenderers.sf_medium_mini
            .drawString(
               matrixStack,
               this.setting2.currentEnumName(),
               this.x + this.width - 18.0F - FontRenderers.sf_medium_mini.getStringWidth(this.setting2.currentEnumName()),
               3.0 + (this.y + this.wheight / 2.0 - 3.0) - this.animation2 * 5.0F + 5.0,
               Render2DEngine.applyOpacity(new Color(-1), 1.0F - this.animation2)
            );
      } else {
         FontRenderers.sf_medium_mini
            .drawString(
               matrixStack,
               this.setting2.currentEnumName(),
               this.x + this.width - 18.0F - FontRenderers.sf_medium_mini.getStringWidth(this.setting.currentEnumName()),
               3.0 + (this.y + this.wheight / 2.0 - 3.0),
               Color.WHITE.getRGB()
            );
      }

      if (this.open) {
         Color selectedColor = HudEditor.getColor(0);
         Color outlineColor = this.getChipOutlineColor();

         for (ModeElement.ModeChip chip : chipLayout.chips) {
            this.drawChipOutline(context, chip, outlineColor);
            float textX = chip.x + (chip.width - FontRenderers.sf_medium_mini.getStringWidth(chip.mode)) / 2.0F;
            float textY = chip.y + (chip.height - FontRenderers.sf_medium_mini.getFontHeight(chip.mode)) / 2.0F + 2.9F;
            FontRenderers.sf_medium_mini
               .drawString(
                  matrixStack,
                  chip.mode,
                  textX,
                  textY,
                  this.setting2.currentEnumName().equalsIgnoreCase(chip.mode) ? selectedColor.getRGB() : Color.WHITE.getRGB()
               );
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.wheight)) {
         if (button == 0) {
            this.prevMode = this.setting2.currentEnumName();
            this.animation2 = 0.0F;
            this.setting2.increaseEnum();
            Managers.SOUND.playBoolean();
         } else {
            this.open = !this.open;
            if (this.open) {
               Managers.SOUND.playSwipeIn();
            } else {
               Managers.SOUND.playSwipeOut();
            }
         }
      }

      if (this.open && button == 0) {
         for (ModeElement.ModeChip chip : this.buildChipLayout().chips) {
            if (Render2DEngine.isHovered(mouseX, mouseY, chip.x, chip.y, chip.width, chip.height)) {
               this.prevMode = this.setting2.currentEnumName();
               this.animation2 = 0.0F;
               this.setting2.setEnumByNumber(chip.index);
               Managers.SOUND.playBoolean();
               break;
            }
         }
      }

      super.mouseClicked(mouseX, mouseY, button);
   }

   public float getExpandedHeight() {
      return this.open ? (float)this.wheight + this.buildChipLayout().contentHeight : (float)this.wheight;
   }

   public void setWHeight(double height) {
      this.wheight = height;
   }

   public boolean isOpen() {
      return this.open;
   }

   private ModeElement.ModeChipLayout buildChipLayout() {
      List<ModeElement.ModeChip> chips = new ArrayList<>();
      if (!this.open) {
         return new ModeElement.ModeChipLayout(chips, 0.0F);
      }

      float startX = this.x + 6.0F + this.getGroupIndent();
      float maxRight = this.x + this.width - 8.0F;
      float chipHeight = Math.max(8.5F, FontRenderers.sf_medium_mini.getFontHeight("A") + 1.0F);
      float currentX = startX;
      float currentY = this.y + (float)this.wheight + 3.0F;
      float maxBottom = currentY;
      String[] modes = this.setting2.getModes();

      for (int i = 0; i < modes.length; i++) {
         String mode = modes[i];
         float chipWidth = Math.max(8.0F, FontRenderers.sf_medium_mini.getStringWidth(mode) + 3.5F);
         if (currentX + chipWidth > maxRight && currentX > startX) {
            currentX = startX;
            currentY += chipHeight + 0.85F;
         }

         chips.add(new ModeElement.ModeChip(i, mode, currentX, currentY, chipWidth, chipHeight));
         currentX += chipWidth + 0.85F;
         maxBottom = currentY + chipHeight;
      }

      float contentHeight = chips.isEmpty() ? 0.0F : maxBottom - (this.y + (float)this.wheight) + 3.0F;
      return new ModeElement.ModeChipLayout(chips, contentHeight);
   }

   private void drawChipOutline(DrawContext context, ModeElement.ModeChip chip, Color outlineColor) {
      MatrixStack matrices = context.getMatrices();
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      int color = outlineColor.getRGB();
      float left = chip.x + 0.5F;
      float top = chip.y + 0.5F;
      float right = chip.x + chip.width - 0.5F;
      float bottom = chip.y + chip.height - 0.5F;
      float radius = Math.min(2.0F, Math.min((right - left) / 2.0F, (bottom - top) / 2.0F));
      float innerLeft = left + 0.3F;
      float innerTop = top + 0.3F;
      float innerRight = right - 0.3F;
      float innerBottom = bottom - 0.3F;
      if (!(innerRight <= innerLeft) && !(innerBottom <= innerTop)) {
         float innerRadius = Math.max(0.0F, radius - 0.3F);
         List<ModeElement.OutlinePoint> outerPoints = this.buildRoundedRectOutline(left, top, right, bottom, radius);
         List<ModeElement.OutlinePoint> innerPoints = this.buildRoundedRectOutline(innerLeft, innerTop, innerRight, innerBottom, innerRadius);
         Render2DEngine.setupRender();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         int pointCount = Math.min(outerPoints.size(), innerPoints.size());

         for (int i = 0; i < pointCount; i++) {
            int next = (i + 1) % pointCount;
            ModeElement.OutlinePoint outerStart = outerPoints.get(i);
            ModeElement.OutlinePoint outerEnd = outerPoints.get(next);
            ModeElement.OutlinePoint innerEnd = innerPoints.get(next);
            ModeElement.OutlinePoint innerStart = innerPoints.get(i);
            buffer.method_22918(matrix, outerStart.x, outerStart.y, 0.0F).color(color);
            buffer.method_22918(matrix, outerEnd.x, outerEnd.y, 0.0F).color(color);
            buffer.method_22918(matrix, innerEnd.x, innerEnd.y, 0.0F).color(color);
            buffer.method_22918(matrix, innerStart.x, innerStart.y, 0.0F).color(color);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         Render2DEngine.endRender();
      }
   }

   private List<ModeElement.OutlinePoint> buildRoundedRectOutline(float left, float top, float right, float bottom, float radius) {
      List<ModeElement.OutlinePoint> points = new ArrayList<>();
      points.add(new ModeElement.OutlinePoint(left + radius, top));
      points.add(new ModeElement.OutlinePoint(right - radius, top));
      this.addArcPoints(points, right - radius, top + radius, radius, 270.0F, 360.0F, false, true);
      points.add(new ModeElement.OutlinePoint(right, bottom - radius));
      this.addArcPoints(points, right - radius, bottom - radius, radius, 0.0F, 90.0F, false, true);
      points.add(new ModeElement.OutlinePoint(left + radius, bottom));
      this.addArcPoints(points, left + radius, bottom - radius, radius, 90.0F, 180.0F, false, true);
      points.add(new ModeElement.OutlinePoint(left, top + radius));
      this.addArcPoints(points, left + radius, top + radius, radius, 180.0F, 270.0F, false, false);
      return points;
   }

   private void addArcPoints(
      List<ModeElement.OutlinePoint> points,
      float centerX,
      float centerY,
      float radius,
      float startDeg,
      float endDeg,
      boolean includeStartPoint,
      boolean includeEndPoint
   ) {
      int startStep = includeStartPoint ? 0 : 1;
      int endStep = includeEndPoint ? 6 : 5;

      for (int i = startStep; i <= endStep; i++) {
         float progress = i / 6.0F;
         double angle = Math.toRadians(startDeg + (endDeg - startDeg) * progress);
         float px = centerX + (float)Math.cos(angle) * radius;
         float py = centerY + (float)Math.sin(angle) * radius;
         points.add(new ModeElement.OutlinePoint(px, py));
      }
   }

   private Color getChipOutlineColor() {
      int alpha = HudEditor.plateColor.getValue().getAlpha();
      return new Color(255, 255, 255, alpha);
   }

   private static final class ModeChip {
      private final int index;
      private final String mode;
      private final float x;
      private final float y;
      private final float width;
      private final float height;

      private ModeChip(int index, String mode, float x, float y, float width, float height) {
         this.index = index;
         this.mode = mode;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }
   }

   private static final class ModeChipLayout {
      private final List<ModeElement.ModeChip> chips;
      private final float contentHeight;

      private ModeChipLayout(List<ModeElement.ModeChip> chips, float contentHeight) {
         this.chips = chips;
         this.contentHeight = contentHeight;
      }
   }

   private static final class OutlinePoint {
      private final float x;
      private final float y;

      private OutlinePoint(float x, float y) {
         this.x = x;
         this.y = y;
      }
   }
}
