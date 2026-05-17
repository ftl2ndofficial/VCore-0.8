package vcore.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class NoPush extends Module {
   public Setting<Boolean> blocks = new Setting<>("Blocks", true);
   public Setting<Boolean> players = new Setting<>("Players", true);
   public Setting<Boolean> water = new Setting<>("Liquids", false);
   public Setting<Boolean> fishingHook = new Setting<>("FishingHook", false);

   public NoPush() {
      super("NoPush", "Prevents being pushed.", Module.Category.PLAYER);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof EntityStatusS2CPacket pac
         && pac.getStatus() == 31
         && pac.getEntity(mc.world) instanceof FishingBobberEntity hook
         && this.fishingHook.getValue()
         && hook.getHookedEntity() == mc.player) {
         e.cancel();
      }
   }
}
