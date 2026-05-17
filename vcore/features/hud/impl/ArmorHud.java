package vcore.features.hud.impl;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import vcore.features.hud.HudElement;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.math.MathUtility;

public class ArmorHud extends HudElement {
   public ArmorHud(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("ArmorHud", 60, 25, position, activeElementsSupplier);
   }

   @Override
   public void render(DrawContext context) {
      if (mc.player != null) {
         int u = mc.player.method_5748();
         int v = Math.min(mc.player.method_5669(), u);
         int y = mc.getWindow().getScaledHeight() - 55 - (!mc.player.method_5869() && v >= u ? 0 : 10);
         int i = 0;

         for (ItemStack is : mc.player.method_31548().armor) {
            i++;
            if (!is.isEmpty()) {
               int x = mc.getWindow().getScaledWidth() / 2 - 90 + (9 - i) * 20 + 2;
               context.drawItem(is, x, y);
               context.drawItemInSlot(mc.textRenderer, is, x, y);
               String s = is.getCount() > 1 ? is.getCount() + "" : "";
               float countX = x + 19 - 2 - FontRenderers.sf_bold.getStringWidth(s);
               float countY = y + 6;
               FontRenderers.sf_bold.drawString(context.getMatrices(), s, countX, countY, 16777215);
               float green = (float)(is.getMaxDamage() - is.getDamage()) / is.getMaxDamage();
               float red = 1.0F - green;
               int dmg = 100 - (int)(red * 100.0F);
               int color = new Color((int)MathUtility.clamp(red * 255.0F, 0.0F, 255.0F), (int)MathUtility.clamp(green * 255.0F, 0.0F, 255.0F), 0).getRGB();
               String dmgStr = dmg + "";
               float dmgX = x + 8 - FontRenderers.sf_bold.getStringWidth(dmgStr) / 2.0F;
               float dmgY = y - 6;
               FontRenderers.sf_bold.drawString(context.getMatrices(), dmgStr, dmgX, dmgY, color);
            }
         }
      }
   }
}
