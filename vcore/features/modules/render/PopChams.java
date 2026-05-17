package vcore.features.modules.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IEntity;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class PopChams extends Module {
   private final Setting<PopChams.Mode> mode = new Setting<>("Mode", PopChams.Mode.Simple);
   private final Setting<Boolean> secondLayer = new Setting<>("SecondLayer", false);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(-1, true)));
   private final Setting<Integer> ySpeed = new Setting<>("YSpeed", 1, -10, 10);
   private final Setting<Integer> aSpeed = new Setting<>("AlphaSpeed", 35, 1, 100);
   private final Setting<Float> rotSpeed = new Setting<>("RotationSpeed", 0.0F, 0.0F, 6.0F);
   private final CopyOnWriteArrayList<PopChams.Person> popList = new CopyOnWriteArrayList<>();

   public PopChams() {
      super("PopChams", "Highlights totem pops.", Module.Category.RENDER);
   }

   @Override
   public void onUpdate() {
      this.popList.forEach(person -> person.update(this.popList));
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      if (this.mode.is(PopChams.Mode.Simple)) {
         RenderSystem.defaultBlendFunc();
      } else {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      }

      this.popList.forEach(person -> this.renderEntity(stack, person.player, person.modelPlayer, person.getTexture(), person.getAlpha()));
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   @EventHandler
   private void onTotemPop(@NotNull TotemPopEvent e) {
      if (!e.getEntity().equals(mc.player) && mc.world != null) {
         PlayerEntity entity = new PlayerEntity(
            mc.world, BlockPos.ORIGIN, e.getEntity().field_6283, new GameProfile(e.getEntity().method_5667(), e.getEntity().method_5477().getString())
         ) {
            public boolean method_7325() {
               return false;
            }

            public boolean method_7337() {
               return false;
            }
         };
         entity.method_5719(e.getEntity());
         entity.field_6283 = e.getEntity().field_6283;
         entity.field_6241 = e.getEntity().field_6241;
         entity.field_6251 = e.getEntity().field_6251;
         entity.field_6279 = e.getEntity().field_6279;
         entity.method_5660(e.getEntity().method_5715());
         entity.field_42108.setSpeed(e.getEntity().field_42108.getSpeed());
         entity.field_42108.pos = e.getEntity().field_42108.getPos();
         this.popList.add(new PopChams.Person(entity, ((AbstractClientPlayerEntity)e.getEntity()).getSkinTextures().texture()));
      }
   }

   private void renderEntity(
      @NotNull MatrixStack matrices, @NotNull LivingEntity entity, @NotNull PlayerEntityModel<PlayerEntity> modelBase, Identifier texture, int alpha
   ) {
      modelBase.leftPants.visible = this.secondLayer.getValue();
      modelBase.rightPants.visible = this.secondLayer.getValue();
      modelBase.leftSleeve.visible = this.secondLayer.getValue();
      modelBase.rightSleeve.visible = this.secondLayer.getValue();
      modelBase.jacket.visible = this.secondLayer.getValue();
      modelBase.field_3394.visible = this.secondLayer.getValue();
      double x = entity.method_23317() - mc.getEntityRenderDispatcher().camera.getPos().method_10216();
      double y = entity.method_23318() - mc.getEntityRenderDispatcher().camera.getPos().method_10214();
      double z = entity.method_23321() - mc.getEntityRenderDispatcher().camera.getPos().method_10215();
      ((IEntity)entity).setPos(entity.method_19538().add(0.0, this.ySpeed.getValue().intValue() / 50.0, 0.0));
      matrices.push();
      matrices.translate((float)x, (float)y, (float)z);
      float yRotYaw = alpha / 255.0F * 360.0F * this.rotSpeed.getValue();
      yRotYaw = yRotYaw == 0.0F
         ? 0.0F
         : Render2DEngine.interpolateFloat(
            yRotYaw, yRotYaw - this.aSpeed.getValue().intValue() / 255.0F * 360.0F * this.rotSpeed.getValue(), Render3DEngine.getTickDelta()
         );
      matrices.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtility.rad(180.0F - entity.bodyYaw + yRotYaw)));
      prepareScale(matrices);
      modelBase.method_17086((PlayerEntity)entity, entity.limbAnimator.getPos(), entity.limbAnimator.getSpeed(), Render3DEngine.getTickDelta());
      float limbSpeed = Math.min(entity.limbAnimator.getSpeed(), 1.0F);
      modelBase.setAngles(
         (PlayerEntity)entity, entity.limbAnimator.getPos(), limbSpeed, entity.field_6012, entity.headYaw - entity.bodyYaw, entity.method_36455()
      );
      BufferBuilder buffer;
      if (this.mode.is(PopChams.Mode.Textured)) {
         RenderSystem.setShaderTexture(0, texture);
         RenderSystem.setShader(GameRenderer::getPositionTexProgram);
         buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      } else {
         RenderSystem.setShader(GameRenderer::getPositionProgram);
         buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
      }

      RenderSystem.setShaderColor(this.color.getValue().getGlRed(), this.color.getValue().getGlGreen(), this.color.getValue().getGlBlue(), alpha / 255.0F);
      modelBase.method_60879(matrices, buffer, 10, 0);
      Render2DEngine.endBuilding(buffer);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      matrices.pop();
   }

   private static void prepareScale(@NotNull MatrixStack matrixStack) {
      matrixStack.scale(-1.0F, -1.0F, 1.0F);
      matrixStack.scale(1.6F, 1.8F, 1.6F);
      matrixStack.translate(0.0F, -1.501F, 0.0F);
   }

   private enum Mode {
      Simple,
      Textured;
   }

   private class Person {
      private final PlayerEntity player;
      private final PlayerEntityModel<PlayerEntity> modelPlayer;
      private Identifier texture;
      private int alpha;

      public Person(PlayerEntity player, Identifier texture) {
         this.player = player;
         this.modelPlayer = new PlayerEntityModel(
            new Context(
                  Module.mc.getEntityRenderDispatcher(),
                  Module.mc.getItemRenderer(),
                  Module.mc.getBlockRenderManager(),
                  Module.mc.getEntityRenderDispatcher().getHeldItemRenderer(),
                  Module.mc.getResourceManager(),
                  Module.mc.getEntityModelLoader(),
                  Module.mc.textRenderer
               )
               .getPart(EntityModelLayers.PLAYER),
            false
         );
         this.modelPlayer.method_2838().scale(new Vector3f(-0.3F, -0.3F, -0.3F));
         this.alpha = PopChams.this.color.getValue().getAlpha();
         this.texture = texture;
      }

      public void update(CopyOnWriteArrayList<PopChams.Person> arrayList) {
         if (this.alpha <= 0) {
            arrayList.remove(this);
            this.player.method_5768();
            this.player.method_5650(RemovalReason.KILLED);
            this.player.method_36209();
         } else {
            this.alpha = this.alpha - PopChams.this.aSpeed.getValue();
         }
      }

      public int getAlpha() {
         return MathUtility.clamp(this.alpha, 0, 255);
      }

      public Identifier getTexture() {
         return this.texture;
      }
   }
}
