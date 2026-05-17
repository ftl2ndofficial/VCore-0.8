package vcore.gui.clickui.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderer;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.render.Render2DEngine;

public class ItemSelectScreen extends Screen {
   private static final float WIDTH = 260.0F;
   private static final float HEIGHT = 320.0F;
   private static final float PADDING = 10.0F;
   private static final float HEADER_HEIGHT = 28.0F;
   private static final float SEARCH_WIDTH = 90.0F;
   private static final float SEARCH_HEIGHT = 12.0F;
   private static final float CLOSE_BUTTON_SIZE = 10.0F;
   private static final float SEARCH_CLOSE_GAP = 6.0F;
   private static final float TAB_HEIGHT = 16.0F;
   private static final float ROW_HEIGHT = 20.0F;
   private static final float ROW_BACKGROUND_HEIGHT = 18.0F;
   private static final float ACTION_BUTTON_SIZE = 12.0F;
   private static final float ACTION_BUTTON_OFFSET_X = 18.0F;
   private static final float ITEM_ICON_SIZE = 16.0F;
   private static final float FONT_RENDER_Y_OFFSET = 3.0F;
   private static List<ItemSelectScreen.ItemPlate> cachedCatalog = List.of();
   private static String cachedLanguage;
   private final Screen parent;
   private final Setting<ItemSelectSetting> itemSetting;
   private final List<ItemSelectScreen.ItemPlate> selectedItems = new ArrayList<>();
   private final List<ItemSelectScreen.ItemPlate> allItems = new ArrayList<>();
   private final Set<String> selectedItemIds = new HashSet<>();
   private boolean allTab = true;
   private boolean listening;
   private String search = "Search";
   private float scrollOffset;

   public ItemSelectScreen(Screen parent, Setting<ItemSelectSetting> itemSetting) {
      super(Text.of("ItemSelect"));
      this.parent = parent;
      this.itemSetting = itemSetting;
      ensureCatalog();
      this.refreshSelectedIds();
      this.refreshItemPlates();
      this.refreshAllItems();
   }

   public boolean method_25421() {
      return false;
   }

