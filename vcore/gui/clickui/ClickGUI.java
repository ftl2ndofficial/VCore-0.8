package vcore.gui.clickui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.hud.HudElement;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.impl.SearchBar;
import vcore.gui.clickui.impl.ThemeSelector;
import vcore.gui.font.FontRenderer;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.animation.EaseOutBack;

public class ClickGUI extends Screen {
   public static List<AbstractCategory> windows;
   public static boolean anyHovered;
   private boolean firstOpen;
   private float scrollY;
   public static boolean close = false;
   public static boolean imageDirection;
   private final SearchBar searchBar = new SearchBar();
   private final ThemeSelector themeSelector = new ThemeSelector();
   private float managerBtnX;
   private float managerBtnY;
   private float managerBtnSize;
   public static String currentDescription = "";
   public static boolean descriptionActive;
   public EaseOutBack imageAnimation = new EaseOutBack(20);
   private boolean closing;
   private static final float TOP_OFFSET = 90.0F;
   private static final float BOTTOM_OFFSET = 60.0F;
   private static final float HINT_MARGIN = 8.0F;
   private static final float HINT_LINE_GAP = 2.0F;
   private static final float HINT_OUTLINE_OFFSET = 0.45F;
   private static ClickGUI INSTANCE = new ClickGUI();

   public ClickGUI() {
      super(Text.of("NewClickGUI"));
      windows = Lists.newArrayList();
      this.firstOpen = true;
      this.setInstance();
   }

