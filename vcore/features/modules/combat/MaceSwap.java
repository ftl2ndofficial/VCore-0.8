package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class MaceSwap extends Module {
   private final Setting<MaceSwap.Mode> mode = new Setting<>("Mode", MaceSwap.Mode.Normal);
   private final Setting<Boolean> maceStun = new Setting<>("Mace Stun", true);
   private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   private final Setting<Boolean> silent = new Setting<>("Silent", false, v -> this.switchBack.getValue());
   private final Setting<Boolean> autoDisable = new Setting<>("AutoDisable", false);
   private boolean isSwitching = false;
   private int previousSlot = -1;
   private int currentSwitchDelay = -1;
   public static boolean calledFromAura = false;
   private static volatile boolean auraPostAttackTriggered;

   public MaceSwap() {
      super("MaceSwap", "Auto swap to mace on hit.", Module.Category.COMBAT);
   }

   public boolean isMaceStun() {
      return this.maceStun.getValue();
   }

   public boolean isSilent() {
      return this.silent.getValue();
   }

   public boolean isSwitching() {
      return this.isSwitching;
   }

   public boolean hasMaceInHotbar() {
      return this.findMaceInHotbar() != -1;
   }

   @Override
   public void onEnable() {
      this.resetState();
      auraPostAttackTriggered = false;
      calledFromAura = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.resetState();
      auraPostAttackTriggered = false;
      calledFromAura = false;
      super.onDisable();
   }

   @EventHandler
   public void onSync(EventSync event) {
      this.processSwitchBack(false);
      this.processAutoDisable();
   }

   private void swapToSlot(int slot) {
      if (mc.player != null && mc.player.method_31548() != null) {
         if (slot >= 0 && slot <= 8) {
            mc.player.method_31548().selectedSlot = slot;
         }
      }
   }

   private int findMaceInHotbar() {
      if (mc.player != null && mc.player.method_31548() != null) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.method_31548().method_5438(i);
            Item item = stack.getItem();
            if (item != null) {
               String key = item.getTranslationKey() != null ? item.getTranslationKey().toLowerCase() : item.toString().toLowerCase();
               if (key.contains("mace") || item.toString().toLowerCase().contains("mace")) {
                  switch ((MaceSwap.Mode)this.mode.getValue()) {
                     case Normal:
                        return i;
                     case Breach:
                        if (this.hasEnchantment(stack, "breach")) {
                           return i;
                        }
                        break;
                     case WindBurst:
                        if (this.hasEnchantment(stack, "wind_burst")) {
                           return i;
                        }
                  }
               }
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean hasEnchantment(ItemStack stack, String enchantName) {
      return stack != null && stack.hasEnchantments() ? stack.getEnchantments().getEnchantments().stream().anyMatch(enchant -> {
         String enchantId = enchant.toString().toLowerCase();
         return enchantId.contains(enchantName.toLowerCase());
      }) : false;
   }

   public int silentSwapToMace() {
      if (mc.player == null || mc.player.method_31548() == null) {
         return -1;
      }

      if (this.isSwitching) {
         return this.previousSlot;
      }

      int currentSlot = mc.player.method_31548().selectedSlot;
      int maceSlot = this.findMaceInHotbar();
      if (maceSlot != -1 && maceSlot != currentSlot) {
         this.previousSlot = currentSlot;
         this.swapToSlot(maceSlot);
         this.isSwitching = true;
         this.currentSwitchDelay = -1;
         if (!this.switchBack.getValue()) {
            this.resetState();
         }

         return currentSlot;
      } else {
         return -1;
      }
   }

   public void handleSwitchBackManual() {
      this.processSwitchBack(true);
   }

   private void processAutoDisable() {
      if (!this.autoDisable.getValue()) {
         auraPostAttackTriggered = false;
      } else if (auraPostAttackTriggered) {
         if (!this.isSwitching || !this.switchBack.getValue()) {
            auraPostAttackTriggered = false;
            calledFromAura = false;
            this.disable();
         }
      }
   }

   private void processSwitchBack(boolean manual) {
      if (this.isSwitching && this.switchBack.getValue()) {
         int switchDelay = this.silent.getValue() ? -1 : 2;
         if (this.currentSwitchDelay < switchDelay) {
            this.currentSwitchDelay++;
         } else {
            this.swapToSlot(this.previousSlot);
            this.resetState();
            if (manual) {
               calledFromAura = false;
            }
         }
      } else {
         if (manual) {
            calledFromAura = false;
         }
      }
   }

   private void resetState() {
      this.previousSlot = -1;
      this.currentSwitchDelay = -1;
      this.isSwitching = false;
   }

   public static void markAuraPostAttack(boolean attacked) {
      if (attacked) {
         auraPostAttackTriggered = true;
      }
   }

   public enum Mode {
      Normal,
      Breach,
      WindBurst;
   }
}
