package vcore.gui.clickui.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;
import vcore.core.Managers;
import vcore.core.manager.client.MacroManager;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderer;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;

public class ManagerPanel {
   private static final String ONLINE_LABEL = "Online";
   private static final String ACTIVE_CONFIG_LABEL = "<==";
   private static final Color ONLINE_LABEL_COLOR = new Color(0, 255, 0, 255);
   private static final Color ACTIVE_CONFIG_MARKER_COLOR = new Color(0, 255, 0, 255);
   private static final int FRIEND_ONLINE_BG_ALPHA = 70;
   private static final int FRIEND_OFFLINE_BG_ALPHA = 25;
   private static final int FRIEND_ONLINE_TEXT_ALPHA = 255;
   private static final int FRIEND_OFFLINE_TEXT_ALPHA = 100;
   private static final float SIDEBAR_TEXT_Y_OFFSET = 2.5F;
   private static final float LIST_ITEM_TEXT_Y_OFFSET = 2.5F;
   private static final float ONLINE_TEXT_Y_OFFSET = 2.5F;
   private static final float SIDEBAR_WIDTH = 80.0F;
   private static final float SIDEBAR_TAB_HEIGHT = 20.0F;
   private static final float SIDEBAR_TAB_GAP = 3.5F;
   private static final float HEADER_HEIGHT = 30.0F;
   private static final float PADDING = 10.0F;
   private static final float ITEM_HEIGHT = 20.0F;
   private static final float FRIEND_SECTION_HEADER_HEIGHT = 14.0F;
   private static final float MACRO_HEADER_HEIGHT = 18.0F;
   private static final float MACRO_ROW_HEIGHT = 20.0F;
   private static final float MACRO_ROW_GAP = 5.0F;
   private static final float MACRO_CARD_PADDING = 2.0F;
   private static final float MACRO_FIELD_GAP = 6.0F;
   private static final float MACRO_BIND_WIDTH = 62.0F;
   private static final float MACRO_TEXT_PADDING = 5.0F;
   private static final float HINT_MARGIN = 8.0F;
   private static final float HINT_LINE_GAP = 2.0F;
   private static final float HINT_OUTLINE_OFFSET = 0.45F;
   public boolean open = false;
   private float alphaAnim = 0.0F;
   private ManagerPanel.Tab currentTab = ManagerPanel.Tab.FRIEND;
   private float x;
   private float y;
   private float width;
   private float height;
   private float scrollAmount = 0.0F;
   private MacroManager.Macro editingMacro;
   private ManagerPanel.MacroField editingField = ManagerPanel.MacroField.NONE;
   private String editBuffer = "";
   private String originalBuffer = "";
   private MacroManager.Macro bindingMacro;

   public void setOpen(boolean open) {
      this.open = open;
   }

   public boolean isOpen() {
      return this.open || this.alphaAnim > 0.01F;
   }

   private float lerp(float current, float target, float delta) {
      return current + (target - current) * delta;
   }

