package vcore.gui.clickui.impl;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;
import vcore.Vcore;
import vcore.features.modules.Module;
import vcore.gui.clickui.AbstractElement;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.render.Render2DEngine;

public class StringElement extends AbstractElement {
   public boolean listening;
   private String currentString = "";

   public StringElement(Setting setting) {
      super(setting);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.drawGroupGuide(context, this.height);
      Render2DEngine.drawClickGuiRect(
         context.getMatrices(), this.getX() + 5.0F, this.getY() + 2.0F, this.getWidth() - 11.0F, 10.0F, new Color(-1811939328, true)
      );
      FontRenderers.sf_medium_mini
         .drawString(
            context.getMatrices(),
            this.listening
               ? this.currentString + (Module.mc.player != null && Module.mc.player.field_6012 % 5 != 0 ? "" : "_")
               : (String)this.setting.getValue(),
            this.getSettingNameX(),
            this.y + this.height / 2.0F,
            -1
         );
      if (Render2DEngine.isHovered(mouseX, mouseY, this.getX() + 5.0F, this.getY() + 2.0F, this.getWidth() - 11.0F, 10.0)) {
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(Module.mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221186));
         }

         ClickGUI.anyHovered = true;
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (this.hovered && button == 0) {
         this.listening = !this.listening;
      }

      if (this.listening) {
         Vcore.currentKeyListener = Vcore.KeyListening.Strings;
         this.currentString = (String)this.setting.getValue();
      }

      super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void charTyped(char key, int keyCode) {
      if (StringHelper.isValidChar(key)) {
         this.currentString = this.currentString + key;
      }
   }

   @Override
   public void keyTyped(int keyCode) {
      if (Vcore.currentKeyListener == Vcore.KeyListening.Strings) {
         if (this.listening) {
            switch (keyCode) {
               case 32:
                  this.currentString = this.currentString + " ";
               case 256:
               default:
                  break;
               case 257:
                  this.setting.setValue(this.currentString != null && !this.currentString.isEmpty() ? this.currentString : this.setting.getDefaultValue());
                  this.currentString = "";
                  this.listening = !this.listening;
                  break;
               case 259:
                  this.currentString = SliderElement.removeLastChar(this.currentString);
            }
         }
      }
   }
}