   public static ClickGUI getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGUI();
      }

      imageDirection = true;
      return INSTANCE;
   }

   public static ClickGUI getClickGui() {
      windows.forEach(AbstractCategory::init);
      return getInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   protected void method_25426() {
      SearchBar.resetState();
      List<Module.Category> visibleCategories = Lists.newArrayList(Managers.MODULE.getCategories());
      int panelWidth = 125;
      int panelHeight = 280;
      int panelMargin = 8;
      int totalWidth = visibleCategories.size() * (panelWidth + panelMargin) - panelMargin;
      int startX = (Module.mc.getWindow().getScaledWidth() - totalWidth) / 2;
      int startY = (Module.mc.getWindow().getScaledHeight() - panelHeight) / 2;
      if (this.firstOpen) {
         int i = 0;

         for (Module.Category category : visibleCategories) {
            Category window = new Category(
               category, Managers.MODULE.getModulesByCategory(category), startX + i * (panelWidth + panelMargin), startY, panelWidth, 20.0F
            );
            window.setOpen(true);
            windows.add(window);
            i++;
         }

         this.firstOpen = false;
      }

      this.imageAnimation.reset();
      imageDirection = true;
      this.closing = false;
      int i = 0;

      for (AbstractCategory w : windows) {
         w.setX(startX + i * (panelWidth + panelMargin));
         w.setY(startY);
         w.setHeight(panelHeight);
         i++;
      }

      windows.forEach(AbstractCategory::init);
   }

   public boolean method_25421() {
      return false;
   }

   public void method_25393() {
      windows.forEach(AbstractCategory::tick);
      this.searchBar.tick();
      this.themeSelector.tick();
      this.imageAnimation.update(imageDirection);
      if (this.closing && this.getOpenProgress() <= 0.01F) {
         Module.mc.setScreen(null);
         this.closing = false;
      }
   }

   public void method_25394(DrawContext context, int mouseX, int mouseY, float delta) {
      if (ModuleManager.clickGui.blur.getValue()) {
         this.method_57734(delta);
      }

      anyHovered = false;
      descriptionActive = false;
      currentDescription = "";
      if (Module.fullNullCheck()) {
         this.method_25420(context, mouseX, mouseY, delta);
      }

      int panelWidth = 125;
      int panelHeight = 280;
      int panelMargin = 8;
      List<Module.Category> visibleCategories = Lists.newArrayList(Managers.MODULE.getCategories());
      int totalWidth = visibleCategories.size() * (panelWidth + panelMargin) - panelMargin;
      int startX = (Module.mc.getWindow().getScaledWidth() - totalWidth) / 2;
      int startY = (Module.mc.getWindow().getScaledHeight() - panelHeight) / 2;
      float anim = this.getOpenProgress();
      int i = 0;

      for (AbstractCategory w : windows) {
         float targetX = startX + i * (panelWidth + panelMargin);
         float targetY = startY;
         float offsetY = 0.0F;
         String name = w.getName();
         float progress = this.getDelayedProgress(anim, name);
         float invProgress = 1.0F - progress;
         offsetY = -90.0F * invProgress;
         w.setX(targetX);
         w.setY(targetY + offsetY);
         i++;
      }

      for (AbstractCategory window : windows) {
         if (this.scrollY != 0.0F) {
            window.setModuleOffset(this.scrollY, mouseX, mouseY);
         }
      }

      this.scrollY = 0.0F;

      for (AbstractCategory w : windows) {
         float progress = this.getDelayedProgress(anim, w.getName());
         if (!(progress <= 0.0F)) {
            float alpha = MathHelper.clamp(anim * progress, 0.0F, 1.0F);
            Render2DEngine.setGlobalAlpha(alpha);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            w.render(context, mouseX, mouseY, delta);
            Render2DEngine.setGlobalAlpha(1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      Render2DEngine.setGlobalAlpha(anim);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, anim);
      this.renderBottomBar(context, mouseX, mouseY, delta, anim);
      Render2DEngine.setGlobalAlpha(1.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.renderInteractionHints(context.getMatrices());
      boolean fullyOpen = anim >= 0.999F && imageDirection;
      if (fullyOpen && descriptionActive && !Objects.equals(currentDescription, "")) {
         float paddingX = 8.0F;
         float paddingY = 4.0F;
         float textWidth = FontRenderers.sf_medium.getStringWidth(currentDescription);
         float textHeight = FontRenderers.sf_medium.getFontHeight(currentDescription);
         float descWidth = Math.max(60.0F, textWidth + paddingX * 2.0F);
         float descHeight = textHeight + paddingY * 2.0F;
         float descY = startY - descHeight - 10.0F;
         float descX = (Module.mc.getWindow().getScaledWidth() - descWidth) / 2.0F;
         float textY = descY + (descHeight - textHeight) / 2.0F + 3.0F;
         Color bg = new Color(25, 25, 28, 200);
         Render2DEngine.drawClickGuiRound(context.getMatrices(), descX, descY, descWidth, descHeight, descHeight / 2.0F, bg);
         FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), currentDescription, descX + descWidth / 2.0F, textY, Color.WHITE.getRGB());
      }

      if (!HudElement.anyHovered && !anyHovered && GLFW.glfwGetPlatform() != 393219) {
         GLFW.glfwSetCursor(Module.mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221185));
      }
   }

   public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.scrollY += (int)(verticalAmount * 15.0);
      return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (Render2DEngine.isHovered((float)mouseX, (float)mouseY, this.managerBtnX, this.managerBtnY, this.managerBtnSize, this.managerBtnSize) && button == 0) {
         Module.mc.setScreen(new ManagerScreen(this));
         return true;
      } else {
         this.searchBar.mouseClicked((int)mouseX, (int)mouseY, button);
         this.themeSelector.mouseClicked((int)mouseX, (int)mouseY, button);
         windows.forEach(w -> {
            w.mouseClicked((int)mouseX, (int)mouseY, button);
            windows.forEach(w1 -> {
               if (w.dragging && w != w1) {
                  w1.dragging = false;
               }
            });
         });
         return super.method_25402(mouseX, mouseY, button);
      }
   }

   public boolean method_25406(double mouseX, double mouseY, int button) {
      this.searchBar.mouseReleased((int)mouseX, (int)mouseY, button);
      this.themeSelector.mouseReleased((int)mouseX, (int)mouseY, button);
      windows.forEach(w -> w.mouseReleased((int)mouseX, (int)mouseY, button));
      return super.method_25406(mouseX, mouseY, button);
   }

   private void renderBottomBar(DrawContext context, int mouseX, int mouseY, float delta, float anim) {
      float panelHeight = 280.0F;
      float searchWidth = Math.min(180.0F, Module.mc.getWindow().getScaledWidth() - 40.0F);
      float searchHeight = 20.0F;
      float searchMarginBottom = 10.0F;
      float themeButtonSize = 16.0F;
      float gap = 5.0F;
      float themeHeight = 16.0F;
      float themeMarginBottom = 40.0F;
      float searchX = (Module.mc.getWindow().getScaledWidth() - searchWidth) / 2.0F;
      float searchY = (Module.mc.getWindow().getScaledHeight() + panelHeight) / 2.0F + searchMarginBottom;
      float buttonX = searchX + searchWidth + gap;
      float buttonY = searchY + (searchHeight - themeButtonSize) / 2.0F;
      float themeWidth = searchWidth;
      float paletteX = (Module.mc.getWindow().getScaledWidth() - themeWidth) / 2.0F;
      float paletteY = (Module.mc.getWindow().getScaledHeight() + panelHeight) / 2.0F + themeMarginBottom;
      float bottomOffset = 60.0F * (1.0F - anim);
      searchY += bottomOffset;
      buttonY += bottomOffset;
      paletteY += bottomOffset;
      this.searchBar.setX(searchX);
      this.searchBar.setY(searchY);
      this.searchBar.setWidth(searchWidth);
      this.searchBar.setHeight(searchHeight);
      this.searchBar.render(context, mouseX, mouseY, delta);
      this.themeSelector.setLayout(buttonX, buttonY, themeButtonSize, paletteX, themeWidth, paletteY, themeHeight);
      this.themeSelector.render(context, mouseX, mouseY, delta);
      this.managerBtnX = buttonX + themeButtonSize + gap;
      this.managerBtnY = buttonY;
      this.managerBtnSize = themeButtonSize;
      boolean managerHovered = Render2DEngine.isHovered(mouseX, mouseY, this.managerBtnX, this.managerBtnY, this.managerBtnSize, this.managerBtnSize);
      int hudAlpha = Math.round(255.0F * HudEditor.getAlpha());
      Color btnBg = Render2DEngine.injectAlpha(new Color(25, 25, 28), hudAlpha);
      Render2DEngine.drawClickGuiRound(
         context.getMatrices(), this.managerBtnX, this.managerBtnY, this.managerBtnSize, this.managerBtnSize, this.managerBtnSize / 4.0F, btnBg
      );
      Color iconCol = Render2DEngine.injectAlpha(Color.WHITE, hudAlpha);
      FontRenderers.sf_bold_mini
         .drawString(
            context.getMatrices(),
            "...",
            this.managerBtnX + this.managerBtnSize / 2.0F - FontRenderers.sf_bold_mini.getStringWidth("...") / 2.0F,
            this.managerBtnY + this.managerBtnSize / 2.0F - FontRenderers.sf_bold_mini.getFontHeight("...") / 2.0F + 1.0F,
            iconCol.getRGB()
         );
      if (managerHovered) {
         anyHovered = true;
      }

      if (this.themeSelector.shouldRenderPalette()) {
         this.themeSelector.renderPalette(context, mouseX, mouseY, delta, paletteX, themeWidth, paletteY, themeHeight);
      }
   }

   private void renderInteractionHints(MatrixStack matrices) {
      if (Module.mc.getWindow() != null) {
         FontRenderer font = FontRenderers.sf_bold_mini;
         List<String> hints = this.getInteractionHints(font);
         float lineHeight = font.getFontHeight("A") + 2.0F;
         float startY = Module.mc.getWindow().getScaledHeight() - 8.0F - lineHeight * hints.size();
         float startX = 8.0F;

         for (int i = 0; i < hints.size(); i++) {
            this.drawOutlinedHintText(matrices, font, hints.get(i), startX, startY + i * lineHeight);
         }
      }
   }

   private List<String> getInteractionHints(FontRenderer font) {
      return List.of(
            "Left Click: Enable/Disable modules",
            "Right Click: Open setting",
            "Mid Click: Change bind",
            "Del + Left Click: Reset modules",
            "Ctrl + F: Search modules"
         )
         .stream()
         .sorted((left, right) -> Float.compare(font.getStringWidth(left), font.getStringWidth(right)))
         .toList();
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

   public boolean method_25400(char key, int modifier) {
      this.searchBar.charTyped(key, modifier);
      windows.forEach(w -> w.charTyped(key, modifier));
      return true;
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      this.searchBar.keyTyped(keyCode);
      windows.forEach(w -> w.keyTyped(keyCode));
      if (keyCode == 256) {
         imageDirection = false;
         this.closing = false;
         Module.mc.setScreen(null);
         return true;
      } else {
         return false;
      }
   }

   private float getOpenProgress() {
      return MathHelper.clamp((float)this.imageAnimation.getAnimationd(), 0.0F, 1.0F);
   }

   private float getDelayedProgress(float progress, String name) {
      float delay = 0.0F;
      if (name != null) {
         if ("Movement".equalsIgnoreCase(name) || "Player".equalsIgnoreCase(name)) {
            delay = 0.4F;
         } else if ("Combat".equalsIgnoreCase(name) || "Misc".equalsIgnoreCase(name)) {
            delay = 0.8F;
         }
      }

      if (delay <= 0.0F) {
         return MathHelper.clamp(progress, 0.0F, 1.0F);
      } else {
         return imageDirection ? this.applyDelay(progress, delay) : MathHelper.clamp(progress, 0.0F, 1.0F);
      }
   }

   private float applyDelay(float value, float delay) {
      return value <= delay ? 0.0F : MathHelper.clamp((value - delay) / (1.0F - delay), 0.0F, 1.0F);
   }

   public static void requestDescription(String description) {
      currentDescription = description;
      descriptionActive = true;
   }
}
