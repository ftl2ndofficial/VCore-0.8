package vcore.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import vcore.features.hud.HudElement;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

public class Coords extends HudElement {
   public Coords(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("Coords", 100, 10, position, activeElementsSupplier);
   }

   @Override
   public void render(DrawContext context) {
      super.render(context);
      int posX = (int)mc.player.method_23317();
      int posY = (int)mc.player.method_23318();
      int posZ = (int)mc.player.method_23321();
      String coordinates = "XYZ " + Formatting.WHITE + posX + " " + posY + " " + posZ;
      float width = FontRenderers.getModulesRenderer().getStringWidth(coordinates) + 21.0F;
      float pX = this.getPosX() > mc.getWindow().getScaledWidth() / 2.0F ? this.getPosX() - width : this.getPosX();
      Render2DEngine.drawHudBase(context.getMatrices(), pX, this.getPosY(), width, 13.0F, 3.0F);
      Render2DEngine.drawRect(context.getMatrices(), pX + 14.0F, this.getPosY() + 2.0F, 0.5F, 8.0F, new Color(1157627903, true));
      Render2DEngine.setupRender();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShaderTexture(0, TextureStorage.coordsIcon);
      Render2DEngine.renderGradientTexture(
         context.getMatrices(),
         pX + 2.0F,
         this.getPosY() + 1.0F,
         10.0,
         10.0,
         0.0F,
         0.0F,
         512.0,
         512.0,
         512.0,
         512.0,
         HudEditor.getColor(270),
         HudEditor.getColor(0),
         HudEditor.getColor(180),
         HudEditor.getColor(90)
      );
      Render2DEngine.endRender();
      FontRenderers.getModulesRenderer().drawString(context.getMatrices(), coordinates, pX + 18.0F, this.getPosY() + 5.0F, HudEditor.getColor(1).getRGB());
      this.setBounds(pX, this.getPosY(), width, 13.0F);
   }
}
