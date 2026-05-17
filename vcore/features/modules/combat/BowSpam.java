package vcore.features.modules.combat;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import vcore.core.InputBlocker;
import vcore.features.modules.Module;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;

public class BowSpam extends Module {
   private final Setting<Boolean> auto = new Setting<>("Auto", false);
   private final Setting<Integer> holdDelay = new Setting<>("HoldDelay", 400, 120, 1000);
   private final Setting<Integer> shootsDelay = new Setting<>("ShootsDelay", 60, 0, 1000);
   private long holdStartAt = 0L;
   private long lastShotAt = 0L;
   private boolean autoPullActive;
   private boolean useKeyHeld;

   public BowSpam() {
      super("BowSpam", "Auto releases bow.", Module.Category.COMBAT);
   }

   @Override
   public void onUpdate() {
      if (!this.canProcess()) {
         this.resetState();
      } else if (this.isUsingBow()) {
         this.handleActiveBowUse();
      } else {
         this.resetPullState();
         if (this.auto.getValue()) {
            this.tryStartAutoPull();
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.canProcess()) {
         this.releaseAutoPull(this.isUsingBow());
      } else {
         this.resetState();
      }

      super.onDisable();
   }

   private void handleActiveBowUse() {
      if (!this.canUseBowNow()) {
         this.stopUsingBow(false);
      } else {
         long now = System.currentTimeMillis();
         if (this.holdStartAt == 0L) {
            this.holdStartAt = now;
         }

         if (now - this.holdStartAt >= this.holdDelay.getValue().intValue()) {
            this.stopUsingBow(true);
         }
      }
   }

   private void tryStartAutoPull() {
      if (this.canUseBowNow()) {
         Hand bowHand = this.getAvailableBowHand();
         if (bowHand != null && this.canUseBowFromHand(bowHand)) {
            this.startUsingBow(bowHand);
            this.holdStartAt = System.currentTimeMillis();
            this.autoPullActive = true;
         }
      }
   }

   private void stopUsingBow(boolean releasedShot) {
      this.releaseAutoPull(releasedShot);
      if (releasedShot) {
         this.lastShotAt = System.currentTimeMillis();
         int delayMs = this.shootsDelay.getValue();
         if (delayMs > 0) {
            InputBlocker.blockUseFor(delayMs);
         }
      }
   }

   private boolean canProcess() {
      return mc.player != null && mc.interactionManager != null;
   }

   private boolean canUseBowNow() {
      if (InputBlocker.isUseBlocked()) {
         return false;
      }

      int delayMs = this.shootsDelay.getValue();
      return delayMs <= 0 || System.currentTimeMillis() - this.lastShotAt >= delayMs;
   }

   private boolean isUsingBow() {
      return mc.player != null && mc.player.method_6115() && mc.player.method_6030().getItem() == Items.BOW;
   }

   private Hand getAvailableBowHand() {
      if (mc.player == null) {
         return null;
      } else if (this.isBow(mc.player.method_6047())) {
         return Hand.MAIN_HAND;
      } else {
         return this.isBow(mc.player.method_6079()) ? Hand.OFF_HAND : null;
      }
   }

   private boolean canUseBowFromHand(Hand hand) {
      if (mc.player == null) {
         return false;
      }

      ItemStack bowStack = mc.player.method_5998(hand);
      return this.isBow(bowStack) && (mc.player.method_31549().creativeMode || !mc.player.method_18808(bowStack).isEmpty());
   }

   private boolean isBow(ItemStack stack) {
      return stack.getItem() == Items.BOW;
   }

   private void startUsingBow(Hand hand) {
      if (mc.currentScreen != null) {
         ((IMinecraftClient)mc).idoItemUse();
      } else {
         mc.options.useKey.setPressed(true);
         this.useKeyHeld = true;
      }
   }

   private void releaseAutoPull(boolean stopUsingItem) {
      if (this.useKeyHeld) {
         mc.options.useKey.setPressed(false);
         this.useKeyHeld = false;
      }

      if (stopUsingItem && this.canProcess() && this.isUsingBow()) {
         mc.interactionManager.stopUsingItem(mc.player);
      }

      this.resetPullState();
   }

   private void resetPullState() {
      this.holdStartAt = 0L;
      this.autoPullActive = false;
   }

   private void resetState() {
      this.resetPullState();
      this.lastShotAt = 0L;
      if (this.useKeyHeld) {
         mc.options.useKey.setPressed(false);
         this.useKeyHeld = false;
      }
   }
}
