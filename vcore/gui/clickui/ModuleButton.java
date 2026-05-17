package vcore.gui.clickui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.cmd.Command;
import vcore.features.hud.impl.KeyBinds;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClickGui;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.impl.BindElement;
import vcore.gui.clickui.impl.BooleanElement;
import vcore.gui.clickui.impl.BooleanParentElement;
import vcore.gui.clickui.impl.ColorPickerElement;
import vcore.gui.clickui.impl.ItemBindElement;
import vcore.gui.clickui.impl.ItemSelectElement;
import vcore.gui.clickui.impl.ModeElement;
import vcore.gui.clickui.impl.ParentElement;
import vcore.gui.clickui.impl.SliderElement;
import vcore.gui.clickui.impl.StringElement;
import vcore.gui.font.FontRenderers;
import vcore.gui.misc.DialogScreen;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.ItemBindSetting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.setting.impl.PositionSetting;
import vcore.setting.impl.SettingGroup;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;
import vcore.utility.render.animation.AnimationUtility;

public class ModuleButton extends AbstractButton {
   private static final float DEFAULT_RADIUS = 4.0F;
   private static final float SETTINGS_BOTTOM_PADDING = 2.0F;
   private static final float MODULE_BACKGROUND_OPACITY = 0.3F;
   private final List<AbstractElement> elements;
   public final Module module;
   private boolean open;
   private boolean hovered;
   private float animation;
   private float animation2;
   float category_animation = 0.0F;
   int ticksOpened;
   private float contentX;
   private float contentY;
   private float contentWidth;
   private float contentHeight;
   private boolean binding = false;
   private boolean holdbind = false;

   private float getCornerRadius() {
      return ModuleManager.clickGui != null && ModuleManager.clickGui.moduleRound != null ? ModuleManager.clickGui.moduleRound.getValue().intValue() : 4.0F;
   }

   private float clampRadius(float baseRadius, float targetHeight) {
      return Math.min(baseRadius, Math.max(0.0F, targetHeight * 0.5F));
   }

   private Color getModuleBackgroundColor() {
      return Render2DEngine.applyOpacity(HudEditor.plateColor.getValue().getColorObject().darker(), 0.3F);
   }

   public ModuleButton(Module module) {
      this.module = module;
      this.elements = new ArrayList<>();

      for (Setting<?> setting : module.getSettings()) {
         if (setting.getValue() instanceof Boolean && !setting.getName().equals("Enabled")) {
            this.elements.add(new BooleanElement(setting));
         } else if (setting.getValue() instanceof ColorSetting) {
            this.elements.add(new ColorPickerElement(setting));
         } else if (setting.getValue() instanceof BooleanSettingGroup) {
            Setting<BooleanSettingGroup> groupSetting = (Setting<BooleanSettingGroup>)setting;
            this.elements.add(new BooleanParentElement(groupSetting));
         } else if (setting.isNumberSetting() && setting.hasRestriction()) {
            this.elements.add(new SliderElement(setting));
         } else if (setting.getValue() instanceof ItemSelectSetting) {
            this.elements.add(new ItemSelectElement(setting));
         } else if (setting.getValue() instanceof ItemBindSetting) {
            this.elements.add(new ItemBindElement(setting));
         } else if (setting.getValue() instanceof SettingGroup) {
            this.elements.add(new ParentElement(setting));
         } else if (setting.isEnumSetting() && !(setting.getValue() instanceof PositionSetting)) {
            this.elements.add(new ModeElement(setting));
         } else if (setting.getValue() instanceof Bind && !setting.getName().equals("Keybind")) {
            this.elements.add(new BindElement(setting));
         } else if ((setting.getValue() instanceof String || setting.getValue() instanceof Character) && !setting.getName().equalsIgnoreCase("displayName")) {
            this.elements.add(new StringElement(setting));
         }
      }
   }

