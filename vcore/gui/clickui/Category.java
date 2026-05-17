package vcore.gui.clickui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.clickui.impl.SearchBar;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.animation.AnimationUtility;

public class Category extends AbstractCategory {
   private static final float MODULE_SPACING = 0.0F;
   private static final int CAT_HEIGHT = 280;
   private final List<AbstractButton> buttons;
   private float scrollOffset = 0.0F;
   private float scrollTarget = 0.0F;

   public Category(Module.Category category, ArrayList<Module> features, float x, float y, float width, float height) {
      super(category.getName(), x, y, width, height);
      this.buttons = new ArrayList<>();
      features.forEach(feature -> this.buttons.add(new ModuleButton(feature)));
      this.setOpen(true);
   }

   @Override
   public void init() {
      this.buttons.forEach(AbstractButton::init);
   }

   @Override
   public void setModuleOffset(float offset, int mouseX, int mouseY) {
      if (Render2DEngine.isHovered(mouseX, mouseY, this.getX(), this.getY(), this.width, 280.0)) {
         this.scrollTarget -= offset;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.hovered = Render2DEngine.isHovered(mouseX, mouseY, this.x, this.y, this.width, 25.0);
      this.setWidth(125.0F);
      Render2DEngine.drawGuiBase(context.getMatrices(), this.getX(), this.getY(), this.width, this.isOpen() ? 280.0F : 25.0F, 12.0F, HudEditor.getBlurOpacity());
      FontRenderers.categories.drawCenteredString(context.getMatrices(), this.getName(), this.getX() + this.width / 2.0F, this.getY() + 7.0F, -1);
      if (this.isOpen()) {
         float maxScroll = (float)Math.max(0.0, this.getButtonsHeight() - 250.0);
         if (this.scrollTarget < 0.0F) {
            this.scrollTarget = 0.0F;
         }

         if (this.scrollTarget > maxScroll) {
            this.scrollTarget = maxScroll;
         }

         this.scrollOffset = AnimationUtility.fast(this.scrollOffset, this.scrollTarget, 10.0F);
         context.getMatrices().push();
         Render2DEngine.addWindow(context.getMatrices(), this.getX(), this.getY() + 25.0F, this.getX() + this.width, this.getY() + 280.0F - 5.0F, 1.0);
         float baseY = this.getY() + 25.0F - this.scrollOffset;

         for (AbstractButton button : this.buttons) {
            if (!(button instanceof ModuleButton mb && !SearchBar.matchesQuery(mb.module.getName()))) {
               button.setX(this.getX() + 4.0F);
               button.setWidth(this.width - 8.0F);
               button.setHeight(20.0F);
               button.setY(baseY);
               if (button instanceof ModuleButton mb) {
                  mb.setContentArea(this.getX(), this.getY() + 25.0F, this.width, 250.0F);
               }

               button.render(context, mouseX, mouseY, delta);
            }
         }

         Render2DEngine.popWindow();
         context.getMatrices().pop();
      }

      this.updatePosition();
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int button) {
      if (button == 1 && this.hovered) {
         this.setOpen(!this.isOpen());
      }

      if (this.isOpen() && Render2DEngine.isHovered(mouseX, mouseY, this.getX(), this.getY() + 25.0F, this.width, 250.0)) {
         this.buttons.forEach(b -> b.mouseClicked(mouseX, mouseY, button));
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int button) {
      super.mouseReleased(mouseX, mouseY, button);
      if (this.isOpen()) {
         this.buttons.forEach(b -> b.mouseReleased(mouseX, mouseY, button));
      }
   }

   @Override
   public boolean keyTyped(int keyCode) {
      if (this.isOpen()) {
         for (AbstractButton button : this.buttons) {
            button.keyTyped(keyCode);
         }
      }

      return false;
   }

   @Override
   public void charTyped(char key, int keyCode) {
      if (this.isOpen()) {
         for (AbstractButton button : this.buttons) {
            button.charTyped(key, keyCode);
         }
      }
   }

   @Override
   public void onClose() {
      super.onClose();
      this.buttons.forEach(AbstractButton::onGuiClosed);
   }

   @Override
   public void tick() {
      this.buttons.forEach(AbstractButton::tick);
   }

   private void updatePosition() {
      float offsetY = 0.0F;

      for (AbstractButton button : this.buttons) {
         if (!(button instanceof ModuleButton mb && !SearchBar.matchesQuery(mb.module.getName()))) {
            button.setTargetOffset(offsetY);
            if (button instanceof ModuleButton mbutton && mbutton.isOpen()) {
               offsetY += (float)mbutton.getTargetElementsHeight();
            }

            offsetY += button.getHeight() + 0.0F;
         }
      }
   }

   @Override
   public void hudClicked(Module module) {
      for (AbstractButton button : this.buttons) {
         if (button instanceof ModuleButton mbutton && mbutton.module == module) {
            mbutton.setOpen(true);
         }
      }
   }

   public double getButtonsHeight() {
      double height = 8.0;
      int visible = 0;

      for (AbstractButton button : this.buttons) {
         if (!(button instanceof ModuleButton mb && !SearchBar.matchesQuery(mb.module.getName()))) {
            visible++;
            if (button instanceof ModuleButton mbutton) {
               height += mbutton.getElementsHeight();
            }

            height += button.getHeight();
         }
      }

      if (visible > 1) {
         height += 0.0F * (visible - 1);
      }

      return height;
   }
}
