package vcore.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import vcore.core.InputBlocker;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.utility.Timer;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class ElytraSwap extends Module {
   private final Setting<Bind> switchButton = new Setting<>("SwitchButton", new Bind(-1, false, false));
   private final Setting<Bind> fireWorkButton = new Setting<>("FireWorkButton", new Bind(-1, false, false));
   private final Setting<Boolean> delay = new Setting<>("Delay", false);
   private final Setting<Boolean> grimStop = new Setting<>("GrimStop", false);
   private final Setting<Boolean> startFireWork = new Setting<>("StartFireWork", false);
   private final Timer switchTimer = new Timer();
   private final Timer fireworkTimer = new Timer();
   public static boolean swapping = false;
   private int swapDelayTicks;
   private long pendingSwapAt = -1L;
   private int pendingSwapSlot = -1;
   private boolean pendingStartFirework;
   private boolean scheduledSwap;
   private int scheduledSwapSlot = -1;
   private boolean scheduledStartFirework;
   private long scheduledActionAt = -1L;

   public ElytraSwap() {
      super("ElytraHelper", "Swaps chestplates and elytras with firework support.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      this.handleScheduledSwap();
      if (this.swapDelayTicks > 0) {
         this.swapDelayTicks--;
      }

      if (mc.currentScreen == null) {
         if (this.switchButton.getValue().getKey() != -1 && this.isKeyPressed(this.switchButton.getValue().getKey()) && this.switchTimer.every(500L)) {
            this.swapChest();
         }

         if (this.fireWorkButton.getValue().getKey() != -1
            && this.isKeyPressed(this.fireWorkButton.getValue().getKey())
            && this.fireworkTimer.every(200L)
            && mc.player.method_6128()) {
            this.useFireWork();
         }
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.SendPost e) {
      if (e.getPacket() instanceof ClientCommandC2SPacket command && command.getMode() == Mode.START_FALL_FLYING && this.startFireWork.getValue()) {
         this.useFireWork();
      }
   }

   public void useFireWork() {
      if (mc.player != null && mc.interactionManager != null) {
         if (!mc.player.method_7357().isCoolingDown(Items.FIREWORK_ROCKET)) {
            if (mc.player.method_6079().getItem() == Items.FIREWORK_ROCKET) {
               float[] rotation = this.getUseRotation();
               this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotation[0], rotation[1]));
            } else {
               SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);
               if (!fireWorkResult.found()) {
                  this.sendMessage("You've got no fireworks!");
               } else {
                  int currentSlot = mc.player.method_31548().selectedSlot;
                  int hotbarSlot = currentSlot % 8 + 1;
                  int itemSlot = fireWorkResult.slot();
                  if (mc.player.method_6115() && mc.player.method_6058() == Hand.MAIN_HAND) {
                     this.useFireworkWithOffhandSwap(itemSlot);
                  } else if (itemSlot >= 36) {
                     this.useFireworkFromHotbar(itemSlot - 36, currentSlot);
                  } else {
                     mc.interactionManager.clickSlot(mc.player.field_7512.syncId, itemSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
                     this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
                     this.useFireworkFromHotbar(hotbarSlot, currentSlot);
                     mc.interactionManager.clickSlot(mc.player.field_7512.syncId, itemSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
                     this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
                  }
               }
            }
         }
      }
   }

   private void useFireworkFromHotbar(int fireworkSlot, int returnSlot) {
      InventoryUtility.switchToSilent(fireworkSlot);
      float[] rotation = this.getUseRotation();
      this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, rotation[0], rotation[1]));
      InventoryUtility.switchToSilent(returnSlot);
   }

   private void useFireworkWithOffhandSwap(int itemSlot) {
      int slotIndex = itemSlot <= 8 ? itemSlot + 36 : itemSlot;
      mc.interactionManager.clickSlot(mc.player.field_7512.syncId, slotIndex, 40, SlotActionType.SWAP, mc.player);
      this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
      float[] rotation = this.getUseRotation();
      this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotation[0], rotation[1]));
      mc.interactionManager.clickSlot(mc.player.field_7512.syncId, slotIndex, 40, SlotActionType.SWAP, mc.player);
      this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
   }

   private float[] getUseRotation() {
      float yaw = mc.player.method_36454();
      float pitch = mc.player.method_36455();
      if (ModuleManager.aura.isEnabled() && Aura.target != null) {
         yaw = ModuleManager.aura.rotationYaw;
         pitch = ModuleManager.aura.rotationPitch;
      }

      return new float[]{yaw, pitch};
   }

   public static int getChestPlateSlot() {
      Item[] items = new Item[]{
         Items.NETHERITE_CHESTPLATE,
         Items.DIAMOND_CHESTPLATE,
         Items.CHAINMAIL_CHESTPLATE,
         Items.IRON_CHESTPLATE,
         Items.GOLDEN_CHESTPLATE,
         Items.LEATHER_CHESTPLATE
      };

      for (Item item : items) {
         SearchInvResult slot = InventoryUtility.findItemInInventory(item);
         if (slot.found()) {
            return slot.slot();
         }
      }

      return -1;
   }

   private void swapChest() {
      if (this.swapDelayTicks <= 0 && mc.player != null) {
         ElytraSwap.SwapRequest request = this.getSwapRequest();
         if (request != null) {
            if (this.delay.getValue()) {
               if (this.pendingSwapSlot != request.slot || this.pendingStartFirework != request.startFirework) {
                  this.pendingSwapSlot = request.slot;
                  this.pendingStartFirework = request.startFirework;
                  this.pendingSwapAt = System.currentTimeMillis() + this.getSwapDelayMs();
                  return;
               }

               if (System.currentTimeMillis() < this.pendingSwapAt) {
                  return;
               }
            }

            if (this.grimStop.getValue()) {
               try {
                  InputBlocker.block();
               } catch (Throwable var3) {
               }

               this.scheduledSwap = true;
               this.scheduledSwapSlot = request.slot;
               this.scheduledStartFirework = request.startFirework;
               this.scheduledActionAt = System.currentTimeMillis() + 60L;
            } else {
               this.executeSwap(request);
            }
         }
      }
   }

   private void handleScheduledSwap() {
      if (this.scheduledSwap) {
         if (System.currentTimeMillis() >= this.scheduledActionAt) {
            if (mc.player != null) {
               this.executeSwap(new ElytraSwap.SwapRequest(this.scheduledSwapSlot, this.scheduledStartFirework));

               try {
                  InputBlocker.blockFor(10L);
               } catch (Throwable var2) {
               }

               this.scheduledSwap = false;
               this.scheduledSwapSlot = -1;
               this.scheduledStartFirework = false;
               this.scheduledActionAt = -1L;
            }
         }
      }
   }

   private void executeSwap(ElytraSwap.SwapRequest request) {
      swapping = true;
      clickSlot(request.slot);
      clickSlot(6);
      clickSlot(request.slot);
      if (request.startFirework) {
         this.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
      }

      swapping = false;
      this.resetPendingDelay();
      this.swapDelayTicks = this.getPostSwapDelayTicks();
   }

   private ElytraSwap.SwapRequest getSwapRequest() {
      if (mc.player.method_31548().method_5438(38).getItem() == Items.ELYTRA) {
         int slot = getChestPlateSlot();
         if (slot == -1) {
            this.sendMessage("You don't have a chestplate!");
            return null;
         } else {
            return new ElytraSwap.SwapRequest(slot, false);
         }
      } else {
         SearchInvResult result = InventoryUtility.findItemInInventory(Items.ELYTRA);
         if (!result.found()) {
            this.sendMessage("You don't have an elytra!");
            return null;
         } else {
            boolean startFirework = this.startFireWork.getValue() && mc.player.field_6017 > 0.0F;
            return new ElytraSwap.SwapRequest(result.slot(), startFirework);
         }
      }
   }

   private int getPostSwapDelayTicks() {
      return (int)(2.0F + Managers.SERVER.getPing() / 25.0F);
   }

   private long getSwapDelayMs() {
      return 200L;
   }

   private void resetPendingDelay() {
      this.pendingSwapAt = -1L;
      this.pendingSwapSlot = -1;
      this.pendingStartFirework = false;
   }

   private record SwapRequest(int slot, boolean startFirework) {
   }
}
