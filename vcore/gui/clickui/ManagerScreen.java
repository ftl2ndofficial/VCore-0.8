package vcore.gui.clickui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.gui.clickui.impl.ManagerPanel;

public class ManagerScreen extends Screen {
   private final Screen parent;
   private final ManagerPanel panel;

   public ManagerScreen(Screen parent) {
      super(Text.of("ManagerScreen"));
      this.parent = parent;
      this.panel = new ManagerPanel();
      this.panel.setOpen(true);
   }

   public void method_25394(DrawContext context, int mouseX, int mouseY, float delta) {
      if (ModuleManager.clickGui.blur.getValue()) {
         this.method_57734(delta);
      }

      if (Module.fullNullCheck()) {
         this.method_25420(context, mouseX, mouseY, delta);
      }

      this.panel.setSize(400.0F, 250.0F);
      this.panel.render(context, mouseX, mouseY, delta);
   }

   public boolean method_25421() {
      return false;
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      return this.panel.mouseClicked((int)mouseX, (int)mouseY, button) ? true : super.method_25402(mouseX, mouseY, button);
   }

   public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.panel.mouseScrolled(mouseX, mouseY, verticalAmount);
      return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (this.panel.keyPressed(keyCode)) {
         return true;
      } else if (keyCode == 256) {
         this.method_25419();
         return true;
      } else {
         return super.method_25404(keyCode, scanCode, modifiers);
      }
   }

   public boolean method_25400(char chr, int modifiers) {
      return this.panel.charTyped(chr) ? true : super.method_25400(chr, modifiers);
   }

   public void method_25419() {
      if (this.field_22787 != null) {
         this.field_22787.setScreen(this.parent);
      }
   }
}
