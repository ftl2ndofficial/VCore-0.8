package vcore.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.ThunderUtility;
import vcore.utility.Timer;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

public class JumpCircle extends Module {
   private final Setting<JumpCircle.Mode> mode = new Setting<>("Mode", JumpCircle.Mode.Default);
   private final Setting<Boolean> easeOut = new Setting<>("EaseOut", true);
   private final Setting<Float> rotateSpeed = new Setting<>("RotateSpeed", 2.0F, 0.5F, 5.0F);
   private final Setting<Float> circleScale = new Setting<>("CircleScale", 1.0F, 0.5F, 5.0F);
   private final Setting<Boolean> onlySelf = new Setting<>("OnlySelf", true);
   private static final float GLOW_BOOST_ALPHA = 0.65F;
   private final List<UUID> cache = new CopyOnWriteArrayList<>();
   private final List<JumpCircle.Circle> circles = new ArrayList<>();
   private Identifier custom;
   private int lastWorldId = -1;

   public JumpCircle() {
      super("JumpCircle", "Circle effect on jump.", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      try {
         this.custom = ThunderUtility.getCustomImg("circle");
      } catch (Exception e) {
         this.sendMessage(e.getMessage());
      }

      this.lastWorldId = this.getWorldId();
      this.circles.clear();
      this.cache.clear();
   }

   @Override
   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         int worldId = this.getWorldId();
         if (worldId != this.lastWorldId) {
            this.circles.clear();
            this.cache.clear();
            this.lastWorldId = worldId;
         }

         if (this.mode.is(JumpCircle.Mode.Custom) && this.custom == null) {
            try {
               this.custom = ThunderUtility.getCustomImg("circle");
            } catch (Exception e) {
               this.sendMessage(".minecraft -> Vcore -> misc -> images -> circle.png");
            }
         }

         for (PlayerEntity pl : mc.world.method_18456()) {
            if (!this.cache.contains(pl.method_5667()) && pl.method_24828() && (mc.player == pl || !this.onlySelf.getValue())) {
               this.cache.add(pl.method_5667());
            }
         }

         for (UUID uuid : new ArrayList<>(this.cache)) {
            PlayerEntity pl = mc.world.method_18470(uuid);
            if (pl != null && !pl.method_24828()) {
               this.circles
                  .add(new JumpCircle.Circle(new Vec3d(pl.method_23317(), (int)Math.floor(pl.method_23318()) + 0.001F, pl.method_23321()), new Timer()));
               this.cache.remove(uuid);
            }
         }

         this.circles.removeIf(c -> c.timer.passedMs(this.easeOut.getValue() ? 5000L : 6000L));
      } else {
         this.cache.clear();
         this.circles.clear();
         this.lastWorldId = -1;
      }
   }

   private int getWorldId() {
      return mc.world == null ? -1 : mc.world.method_27983().getValue().hashCode();
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      Collections.reverse(this.circles);
      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      switch ((JumpCircle.Mode)this.mode.getValue()) {
         case Default:
            RenderSystem.setShaderTexture(0, TextureStorage.defaultCircle);
            break;
         case Portal:
            RenderSystem.setShaderTexture(0, TextureStorage.bubble);
            break;
         case Custom:
            RenderSystem.setShaderTexture(0, Objects.requireNonNullElse(this.custom, TextureStorage.defaultCircle));
      }

      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (JumpCircle.Circle c : this.circles) {
         float colorAnim = (float)c.timer.getPassedTimeMs() / 6000.0F;
         float sizeAnim = this.circleScale.getValue()
            - (float)Math.pow(1.0F - (float)c.timer.getPassedTimeMs() * (this.easeOut.getValue() ? 2.0F : 1.0F) / 5000.0F, 4.0);
         stack.push();
         stack.translate(
            c.pos().x - mc.getEntityRenderDispatcher().camera.getPos().method_10216(),
            c.pos().y - mc.getEntityRenderDispatcher().camera.getPos().method_10214(),
            c.pos().z - mc.getEntityRenderDispatcher().camera.getPos().method_10215()
         );
         stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sizeAnim * this.rotateSpeed.getValue() * 1000.0F));
         Matrix4f matrix = stack.peek().getPositionMatrix();
         float alpha = 1.0F - colorAnim;
         this.drawCircleLayer(buffer, matrix, sizeAnim, alpha * 0.65F);
         this.drawCircleLayer(buffer, matrix, sizeAnim, alpha);
         stack.pop();
      }

      Render2DEngine.endBuilding(buffer);
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableDepthTest();
      Collections.reverse(this.circles);
   }

   private void drawCircleLayer(@NotNull BufferBuilder buffer, @NotNull Matrix4f matrix, float size, float alpha) {
      float scale = size * 2.0F;
      buffer.method_22918(matrix, -size, -size + scale, 0.0F).texture(0.0F, 1.0F).color(Render2DEngine.applyOpacity(HudEditor.getColor(270), alpha).getRGB());
      buffer.method_22918(matrix, -size + scale, -size + scale, 0.0F)
         .texture(1.0F, 1.0F)
         .color(Render2DEngine.applyOpacity(HudEditor.getColor(0), alpha).getRGB());
      buffer.method_22918(matrix, -size + scale, -size, 0.0F).texture(1.0F, 0.0F).color(Render2DEngine.applyOpacity(HudEditor.getColor(180), alpha).getRGB());
      buffer.method_22918(matrix, -size, -size, 0.0F).texture(0.0F, 0.0F).color(Render2DEngine.applyOpacity(HudEditor.getColor(90), alpha).getRGB());
   }

   public record Circle(Vec3d pos, Timer timer) {
   }

   public enum Mode {
      Default,
      Portal,
      Custom;
   }
}
