package vcore.gui.clickui.impl;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;
import vcore.Vcore;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.AbstractButton;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderer;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;

public class SearchBar extends AbstractButton {
   private static final float TEXT_X_PADDING = 9.0F;
   private static final float TEXT_Y_OFFSET = 3.0F;
   public static String moduleName = "";
   public static boolean listening;

   public static void resetState() {
      moduleName = "";
      listening = false;
      if (Vcore.currentKeyListener == Vcore.KeyListening.Search) {
         Vcore.currentKeyListener = null;
      }
   }

   public static boolean hasQuery() {
      return !moduleName.isBlank();
   }

   public static boolean matchesQuery(String moduleName) {
      return !hasQuery() || moduleName.toLowerCase().contains(SearchBar.moduleName.toLowerCase());
   }

   private static void stopListening() {
      listening = false;
      if (Vcore.currentKeyListener == Vcore.KeyListening.Search) {
         Vcore.currentKeyListener = null;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      boolean hovered = Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      int hudAlpha = Math.round(255.0F * HudEditor.getAlpha());
      Color base = Render2DEngine.injectAlpha(new Color(25, 25, 28), hudAlpha);
      Render2DEngine.drawClickGuiRound(context.getMatrices(), this.x, this.y, this.width, this.height, this.height / 2.0F, base);
      FontRenderer font = FontRenderers.sf_bold;
      String displayText;
      int textColor;
      if (!listening && !hasQuery()) {
         displayText = "Search...";
         textColor = Render2DEngine.injectAlpha(new Color(150, 150, 150), Math.round(200.0F * HudEditor.getAlpha())).getRGB();
      } else {
         String suffix = !listening || Module.mc.player != null && Module.mc.player.field_6012 / 10 % 2 != 0 ? (listening ? "_" : "") : " ";
         displayText = moduleName + suffix;
         textColor = Render2DEngine.injectAlpha(Color.WHITE, hudAlpha).getRGB();
      }

      float textY = this.y + (this.height - font.getFontHeight(displayText.isEmpty() ? "A" : displayText)) / 2.0F + 3.0F;
      font.drawString(context.getMatrices(), displayText, this.x + 9.0F, textY, textColor);
      if (hovered) {
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(Module.mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221186));
         }

         ClickGUI.anyHovered = true;
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      super.mouseClicked(mouseX, mouseY, button);
      boolean isHovered = Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      if (isHovered) {
         listening = true;
         Vcore.currentKeyListener = Vcore.KeyListening.Search;
      } else {
         stopListening();
      }
   }

   @Override
   public void charTyped(char key, int keyCode) {
      if (StringHelper.isValidChar(key) && listening) {
         moduleName = moduleName + key;
      }
   }

   @Override
   public void keyTyped(int keyCode) {
      super.keyTyped(keyCode);
      if (keyCode != 70 || !InputUtil.isKeyPressed(Module.mc.getWindow().getHandle(), 341) && !InputUtil.isKeyPressed(Module.mc.getWindow().getHandle(), 345)) {
         if (Vcore.currentKeyListener == Vcore.KeyListening.Search) {
            if (listening) {
               switch (keyCode) {
                  case 32:
                     moduleName = moduleName + " ";
                     break;
                  case 256:
                  case 257:
                     stopListening();
                     break;
                  case 259:
                     moduleName = SliderElement.removeLastChar(moduleName);
               }
            }
         }
      } else {
         moduleName = "";
         listening = true;
         Vcore.currentKeyListener = Vcore.KeyListening.Search;
      }
   }
}
