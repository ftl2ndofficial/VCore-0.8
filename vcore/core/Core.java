package vcore.core;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.NotNull;
import vcore.Vcore;
import vcore.core.manager.client.MacroManager;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventDeath;
import vcore.events.impl.EventKeyPress;
import vcore.events.impl.EventMouse;
import vcore.events.impl.EventSync;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.cmd.Command;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.gui.notification.Notification;
import vcore.utility.Timer;
import vcore.utility.player.InteractionUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.animation.CaptureMark;

public final class Core {
   public static boolean lockSprint;
   public static boolean serverSprint;
   public static boolean hold_mouse0;
   public static final Map<String, Identifier> HEADS = new ConcurrentHashMap<>();
   public ArrayList<Packet<?>> silentPackets = new ArrayList<>();
   private final Timer lastPacket = new Timer();
   private final Timer autoSave = new Timer();
   private final Timer setBackTimer = new Timer();

   @EventHandler
   public void onTick(PlayerUpdateEvent event) {
      if (!Module.fullNullCheck()) {
         Managers.NOTIFICATION.onUpdate();
         Managers.MODULE.onUpdate();
         if (ModuleManager.clickGui.getBind().getKey() == -1) {
            Command.sendMessage(Formatting.RED + "Default clickgui keybind --> Right Shift");
            ModuleManager.clickGui.setBind(InputUtil.fromTranslationKey("key.keyboard.right.shift").getCode(), false, false);
         }

         for (PlayerEntity p : Module.mc.world.method_18456()) {
            if (p.method_29504() || p.method_6032() == 0.0F) {
               Vcore.EVENT_BUS.post(new EventDeath(p));
            }
         }

         if (!Objects.equals(Managers.COMMAND.getPrefix(), ClientSettings.prefix.getValue())) {
            Managers.COMMAND.setPrefix(ClientSettings.prefix.getValue());
         }

         new HashMap<>(InteractionUtility.awaiting).forEach((bp, time) -> {
            if ((float)(System.currentTimeMillis() - time) > Managers.SERVER.getPing() * 2.0F) {
               InteractionUtility.awaiting.remove(bp);
            }
         });
         if (this.autoSave.every(600000L)) {
            Managers.FRIEND.saveFriends();
            Managers.CONFIG.save(Managers.CONFIG.getCurrentConfig());
            Managers.MACRO.saveMacro();
            Managers.NOTIFICATION.publicity("AutoSave", "Saving config..", 3, Notification.Type.INFO);
         }
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.@NotNull Send e) {
      if (e.getPacket() instanceof PlayerMoveC2SPacket && !(e.getPacket() instanceof OnGroundOnly)) {
         this.lastPacket.reset();
      }

      if (e.getPacket() instanceof ClientCommandC2SPacket c && (c.getMode() == Mode.START_SPRINTING || c.getMode() == Mode.STOP_SPRINTING)) {
         if (lockSprint) {
            e.cancel();
            return;
         }

         switch (c.getMode()) {
            case START_SPRINTING:
               serverSprint = true;
               break;
            case STOP_SPRINTING:
               serverSprint = false;
         }
      }
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (!Module.fullNullCheck()) {
         CaptureMark.tick();
         Render3DEngine.updateTargetESP();
      }
   }

   public void onRender2D(DrawContext e) {
      this.drawGps(e);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!Module.fullNullCheck()) {
         if (e.getPacket() instanceof GameJoinS2CPacket) {
            Managers.MODULE.onLogin();
         }

         if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.setBackTimer.reset();
         }
      }
   }

   public void drawGps(DrawContext e) {
      if (Vcore.gps_position != null) {
         float dst = this.getDistance(Vcore.gps_position);
         float xOffset = Module.mc.getWindow().getScaledWidth() / 2.0F;
         float yOffset = Module.mc.getWindow().getScaledHeight() / 6.0F;
         float radius = 5.0F;
         float yaw = getRotations(new Vec2f(Vcore.gps_position.method_10263(), Vcore.gps_position.method_10260())) - Module.mc.player.method_36454();
         float pointerHeight = 12.5F;
         float pointerCenterOffset = pointerHeight * 2.0F / 3.0F - 2.0F;
         float px = (float)(Math.sin(Math.toRadians(yaw)) * radius) + xOffset;
         float py = (float)(yOffset - Math.cos(Math.toRadians(yaw)) * radius) + pointerCenterOffset;
         e.getMatrices().translate(px, py, 0.0F);
         e.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
         Render2DEngine.drawTracerPointer(e.getMatrices(), 0.0F, -pointerCenterOffset, pointerHeight, 0.5F, 3.63F, true, true, HudEditor.getColor(1).getRGB());
         e.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-yaw));
         e.getMatrices().translate(-px, -py, 0.0F);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         FontRenderers.modules.drawCenteredString(e.getMatrices(), "gps (" + dst + "m)", xOffset, yOffset + 20.0F, -1);
         if (dst < 10.0F) {
            Vcore.gps_position = null;
         }
      }
   }

   @EventHandler
   public void onKeyPress(EventKeyPress event) {
      if (event.getKey() != -1) {
         for (MacroManager.Macro m : Managers.MACRO.getMacros()) {
            if (m.getBind() == event.getKey()) {
               m.runMacro();
            }
         }
      }
   }

   @EventHandler
   public void onMouse(EventMouse event) {
      if (event.getAction() == 0) {
         hold_mouse0 = false;
      }

      if (event.getAction() == 1) {
         hold_mouse0 = true;
      }
   }

   public int getDistance(BlockPos bp) {
      double d0 = Module.mc.player.method_23317() - bp.method_10263();
      double d2 = Module.mc.player.method_23321() - bp.method_10260();
      return (int)MathHelper.sqrt((float)(d0 * d0 + d2 * d2));
   }

   public long getSetBackTime() {
      return this.setBackTimer.getPassedTimeMs();
   }

   public static float getRotations(Vec2f vec) {
      if (Module.mc.player == null) {
         return 0.0F;
      }

      double x = vec.x - Module.mc.player.method_19538().x;
      double z = vec.y - Module.mc.player.method_19538().z;
      return (float)(-(Math.atan2(x, z) * (180.0 / Math.PI)));
   }

   public void bobView(MatrixStack matrices, float tickDelta) {
      if (Module.mc.getCameraEntity() instanceof PlayerEntity playerEntity) {
         float var6 = -(playerEntity.field_5973 + (playerEntity.field_5973 - playerEntity.field_6039) * tickDelta);
         float h = MathHelper.lerp(tickDelta, playerEntity.prevStrideDistance, playerEntity.strideDistance);
         matrices.translate(MathHelper.sin(var6 * (float) Math.PI) * h * 0.1F, -Math.abs(MathHelper.cos(var6 * (float) Math.PI) * h) * 0.3, 0.0);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(var6 * (float) Math.PI) * h * 3.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(MathHelper.cos(var6 * (float) Math.PI - 0.2F) * h) * 0.3F));
      }
   }
}
