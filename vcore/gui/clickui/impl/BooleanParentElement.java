package vcore.gui.clickui.impl;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import vcore.core.Managers;
import vcore.core.manager.IManager;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.SettingControlRenderer;
import vcore.utility.render.TextureStorage;
import vcore.utility.render.animation.AnimationUtility;

public class BooleanParentElement extends AbstractElement {
   private static final float CHECKBOX_RIGHT_PADDING = 22.0F;
   private final Setting<BooleanSettingGroup> parentSetting;
   private float groupGuideHeight;
   float animation;
   float arrowAnimation;

   public BooleanParentElement(Setting<BooleanSettingGroup> setting) {
      super(setting);
      this.parentSetting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      MatrixStack matrixStack = context.getMatrices();
      float tx = this.x + this.width - 11.0F;
      float ty = this.y + 7.5F;
      this.drawGroupGuide(context, this.height);
      if (this.getParentSetting().getValue().isExtended()) {
         this.drawGuideAt(context, this.getChildGroupGuideX(), Math.max(this.height, this.groupGuideHeight));
      }

      this.arrowAnimation = AnimationUtility.fast(this.arrowAnimation, this.getParentSetting().getValue().isExtended() ? 0.0F : 1.0F, 15.0F);
      matrixStack.push();
      matrixStack.translate(tx, ty, 0.0F);
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-180.0F * this.arrowAnimation));
      matrixStack.translate(-tx, -ty, 0.0F);
      matrixStack.translate(this.x + this.width - 14.0F, this.y + 4.5F, 0.0F);
      context.drawTexture(TextureStorage.guiArrow, 0, 0, 0.0F, 0.0F, 6, 6, 6, 6);
      matrixStack.translate(-(this.x + this.width - 14.0F), -(this.y + 4.5F), 0.0F);
      matrixStack.pop();
      FontRenderers.sf_medium_mini
         .drawString(matrixStack, this.setting.getName(), this.getSettingNameX(), this.y + this.height / 2.0F - 1.0F, new Color(-1).getRGB());
      this.animation = AnimationUtility.fast(this.animation, this.getParentSetting().getValue().isEnabled() ? 1.0F : 0.0F, 15.0F);
      float checkboxX = this.x + this.width - 10.0F - 22.0F;
      float checkboxY = this.y + this.height / 2.0F - 5.0F;
      SettingControlRenderer.drawCheckbox(context.getMatrices(), checkboxX, checkboxY, this.animation);
      if (Render2DEngine.isHovered(mouseX, mouseY, checkboxX, checkboxY, 10.0, 10.0)) {
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(IManager.mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221188));
         }

         ClickGUI.anyHovered = true;
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (this.hovered) {
         if (button == 0) {
            this.getParentSetting().getValue().setEnabled(!this.getParentSetting().getValue().isEnabled());
            Managers.SOUND.playBoolean();
         } else {
            this.getParentSetting().getValue().setExtended(!this.getParentSetting().getValue().isExtended());
            if (this.getParentSetting().getValue().isExtended()) {
               Managers.SOUND.playSwipeIn();
            } else {
               Managers.SOUND.playSwipeOut();
            }
         }
      }

      super.mouseClicked(mouseX, mouseY, button);
   }

   public Setting<BooleanSettingGroup> getParentSetting() {
      return this.parentSetting;
   }

   public void setGroupGuideHeight(float groupGuideHeight) {
      this.groupGuideHeight = groupGuideHeight;
   }

   private float getChildGroupGuideX() {
      return this.x + 4.0F + 6.0F * Math.max(0, this.setting.getGroupDepth());
   }
}
