package vcore.gui.clickui;

import net.minecraft.client.gui.DrawContext;

public class AbstractButton {
   public float x;
   public float y;
   public float width;
   public float height;
   public float target_offset;
   public float offsetY;

   public void init() {
   }

   public float getX() {
      return this.x;
   }

   public void setX(float x) {
      this.x = x;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y + this.offsetY;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setTargetOffset(float offsetY) {
      this.target_offset = offsetY;
   }

   public void setOffset(float offsetY) {
      this.offsetY = offsetY;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void mouseReleased(int mouseX, int mouseY, int button) {
   }

   public void mouseClicked(int mouseX, int mouseY, int button) {
   }

   public void keyTyped(int keyCode) {
   }

   public void onGuiClosed() {
   }

   public void tick() {
   }

   public void charTyped(char key, int keyCode) {
   }
}
