package vcore.gui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderer;
import vcore.gui.font.FontRenderers;
import vcore.utility.ThunderUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

public class MainMenuScreen extends Screen {
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
   private static final float BUTTON_SCALE = 0.4F;
   private static final float BUTTON_SIZE = 21.6F;
   private static final float BUTTON_RADIUS = 8.0F;
   private static final float BUTTON_GAP = 4.0F;
   private static final float BUTTON_BOTTOM_MARGIN = 8.0F;
   private static final float BUTTON_GROUP_PADDING_X = 6.0F;
   private static final float BUTTON_GROUP_PADDING_Y = 5.0F;
   private static final float BUTTON_GROUP_RADIUS = 12.0F;
   private static final float TIME_TEXT_Y = 0.11F;
   private static final int DONATION_SIZE = 30;
   private static final int DONATION_MARGIN = 40;
   private final List<MainMenuButton> buttons = new ArrayList<>();
   public boolean confirm = false;
   public static int ticksActive;
   private static MainMenuScreen INSTANCE = new MainMenuScreen();

   protected MainMenuScreen() {
      super(Text.of("THMainMenuScreen"));
      INSTANCE = this;
   }

   protected void method_25426() {
      super.init();
      this.rebuildButtons();
   }

   public static MainMenuScreen getInstance() {
      ticksActive = 0;
      if (INSTANCE == null) {
         INSTANCE = new MainMenuScreen();
      }

      return INSTANCE;
   }

   public void method_25393() {
      ticksActive++;
      if (ticksActive > 400) {
         ticksActive = 0;
      }
   }

   private void rebuildButtons() {
      this.buttons.clear();
      float totalWidth = 124.0F;
      float startX = this.field_22789 / 2.0F - totalWidth / 2.0F;
      float buttonY = this.field_22790 - 21.6F - 8.0F;
      this.buttons
         .add(
            new MainMenuButton(
               startX,
               buttonY,
               21.6F,
               21.6F,
               8.0F,
               "Singleplayer",
               MainMenuButton.IconType.SINGLEPLAYER,
               () -> Module.mc.setScreen(new SelectWorldScreen(this))
            )
         );
      this.buttons
         .add(
            new MainMenuButton(
               startX + 25.6F,
               buttonY,
               21.6F,
               21.6F,
               8.0F,
               "Multiplayer",
               MainMenuButton.IconType.MULTIPLAYER,
               () -> Module.mc.setScreen(new MultiplayerScreen(this))
            )
         );
      this.buttons
         .add(new MainMenuButton(startX + 51.2F, buttonY, 21.6F, 21.6F, 8.0F, "Alt", MainMenuButton.IconType.ALT, () -> Module.mc.setScreen(new AltScreen())));
      this.buttons
         .add(
            new MainMenuButton(
               startX + 76.8F,
               buttonY,
               21.6F,
               21.6F,
               8.0F,
               "Setting",
               MainMenuButton.IconType.SETTING,
               () -> Module.mc.setScreen(new OptionsScreen(this, Module.mc.options))
            )
         );
      this.buttons.add(new MainMenuButton(startX + 102.4F, buttonY, 21.6F, 21.6F, 8.0F, "Leave", MainMenuButton.IconType.LEAVE, () -> Module.mc.stop()));
   }

   private void renderButtonDock(@NotNull DrawContext context) {
      float totalWidth = 124.0F;
      float groupWidth = totalWidth + 12.0F;
      float groupHeight = 31.6F;
      float groupX = this.field_22789 / 2.0F - groupWidth / 2.0F;
      float groupY = this.field_22790 - 21.6F - 8.0F - 5.0F;
      Render2DEngine.drawBlurredShadow(context.getMatrices(), groupX, groupY, groupWidth, groupHeight, 10, new Color(0, 0, 0, 145));
      Render2DEngine.drawHudBase(context.getMatrices(), groupX, groupY, groupWidth, groupHeight, 12.0F, 0.94F);
      Render2DEngine.drawRound(context.getMatrices(), groupX + 5.0F, groupY + 5.0F, groupWidth - 10.0F, groupHeight - 10.0F, 8.0F, new Color(255, 255, 255, 14));
   }

