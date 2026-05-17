package vcore.features.modules.movement;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import vcore.events.impl.EventClickSlot;
import vcore.events.impl.EventKeyboardInput;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.MovementUtility;

public class GuiMove extends Module {
   private final Setting<GuiMove.Bypass> clickBypass = new Setting<>("Bypass", GuiMove.Bypass.None);
   private final Queue<ClickSlotC2SPacket> grimPacketQueue = new ConcurrentLinkedQueue<>();
   private final Set<ClickSlotC2SPacket> sendingPackets = new HashSet<>();
   private CloseHandledScreenC2SPacket pendingClosePacket;
   private boolean stopInputQueued;
   private boolean stopInputApplied;
   private boolean sendingClosePacket;

   public GuiMove() {
      super("GuiMove", "Move while in GUI.", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      KeyBinding[] keys = this.getMovementKeys();
      if (!(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof SignEditScreen)) {
         this.updateKeyBindingState(keys);
         if (this.isGrim()) {
            this.queueStopInputBeforeGrimAction();
         } else {
            this.clearGrimState();
         }
      } else {
         for (KeyBinding key : keys) {
            key.setPressed(false);
         }
      }
   }

   @Override
   public void onDisable() {
      this.clearGrimState();
   }

   @EventHandler
   public void onClickSlot(EventClickSlot e) {
      if (this.clickBypass.is(GuiMove.Bypass.DisableClicks) && (MovementUtility.isMoving() || mc.options.jumpKey.isPressed())) {
         e.cancel();
      }
   }

   @EventHandler
   public void onKeyboardInput(EventKeyboardInput e) {
      if (this.isGrim() && this.stopInputQueued) {
         e.clearMovementInput();
         this.stopInputApplied = true;
      }
   }

   @EventHandler
   public void onPostSync(EventPostSync e) {
      if (this.isGrim() && this.stopInputQueued && this.stopInputApplied) {
         this.stopInputQueued = false;
         this.stopInputApplied = false;
         this.flushQueuedClicks();
         this.flushPendingClose();
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (this.isGrim() && e.getPacket() instanceof CloseHandledScreenC2SPacket closePacket) {
         if (this.sendingClosePacket) {
            this.sendingClosePacket = false;
            return;
         }

         if (mc.currentScreen instanceof InventoryScreen && this.isGrimActionInputActive()) {
            this.pendingClosePacket = closePacket;
            e.cancel();
            return;
         }
      }

      if (this.isGrim() && e.getPacket() instanceof ClickSlotC2SPacket click) {
         if (this.sendingPackets.remove(click)) {
            return;
         }

         if (click.getSyncId() == 0 && mc.currentScreen instanceof InventoryScreen && this.isGrimActionInputActive()) {
            this.grimPacketQueue.add(click);
            e.cancel();
            return;
         }
      }

      if (MovementUtility.isMoving() && mc.options.jumpKey.isPressed()) {
         if (this.clickBypass.is(GuiMove.Bypass.DisableClicks) && e.getPacket() instanceof ClickSlotC2SPacket) {
            e.cancel();
         }
      }
   }

   private void updateKeyBindingState(KeyBinding[] keys) {
      for (KeyBinding key : keys) {
         key.setPressed(this.isKeyPressed(InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode()));
      }
   }

   private KeyBinding[] getMovementKeys() {
      return new KeyBinding[]{mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey};
   }

   private void queueStopInputBeforeGrimAction() {
      if (!this.grimPacketQueue.isEmpty() || this.pendingClosePacket != null) {
         if (!this.stopInputQueued) {
            this.stopInputQueued = true;
            this.stopInputApplied = false;
         }
      }
   }

   private void flushQueuedClicks() {
      if (mc.getNetworkHandler() != null) {
         while (!this.grimPacketQueue.isEmpty()) {
            ClickSlotC2SPacket packet = this.grimPacketQueue.poll();
            if (packet != null) {
               this.sendingPackets.add(packet);
               mc.getNetworkHandler().method_52787(packet);
            }
         }
      }
   }

   private void flushPendingClose() {
      if (mc.getNetworkHandler() != null && this.pendingClosePacket != null) {
         this.sendingClosePacket = true;
         mc.getNetworkHandler().method_52787(this.pendingClosePacket);
         this.pendingClosePacket = null;
      }
   }

   private void clearGrimState() {
      this.grimPacketQueue.clear();
      this.sendingPackets.clear();
      this.pendingClosePacket = null;
      this.stopInputQueued = false;
      this.stopInputApplied = false;
      this.sendingClosePacket = false;
   }

   private boolean isGrim() {
      return this.clickBypass.is(GuiMove.Bypass.Grim);
   }

   private boolean isGrimActionInputActive() {
      return mc.options.forwardKey.isPressed()
         || mc.options.backKey.isPressed()
         || mc.options.leftKey.isPressed()
         || mc.options.rightKey.isPressed()
         || mc.options.jumpKey.isPressed()
         || mc.options.sneakKey.isPressed()
         || mc.options.sprintKey.isPressed()
         || mc.player != null && (mc.player.method_5624() || mc.player.method_5715());
   }

   public boolean shouldSuppressSprint() {
      return false;
   }

   private enum Bypass {
      None,
      Grim,
      DisableClicks;
   }
}
