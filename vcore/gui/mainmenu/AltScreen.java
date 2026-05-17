package vcore.gui.mainmenu;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.utility.render.Render2DEngine;

public class AltScreen extends Screen {
   public AltScreen() {
      super(Text.of("AltScreen"));
   }

   public void method_25394(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
      this.method_25420(context, mouseX, mouseY, delta);
      int w = Module.mc.getWindow().getScaledWidth();
      int h = Module.mc.getWindow().getScaledHeight();
      context.fillGradient(0, 0, w, h, Integer.MIN_VALUE, Integer.MIN_VALUE);
      int padX = 12;
      int padY = 8;
      int panelW = Math.round(w * 0.25F);
      int panelH = Math.round(h * 0.5F);
      int panelX = (w - panelW) / 2;
      int panelY = (h - panelH) / 2;
      Color c1 = new Color(-2013265920, true);
      Color c2 = new Color(1711276032, true);
      Render2DEngine.renderRoundedGradientRect(context.getMatrices(), c1, c2, c2, c1, panelX, panelY, panelW, panelH, 12.0F);
      String header = "Alt Manager";
      float headerW = FontRenderers.sf_medium.getStringWidth(header);
      float headerH = FontRenderers.sf_medium.getStringHeight(header);
      int headerPadX = 10;
      int headerPadY = 4;
      float headerBoxW = headerW + headerPadX * 2.0F;
      float headerBoxH = headerH + headerPadY * 2.0F;
      float headerX = w / 2.0F - headerBoxW / 2.0F;
      float headerY = panelY - 4.0F - headerBoxH;
      Color hc1 = new Color(-1441722095, true);
      Render2DEngine.drawRound(context.getMatrices(), headerX, headerY, headerBoxW, headerBoxH, 8.0F, hc1);
      FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), header, w / 2.0F, headerY + headerBoxH / 2.0F, -1);
      float gapBelow = 4.0F;
      float barsY = panelY + panelH + gapBelow;
      float longBarW = panelW * 0.6F;
      float longBarH = 14.0F;
      float smallBarW = panelW * 0.12F;
      float smallBarH = longBarH;
      float between = 12.0F;
      float totalBarsW = longBarW + between + smallBarW;
      float barsStartX = panelX + (panelW - totalBarsW) / 2.0F;
      float longBarX = barsStartX;
      float smallBarX = barsStartX + longBarW + between;
      Render2DEngine.renderRoundedGradientRect(
         context.getMatrices(), c1, c2, c2, c1, Math.round(longBarX), barsY, Math.round(longBarW), Math.round(longBarH), 6.0F
      );
      Render2DEngine.renderRoundedGradientRect(
         context.getMatrices(), c1, c2, c2, c1, Math.round(smallBarX), barsY, Math.round(smallBarW), Math.round(smallBarH), 6.0F
      );
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      return false;
   }
}