   private void renderCurrentTime(@NotNull DrawContext context) {
      String timeText = LocalTime.now().format(TIME_FORMATTER);
      float centerX = this.field_22789 / 2.0F;
      float textY = this.field_22790 * 0.11F;
      FontRenderer clockFont = FontRenderers.sf_bold_large != null ? FontRenderers.sf_bold_large : FontRenderers.sf_bold;
      clockFont.drawCenteredString(context.getMatrices(), timeText, centerX + 2.0F, textY + 3.0F, Render2DEngine.applyOpacity(Color.BLACK.getRGB(), 0.18F));
      clockFont.drawCenteredString(context.getMatrices(), timeText, centerX, textY, Render2DEngine.applyOpacity(-1, 0.94F));
   }

   public void method_25394(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
      this.method_25420(context, mouseX, mouseY, delta);
      this.renderCurrentTime(context);
      this.renderButtonDock(context);
      this.buttons.forEach(button -> button.render(context, mouseX, mouseY));
      int donationX = Module.mc.getWindow().getScaledWidth() - 40;
      int donationY = Module.mc.getWindow().getScaledHeight() - 40;
      boolean hoveredDonation = Render2DEngine.isHovered(mouseX, mouseY, donationX, donationY, 30.0, 30.0);
      Render2DEngine.drawHudBase(context.getMatrices(), donationX, donationY, 30.0F, 30.0F, 5.0F, hoveredDonation ? 0.7F : 1.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, hoveredDonation ? 0.7F : 1.0F);
      context.drawTexture(TextureStorage.donation, donationX, donationY, 28, 28, 0.0F, 0.0F, 30, 30, 30, 30);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int offsetY = 10;

      for (String change : ThunderUtility.changeLog) {
         String prefix = getPrefix(change);
         FontRenderers.sf_medium.drawString(context.getMatrices(), prefix, 10.0, offsetY, Render2DEngine.applyOpacity(-1, 0.4F));
         offsetY += 10;
      }

      MainMenuButton hoveredButton = this.buttons.stream().filter(button -> button.isHovered(mouseX, mouseY)).findFirst().orElse(null);
      if (hoveredButton != null) {
         FontRenderers.sf_medium
            .drawCenteredString(
               context.getMatrices(),
               hoveredButton.getLabel(),
               hoveredButton.getCenterX(),
               hoveredButton.getTop() - 15.0F,
               Render2DEngine.applyOpacity(-1, 0.8F)
            );
      }
   }

   @NotNull
   private static String getPrefix(@NotNull String change) {
      String prefix = "";
      if (change.contains("[+]")) {
         change = change.replace("[+] ", "");
         prefix = Formatting.GREEN + "[+] " + Formatting.RESET;
      } else if (change.contains("[-]")) {
         change = change.replace("[-] ", "");
         prefix = Formatting.RED + "[-] " + Formatting.RESET;
      } else if (change.contains("[/]")) {
         change = change.replace("[/] ", "");
         prefix = Formatting.LIGHT_PURPLE + "[/] " + Formatting.RESET;
      } else if (change.contains("[*]")) {
         change = change.replace("[*] ", "");
         prefix = Formatting.GOLD + "[*] " + Formatting.RESET;
      }

      return prefix + change;
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      for (MainMenuButton mainMenuButton : this.buttons) {
         if (mainMenuButton.onClick((int)mouseX, (int)mouseY)) {
            return true;
         }
      }

      if (Render2DEngine.isHovered(mouseX, mouseY, Module.mc.getWindow().getScaledWidth() - 40, Module.mc.getWindow().getScaledHeight() - 40, 30.0, 30.0)) {
         Util.getOperatingSystem().open(URI.create("https://www.mediafire.com/view/8k7sa659gb8k97v/Donate.jpg/file"));
         return true;
      } else {
         return super.method_25402(mouseX, mouseY, button);
      }
   }
}