   public void setSize(float width, float height) {
      this.width = width;
      this.height = height;
      if (Module.mc.getWindow() != null) {
         this.x = (Module.mc.getWindow().getScaledWidth() - width) / 2.0F;
         this.y = (Module.mc.getWindow().getScaledHeight() - height) / 2.0F;
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float target = this.open ? 1.0F : 0.0F;
      this.alphaAnim = this.lerp(this.alphaAnim, target, 0.15F);
      if (!(this.alphaAnim < 0.01F) || this.open) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         float scale = 0.9F + 0.1F * this.alphaAnim;
         matrices.translate(this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         matrices.translate(-(this.x + this.width / 2.0F), -(this.y + this.height / 2.0F), 0.0F);
         int panelAlpha = Math.round(255.0F * HudEditor.getAlpha() * this.alphaAnim);
         Color mainBg = Render2DEngine.injectAlpha(new Color(30, 30, 30), panelAlpha);
         this.drawPanelBackground(matrices, this.x, this.y, this.width, this.height, 10.0F, mainBg);
         Color sidebarBg = Render2DEngine.injectAlpha(new Color(25, 25, 25), panelAlpha);
         this.drawPanelBackground(matrices, this.x, this.y, 80.0F, this.height, 10.0F, sidebarBg);
         this.renderSidebar(matrices);

         String title = switch (this.currentTab) {
            case FRIEND -> "Friend List";
            case CONFIG -> "Config Manager";
            case MACRO -> "Macro Manager";
         };
         Color titleCol = Render2DEngine.injectAlpha(Color.WHITE, (int)(255.0F * this.alphaAnim));
         FontRenderers.sf_bold
            .drawString(
               matrices,
               title,
               this.x + 80.0F + (this.width - 80.0F) / 2.0F - FontRenderers.sf_bold.getStringWidth(title) / 2.0F,
               this.y + 10.0F,
               titleCol.getRGB()
            );
         float listX = this.x + 80.0F + 10.0F;
         float listY = this.y + 30.0F + 10.0F;
         float listWidth = this.width - 80.0F - 20.0F;
         float listHeight = this.height - 30.0F - 20.0F;
         this.clampScroll(listHeight);
         Render2DEngine.beginScissor(listX, listY, listX + listWidth, listY + listHeight);
         matrices.translate(0.0F, this.scrollAmount, 0.0F);
         float currentY = listY;
         if (this.currentTab == ManagerPanel.Tab.FRIEND) {
            for (ManagerPanel.FriendDisplayEntry friend : this.getFriendDisplayEntries()) {
               if (friend.header()) {
                  this.drawFriendSectionHeader(matrices, friend.name(), listX, currentY);
                  currentY += 14.0F;
               } else {
                  this.drawFriendItem(matrices, friend.name(), listX, currentY, listWidth, 20.0F, friend.online());
                  currentY += 25.0F;
               }
            }
         } else if (this.currentTab == ManagerPanel.Tab.CONFIG) {
            String currentConf = this.getCurrentConfigName();

            for (String config : Managers.CONFIG.getConfigList()) {
               this.drawConfigItem(matrices, config, listX, currentY, listWidth, 20.0F, config.equalsIgnoreCase(currentConf));
               currentY += 25.0F;
            }
         } else {
            this.drawMacroHeader(matrices, listX, currentY, listWidth);
            currentY += 18.0F;
            List<MacroManager.Macro> macros = this.getMacroEntries();
            if (!macros.isEmpty()) {
               for (MacroManager.Macro macro : macros) {
                  this.drawMacroRow(matrices, macro, this.createMacroLayout(listX, currentY, listWidth), mouseX, mouseY);
                  currentY += 25.0F;
               }
            }
         }

         matrices.translate(0.0F, -this.scrollAmount, 0.0F);
         Render2DEngine.endScissor();
         matrices.pop();
         this.renderTabHints(context.getMatrices());
         if (Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height) && this.isOpen()) {
            ClickGUI.anyHovered = true;
         }
      }
   }

   private void renderSidebar(MatrixStack matrices) {
      this.renderSidebarTab(matrices, ManagerPanel.Tab.FRIEND, this.getTopTabY(0));
      this.renderSidebarTab(matrices, ManagerPanel.Tab.CONFIG, this.getTopTabY(1));
      this.renderSidebarTab(matrices, ManagerPanel.Tab.MACRO, this.getTopTabY(2));
   }

   private void renderSidebarTab(MatrixStack matrices, ManagerPanel.Tab tab, float tabY) {
      boolean isCurrent = this.currentTab == tab;
      Color textCol = isCurrent ? new Color(255, 255, 255) : new Color(150, 150, 150);
      textCol = Render2DEngine.injectAlpha(textCol, (int)(textCol.getAlpha() * this.alphaAnim));
      Color bgCol = isCurrent ? new Color(200, 200, 200, 40) : new Color(0, 0, 0, 0);
      bgCol = Render2DEngine.injectAlpha(bgCol, (int)(bgCol.getAlpha() * this.alphaAnim));
      this.drawPanelBackground(matrices, this.x + 5.0F, tabY, 70.0F, 20.0F, 4.0F, bgCol);
      FontRenderer markedTextFont = this.getMarkedTextFont();
      String name = tab.getDisplayName();
      markedTextFont.drawString(
         matrices,
         name,
         this.x + 40.0F - markedTextFont.getStringWidth(name) / 2.0F,
         tabY + 10.0F - markedTextFont.getFontHeight(name) / 2.0F + 2.5F,
         textCol.getRGB()
      );
   }

   private float getTopTabY(int index) {
      return this.y + 20.0F + index * 23.5F;
   }

