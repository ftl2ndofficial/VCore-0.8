package vcore.gui.misc;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;

public class DialogScreen extends Screen {
   private final Identifier pic;
   private final String header;
   private final String description;
   private final String yesText;
   private final String noText;
   private final Runnable yesAction;
   private final Runnable noAction;

   public DialogScreen(Identifier pic, String header, String description, String yesText, String noText, Runnable yesAction, Runnable noAction) {
      super(Text.of("ThDialogScreen"));
      this.pic = pic;
      this.header = header;
      this.description = description;
      this.yesText = yesText;
      this.noText = noText;
      this.yesAction = yesAction;
      this.noAction = noAction;
   }

   public void method_25394(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
      float halfOfWidth = Module.mc.getWindow().getScaledWidth() / 2.0F;
      float halfOfHeight = Module.mc.getWindow().getScaledHeight() / 2.0F;
      float mainX = halfOfWidth - 120.0F;
      float mainY = halfOfHeight - 80.0F;
      float mainWidth = 240.0F;
      float mainHeight = 140.0F;
      Render2DEngine.drawHudBase(context.getMatrices(), mainX, mainY, mainWidth, mainHeight, 20.0F, false);
      FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), this.header, mainX + mainWidth / 2.0F, mainY + 5.0F, -1);
      FontRenderers.sf_medium
         .drawCenteredString(context.getMatrices(), this.description, mainX + mainWidth / 2.0F, mainY + 12.0F, new Color(-1409286145, true).getRGB());
      Render2DEngine.drawHudBase(context.getMatrices(), mainX + 5.0F, mainY + 95.0F, 110.0F, 40.0F, 15.0F, false);
      FontRenderers.sf_medium
         .drawCenteredString(
            context.getMatrices(), this.yesText, mainX + 60.0F, mainY + 112.0F, this.yesHovered(mouseX, mouseY) ? -1 : new Color(-1409286145, true).getRGB()
         );
      Render2DEngine.drawHudBase(context.getMatrices(), mainX + 125.0F, mainY + 95.0F, 110.0F, 40.0F, 15.0F, false);
      FontRenderers.sf_medium
         .drawCenteredString(
            context.getMatrices(), this.noText, mainX + 180.0F, mainY + 112.0F, this.noHovered(mouseX, mouseY) ? -1 : new Color(-1409286145, true).getRGB()
         );
      context.drawTexture(this.pic, (int)(mainX + mainWidth / 2.0F - 35.0F), (int)mainY + 25, 0.0F, 0.0F, 70, 65, 70, 65);
   }

   private boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
   }

   private boolean yesHovered(int mX, int mY) {
      float mainX = Module.mc.getWindow().getScaledWidth() / 2.0F - 120.0F;
      float mainY = Module.mc.getWindow().getScaledHeight() / 2.0F - 80.0F;
      return this.isHovered(mX, mY, (int)mainX + 5, (int)mainY + 95, 110, 40);
   }

   private boolean noHovered(int mX, int mY) {
      float mainX = Module.mc.getWindow().getScaledWidth() / 2.0F - 120.0F;
      float mainY = Module.mc.getWindow().getScaledHeight() / 2.0F - 80.0F;
      return this.isHovered(mX, mY, (int)mainX + 125, (int)mainY + 95, 110, 40);
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (this.yesHovered((int)mouseX, (int)mouseY)) {
         this.yesAction.run();
      } else if (this.noHovered((int)mouseX, (int)mouseY)) {
         this.noAction.run();
      }

      return super.method_25402(mouseX, mouseY, button);
   }
}
