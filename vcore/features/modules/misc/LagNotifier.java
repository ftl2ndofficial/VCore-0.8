package vcore.features.modules.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.DecimalFormat;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

public class LagNotifier extends Module {
   private final Setting<Boolean> rubberbandNotify = new Setting<>("Rubberband", true);
   private final Setting<Boolean> serverResponseNotify = new Setting<>("ServerResponse", true);
   private final Setting<Integer> responseTreshold = new Setting<>("ResponseTreshold", 5, 0, 15, v -> this.serverResponseNotify.getValue());
   private final Setting<Boolean> tpsNotify = new Setting<>("TPS", true);
   private Timer notifyTimer = new Timer();
   private Timer rubberbandTimer = new Timer();
   private Timer packetTimer = new Timer();
   private boolean isLagging = false;

   public LagNotifier() {
      super("LagNotifier", "Notifies about server lag.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.notifyTimer = new Timer();
      this.rubberbandTimer = new Timer();
      this.packetTimer = new Timer();
      this.isLagging = false;
      super.onEnable();
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!fullNullCheck()) {
         if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.rubberbandTimer.reset();
         }

         if (e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            this.packetTimer.reset();
         }
      }
   }

   @Override
   public void onRender2D(DrawContext context) {
      Render2DEngine.setupRender();
      RenderSystem.defaultBlendFunc();
      if (!this.rubberbandTimer.passedMs(5000L) && this.rubberbandNotify.getValue()) {
         DecimalFormat decimalFormat = new DecimalFormat("#.#");
         FontRenderers.modules
            .drawCenteredString(
               context.getMatrices(),
               "Rubberband detected! " + decimalFormat.format((5000.0F - (float)this.rubberbandTimer.getTimeMs()) / 1000.0F),
               mc.getWindow().getScaledWidth() / 2.0F,
               mc.getWindow().getScaledHeight() / 3.0F,
               new Color(16768768).getRGB()
            );
      }

      if (this.packetTimer.passedMs(this.responseTreshold.getValue().intValue() * 1000L) && this.serverResponseNotify.getValue()) {
         DecimalFormat decimalFormat = new DecimalFormat("#.#");
         FontRenderers.modules
            .drawCenteredString(
               context.getMatrices(),
               "The server stopped responding! " + decimalFormat.format((float)this.packetTimer.getTimeMs() / 1000.0F),
               mc.getWindow().getScaledWidth() / 2.0F,
               mc.getWindow().getScaledHeight() / 3.0F,
               new Color(16768768).getRGB()
            );
         RenderSystem.setShaderColor(1.0F, 0.87F, 0.0F, 1.0F);
         context.drawTexture(
            TextureStorage.lagIcon,
            (int)(mc.getWindow().getScaledWidth() / 2.0F - 40.0F),
            (int)(mc.getWindow().getScaledHeight() / 3.0F - 120.0F),
            0.0F,
            0.0F,
            80,
            80,
            80,
            80
         );
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (Managers.SERVER.getTPS() < 10.0F && this.notifyTimer.passedMs(60000L) && this.tpsNotify.getValue()) {
         String msg = "Server TPS is below 10!";
         Managers.NOTIFICATION.publicity("LagNotifier", msg, 8, Notification.Type.ERROR);
         this.isLagging = true;
         this.notifyTimer.reset();
      }

      if (Managers.SERVER.getTPS() > 15.0F && this.isLagging) {
         Managers.NOTIFICATION.publicity("LagNotifier", "Server TPS has stabilized!", 8, Notification.Type.SUCCESS);
         this.isLagging = false;
      }

      Render2DEngine.endRender();
   }
}
