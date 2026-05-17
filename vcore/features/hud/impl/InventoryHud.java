package vcore.features.hud.impl;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import vcore.features.hud.HudElement;
import vcore.features.modules.render.HudEditor;
import vcore.setting.impl.PositionSetting;

public class InventoryHud extends HudElement {
   public InventoryHud(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("InventoryHud", 60, 25, position, activeElementsSupplier);
   }

   @Override
   public void render(DrawContext context) {
      super.render(context);
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         float x = this.getPosX();
         float y = this.getPosY();
         float s = 0.9F;
         int slotSize = 18;
         int rows = 3;
         int cols = 9;
         int gap = Math.max(1, Math.round(2.0F * s));
         int bgAlpha = Math.round(200.0F * HudEditor.getAlpha()) & 0xFF;
         int bgColor = bgAlpha << 24;
         int cellSize = Math.round(slotSize * s);
         int totalW = cols * cellSize + (cols - 1) * gap;
         int totalH = rows * cellSize + (rows - 1) * gap;

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               int itemX = Math.round(x + col * (cellSize + gap));
               int itemY = Math.round(y + row * (cellSize + gap));
               context.fill(itemX, itemY, itemX + cellSize, itemY + cellSize, bgColor);
            }
         }

         context.getMatrices().push();

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               int slot = 9 + row * 9 + col;
               ItemStack stack = mc.player.method_31548().method_5438(slot);
               if (!stack.isEmpty()) {
                  float itemX = x + col * (cellSize + gap);
                  float itemY = y + row * (cellSize + gap);
                  context.getMatrices().push();
                  context.getMatrices().translate(itemX, itemY, 0.0F);
                  context.getMatrices().scale(s, s, 1.0F);
                  context.drawItem(stack, 0, 0);
                  context.drawItemInSlot(mc.textRenderer, stack, 0, 0);
                  context.getMatrices().pop();
               }
            }
         }

         context.getMatrices().pop();
         this.setBounds((int)x, (int)y, totalW, totalH);
      }
   }
}
