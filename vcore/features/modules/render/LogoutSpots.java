package vcore.features.modules.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4d;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.misc.FakePlayer;
import vcore.gui.font.FontRenderers;
import vcore.injection.accesors.IEntity;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class LogoutSpots extends Module {
   private final Setting<LogoutSpots.RenderMode> renderMode = new Setting<>("Mode", LogoutSpots.RenderMode.TexturedChams);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(-2013200640));
   private final Setting<Boolean> notifications = new Setting<>("Notifications", true);
   private final Setting<Boolean> ignoreBots = new Setting<>("IgnoreBots", true);
   private final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
   private final Map<UUID, PlayerEntity> logoutCache = Maps.newConcurrentMap();

   public LogoutSpots() {
      super("LogoutSpots", "Shows logout locations.", Module.Category.RENDER);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof PlayerListS2CPacket pac) {
         if (pac.getActions().contains(Action.ADD_PLAYER)) {
            for (Entry ple : pac.getPlayerAdditionEntries()) {
               for (UUID uuid : this.logoutCache.keySet()) {
                  if (uuid.equals(ple.profile().getId())) {
                     PlayerEntity pl = this.logoutCache.get(uuid);
                     if (!this.ignoreBots.getValue() || !this.isABot(pl)) {
                        if (this.notifications.getValue()) {
                           this.sendMessage(
                              pl.method_5477().getString()
                                 + " logged back at  X: "
                                 + (int)pl.method_23317()
                                 + " Y: "
                                 + (int)pl.method_23318()
                                 + " Z: "
                                 + (int)pl.method_23321()
                           );
                        }

                        this.logoutCache.remove(uuid);
                     }
                  }
               }
            }
         }

         this.playerCache.clear();
      }

      if (e.getPacket() instanceof PlayerRemoveS2CPacket pac) {
         for (UUID uuid2 : pac.profileIds) {
            for (UUID uuid : this.playerCache.keySet()) {
               if (uuid.equals(uuid2)) {
                  PlayerEntity pl = this.playerCache.get(uuid);
                  if ((!this.ignoreBots.getValue() || !this.isABot(pl)) && pl != null) {
                     if (this.notifications.getValue()) {
                        this.sendMessage(
                           pl.method_5477().getString()
                              + " logged out at  X: "
                              + (int)pl.method_23317()
                              + " Y: "
                              + (int)pl.method_23318()
                              + " Z: "
                              + (int)pl.method_23321()
                        );
                     }

                     if (!this.logoutCache.containsKey(uuid)) {
                        this.logoutCache.put(uuid, pl);
                     }
                  }
               }
            }
         }

         this.playerCache.clear();
      }
   }

   @Override
   public void onEnable() {
      this.playerCache.clear();
      this.logoutCache.clear();
   }

   @Override
   public void onUpdate() {
      for (PlayerEntity player : mc.world.method_18456()) {
         if (player != null && !player.equals(mc.player)) {
            this.playerCache.put(player.getGameProfile().getId(), player);
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack s) {
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      if (this.renderMode.is(LogoutSpots.RenderMode.Box)) {
         RenderSystem.defaultBlendFunc();
      } else {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      }

      for (UUID uuid : this.logoutCache.keySet()) {
         PlayerEntity data = this.logoutCache.get(uuid);
         if (data != null) {
            if (this.renderMode.is(LogoutSpots.RenderMode.Box)) {
               Render3DEngine.drawBoxOutline(data.method_5829(), this.color.getValue().getColorObject(), 2.0F);
            } else {
               PlayerEntityModel<PlayerEntity> modelPlayer = new PlayerEntityModel(
                  new Context(
                        mc.getEntityRenderDispatcher(),
                        mc.getItemRenderer(),
                        mc.getBlockRenderManager(),
                        mc.getEntityRenderDispatcher().getHeldItemRenderer(),
                        mc.getResourceManager(),
                        mc.getEntityModelLoader(),
                        mc.textRenderer
                     )
                     .getPart(EntityModelLayers.PLAYER),
                  false
               );
               modelPlayer.method_2838().scale(new Vector3f(-0.3F, -0.3F, -0.3F));
               this.renderEntity(s, data, modelPlayer, ((OtherClientPlayerEntity)data).method_52814().texture(), this.color.getValue().getAlpha());
            }
         }
      }

      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   @Override
   public void onRender2D(DrawContext context) {
      for (UUID uuid : this.logoutCache.keySet()) {
         PlayerEntity data = this.logoutCache.get(uuid);
         if (data != null) {
            Vec3d vector = new Vec3d(data.method_23317(), data.method_23318() + 2.0, data.method_23321());
            Vector4d position = null;
            vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0.0 && vector.z < 1.0) {
               position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
               position.x = Math.min(vector.x, position.x);
               position.y = Math.min(vector.y, position.y);
               position.z = Math.max(vector.x, position.z);
            }

            String string = data.method_5477().getString()
               + " "
               + String.format("%.1f", data.method_6032() + data.method_6067())
               + " X: "
               + (int)data.method_23317()
               + "  Z: "
               + (int)data.method_23321();
            if (position != null) {
               float diff = (float)(position.z - position.x) / 2.0F;
               float textWidth = FontRenderers.sf_bold.getStringWidth(string) * 1.0F;
               float tagX = (float)((position.x + diff - textWidth / 2.0F) * 1.0);
               Render2DEngine.drawRect(context.getMatrices(), tagX - 2.0F, (float)(position.y - 13.0), textWidth + 4.0F, 11.0F, new Color(-1728053247, true));
               FontRenderers.sf_bold.drawString(context.getMatrices(), string, tagX, (float)position.y - 10.0F, -1);
            }
         }
      }
   }

   private void renderEntity(
      @NotNull MatrixStack matrices, @NotNull LivingEntity entity, @NotNull PlayerEntityModel<PlayerEntity> modelBase, Identifier texture, int alpha
   ) {
      modelBase.leftPants.visible = true;
      modelBase.rightPants.visible = true;
      modelBase.leftSleeve.visible = true;
      modelBase.rightSleeve.visible = true;
      modelBase.jacket.visible = true;
      modelBase.field_3394.visible = true;
      double x = entity.method_23317() - mc.getEntityRenderDispatcher().camera.getPos().method_10216();
      double y = entity.method_23318() - mc.getEntityRenderDispatcher().camera.getPos().method_10214();
      double z = entity.method_23321() - mc.getEntityRenderDispatcher().camera.getPos().method_10215();
      ((IEntity)entity).setPos(entity.method_19538());
      matrices.push();
      matrices.translate((float)x, (float)y, (float)z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtility.rad(180.0F - entity.bodyYaw)));
      prepareScale(matrices);
      modelBase.method_17086((PlayerEntity)entity, entity.limbAnimator.getPos(), entity.limbAnimator.getSpeed(), Render3DEngine.getTickDelta());
      float limbSpeed = Math.min(entity.limbAnimator.getSpeed(), 1.0F);
      modelBase.setAngles(
         (PlayerEntity)entity, entity.limbAnimator.getPos(), limbSpeed, entity.field_6012, entity.headYaw - entity.bodyYaw, entity.method_36455()
      );
      BufferBuilder buffer;
      if (this.renderMode.is(LogoutSpots.RenderMode.TexturedChams)) {
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

   private boolean isABot(PlayerEntity ent) {
      return !ent.method_5667().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + ent.method_5477().getString()).getBytes(StandardCharsets.UTF_8)))
         && ent instanceof OtherClientPlayerEntity
         && (FakePlayer.fakePlayer == null || ent.method_5628() != FakePlayer.fakePlayer.method_5628())
         && !ent.method_5477().getString().contains("-");
   }

   private enum RenderMode {
      Chams,
      TexturedChams,
      Box;
   }
}
