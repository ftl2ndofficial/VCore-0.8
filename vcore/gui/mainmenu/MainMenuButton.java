package vcore.gui.mainmenu;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import vcore.utility.render.Render2DEngine;

public class MainMenuButton {
   private final float x;
   private final float y;
   private final float width;
   private final float height;
   private final float radius;
   private final String label;
   private final MainMenuButton.IconType iconType;
   private final Runnable action;

   public MainMenuButton(
      float x, float y, float width, float height, float radius, @NotNull String label, @NotNull MainMenuButton.IconType iconType, @NotNull Runnable action
   ) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.radius = radius;
      this.label = label;
      this.iconType = iconType;
      this.action = action;
   }

   public void render(@NotNull DrawContext context, int mouseX, int mouseY) {
      boolean hovered = this.isHovered(mouseX, mouseY);
      Color shadowColor = new Color(0, 0, 0, hovered ? 135 : 105);
      float hudAlpha = hovered ? 1.0F : 0.92F;
      float iconSize = Math.min(this.width, this.height) * 0.46F;
      Render2DEngine.drawBlurredShadow(context.getMatrices(), this.x, this.y, this.width, this.height, 6, shadowColor);
      Render2DEngine.drawHudBase(context.getMatrices(), this.x, this.y, this.width, this.height, this.radius, hudAlpha);
      MainMenuIconRenderer.render(context, this.iconType, this.x + this.width / 2.0F, this.y + this.height / 2.0F, iconSize, hovered);
   }

   public boolean onClick(int mouseX, int mouseY) {
      if (!this.isHovered(mouseX, mouseY)) {
         return false;
      }

      this.action.run();
      return true;
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   public float getCenterX() {
      return this.x + this.width / 2.0F;
   }

   public float getTop() {
      return this.y;
   }

   @NotNull
   public String getLabel() {
      return this.label;
   }

   public enum IconType {
      SINGLEPLAYER,
      MULTIPLAYER,
      ALT,
      SETTING,
      LEAVE;
   }
}
