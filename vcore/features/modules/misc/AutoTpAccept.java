package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.ThunderUtility;
import vcore.utility.math.MathUtility;

public class AutoTpAccept extends Module {
   public Setting<Boolean> grief = new Setting<>("Grief", false);
   public Setting<Boolean> onlyFriends = new Setting<>("onlyFriends", true);
   public Setting<Boolean> duo = new Setting<>("Duo", false);
   private final Setting<Integer> timeOut = new Setting<>("TimeOut", 60, 1, 180, v -> this.duo.getValue());
   private AutoTpAccept.TpTask tpTask;

   public AutoTpAccept() {
      super("AutoTPaccept", "Auto accepts teleport requests.", Module.Category.MISC);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket packet = event.getPacket();
            if (packet.content().getString().contains("Ñ‚ÐµÐ»ÐµÐ¿Ð¾Ñ€Ñ‚Ð¸Ñ€Ð¾Ð²Ð°Ñ‚ÑŒÑ\u0081Ñ\u008f") || packet.content().getString().contains("tpaccept")) {
               if (this.onlyFriends.getValue()) {
                  if (Managers.FRIEND.isFriend(ThunderUtility.solveName(packet.content().getString()))) {
                     if (!this.duo.getValue()) {
                        this.acceptRequest(packet.content().getString());
                     } else {
                        this.tpTask = new AutoTpAccept.TpTask(() -> this.acceptRequest(packet.content.getString()), System.currentTimeMillis());
                     }
                  }
               } else {
                  this.acceptRequest(packet.content().getString());
               }
            }
         }
      }
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (this.duo.getValue() && this.tpTask != null) {
         String text = "Awaiting target "
            + MathUtility.round((float)(this.timeOut.getValue() * 1000 - (System.currentTimeMillis() - this.tpTask.time())) / 1000.0F, 1);
         FontRenderers.sf_bold
            .drawCenteredString(
               context.getMatrices(),
               text,
               mc.getWindow().getScaledWidth() / 2.0F,
               mc.getWindow().getScaledHeight() / 2.0F + 30.0F,
               HudEditor.getColor(1).getRGB()
            );
      }
   }

   @Override
   public void onUpdate() {
      if (this.duo.getValue() && this.tpTask != null) {
         if (System.currentTimeMillis() - this.tpTask.time > this.timeOut.getValue() * 1000) {
            this.tpTask = null;
            return;
         }

         for (PlayerEntity pl : mc.world.method_18456()) {
            if (pl != mc.player && !Managers.FRIEND.isFriend(pl)) {
               this.tpTask.task.run();
               this.tpTask = null;
               break;
            }
         }
      }
   }

   public void acceptRequest(String name) {
      if (this.grief.getValue()) {
         mc.getNetworkHandler().sendChatCommand("tpaccept " + ThunderUtility.solveName(name));
      } else {
         mc.getNetworkHandler().sendChatCommand("tpaccept");
      }
   }

   private record TpTask(Runnable task, long time) {
   }
}
