package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventMove;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.combat.ElytraTarget;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class ElytraMotion extends Module {
   public final Setting<Float> attackDistance = new Setting<>("AttackDistance", 3.0F, 0.1F, 5.0F);
   public boolean freeze;
   private final Setting<Boolean> autoFirework = new Setting<>("AutoFirework", false);
   private final Setting<Boolean> bypass = new Setting<>("MatrixBypass", false);
   private final Timer timer = new Timer();

   public ElytraMotion() {
      super("ElytraMotion", "Helps stabilize elytra targeting movement.", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      if (mc.player != null) {
         if (this.shouldPauseForPeakAssist()) {
            this.freeze = false;
         } else if (!mc.player.method_6128()) {
            this.freeze = false;
         } else {
            LivingEntity target = Aura.target instanceof LivingEntity living ? living : null;
            ElytraTarget elytraTarget = ModuleManager.elytraTarget;
            this.freeze = this.check(target, elytraTarget);
            if (this.freeze) {
               if (this.bypass.getValue() && this.timer.passedMs(500L)) {
                  this.useFirework();
                  this.timer.reset();
               }
            } else {
               if (this.autoFirework.getValue() && target != null && getBps(mc.player) <= 30.0 && this.timer.passedMs(500L)) {
                  this.useFirework();
                  this.timer.reset();
               }
            }
         }
      }
   }

   @EventHandler
   public void onMove(EventMove event) {
      if (this.shouldPauseForPeakAssist()) {
         this.freeze = false;
      } else if (this.freeze) {
         event.cancel();
         event.setX(0.0);
         event.setY(0.0);
         event.setZ(0.0);
      }
   }

   public boolean check(LivingEntity target, ElytraTarget elytraTarget) {
      if (target != null && mc.player != null && mc.player.method_6128()) {
         boolean canTarget = elytraTarget != null && elytraTarget.shouldTarget(target);
         return !canTarget && target.method_5739(mc.player) < this.attackDistance.getValue();
      } else {
         return false;
      }
   }

   @Override
   public void onDisable() {
      this.freeze = false;
      super.onDisable();
   }

   private boolean shouldPauseForPeakAssist() {
      return ModuleManager.eMaceHelper != null && ModuleManager.eMaceHelper.isEnabled() && ModuleManager.eMaceHelper.isPeakAssistActive();
   }

   private void useFirework() {
      if (mc.player != null && mc.interactionManager != null) {
         if (!mc.player.method_7357().isCoolingDown(Items.FIREWORK_ROCKET)) {
            if (mc.player.method_6079().getItem() == Items.FIREWORK_ROCKET) {
               float[] rotation = this.getUseRotation();
               this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotation[0], rotation[1]));
            } else {
               SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);
               if (fireWorkResult.found()) {
                  int currentSlot = mc.player.method_31548().selectedSlot;
                  int hotbarSlot = currentSlot % 8 + 1;
                  int itemSlot = fireWorkResult.slot();
                  if (mc.player.method_6115() && mc.player.method_6058() == Hand.MAIN_HAND) {
                     this.useFireworkWithOffhandSwap(itemSlot);
                  } else if (itemSlot >= 36) {
                     this.useFireworkFromHotbar(itemSlot - 36, currentSlot);
                  } else {
                     mc.interactionManager.clickSlot(mc.player.field_7512.syncId, itemSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
                     this.useFireworkFromHotbar(hotbarSlot, currentSlot);
                     mc.interactionManager.clickSlot(mc.player.field_7512.syncId, itemSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
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
      float[] rotation = this.getUseRotation();
      this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotation[0], rotation[1]));
      mc.interactionManager.clickSlot(mc.player.field_7512.syncId, slotIndex, 40, SlotActionType.SWAP, mc.player);
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

   private static double getBps(LivingEntity entity) {
      if (entity == null) {
         return 0.0;
      }

      double dx = entity.method_23317() - entity.field_6014;
      double dy = entity.method_23318() - entity.field_6036;
      double dz = entity.method_23321() - entity.field_5969;
      return Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
   }
}