   private void drawFriendSectionHeader(MatrixStack matrices, String title, float ix, float iy) {
      Color headerCol = Render2DEngine.injectAlpha(new Color(190, 190, 190), (int)(180.0F * this.alphaAnim));
      FontRenderers.sf_bold_mini.drawString(matrices, title, ix + 2.0F, iy + 2.0F, headerCol.getRGB());
   }

   private void drawFriendItem(MatrixStack matrices, String name, float ix, float iy, float iw, float ih, boolean online) {
      FontRenderer markedTextFont = this.getMarkedTextFont();
      int backgroundAlpha = online ? 70 : 25;
      int textAlpha = online ? 255 : 100;
      Color itemBg = Render2DEngine.injectAlpha(new Color(200, 200, 200), (int)(backgroundAlpha * this.alphaAnim));
      Color textCol = Render2DEngine.injectAlpha(Color.WHITE, (int)(textAlpha * this.alphaAnim));
      this.drawPanelBackground(matrices, ix, iy, iw, ih, 3.0F, itemBg);
      float nameX = ix + 10.0F;
      float textY = iy + ih / 2.0F - markedTextFont.getFontHeight(name) / 2.0F + 2.5F;
      float reservedWidth = online ? FontRenderers.sf_bold_mini.getStringWidth("Online") + 26.0F : 20.0F;
      float visibleWidth = Math.max(0.0F, iw - reservedWidth - 10.0F);
      markedTextFont.drawStringWithHorizontalFade(matrices, name, nameX, textY, textCol.getRGB(), visibleWidth, 10.0F);
      if (online) {
         Color onlineCol = Render2DEngine.injectAlpha(ONLINE_LABEL_COLOR, (int)(ONLINE_LABEL_COLOR.getAlpha() * this.alphaAnim));
         FontRenderers.sf_bold_mini
            .drawString(
               matrices,
               "Online",
               ix + iw - FontRenderers.sf_bold_mini.getStringWidth("Online") - 10.0F,
               iy + ih / 2.0F - FontRenderers.sf_bold_mini.getFontHeight("Online") / 2.0F + 2.5F,
               onlineCol.getRGB()
            );
      }
   }

   private void drawConfigItem(MatrixStack matrices, String name, float ix, float iy, float iw, float ih, boolean activeConfig) {
      FontRenderer markedTextFont = this.getMarkedTextFont();
      Color itemBg = Render2DEngine.injectAlpha(new Color(200, 200, 200), (int)((activeConfig ? 70 : 25) * this.alphaAnim));
      this.drawPanelBackground(matrices, ix, iy, iw, ih, 3.0F, itemBg);
      Color textCol = Render2DEngine.injectAlpha(Color.WHITE, (int)(255.0F * this.alphaAnim));
      float textY = iy + ih / 2.0F - markedTextFont.getFontHeight(name) / 2.0F + 2.5F;
      float reservedWidth = activeConfig ? FontRenderers.sf_bold_mini.getStringWidth("<==") + 26.0F : 20.0F;
      float visibleWidth = Math.max(0.0F, iw - reservedWidth - 10.0F);
      markedTextFont.drawStringWithHorizontalFade(matrices, name, ix + 10.0F, textY, textCol.getRGB(), visibleWidth, 10.0F);
      if (activeConfig) {
         Color markerCol = Render2DEngine.injectAlpha(ACTIVE_CONFIG_MARKER_COLOR, (int)(ACTIVE_CONFIG_MARKER_COLOR.getAlpha() * this.alphaAnim));
         FontRenderers.sf_bold_mini
            .drawString(
               matrices,
               "<==",
               ix + iw - FontRenderers.sf_bold_mini.getStringWidth("<==") - 10.0F,
               iy + ih / 2.0F - FontRenderers.sf_bold_mini.getFontHeight("<==") / 2.0F + 2.5F,
               markerCol.getRGB()
            );
      }
   }

   private void drawMacroHeader(MatrixStack matrices, float ix, float iy, float iw) {
      ManagerPanel.MacroLayout layout = this.createMacroLayout(ix, iy, iw);
      Color textColor = Render2DEngine.injectAlpha(new Color(190, 190, 190), (int)(190.0F * this.alphaAnim));
      FontRenderer font = FontRenderers.sf_bold_mini;
      font.drawString(matrices, "Name", layout.nameX() + 5.0F, iy + 3.0F, textColor.getRGB());
      font.drawString(matrices, "Bind", layout.bindX() + 5.0F, iy + 3.0F, textColor.getRGB());
      font.drawString(matrices, "Text", layout.textX() + 5.0F, iy + 3.0F, textColor.getRGB());
   }

