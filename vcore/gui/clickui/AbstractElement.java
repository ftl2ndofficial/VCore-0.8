package vcore.gui.clickui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import vcore.features.modules.misc.ClickGui;
import vcore.features.modules.render.HudEditor;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.render.Render2DEngine;

public abstract class AbstractElement {
   private static final float GROUP_GUIDE_ALPHA = 0.55F;
   protected Setting setting;
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected float offsetY;
   protected float guideClipLeft = Float.NEGATIVE_INFINITY;
   protected float guideClipRight = Float.POSITIVE_INFINITY;
   protected float guideClipBottom = Float.POSITIVE_INFINITY;
   protected float guideCornerRadius = 0.0F;
   protected boolean directGroupGuideSuppressed;
   protected boolean hovered;

   public AbstractElement(Setting setting) {
      this.setting = setting;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.hovered = Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   public void init() {
   }

   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 261) && button == 2 && this.hovered) {
         if (this.setting.getValue() instanceof ColorSetting cs) {
            cs.setDefault();
         } else {
            this.setting.setValue(this.setting.getDefaultValue());
         }

         if (this.setting.getModule() instanceof ClickGui clickGui && (this.setting == clickGui.settingFontScale || this.setting == clickGui.modulesFontScale)) {
            clickGui.applyFontSettings();
         }
      }
   }

   public void mouseReleased(int mouseX, int mouseY, int button) {
   }

   public void keyTyped(int keyCode) {
   }

   public void onClose() {
   }

   public Setting getSetting() {
      return this.setting;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setY(float y) {
      this.y = y + this.offsetY;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setOffsetY(float offsetY) {
      this.offsetY = offsetY;
   }

   public void setGuideClip(float guideClipLeft, float guideClipRight, float guideClipBottom, float guideCornerRadius) {
      this.guideClipLeft = guideClipLeft;
      this.guideClipRight = guideClipRight;
      this.guideClipBottom = guideClipBottom;
      this.guideCornerRadius = Math.max(0.0F, guideCornerRadius);
   }

   public void setDirectGroupGuideSuppressed(boolean directGroupGuideSuppressed) {
      this.directGroupGuideSuppressed = directGroupGuideSuppressed;
   }

   public boolean isVisible() {
      return this.setting.isVisible();
   }

   public void charTyped(char key, int keyCode) {
   }

   protected float getGroupGuideX() {
      return this.x + 4.0F + 6.0F * Math.max(0, this.setting.getGroupDepth() - 1);
   }

   protected float getGroupIndent() {
      return this.setting.group == null ? 0.0F : 2.0F + 6.0F * Math.max(0, this.setting.getGroupDepth() - 1);
   }

   protected float getSettingNameX() {
      return this.x + 6.0F + this.getGroupIndent();
   }

   protected void drawGroupGuide(DrawContext context, float requestedHeight) {
      if (!this.directGroupGuideSuppressed) {
         if (this.setting.group != null) {
            this.drawGuideAt(context, this.getGroupGuideX(), requestedHeight);
         }
      }
   }

   protected void drawGuideAt(DrawContext context, float lineX, float requestedHeight) {
      float guideHeight = this.getClippedGuideHeight(lineX, lineX + 1.0F, requestedHeight);
      if (!(guideHeight <= 0.0F)) {
         Render2DEngine.drawRect(context.getMatrices(), lineX, this.y, 1.0F, guideHeight, Render2DEngine.applyOpacity(HudEditor.getColor(1), 0.55F));
      }
   }

   protected float getClippedGuideHeight(float lineLeftX, float lineRightX, float requestedHeight) {
      float lineBottom = this.y + requestedHeight;
      if (Float.isFinite(this.guideClipBottom)) {
         lineBottom = Math.min(lineBottom, this.getRoundedGuideBottom(lineLeftX, lineRightX));
      }

      return Math.max(0.0F, lineBottom - this.y);
   }

   protected float getRoundedGuideBottom(float lineLeftX, float lineRightX) {
      return Math.min(this.getRoundedGuideBottomAtX(lineLeftX), this.getRoundedGuideBottomAtX(lineRightX));
   }

   protected float getRoundedGuideBottomAtX(float x) {
      float bottom = this.guideClipBottom;
      float radius = this.guideCornerRadius;
      if (!(radius <= 0.0F) && Float.isFinite(this.guideClipLeft) && Float.isFinite(this.guideClipRight)) {
         float bottomArcCenterY = bottom - radius;
         float leftArcCenterX = this.guideClipLeft + radius;
         float rightArcCenterX = this.guideClipRight - radius;
         if (x < leftArcCenterX) {
            return bottomArcCenterY + this.getCornerArcYOffset(x - leftArcCenterX, radius);
         } else {
            return x > rightArcCenterX ? bottomArcCenterY + this.getCornerArcYOffset(x - rightArcCenterX, radius) : bottom;
         }
      } else {
         return bottom;
      }
   }

   private float getCornerArcYOffset(float dx, float radius) {
      float distanceSquared = radius * radius - dx * dx;
      return (float)Math.sqrt(Math.max(0.0F, distanceSquared));
   }
}
