package vcore.core.manager.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.IManager;
import vcore.events.impl.PacketEvent;
import vcore.utility.math.MathUtility;

public class ServerManager implements IManager {
   private final ArrayDeque<Float> tpsResult = new ArrayDeque<>(20);
   private long time;
   private long tickTime;
   private float tps;

   public float getTPS() {
      return round2(this.tps);
   }

   public float getTPS2() {
      return round2(20.0F * ((float)this.tickTime / 1000.0F));
   }

   public float getTPSFactor() {
      return (float)this.tickTime / 1000.0F;
   }

   public static float round2(double value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return bd.floatValue();
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.@NotNull Receive event) {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         if (this.time != 0L) {
            this.tickTime = System.currentTimeMillis() - this.time;
            if (this.tpsResult.size() > 20) {
               this.tpsResult.poll();
            }

            this.tpsResult.add(20.0F * (1000.0F / (float)this.tickTime));
            float average = 0.0F;

            for (Float value : this.tpsResult) {
               average += MathUtility.clamp(value, 0.0F, 20.0F);
            }

            this.tps = average / this.tpsResult.size();
         }

         this.time = System.currentTimeMillis();
      }
   }

   public int getPing() {
      if (mc.getNetworkHandler() != null && mc.player != null) {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.method_5667());
         return playerListEntry == null ? 0 : playerListEntry.getLatency();
      } else {
         return 0;
      }
   }
}