   @Override
   public void init() {
      this.elements.forEach(AbstractElement::init);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      boolean withinContent = Render2DEngine.isHovered(mouseX, mouseY, this.contentX, this.contentY, this.contentWidth, this.contentHeight);
      this.hovered = withinContent && Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      this.animation = AnimationUtility.fast(this.animation, this.module.isEnabled() ? 1.0F : 0.0F, 8.0F);
      this.animation2 = AnimationUtility.fast(this.animation2, 1.0F, 10.0F);
      if (this.hovered) {
         ClickGUI.requestDescription(this.module.getDescription());
      }

      float ix = this.x + 5.0F;
      float textHeight = FontRenderers.sf_medium_modules.getFontHeight("A");
      float textY = this.y + (this.height - textHeight) / 2.0F + 3.0F;
      this.offsetY = AnimationUtility.fast(this.offsetY, this.target_offset, 20.0F);
      float offsetY = 0.0F;
      float rawRadius = this.getCornerRadius();
      float cornerRadiusCard = this.clampRadius(rawRadius, this.height - 2.0F);
      if (this.isOpen()) {
         float panelHeight = this.height + (float)this.getElementsHeight();
         float cornerRadiusPanel = this.clampRadius(rawRadius, panelHeight);
         float guideClipLeft = this.x + 4.0F;
         float guideClipRight = this.x + this.width - 4.0F;
         float guideClipBottom = this.y + panelHeight;
         if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            Render2DEngine.drawGuiBase(context.getMatrices(), this.x + 4.0F, this.y + 1.0F, this.width - 8.0F, panelHeight - 2.0F, cornerRadiusPanel, 0.0F);
            Render2DEngine.drawRound(
               context.getMatrices(), this.x + 4.0F, this.y + 1.0F, this.width - 8.0F, panelHeight - 2.0F, cornerRadiusPanel, this.getModuleBackgroundColor()
            );
         } else {
            Render2DEngine.drawClickGuiRound(
               context.getMatrices(), this.x + 4.0F, this.y + 1.0F, this.width - 8.0F, panelHeight - 2.0F, cornerRadiusPanel, this.getModuleBackgroundColor()
            );
         }

         Render2DEngine.addWindow(
            context.getMatrices(),
            new Render2DEngine.Rectangle(
               this.x + 1.0F, this.y + this.height, this.width + this.x - 2.0F, (float)(this.height + this.y + this.getElementsHeight())
            )
         );
         if (Render2DEngine.isHovered(
            mouseX, mouseY, this.x + 4.0F, this.y + this.height - 12.0F, this.width - 8.0F, this.height + (float)this.getElementsHeight()
         )) {
            Render2DEngine.drawBlurredShadow(context.getMatrices(), mouseX - 10, mouseY - 10, 20.0F, 20.0F, 40, HudEditor.getColor(270));
         }

         for (AbstractElement element : this.elements) {
            if (element.isVisible()) {
               element.setOffsetY(offsetY);
               element.setX(this.x);
               element.setY(this.y + this.height);
               element.setWidth(this.width);
               element.setHeight(13.0F);
               if (element instanceof ColorPickerElement picker) {
                  element.setHeight(picker.getHeight());
               } else if (element instanceof SliderElement) {
                  element.setHeight(18.0F);
               }

               if (element instanceof ModeElement combobox) {
                  combobox.setWHeight(13.0);
                  element.setHeight(combobox.getExpandedHeight());
               }

               element.setGuideClip(guideClipLeft, guideClipRight, guideClipBottom, cornerRadiusPanel);
               offsetY += element.getHeight();
            }
         }

         this.updateGroupGuides();
         this.elements.forEach(e -> {
            if (e.isVisible()) {
               e.render(context, mouseX, mouseY, delta);
            }
         });
         Render2DEngine.popWindow();
      } else if (this.hovered) {
         Render2DEngine.addWindow(context.getMatrices(), this.x + 1.0F, this.y, this.x + this.width - 2.0F, this.y + this.height, 1.0);
         Render2DEngine.drawBlurredShadow(context.getMatrices(), mouseX - 10, mouseY - 10, 20.0F, 20.0F, 35, HudEditor.getColor(270));
         Render2DEngine.popWindow();
      }

      this.category_animation = AnimationUtility.fast(this.category_animation, offsetY + this.getSettingsBottomPadding(offsetY), 20.0F);
      if (this.animation < 0.05) {
         Render2DEngine.drawClickGuiRound(
            context.getMatrices(), this.x + 4.0F, this.y + 1.0F, this.width - 8.0F, this.height - 2.0F, cornerRadiusCard, this.getModuleBackgroundColor()
         );
      } else {
         Color c1 = Render2DEngine.applyOpacity(HudEditor.getColor(270), this.animation * 2.0F);
         Color c2 = Render2DEngine.applyOpacity(HudEditor.getColor(0), this.animation * 2.0F);
         Color c3 = Render2DEngine.applyOpacity(HudEditor.getColor(180), this.animation);
         Color c4 = Render2DEngine.applyOpacity(HudEditor.getColor(90), this.animation);
         Render2DEngine.drawGradientRound(
            context.getMatrices(), this.x + 4.0F, this.y + 1.0F, this.width - 8.0F, this.height - 2.0F, cornerRadiusCard, c1, c2, c3, c4
         );
      }

