package vcore.gui.clickui.impl;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import vcore.features.hud.impl.KeyBinds;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.ItemBindSetting;
import vcore.utility.render.Render2DEngine;

public class ItemBindElement extends AbstractElement {
   private boolean isListening;

   public ItemBindElement(Setting setting) {
      super(setting);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.drawGroupGuide(context, this.height);
      ItemBindSetting itemSetting = (ItemBindSetting)this.setting.getValue();
      String label = itemSetting.getDisplayName(this.setting.getName());
      FontRenderers.sf_medium_mini
         .drawString(context.getMatrices(), label, this.getSettingNameX(), this.getY() + this.height / 2.0F - 3.0F + 2.0F, new Color(-1).getRGB());
      String bindText = this.getBindText(itemSetting);
      float tWidth = FontRenderers.sf_medium_mini.getStringWidth(bindText);
      float boxX = this.getX() + (this.getWidth() - tWidth - 11.0F);
      Render2DEngine.drawClickGuiRect(context.getMatrices(), boxX, this.getY() + 2.0F, tWidth + 4.0F, 10.0F, new Color(-1811939328, true));
      FontRenderers.sf_medium_mini.drawString(context.getMatrices(), bindText, boxX + 2.0F, this.getY() + this.height / 2.0F - 1.0F, new Color(-1).getRGB());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      ItemBindSetting itemSetting = (ItemBindSetting)this.setting.getValue();
      if (this.isListening) {
         Bind b = new Bind(button, true, false);
         this.setting.setValue(new ItemBindSetting(itemSetting.getItemId(), b));
         this.isListening = false;
      } else {
         if (button == 0 && this.isOverBind(mouseX, mouseY)) {
            this.isListening = true;
         }
      }
   }

   @Override
   public void keyTyped(int keyCode) {
      if (this.isListening) {
         ItemBindSetting itemSetting = (ItemBindSetting)this.setting.getValue();
         Bind b;
         if (keyCode != 256 && keyCode != 261) {
            b = new Bind(keyCode, false, false);
         } else {
            b = new Bind(-1, false, false);
         }

         this.setting.setValue(new ItemBindSetting(itemSetting.getItemId(), b));
         this.isListening = false;
      }
   }

   private boolean isOverBind(int mouseX, int mouseY) {
      ItemBindSetting itemSetting = (ItemBindSetting)this.setting.getValue();
      String bindText = this.getBindText(itemSetting);
      float tWidth = FontRenderers.sf_medium_mini.getStringWidth(bindText);
      float boxX = this.getX() + (this.getWidth() - tWidth - 11.0F);
      return Render2DEngine.isHovered(mouseX, mouseY, boxX, this.getY() + 2.0F, tWidth + 4.0F, 10.0);
   }

   private String getBindText(ItemBindSetting itemSetting) {
      return this.isListening ? "..." : KeyBinds.getShortKeyName(itemSetting.getBind());
   }
}