   private void drawMacroRow(MatrixStack matrices, MacroManager.Macro macro, ManagerPanel.MacroLayout layout, int mouseX, int mouseY) {
      boolean rowHovered = Render2DEngine.isHovered(mouseX, mouseY, layout.cardX(), layout.cardY(), layout.cardWidth(), layout.cardHeight());
      Color cardColor = Render2DEngine.injectAlpha(new Color(200, 200, 200), (int)((rowHovered ? 42 : 28) * this.alphaAnim));
      this.drawPanelBackground(matrices, layout.cardX(), layout.cardY(), layout.cardWidth(), layout.cardHeight(), 4.0F, cardColor);
      this.drawMacroField(
         matrices,
         layout.nameX(),
         layout.fieldY(),
         layout.nameWidth(),
         layout.fieldHeight(),
         this.getMacroFieldText(macro, ManagerPanel.MacroField.NAME),
         this.isEditing(macro, ManagerPanel.MacroField.NAME),
         false,
         mouseX,
         mouseY
      );
      this.drawMacroField(
         matrices,
         layout.bindX(),
         layout.fieldY(),
         layout.bindWidth(),
         layout.fieldHeight(),
         this.bindingMacro == macro ? "Press key..." : this.formatBind(macro.getBind()),
         false,
         this.bindingMacro == macro,
         mouseX,
         mouseY
      );
      this.drawMacroField(
         matrices,
         layout.textX(),
         layout.fieldY(),
         layout.textWidth(),
         layout.fieldHeight(),
         this.getMacroFieldText(macro, ManagerPanel.MacroField.TEXT),
         this.isEditing(macro, ManagerPanel.MacroField.TEXT),
         false,
         mouseX,
         mouseY
      );
   }

   private void drawMacroField(
      MatrixStack matrices, float fx, float fy, float fw, float fh, String text, boolean editing, boolean listening, int mouseX, int mouseY
   ) {
      boolean hovered = Render2DEngine.isHovered(mouseX, mouseY, fx, fy, fw, fh);
      int alpha = !editing && !listening ? (hovered ? 58 : 40) : 85;
      Color fieldBg = Render2DEngine.injectAlpha(new Color(38, 38, 38), (int)(alpha * this.alphaAnim));
      this.drawPanelBackground(matrices, fx, fy, fw, fh, 3.0F, fieldBg);
      Color textColor = Render2DEngine.injectAlpha(Color.WHITE, (int)(255.0F * this.alphaAnim));
      FontRenderer font = this.getMarkedTextFont();
      float textY = fy + fh / 2.0F - font.getFontHeight(text) / 2.0F + 2.0F;
      font.drawStringWithHorizontalFade(matrices, text, fx + 5.0F, textY, textColor.getRGB(), Math.max(0.0F, fw - 10.0F), 10.0F);
   }

   private void renderTabHints(MatrixStack matrices) {
      List<String> hints = this.getTabHints();
      if (!hints.isEmpty() && Module.mc.getWindow() != null) {
         FontRenderer font = FontRenderers.sf_bold_mini;
         float lineHeight = font.getFontHeight("A") + 2.0F;
         float startY = Module.mc.getWindow().getScaledHeight() - 8.0F - lineHeight * hints.size();
         float startX = 8.0F;

         for (int i = 0; i < hints.size(); i++) {
            this.drawOutlinedHintText(matrices, font, hints.get(i), startX, startY + i * lineHeight);
         }
      }
   }

   private void drawPanelBackground(MatrixStack matrices, float x, float y, float width, float height, float radius, Color color) {
      if (this.alphaAnim > 0.99F) {
         Render2DEngine.drawClickGuiRound(matrices, x, y, width, height, radius, color);
      } else {
         Render2DEngine.drawRound(matrices, x, y, width, height, radius, color);
      }
   }

   private List<String> getTabHints() {
      List<String> hints = switch (this.currentTab) {
         case FRIEND -> List.of("Right Click: Remove");
         case CONFIG -> List.of("Right Click: Remove", "Left Click: Load config");
         case MACRO -> List.of("Right Click: Remove", "Mid Click: Change bind", "Left Click: Edit macro");
      };
      FontRenderer font = FontRenderers.sf_bold_mini;
      return hints.stream().sorted((left, right) -> Float.compare(font.getStringWidth(left), font.getStringWidth(right))).toList();
   }