      if (!this.module.getBind().getBind().equalsIgnoreCase("none") && !this.binding) {
         FontRenderers.sf_medium_modules
            .drawString(
               context.getMatrices(),
               this.getSbind(),
               this.x + this.width - 11.0F - FontRenderers.sf_medium_modules.getStringWidth(this.getSbind()),
               textY,
               this.module.isEnabled() ? HudEditor.getTextColor2().getRGB() : HudEditor.getTextColor().getRGB()
            );
      }

      if (this.binding) {
         FontRenderers.sf_medium_modules
            .drawString(
               context.getMatrices(),
               this.holdbind ? Formatting.GRAY + "Toggle / " + Formatting.RESET + "Hold" : Formatting.RESET + "Toggle " + Formatting.GRAY + "/ Hold",
               this.x + this.width - 11.0F - FontRenderers.sf_medium_modules.getStringWidth("Toggle/Hold"),
               textY,
               Render2DEngine.applyOpacity(Color.WHITE.getRGB(), this.animation2)
            );
      }

      if (this.binding) {
         FontRenderers.sf_medium_modules
            .drawString(
               context.getMatrices(),
               "PressKey",
               ix,
               textY,
               this.module.isEnabled()
                  ? Render2DEngine.applyOpacity(HudEditor.getTextColor2().getRGB(), this.animation2)
                  : Render2DEngine.applyOpacity(HudEditor.getTextColor().getRGB(), this.animation2)
            );
      } else {
         FontRenderers.sf_medium_modules
            .drawString(
               context.getMatrices(),
               this.module.getName(),
               ix + 2.0F,
               textY,
               this.module.isEnabled() ? HudEditor.getTextColor2().getRGB() : HudEditor.getTextColor().getRGB()
            );
      }
   }

   @NotNull
   private String getSbind() {
      return KeyBinds.getShortKeyName(this.module.getBind());
   }

   public void setContentArea(float x, float y, float width, float height) {
      this.contentX = x;
      this.contentY = y;
      this.contentWidth = width;
      this.contentHeight = height;
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (this.binding) {
         float bindTextWidth = FontRenderers.sf_medium_modules.getStringWidth("Toggle / Hold");
         float bindStartX = this.x + this.width - 11.0F - bindTextWidth;
         float bindY = this.y + (this.height - FontRenderers.sf_medium_modules.getFontHeight("A")) / 2.0F + 3.0F;
         float bindHeight = FontRenderers.sf_medium_modules.getFontHeight("A");
         float toggleEndX = bindStartX + FontRenderers.sf_medium_modules.getStringWidth("Toggle");
         float holdStartX = bindStartX + FontRenderers.sf_medium_modules.getStringWidth("Toggle / ");
         float holdEndX = holdStartX + FontRenderers.sf_medium_modules.getStringWidth("Hold");
         float yPadding = 3.0F;
         if (mouseX >= bindStartX && mouseX <= toggleEndX && mouseY >= bindY - yPadding && mouseY <= bindY + bindHeight + yPadding) {
            this.holdbind = false;
            this.module.getBind().setHold(false);
            return;
         }

         if (mouseX >= holdStartX && mouseX <= holdEndX && mouseY >= bindY - yPadding && mouseY <= bindY + bindHeight + yPadding) {
            this.holdbind = true;
            this.module.getBind().setHold(true);
            return;
         }

         this.module.setBind(button, true, this.holdbind);
         this.binding = false;
      }

      if (this.hovered) {
         if (InputUtil.isKeyPressed(Module.mc.getWindow().getHandle(), 261) && button == 0) {
            DialogScreen dialogScreen = new DialogScreen(
               TextureStorage.setting, "Reset module", "Are you sure you want to reset " + this.module.getName() + "?", "Yes", "No", () -> {
                  if (this.module.isEnabled()) {
                     this.module.disable("reseting");
                  }

                  for (Setting<?> s : this.module.getSettings()) {
                     if (s.getValue() instanceof ColorSetting cs) {
                        cs.setDefault();
                     } else {
                        resetSetting(s);
                     }
                  }

                  if (this.module instanceof ClickGui clickGui) {
                     clickGui.applyFontSettings();
                  }

                  Module.mc.setScreen(null);
               }, () -> Module.mc.setScreen(null)
            );
            Module.mc.setScreen(dialogScreen);
         }

         if (button == 0) {
            if (this.module.isToggleable()) {
               this.module.toggle();
            }
         } else if (button == 1 && this.canOpenSettings()) {
            this.setOpen(!this.isOpen());
            if (this.open) {
               Managers.SOUND.playSwipeIn();
            } else {
               Managers.SOUND.playSwipeOut();
            }

            this.animation = 0.5F;
         } else if (button == 2) {
            this.animation2 = 0.0F;
            this.binding = !this.binding;
         }
      }

      if (this.open) {
         this.elements.forEach(element -> {
            if (element.isVisible()) {
               element.mouseClicked(mouseX, mouseY, button);
            }
         });
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int button) {
      if (this.isOpen()) {
         this.elements.forEach(element -> element.mouseReleased(mouseX, mouseY, button));
      }
   }

   @Override
   public void charTyped(char key, int keyCode) {
      if (this.isOpen()) {
         for (AbstractElement element : this.elements) {
            element.charTyped(key, keyCode);
         }
      }
   }

   @Override
   public void keyTyped(int keyCode) {
      if (this.isOpen()) {
         for (AbstractElement element : this.elements) {
            element.keyTyped(keyCode);
         }
      }

      if (this.binding) {
         if (keyCode != 256 && keyCode != 261) {
            this.module.setBind(keyCode, false, this.holdbind);
            Command.sendMessage(this.module.getName() + " bind changed to " + this.module.getBind().getBind());
         } else {
            this.module.setBind(-1, false, this.holdbind);
            Command.sendMessage("Removed bind from " + this.module.getName());
         }

         this.binding = false;
      }
   }

   @Override
   public void onGuiClosed() {
      this.elements.forEach(AbstractElement::onClose);
   }

   private static <T> void resetSetting(Setting<T> setting) {
      setting.setValue(setting.getDefaultValue());
   }

   public List<AbstractElement> getElements() {
      return this.elements;
   }

   public double getTargetElementsHeight() {
      float targetHeight = 0.0F;

      for (AbstractElement element : this.elements) {
         if (element.isVisible()) {
            targetHeight += element.getHeight();
         }
      }

      return targetHeight + this.getSettingsBottomPadding(targetHeight);
   }

   public double getElementsHeight() {
      return this.category_animation;
   }

   public double interp(double d, double d2, float d3) {
      return d2 + (d - d2) * d3;
   }

   public boolean isOpen() {
      return this.open;
   }

   public void setOpen(boolean open) {
      this.open = open;
   }

   private boolean canOpenSettings() {
      return !this.elements.isEmpty();
   }

   private void updateGroupGuides() {
      this.elements.forEach(elementx -> elementx.setDirectGroupGuideSuppressed(false));

      for (int i = 0; i < this.elements.size(); i++) {
         AbstractElement element = this.elements.get(i);
         Setting<?> parentSetting = this.getParentGroupSetting(element);
         if (parentSetting != null && element.isVisible()) {
            float groupGuideHeight = this.getGroupGuideHeight(parentSetting, i);
            if (element instanceof ParentElement parentElement) {
               parentElement.setGroupGuideHeight(groupGuideHeight);
            } else if (element instanceof BooleanParentElement parentElement) {
               parentElement.setGroupGuideHeight(groupGuideHeight);
            }

            this.suppressDirectChildGuides(parentSetting, i);
         }
      }
   }

   private Setting<?> getParentGroupSetting(AbstractElement element) {
      if (element instanceof ParentElement parentElement) {
         return parentElement.getParentSetting();
      } else {
         return element instanceof BooleanParentElement parentElement ? parentElement.getParentSetting() : null;
      }
   }

   private float getGroupGuideHeight(Setting<?> parentSetting, int parentIndex) {
      float groupHeight = this.elements.get(parentIndex).getHeight();

      for (int i = parentIndex + 1; i < this.elements.size(); i++) {
         AbstractElement element = this.elements.get(i);
         if (!this.isDescendantOf(element.getSetting(), parentSetting)) {
            break;
         }

         if (element.isVisible()) {
            groupHeight += element.getHeight();
         }
      }

      return groupHeight;
   }

   private void suppressDirectChildGuides(Setting<?> parentSetting, int parentIndex) {
      for (int i = parentIndex + 1; i < this.elements.size(); i++) {
         AbstractElement element = this.elements.get(i);
         Setting<?> elementSetting = element.getSetting();
         if (!this.isDescendantOf(elementSetting, parentSetting)) {
            break;
         }

         if (elementSetting.group == parentSetting) {
            element.setDirectGroupGuideSuppressed(true);
         }
      }
   }

   private boolean isDescendantOf(Setting<?> setting, Setting<?> parentSetting) {
      for (Setting<?> currentGroup = setting.group; currentGroup != null; currentGroup = currentGroup.group) {
         if (currentGroup == parentSetting) {
            return true;
         }
      }

      return false;
   }

   private float getSettingsBottomPadding(float settingsHeight) {
      return settingsHeight > 0.0F ? 2.0F : 0.0F;
   }

   @Override
   public void tick() {
      if (this.isOpen()) {
         this.ticksOpened++;
      } else {
         this.ticksOpened = 0;
      }
   }
}
