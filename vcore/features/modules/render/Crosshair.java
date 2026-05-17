package vcore.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.TextureStorage;

public class Crosshair extends Module {
   private final Setting<Crosshair.Mode> mode = new Setting<>("Mode", Crosshair.Mode.Circle);
   private final Setting<Boolean> animated = new Setting<>("Animated", true, v -> this.mode.is(Crosshair.Mode.Default));
   private final Setting<Boolean> dot = new Setting<>("Dot", false, v -> this.mode.is(Crosshair.Mode.Default));
   private final Setting<Boolean> t = new Setting<>("T", false, v -> this.mode.is(Crosshair.Mode.Default));
   public final Setting<Boolean> syncColor = new Setting<>("SyncColor", true);
   public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 255).getRGB()), v -> !this.syncColor.getValue());
   private float xAnim;
   private float yAnim;
   private float prevProgress;

   public Crosshair() {
      super("Crosshair", "Custom crosshair overlay.", Module.Category.RENDER);
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (mc.options.getPerspective().isFirstPerson()) {
         float midX = mc.getWindow().getScaledWidth() / 2.0F;
         float midY = mc.getWindow().getScaledHeight() / 2.0F;
         this.xAnim = midX;
         this.yAnim = midY;
         float progress = 360.0F * mc.player.method_7261(0.5F);
         progress = progress == 0.0F ? 360.0F : progress;
         switch ((Crosshair.Mode)this.mode.getValue()) {
            case Circle:
               Color c1;
               Color c2;
               if (this.syncColor.getValue()) {
                  HudEditor.Theme theme = HudEditor.getCurrentTheme();
                  c1 = theme.color1;
                  c2 = theme.color2;
               } else {
                  c1 = this.color.getValue().getColorObject();
                  c2 = this.color.getValue().getColorObject();
               }

               Render2DEngine.drawArc(
                  context.getMatrices(),
                  this.xAnim - 25.0F,
                  this.yAnim - 25.0F,
                  50.0F,
                  50.0F,
                  0.025F,
                  0.12F,
                  0.0F,
                  Render2DEngine.interpolateFloat(this.prevProgress, progress, Render3DEngine.getTickDelta()),
                  c1,
                  c2
               );
               this.prevProgress = progress;
               break;
            case Dot:
               Color dotColor;
               if (this.syncColor.getValue()) {
                  HudEditor.Theme theme = HudEditor.getCurrentTheme();
                  dotColor = theme.color1;
               } else {
                  dotColor = this.color.getValue().getColorObject();
               }

               context.getMatrices().push();
               context.getMatrices().translate(this.xAnim + 4.0F, this.yAnim + 4.0F, 0.0F);
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
               RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
               BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               RenderSystem.setShaderTexture(0, TextureStorage.firefly);
               Matrix4f posMatrix = context.getMatrices().peek().getPositionMatrix();
               bufferBuilder.method_22918(posMatrix, 0.0F, -8.0F, 0.0F).texture(0.0F, 1.0F).color(dotColor.getRGB());
               bufferBuilder.method_22918(posMatrix, -8.0F, -8.0F, 0.0F).texture(1.0F, 1.0F).color(dotColor.getRGB());
               bufferBuilder.method_22918(posMatrix, -8.0F, 0.0F, 0.0F).texture(1.0F, 0.0F).color(dotColor.getRGB());
               bufferBuilder.method_22918(posMatrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(dotColor.getRGB());
               BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
               context.getMatrices().pop();
               break;
            case Default:
               Color drawColor;
               if (this.syncColor.getValue()) {
                  HudEditor.Theme theme = HudEditor.getCurrentTheme();
                  drawColor = theme.color1;
               } else {
                  drawColor = this.color.getValue().getColorObject();
               }

               float offset = this.animated.getValue()
                  ? -3.0F + Render2DEngine.interpolateFloat(this.prevProgress, progress, Render3DEngine.getTickDelta()) / 100.0F
                  : 0.0F;
               this.prevProgress = progress;
               if (!this.t.getValue()) {
                  Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 1.0F, this.yAnim - 6.0F + offset, 2.0F, 4.0F, Color.BLACK);
                  Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 0.5F, this.yAnim - 5.5F + offset, 1.0F, 3.0F, drawColor);
               }

               Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 1.0F, this.yAnim + 2.0F - offset, 2.0F, 4.0F, Color.BLACK);
               Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 0.5F, this.yAnim + 2.5F - offset, 1.0F, 3.0F, drawColor);
               Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 6.0F + offset, this.yAnim - 1.0F, 4.0F, 2.0F, Color.BLACK);
               Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 5.5F + offset, this.yAnim - 0.5F, 3.0F, 1.0F, drawColor);
               Render2DEngine.drawRect(context.getMatrices(), this.xAnim + 2.0F - offset, this.yAnim - 1.0F, 4.0F, 2.0F, Color.BLACK);
               Render2DEngine.drawRect(context.getMatrices(), this.xAnim + 2.5F - offset, this.yAnim - 0.5F, 3.0F, 1.0F, drawColor);
               if (this.dot.getValue()) {
                  Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 1.0F, this.yAnim - 1.0F, 2.0F, 2.0F, Color.BLACK);
                  Render2DEngine.drawRect(context.getMatrices(), this.xAnim - 0.5F, this.yAnim - 0.5F, 1.0F, 1.0F, drawColor);
               }
         }
      }
   }

   public float getAnimatedPosX() {
      return this.xAnim == 0.0F ? mc.getWindow().getScaledWidth() / 2.0F : this.xAnim;
   }

   public float getAnimatedPosY() {
      return this.yAnim == 0.0F ? mc.getWindow().getScaledHeight() / 2.0F : this.yAnim;
   }

   private enum Mode {
      Circle,
      Dot,
      Default;
   }
}