   private void drawOutlinedHintText(MatrixStack matrices, FontRenderer font, String text, float x, float y) {
      int outlineColor = Render2DEngine.injectAlpha(Color.BLACK, 255).getRGB();
      int fillColor = Render2DEngine.injectAlpha(Color.WHITE, 255).getRGB();
      font.drawString(matrices, text, x - 0.45F, y, outlineColor);
      font.drawString(matrices, text, x + 0.45F, y, outlineColor);
      font.drawString(matrices, text, x, y - 0.45F, outlineColor);
      font.drawString(matrices, text, x, y + 0.45F, outlineColor);
      font.drawString(matrices, text, x, y, fillColor);
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (this.isOpen() && !(this.alphaAnim < 0.5F)) {
         if (this.currentTab == ManagerPanel.Tab.MACRO) {
            this.handleMacroOutsideClick(mouseX, mouseY);
         }

         for (ManagerPanel.Tab tab : ManagerPanel.Tab.values()) {
            float tabY = this.getTabY(tab);
            if (Render2DEngine.isHovered(mouseX, mouseY, this.x + 5.0F, tabY, 70.0, 20.0)) {
               if (button == 0) {
                  if (this.currentTab == ManagerPanel.Tab.MACRO && tab != ManagerPanel.Tab.MACRO) {
                     this.clearMacroInteraction();
                  }

                  this.currentTab = tab;
                  this.scrollAmount = 0.0F;
               }

               return true;
            }
         }

         float listX = this.x + 80.0F + 10.0F;
         float listY = this.y + 30.0F + 10.0F;
         float listWidth = this.width - 80.0F - 20.0F;
         float listHeight = this.height - 30.0F - 20.0F;
         if (!Render2DEngine.isHovered(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
            return false;
         }

         float currentY = listY;
         if (this.currentTab == ManagerPanel.Tab.FRIEND) {
            for (ManagerPanel.FriendDisplayEntry friend : this.getFriendDisplayEntries()) {
               if (friend.header()) {
                  currentY += 14.0F;
               } else {
                  float actualY = currentY + this.scrollAmount;
                  if (actualY >= listY && actualY <= this.y + this.height - 10.0F && Render2DEngine.isHovered(mouseX, mouseY, listX, actualY, listWidth, 20.0)) {
                     if (button == 1) {
                        Managers.FRIEND.removeFriend(friend.name());
                     }

                     return true;
                  }

                  currentY += 25.0F;
               }
            }

            return false;
         } else if (this.currentTab == ManagerPanel.Tab.CONFIG) {
            for (String config : new ArrayList<>(Managers.CONFIG.getConfigList())) {
               float actualY = currentY + this.scrollAmount;
               if (actualY >= listY && actualY <= this.y + this.height - 10.0F && Render2DEngine.isHovered(mouseX, mouseY, listX, actualY, listWidth, 20.0)) {
                  if (button == 1) {
                     Managers.CONFIG.delete(config);
                  } else if (button == 0) {
                     Managers.CONFIG.load(config);
                  }

                  return true;
               }

               currentY += 25.0F;
            }

            return false;
         } else {
            currentY += 18.0F;

            for (MacroManager.Macro macro : this.getMacroEntries()) {
               float actualY = currentY + this.scrollAmount;
               ManagerPanel.MacroLayout layout = this.createMacroLayout(listX, actualY, listWidth);
               if (actualY + 20.0F < listY) {
                  currentY += 25.0F;
               } else {
                  if (actualY > listY + listHeight) {
                     break;
                  }

                  if (!Render2DEngine.isHovered(mouseX, mouseY, layout.cardX(), layout.cardY(), layout.cardWidth(), layout.cardHeight())) {
                     currentY += 25.0F;
                  } else {
                     if (button == 1) {
                        this.clearMacroInteraction();
                        Managers.MACRO.removeMacro(macro);
                        Managers.MACRO.saveMacro();
                        return true;
                     }

                     if (button == 0 && Render2DEngine.isHovered(mouseX, mouseY, layout.nameX(), layout.fieldY(), layout.nameWidth(), layout.fieldHeight())) {
                        this.beginMacroEdit(macro, ManagerPanel.MacroField.NAME);
                        return true;
                     }

                     if (button == 0 && Render2DEngine.isHovered(mouseX, mouseY, layout.textX(), layout.fieldY(), layout.textWidth(), layout.fieldHeight())) {
                        this.beginMacroEdit(macro, ManagerPanel.MacroField.TEXT);
                        return true;
                     }

                     if (button == 2 && Render2DEngine.isHovered(mouseX, mouseY, layout.bindX(), layout.fieldY(), layout.bindWidth(), layout.fieldHeight())) {
                        this.beginMacroBinding(macro);
                        return true;
                     }

                     currentY += 25.0F;
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public void mouseScrolled(double mouseX, double mouseY, double amount) {
      if (this.isOpen() && !(this.alphaAnim < 0.5F)) {
         float listX = this.x + 80.0F + 10.0F;
         float listY = this.y + 30.0F + 10.0F;
         float listWidth = this.width - 80.0F - 20.0F;
         float listHeight = this.height - 30.0F - 20.0F;
         if (Render2DEngine.isHovered((float)mouseX, (float)mouseY, listX, listY, listWidth, listHeight)) {
            this.scrollAmount += (float)amount * 15.0F;
            this.clampScroll(listHeight);
         }
      }
   }

   public boolean keyPressed(int keyCode) {
      if (this.currentTab != ManagerPanel.Tab.MACRO) {
         return false;
      }

      if (this.bindingMacro != null) {
         if (keyCode == 256) {
            this.bindingMacro = null;
         } else if (keyCode == 261) {
            this.bindingMacro.setBind(-1);
            Managers.MACRO.saveMacro();
            this.bindingMacro = null;
         } else {
            this.bindingMacro.setBind(keyCode);
            Managers.MACRO.saveMacro();
            this.bindingMacro = null;
         }

         return true;
      } else {
         if (this.editingField == ManagerPanel.MacroField.NONE || this.editingMacro == null) {
            return false;
         }

         if (keyCode == 257) {
            this.commitMacroEdit();
            return true;
         }

         if (keyCode == 256) {
            this.cancelMacroEdit();
            return true;
         }

         if (keyCode == 259) {
            if (!this.editBuffer.isEmpty()) {
               this.editBuffer = this.editBuffer.substring(0, this.editBuffer.length() - 1);
            }

            return true;
         } else if (keyCode == 32) {
            this.editBuffer = this.editBuffer + " ";
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean charTyped(char key) {
      if (this.currentTab != ManagerPanel.Tab.MACRO || this.editingField == ManagerPanel.MacroField.NONE || this.editingMacro == null) {
         return false;
      }

      if (!StringHelper.isValidChar(key)) {
         return false;
      }

      this.editBuffer = this.editBuffer + key;
      return true;
   }

   public boolean hasActiveMacroInteraction() {
      return this.editingMacro != null || this.bindingMacro != null;
   }

   private void clampScroll(float listHeight) {
      float minScroll = Math.min(0.0F, listHeight - this.getContentHeight());
      if (this.scrollAmount < minScroll) {
         this.scrollAmount = minScroll;
      }

      if (this.scrollAmount > 0.0F) {
         this.scrollAmount = 0.0F;
      }
   }

   private float getContentHeight() {
      if (this.currentTab == ManagerPanel.Tab.FRIEND) {
         float contentHeight = 0.0F;

         for (ManagerPanel.FriendDisplayEntry entry : this.getFriendDisplayEntries()) {
            contentHeight += entry.header() ? 14.0F : 25.0F;
         }

         return contentHeight;
      } else {
         if (this.currentTab == ManagerPanel.Tab.CONFIG) {
            return Managers.CONFIG.getConfigList().size() * 25.0F;
         }

         List<MacroManager.Macro> macros = this.getMacroEntries();
         return macros.isEmpty() ? 74.0F : 18.0F + macros.size() * 25.0F;
      }
   }

   private List<MacroManager.Macro> getMacroEntries() {
      return new ArrayList<>(Managers.MACRO.getMacros());
   }

   private void handleMacroOutsideClick(int mouseX, int mouseY) {
      if (this.editingMacro != null) {
         ManagerPanel.MacroLayout layout = this.findMacroLayout(this.editingMacro);
         if (layout == null || !Render2DEngine.isHovered(mouseX, mouseY, layout.cardX(), layout.cardY(), layout.cardWidth(), layout.cardHeight())) {
            this.cancelMacroEdit();
         }
      }

      if (this.bindingMacro != null) {
         ManagerPanel.MacroLayout layout = this.findMacroLayout(this.bindingMacro);
         if (layout == null || !Render2DEngine.isHovered(mouseX, mouseY, layout.cardX(), layout.cardY(), layout.cardWidth(), layout.cardHeight())) {
            this.bindingMacro = null;
         }
      }
   }

   private ManagerPanel.MacroLayout findMacroLayout(MacroManager.Macro target) {
      float listX = this.x + 80.0F + 10.0F;
      float listY = this.y + 30.0F + 10.0F;
      float listWidth = this.width - 80.0F - 20.0F;
      float currentY = listY + 18.0F;

      for (MacroManager.Macro macro : this.getMacroEntries()) {
         if (macro == target) {
            return this.createMacroLayout(listX, currentY + this.scrollAmount, listWidth);
         }

         currentY += 25.0F;
      }

      return null;
   }

   private void beginMacroEdit(MacroManager.Macro macro, ManagerPanel.MacroField field) {
      this.bindingMacro = null;
      if (!this.isEditing(macro, field)) {
         this.editingMacro = macro;
         this.editingField = field;
         this.originalBuffer = field == ManagerPanel.MacroField.NAME ? macro.getName() : macro.getText();
         this.editBuffer = this.originalBuffer;
      }
   }

   private void beginMacroBinding(MacroManager.Macro macro) {
      this.cancelMacroEdit();
      this.bindingMacro = macro;
   }

   private void cancelMacroEdit() {
      this.editingMacro = null;
      this.editingField = ManagerPanel.MacroField.NONE;
      this.editBuffer = "";
      this.originalBuffer = "";
   }

   private void commitMacroEdit() {
      if (this.editingMacro != null && this.editingField != ManagerPanel.MacroField.NONE) {
         String nextValue = this.editBuffer.trim().isEmpty() ? this.originalBuffer : this.editBuffer;
         if (this.editingField == ManagerPanel.MacroField.NAME) {
            this.editingMacro.setName(nextValue);
         } else if (this.editingField == ManagerPanel.MacroField.TEXT) {
            this.editingMacro.setText(nextValue);
         }

         Managers.MACRO.saveMacro();
         this.cancelMacroEdit();
      }
   }

   private void clearMacroInteraction() {
      this.cancelMacroEdit();
      this.bindingMacro = null;
   }

   private boolean isEditing(MacroManager.Macro macro, ManagerPanel.MacroField field) {
      return this.editingMacro == macro && this.editingField == field;
   }

   private String getMacroFieldText(MacroManager.Macro macro, ManagerPanel.MacroField field) {
      if (!this.isEditing(macro, field)) {
         return field == ManagerPanel.MacroField.NAME ? macro.getName() : macro.getText();
      } else {
         return this.editBuffer + (this.isCursorVisible() ? "|" : "");
      }
   }

   private boolean isCursorVisible() {
      return System.currentTimeMillis() / 400L % 2L == 0L;
   }

   private float getTabY(ManagerPanel.Tab tab) {
      return switch (tab) {
         case FRIEND -> this.getTopTabY(0);
         case CONFIG -> this.getTopTabY(1);
         case MACRO -> this.getTopTabY(2);
      };
   }

   private ManagerPanel.MacroLayout createMacroLayout(float listX, float rowY, float listWidth) {
      float cardX = listX;
      float cardY = rowY;
      float cardWidth = listWidth;
      float cardHeight = 20.0F;
      float fieldY = cardY + 2.0F;
      float fieldHeight = cardHeight - 4.0F;
      float contentX = cardX + 2.0F;
      float contentWidth = cardWidth - 4.0F;
      float bindWidth = Math.min(62.0F, Math.max(52.0F, contentWidth * 0.18F));
      float nameWidth = Math.max(72.0F, (contentWidth - bindWidth - 12.0F) * 0.3F);
      float textWidth = Math.max(60.0F, contentWidth - nameWidth - bindWidth - 12.0F);
      float nameX = contentX;
      float bindX = nameX + nameWidth + 6.0F;
      float textX = bindX + bindWidth + 6.0F;
      return new ManagerPanel.MacroLayout(cardX, cardY, cardWidth, cardHeight, fieldY, fieldHeight, nameX, nameWidth, bindX, bindWidth, textX, textWidth);
   }

   private String formatBind(int key) {
      if (key == -1) {
         return "None";
      }

      String keyName = GLFW.glfwGetKeyName(key, GLFW.glfwGetKeyScancode(key));
      if (keyName == null) {
         try {
            for (Field field : GLFW.class.getDeclaredFields()) {
               if (field.getName().startsWith("GLFW_KEY_") && (Integer)field.get(null) == key) {
                  String name = field.getName().substring("GLFW_KEY_".length());
                  keyName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                  break;
               }
            }
         } catch (IllegalAccessException ignored) {
            keyName = "Unknown";
         }
      }

      if (keyName == null) {
         keyName = "Unknown";
      }
      return switch (keyName.toUpperCase()) {
         case "LEFT_CONTROL" -> "LCtrl";
         case "RIGHT_CONTROL" -> "RCtrl";
         case "LEFT_SHIFT" -> "LShift";
         case "RIGHT_SHIFT" -> "RShift";
         case "LEFT_ALT" -> "LAlt";
         case "RIGHT_ALT" -> "RAlt";
         default -> keyName.toUpperCase();
      };
   }

   private List<ManagerPanel.FriendDisplayEntry> getFriendDisplayEntries() {
      ManagerPanel.FriendGroups groups = this.getFriendGroups();
      List<ManagerPanel.FriendDisplayEntry> entries = new ArrayList<>();
      this.appendFriendGroup(entries, "Friend Online - " + groups.online().size(), groups.online(), true, true);
      this.appendFriendGroup(entries, "Offline", groups.offline(), false, false);
      return entries;
   }

   private void appendFriendGroup(
      List<ManagerPanel.FriendDisplayEntry> entries, String title, List<String> friends, boolean online, boolean keepHeaderWhenEmpty
   ) {
      if (!friends.isEmpty() || keepHeaderWhenEmpty) {
         entries.add(new ManagerPanel.FriendDisplayEntry(title, online, true));

         for (String friend : friends) {
            entries.add(new ManagerPanel.FriendDisplayEntry(friend, online, false));
         }
      }
   }

   private ManagerPanel.FriendGroups getFriendGroups() {
      List<String> online = new ArrayList<>();
      List<String> offline = new ArrayList<>();

      for (String friend : Managers.FRIEND.getFriends()) {
         if (this.isOnline(friend)) {
            online.add(friend);
         } else {
            offline.add(friend);
         }
      }

      online.sort(String.CASE_INSENSITIVE_ORDER);
      offline.sort(String.CASE_INSENSITIVE_ORDER);
      return new ManagerPanel.FriendGroups(online, offline);
   }

   private String getCurrentConfigName() {
      if (Managers.CONFIG.currentConfig == null) {
         Managers.CONFIG.getCurrentConfig();
      }

      return Managers.CONFIG.currentConfig == null ? "" : this.normalizeConfigName(Managers.CONFIG.currentConfig.getName());
   }

   private String normalizeConfigName(String name) {
      return name == null ? "" : name.replace(".vc", "").replace(".json", "");
   }

   private FontRenderer getMarkedTextFont() {
      return FontRenderers.inter_target_hp != null ? FontRenderers.inter_target_hp : FontRenderers.sf_bold_mini;
   }

   private boolean isOnline(String name) {
      if (Module.mc.getNetworkHandler() == null) {
         return false;
      }

      for (PlayerListEntry entry : Module.mc.getNetworkHandler().getPlayerList()) {
         if (entry.getProfile().getName().equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   private record FriendDisplayEntry(String name, boolean online, boolean header) {
   }

   private record FriendGroups(List<String> online, List<String> offline) {
   }

   private enum MacroField {
      NONE,
      NAME,
      TEXT;
   }

   private record MacroLayout(
      float cardX,
      float cardY,
      float cardWidth,
      float cardHeight,
      float fieldY,
      float fieldHeight,
      float nameX,
      float nameWidth,
      float bindX,
      float bindWidth,
      float textX,
      float textWidth
   ) {
   }

   public enum Tab {
      FRIEND("Friend"),
      CONFIG("Config"),
      MACRO("Macro");

      private final String displayName;

      Tab(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }
}
