package vcore.gui.misc;

import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;

public class GuiScanner extends Screen {
   public static boolean neartrack = false;
   public static boolean track = false;
   public static boolean busy = false;
   public ArrayList<String> consoleout = new ArrayList<>();
   int radarx;
   int radary;
   int radarx1;
   int radary1;
   int centerx;
   int centery;
   int consolex;
   int consoley;
   int consolex1;
   int consoley1;
   int hovery;
   int hoverx;
   int searchx;
   int searchy;
   int wheely;

   public GuiScanner() {
      super(Text.of("GuiScanner"));
   }

   public void method_25394(DrawContext context, int mouseX, int mouseY, float delta) {
      if (Module.mc.player != null) {
         this.radarx = Module.mc.getWindow().getScaledWidth() / 8;
         this.radarx1 = Module.mc.getWindow().getScaledWidth() * 5 / 8;
         this.radary = Module.mc.getWindow().getScaledHeight() / 2 - (this.radarx1 - this.radarx) / 2;
         this.radary1 = Module.mc.getWindow().getScaledHeight() / 2 + (this.radarx1 - this.radarx) / 2;
         this.centerx = (this.radarx + this.radarx1) / 2;
         this.centery = (this.radary + this.radary1) / 2;
         this.consolex = (int)(Module.mc.getWindow().getScaledWidth() * 5.5F / 8.0F);
         this.consolex1 = Module.mc.getWindow().getScaledWidth() - 50;
         this.consoley = this.radary;
         this.consoley1 = this.radary1 - 50;
         Render2DEngine.drawRectDumbWay(context.getMatrices(), this.consolex, this.consoley, this.consolex1, this.consoley1, new Color(-150205428, true));
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(), this.consolex, this.consoley1 + 3, this.consolex1, this.consoley1 + 17, new Color(-150205428, true)
         );
         FontRenderers.monsterrat
            .drawString(context.getMatrices(), "cursor pos: " + this.hoverx * 64 + "x  " + this.hovery * 64 + "z", this.consolex + 4, this.consoley1 + 6, -1);
         if (!track) {
            Render2DEngine.drawRectDumbWay(
               context.getMatrices(), this.consolex, this.consoley1 + 20, this.consolex1, this.consoley1 + 35, new Color(-150205428, true)
            );
            FontRenderers.monsterrat.drawString(context.getMatrices(), "tracker off", this.consolex + 4, this.consoley1 + 26, -1);
         } else {
            Render2DEngine.drawRectDumbWay(
               context.getMatrices(), this.consolex, this.consoley1 + 20, this.consolex1, this.consoley1 + 35, new Color(-144810402, true)
            );
            FontRenderers.monsterrat.drawString(context.getMatrices(), "tracker on", this.consolex + 4, this.consoley1 + 26, -1);
         }

         Render2DEngine.drawRectDumbWay(
            context.getMatrices(), this.consolex, this.consoley1 + 38, this.consolex1, this.consoley1 + 53, new Color(-150205428, true)
         );
         FontRenderers.monsterrat.drawString(context.getMatrices(), "clear console", this.consolex + 4, this.consoley1 + 42, -1);
         Render2DEngine.drawRectDumbWay(context.getMatrices(), this.radarx, this.radary, this.radarx1, this.radary1, new Color(-535489259, true));
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(), this.centerx - 1.0F, this.centery - 1.0F, this.centerx + 1.0F, this.centery + 1.0F, new Color(16712451)
         );
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(),
            (float)(Module.mc.player.method_23317() / 16.0 / 4.0 + this.centerx),
            (float)(Module.mc.player.method_23321() / 16.0 / 4.0 + this.centery),
            (float)(Module.mc.player.method_23317() / 16.0 / 4.0 + (this.radarx1 - this.radarx) / 300.0F + this.centerx),
            (float)(Module.mc.player.method_23321() / 16.0 / 4.0 + (this.radary1 - this.radary) / 300.0F + this.centery),
            new Color(4863)
         );
         if (mouseX > this.radarx && mouseX < this.radarx1 && mouseY > this.radary && mouseY < this.radary1) {
            this.hoverx = mouseX - this.centerx;
            this.hovery = mouseY - this.centery;
         }

         Render2DEngine.addWindow(context.getMatrices(), this.consolex, this.consoley, this.consolex1, this.consoley1 - 10, 1.0);

         for (int i = 0; i < this.consoleout.size(); i++) {
            FontRenderers.monsterrat.drawString(context.getMatrices(), this.consoleout.get(i), this.consolex + 4, this.consoley + 6 + i * 11 + this.wheely, -1);
         }

         Render2DEngine.popWindow();
         FontRenderers.monsterrat.drawString(context.getMatrices(), "X+", this.radarx1 + 5, this.centery, -1);
         FontRenderers.monsterrat.drawString(context.getMatrices(), "X-", this.radarx - 15, this.centery, -1);
         FontRenderers.monsterrat.drawString(context.getMatrices(), "Y+", this.centerx, this.radary1 + 5, -1);
         FontRenderers.monsterrat.drawString(context.getMatrices(), "Y-", this.centerx, this.radary - 8, -1);
      }
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (mouseX > this.radarx && mouseX < this.radarx1 && mouseY > this.radary && mouseY < this.radary1) {
         busy = true;
         this.searchx = (int)(mouseX - this.centerx);
         this.searchy = (int)(mouseY - this.centery);
         this.consoleout.add("Selected pos " + this.searchx * 65 + "x " + this.searchy * 64 + "z ");
      }

      if (mouseX > this.consolex && mouseX < this.consolex1 && mouseY > this.consoley1 + 20 && mouseY < this.consoley1 + 36) {
         track = !track;
      }

      if (mouseX > this.consolex && mouseX < this.consolex1 && mouseY > this.consoley1 + 38 && mouseY < this.consoley1 + 53) {
         this.consoleout.clear();
      }

      return super.method_25402(mouseX, mouseY, button);
   }

   public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.wheely += (int)(verticalAmount * 5.0);
      return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