   public void method_25394(DrawContext context, int mouseX, int mouseY, float delta) {
      if (ModuleManager.clickGui.blur.getValue()) {
         this.method_57734(delta);
      }

      if (Module.fullNullCheck()) {
         this.method_25420(context, mouseX, mouseY, delta);
      }

      float x = (this.field_22789 - 260.0F) / 2.0F;
      float y = (this.field_22790 - 320.0F) / 2.0F;
      Render2DEngine.drawClickGuiRound(context.getMatrices(), x, y, 260.0F, 320.0F, 8.0F, new Color(28, 28, 30, 230));
      Render2DEngine.drawClickGuiRound(context.getMatrices(), x, y, 260.0F, 28.0F, 8.0F, new Color(24, 24, 26, 210));
      String title = "Items / " + this.itemSetting.getModule().getName();
      FontRenderers.sf_medium.drawString(context.getMatrices(), title, x + 8.0F, y + 9.0F, Color.WHITE.getRGB());
      float searchX = this.getSearchX(x);
      float searchY = this.getSearchY(y);
      float closeX = this.getCloseButtonX(x);
      float closeY = this.getCloseButtonY(y);
      Render2DEngine.drawClickGuiRound(context.getMatrices(), closeX, closeY, 10.0F, 10.0F, 2.0F, new Color(75, 75, 75, 180));
      Render2DEngine.drawLine(closeX + 2.0F, closeY + 2.0F, closeX + 8.0F, closeY + 8.0F, Color.WHITE.getRGB());
      Render2DEngine.drawLine(closeX + 2.0F, closeY + 8.0F, closeX + 8.0F, closeY + 2.0F, Color.WHITE.getRGB());
      Render2DEngine.drawClickGuiRound(context.getMatrices(), searchX, searchY, 90.0F, 12.0F, 3.0F, new Color(54, 54, 56, 180));
      String searchDisplay = this.getSearchDisplayText();
      float searchTextX = searchX + 4.0F;
      float searchTextY = this.getCenteredTextY(FontRenderers.sf_medium_mini, searchDisplay.isEmpty() ? "|" : searchDisplay, searchY, 12.0F);
      FontRenderers.sf_medium_mini.drawString(context.getMatrices(), searchDisplay, searchTextX, searchTextY, new Color(14013909).getRGB());
      if (this.shouldRenderSearchCaret()) {
         float caretX = searchTextX + FontRenderers.sf_medium_mini.getStringWidth(searchDisplay);
         FontRenderers.sf_medium_mini.drawString(context.getMatrices(), "|", caretX, searchTextY, Color.WHITE.getRGB());
      }

      float allTabX = x + 10.0F;
      float selectedTabX = allTabX + 48.0F;
      float tabY = y + 28.0F + 4.0F;
      this.drawTab(context, "All", allTabX, tabY, 40.0F, this.allTab);
      this.drawTab(context, "Selected", selectedTabX, tabY, 58.0F, !this.allTab);
      List<ItemSelectScreen.ItemPlate> activeItems = this.allTab ? this.allItems : this.selectedItems;
      float listX = x + 10.0F;
      float listY = y + 28.0F + 16.0F + 10.0F;
      float listWidth = 240.0F;
      float listHeight = 256.0F;
      this.clampScroll(listHeight, activeItems.size());
      Render2DEngine.beginScissor(listX, listY, listX + listWidth, listY + listHeight);
      if (activeItems.isEmpty()) {
         FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), "It's empty here yet", x + 130.0F, listY + 18.0F, new Color(12434877).getRGB());
      }

      int startIndex = Math.max(0, (int)Math.floor(-this.scrollOffset / 20.0F));
      int visibleCount = Math.max(1, (int)Math.ceil(listHeight / 20.0F) + 2);
      int endIndex = Math.min(activeItems.size(), startIndex + visibleCount);

      for (int index = startIndex; index < endIndex; index++) {
         ItemSelectScreen.ItemPlate itemPlate = activeItems.get(index);
         float rowY = listY + this.scrollOffset + index * 20.0F;
         this.drawRow(context, itemPlate, listX, rowY, listWidth, mouseX, mouseY);
      }

      Render2DEngine.endScissor();
   }

   private void drawTab(DrawContext context, String title, float x, float y, float width, boolean active) {
      Render2DEngine.drawClickGuiRound(context.getMatrices(), x, y, width, 16.0F, 3.0F, active ? new Color(90, 90, 96, 170) : new Color(48, 48, 52, 140));
      FontRenderers.sf_medium_mini
         .drawCenteredString(
            context.getMatrices(),
            title,
            x + width / 2.0F,
            this.getCenteredTextY(FontRenderers.sf_medium_mini, title, y, 16.0F),
            active ? Color.WHITE.getRGB() : new Color(190, 190, 190).getRGB()
         );
   }

   private void drawRow(DrawContext context, ItemSelectScreen.ItemPlate itemPlate, float x, float y, float width, int mouseX, int mouseY) {
      boolean hovered = Render2DEngine.isHovered(mouseX, mouseY, x, y, width, 20.0);
      Render2DEngine.drawClickGuiRound(context.getMatrices(), x, y, width, 18.0F, 3.0F, hovered ? new Color(70, 70, 75, 130) : new Color(52, 52, 56, 110));
      context.getMatrices().push();
      context.getMatrices().translate(x + 4.0F, y + 1.0F, 0.0F);
      context.drawItem(itemPlate.stack(), 0, 0);
      context.getMatrices().pop();
      FontRenderers.sf_medium
         .drawString(
            context.getMatrices(),
            itemPlate.displayName(),
            x + 24.0F,
            this.getCenteredTextY(FontRenderers.sf_medium, itemPlate.displayName(), y, 18.0F),
            new Color(14013909).getRGB()
         );
      boolean selected = this.selectedItemIds.contains(itemPlate.simpleId());
      String actionLabel = selected ? "-" : "+";
      float actionX = this.getActionButtonX(x, width);
      float actionY = this.getActionButtonY(y);
      Render2DEngine.drawClickGuiRound(context.getMatrices(), actionX, actionY, 12.0F, 12.0F, 3.0F, new Color(82, 82, 88, 180));
      FontRenderers.sf_medium_mini
         .drawCenteredString(
            context.getMatrices(),
            actionLabel,
            actionX + 6.0F,
            this.getCenteredTextY(FontRenderers.sf_medium_mini, actionLabel, actionY, 12.0F),
            Color.WHITE.getRGB()
         );
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      float x = (this.field_22789 - 260.0F) / 2.0F;
      float y = (this.field_22790 - 320.0F) / 2.0F;
      if (Render2DEngine.isHovered(mouseX, mouseY, this.getCloseButtonX(x), this.getCloseButtonY(y), 10.0, 10.0)) {
         this.method_25419();
         return true;
      }

      if (Render2DEngine.isHovered(mouseX, mouseY, this.getSearchX(x), this.getSearchY(y), 90.0, 12.0)) {
         this.listening = true;
         if ("Search".equals(this.search)) {
            this.search = "";
         }

         return true;
      } else {
         if (this.listening) {
            this.listening = false;
            if (this.search.isEmpty()) {
               this.search = "Search";
            }
         }

         if (Render2DEngine.isHovered(mouseX, mouseY, x + 10.0F, y + 28.0F + 4.0F, 40.0, 16.0)) {
            this.allTab = true;
            this.scrollOffset = 0.0F;
            return true;
         }

         if (Render2DEngine.isHovered(mouseX, mouseY, x + 10.0F + 48.0F, y + 28.0F + 4.0F, 58.0, 16.0)) {
            this.allTab = false;
            this.scrollOffset = 0.0F;
            return true;
         }

         List<ItemSelectScreen.ItemPlate> activeItems = this.allTab ? this.allItems : this.selectedItems;
         float listX = x + 10.0F;
         float listY = y + 28.0F + 16.0F + 10.0F;
         float listWidth = 240.0F;
         float listHeight = 256.0F;
         if (!Render2DEngine.isHovered(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
            return super.method_25402(mouseX, mouseY, button);
         }

         int startIndex = Math.max(0, (int)Math.floor(-this.scrollOffset / 20.0F));
         int visibleCount = Math.max(1, (int)Math.ceil(listHeight / 20.0F) + 2);
         int endIndex = Math.min(activeItems.size(), startIndex + visibleCount);

         for (int index = startIndex; index < endIndex; index++) {
            ItemSelectScreen.ItemPlate itemPlate = activeItems.get(index);
            float rowY = listY + this.scrollOffset + index * 20.0F;
            if (Render2DEngine.isHovered(mouseX, mouseY, this.getActionButtonX(listX, listWidth), this.getActionButtonY(rowY), 12.0, 12.0)) {
               String itemId = itemPlate.simpleId();
               boolean selected = this.selectedItemIds.contains(itemId);
               if (selected) {
                  this.itemSetting.getValue().getItemsById().remove(itemId);
               } else {
                  this.itemSetting.getValue().getItemsById().add(itemId);
               }

               this.refreshSelectedIds();
               this.refreshItemPlates();
               this.refreshAllItems();
               return true;
            }
         }

         return super.method_25402(mouseX, mouseY, button);
      }
   }

   public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float x = (this.field_22789 - 260.0F) / 2.0F;
      float y = (this.field_22790 - 320.0F) / 2.0F;
      float listX = x + 10.0F;
      float listY = y + 28.0F + 16.0F + 10.0F;
      float listWidth = 240.0F;
      float listHeight = 256.0F;
      if (Render2DEngine.isHovered(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
         this.scrollOffset += (float)verticalAmount * 12.0F;
         this.clampScroll(listHeight, (this.allTab ? this.allItems : this.selectedItems).size());
         return true;
      } else {
         return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (this.listening) {
         if (keyCode == 256) {
            this.listening = false;
            this.search = "Search";
            this.refreshAllItems();
            return true;
         }

         if (keyCode == 259) {
            if (!this.search.isEmpty() && !"Search".equals(this.search)) {
               this.search = this.search.substring(0, this.search.length() - 1);
            }

            if (this.search.isEmpty()) {
               this.listening = false;
               this.search = "Search";
            }

            this.refreshAllItems();
            return true;
         }

         if (keyCode == 32) {
            this.search = this.search + " ";
            this.refreshAllItems();
            return true;
         }
      }

      if (keyCode == 256) {
         this.method_25419();
         return true;
      } else {
         return super.method_25404(keyCode, scanCode, modifiers);
      }
   }

   public boolean method_25400(char chr, int modifiers) {
      if (this.listening && StringHelper.isValidChar(chr)) {
         if ("Search".equals(this.search)) {
            this.search = "";
         }

         this.search = this.search + chr;
         this.refreshAllItems();
         return true;
      } else {
         return super.method_25400(chr, modifiers);
      }
   }

   public void method_25419() {
      if (this.field_22787 != null) {
         this.field_22787.setScreen(this.parent);
      }
   }

   private void clampScroll(float listHeight, int itemCount) {
      float contentHeight = itemCount * 20.0F;
      float minScroll = Math.min(0.0F, listHeight - contentHeight);
      if (this.scrollOffset < minScroll) {
         this.scrollOffset = minScroll;
      }

      if (this.scrollOffset > 0.0F) {
         this.scrollOffset = 0.0F;
      }
   }

   private void refreshItemPlates() {
      this.selectedItems.clear();

      for (ItemSelectScreen.ItemPlate itemPlate : cachedCatalog) {
         if (this.selectedItemIds.contains(itemPlate.simpleId())) {
            this.selectedItems.add(itemPlate);
         }
      }
   }

   private void refreshAllItems() {
      this.allItems.clear();
      String loweredQuery = this.getSearchQuery().toLowerCase(Locale.ROOT);

      for (ItemSelectScreen.ItemPlate itemPlate : cachedCatalog) {
         if (!this.selectedItemIds.contains(itemPlate.simpleId()) && (loweredQuery.isEmpty() || itemPlate.searchKey().contains(loweredQuery))) {
            this.allItems.add(itemPlate);
         }
      }
   }

   private void refreshSelectedIds() {
      this.selectedItemIds.clear();
      this.selectedItemIds.addAll(this.itemSetting.getValue().getItemsById());
   }

   private static String simplifyId(String translationKey) {
      return translationKey.replace("item.minecraft.", "").replace("block.minecraft.", "");
   }

   private String getSearchQuery() {
      return "Search".equals(this.search) ? "" : this.search.trim();
   }

   private float getCenteredTextY(FontRenderer font, String text, float top, float height) {
      return top + (height - font.getFontHeight(text)) / 2.0F + 3.0F;
   }

   private float getActionButtonX(float rowX, float rowWidth) {
      return rowX + rowWidth - 18.0F;
   }

   private float getActionButtonY(float rowY) {
      return rowY + 3.0F;
   }

   private float getSearchX(float panelX) {
      return this.getCloseButtonX(panelX) - 6.0F - 90.0F;
   }

   private float getSearchY(float panelY) {
      return panelY + 8.0F;
   }

   private float getCloseButtonX(float panelX) {
      return panelX + 260.0F - 8.0F - 10.0F;
   }

   private float getCloseButtonY(float panelY) {
      return this.getSearchY(panelY) + 1.0F;
   }

   private String getSearchDisplayText() {
      if (this.listening) {
         return "Search".equals(this.search) ? "" : this.search;
      } else {
         return this.search;
      }
   }

   private boolean shouldRenderSearchCaret() {
      return this.listening && System.currentTimeMillis() / 500L % 2L == 0L;
   }

   private static void ensureCatalog() {
      MinecraftClient client = MinecraftClient.getInstance();
      String currentLanguage = client.options.language;
      if (cachedCatalog.isEmpty() || !Objects.equals(cachedLanguage, currentLanguage)) {
         Map<String, ItemSelectScreen.ItemPlate> deduplicated = new LinkedHashMap<>();

         for (Block block : Registries.BLOCK) {
            Item item = block.method_8389();
            if (!shouldSkipCatalogEntry(item)) {
               cachePlate(deduplicated, item, block.getTranslationKey());
            }
         }

         for (Item item : Registries.ITEM) {
            if (!shouldSkipCatalogEntry(item)) {
               cachePlate(deduplicated, item, item.getTranslationKey());
            }
         }

         cachedCatalog = List.copyOf(deduplicated.values());
         cachedLanguage = currentLanguage;
      }
   }

   private static void cachePlate(Map<String, ItemSelectScreen.ItemPlate> destination, Item item, String translationKey) {
      String simpleId = simplifyId(translationKey);
      if (!destination.containsKey(simpleId)) {
         String displayName = "block.minecraft.air".equals(translationKey) ? I18n.translate(translationKey, new Object[0]) : item.getName().getString();
         String searchKey = (translationKey + " " + displayName).toLowerCase(Locale.ROOT);
         destination.put(simpleId, new ItemSelectScreen.ItemPlate(item.getDefaultStack(), simpleId, displayName, searchKey));
      }
   }

   private static boolean shouldSkipCatalogEntry(Item item) {
      return item == Items.AIR || item.getDefaultStack().isEmpty();
   }

   private record ItemPlate(ItemStack stack, String simpleId, String displayName, String searchKey) {
   }
}
