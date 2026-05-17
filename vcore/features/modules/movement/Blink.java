package vcore.features.modules.movement;

import java.awt.Color;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.utility.render.Render3DEngine;

public class Blink extends Module {
   private final Setting<Boolean> autoBlink = new Setting<>("AutoBlink", false);
   private final Setting<Integer> autoBlinkDelay = new Setting<>("Delay", 570, 550, 1200, v -> this.autoBlink.getValue());
   private final Setting<Boolean> render = new Setting<>("Render", true);
   private final Setting<Bind> cancel = new Setting<>("Cancel", new Bind(-1, false, false));
   public static Vec3d lastPos = Vec3d.ZERO;
   private Vec3d prevVelocity = Vec3d.ZERO;
   private float prevYaw = 0.0F;
   private boolean prevSprinting = false;
   private final Queue<Packet<?>> storedPackets = new LinkedList<>();
   private final Queue<Packet<?>> storedTransactions = new LinkedList<>();
   private final AtomicBoolean sending = new AtomicBoolean(false);
   private long lastAutoBlinkTime;
   private int bypassTicks;
   private boolean auraCycleActive;

   public Blink() {
      super("Blink", "Delays movement packets.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null && !mc.isIntegratedServerRunning() && mc.getNetworkHandler() != null) {
         this.storedTransactions.clear();
         lastPos = mc.player.method_19538();
         this.prevVelocity = mc.player.method_18798();
         this.prevYaw = mc.player.method_36454();
         this.prevSprinting = mc.player.method_5624();
         mc.world
            .method_8649(
               new ClientPlayerEntity(
                  mc, mc.world, mc.getNetworkHandler(), mc.player.getStatHandler(), mc.player.getRecipeBook(), mc.player.lastSprinting, mc.player.method_5715()
               )
            );
         this.sending.set(false);
         this.storedPackets.clear();
         this.lastAutoBlinkTime = System.currentTimeMillis();
         this.bypassTicks = 0;
         this.auraCycleActive = false;
      } else {
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      if (mc.world != null && mc.player != null) {
         while (!this.storedPackets.isEmpty()) {
            this.sendPacket(this.storedPackets.poll());
         }

         this.bypassTicks = 0;
         this.auraCycleActive = false;
      }
   }

   @Override
   public String getDisplayInfo() {
      return Integer.toString(this.storedPackets.size());
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send event) {
      if (!fullNullCheck()) {
         Packet<?> packet = event.getPacket();
         if (!this.sending.get()) {
            if (this.bypassTicks <= 0) {
               if (packet instanceof CommonPongC2SPacket) {
                  this.storedTransactions.add(packet);
               }

               if (!(packet instanceof ChatMessageC2SPacket)
                  && !(packet instanceof TeleportConfirmC2SPacket)
                  && !(packet instanceof KeepAliveC2SPacket)
                  && !(packet instanceof AdvancementTabC2SPacket)
                  && !(packet instanceof ClientStatusC2SPacket)) {
                  event.cancel();
                  this.storedPackets.add(packet);
               }
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(EventTick event) {
      if (!fullNullCheck()) {
         this.tickBypassWindow();
         if (!this.isKeyPressed(this.cancel)) {
            this.handleAutoBlink();
         } else {
            this.storedPackets.clear();
            mc.player.method_23327(lastPos.method_10216(), lastPos.method_10214(), lastPos.method_10215());
            mc.player.method_18799(this.prevVelocity);
            mc.player.method_36456(this.prevYaw);
            mc.player.method_5728(this.prevSprinting);
            mc.player.method_5660(false);
            mc.options.sneakKey.setPressed(false);
            this.sending.set(true);

            while (!this.storedTransactions.isEmpty()) {
               this.sendPacket(this.storedTransactions.poll());
            }

            this.sending.set(false);
            this.disable("Canceling..");
         }
      }
   }

   @EventHandler
   public void onAttack(EventAttack event) {
      if (this.autoBlink.getValue() && mc.player != null) {
         if (ModuleManager.aura.isEnabled() && Aura.target != null) {
            if (event.isPre()) {
               this.startForceCycle(true);
            } else if (this.auraCycleActive) {
               this.auraCycleActive = false;
               this.resetAutoBlinkTimer();
            }
         }
      }
   }

   private void sendPackets() {
      if (mc.player != null) {
         this.sending.set(true);

         while (!this.storedPackets.isEmpty()) {
            Packet<?> packet = this.storedPackets.poll();
            this.sendPacket(packet);
            if (packet instanceof PlayerMoveC2SPacket && !(packet instanceof LookAndOnGround)) {
               lastPos = new Vec3d(
                  ((PlayerMoveC2SPacket)packet).getX(mc.player.method_23317()),
                  ((PlayerMoveC2SPacket)packet).getY(mc.player.method_23318()),
                  ((PlayerMoveC2SPacket)packet).getZ(mc.player.method_23321())
               );
            }
         }

         this.sending.set(false);
         this.storedPackets.clear();
      }
   }

   private void handleAutoBlink() {
      if (this.autoBlink.getValue() && !this.auraCycleActive) {
         long now = System.currentTimeMillis();
         if (now - this.lastAutoBlinkTime >= this.autoBlinkDelay.getValue().intValue()) {
            this.startForceCycle(false);
         }
      }
   }

   private void startForceCycle(boolean auraTriggered) {
      this.flushStoredQueues();
      this.bypassTicks = Math.max(this.bypassTicks, 2);
      this.auraCycleActive = auraTriggered;
      this.resetAutoBlinkTimer();
   }

   private void flushStoredQueues() {
      if (!this.storedPackets.isEmpty()) {
         this.sendPackets();
      }

      if (!this.storedTransactions.isEmpty()) {
         this.sending.set(true);

         while (!this.storedTransactions.isEmpty()) {
            this.sendPacket(this.storedTransactions.poll());
         }

         this.sending.set(false);
      }
   }

   private void resetAutoBlinkTimer() {
      this.lastAutoBlinkTime = System.currentTimeMillis();
   }

   private void tickBypassWindow() {
      if (this.bypassTicks > 0) {
         this.bypassTicks--;
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (mc.player != null && mc.world != null) {
         if (!mc.options.getPerspective().isFirstPerson()) {
            if (this.render.getValue() && lastPos != null) {
               Box hitbox = mc.player.method_5829().offset(lastPos.subtract(mc.player.method_19538()));
               Render3DEngine.drawBoxOutline(hitbox, Color.WHITE, 2.0F);
            }
         }
      }
   }
}
