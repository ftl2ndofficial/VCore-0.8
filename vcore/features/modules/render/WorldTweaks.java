package vcore.features.modules.render;

import java.awt.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;

public class WorldTweaks extends Module {
   public static final Setting<BooleanSettingGroup> fogModify = new Setting<>("FogModify", new BooleanSettingGroup(true));
   public static final Setting<Integer> fogStart = new Setting<>("FogStart", 0, 0, 256).addToGroup(fogModify);
   public static final Setting<Integer> fogEnd = new Setting<>("FogEnd", 128, 10, 256).addToGroup(fogModify);
   public static final Setting<ColorSetting> fogColor = new Setting<>("FogColor", new ColorSetting(new Color(30975))).addToGroup(fogModify);
   public final Setting<Boolean> ctime = new Setting<>("ChangeTime", true);
   public final Setting<Integer> ctimeVal = new Setting<>("Time", 21, 0, 23);
   long oldTime;

   public WorldTweaks() {
      super("WorldTweaks", "World rendering tweaks.", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      this.oldTime = mc.world.method_8510();
   }

   @Override
   public void onDisable() {
      mc.world.setTimeOfDay(this.oldTime);
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.ctime.getValue()) {
         this.oldTime = ((WorldTimeUpdateS2CPacket)event.getPacket()).getTime();
         event.cancel();
      }
   }

   @Override
   public void onUpdate() {
      if (this.ctime.getValue()) {
         mc.world.setTimeOfDay(this.ctimeVal.getValue() * 1000);
      }
   }
}
