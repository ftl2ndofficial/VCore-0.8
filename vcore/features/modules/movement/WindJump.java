package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventPostSync;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public final class WindJump extends Module {
   private final Setting<Bind> activationBind = new Setting<>("Bind", new Bind(-1, false, false));
   private static final float WIND_CHARGE_PITCH = 90.0F;
   private boolean activationPressed = false;
   private boolean jumpRequested = false;
   private boolean restoreJumpInput = false;
   private boolean previousJumpPressed = false;
   private boolean silentRotationActive = false;
   private boolean auraPausedByWindJump = false;
   private boolean previousAuraPause = false;
   private boolean resumeAuraOnPostSync = false;
   private int pendingRestoreSlot = -1;

   public WindJump() {
      super("WindJump", "Uses wind charge to boost jump.", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      this.restoreJumpInput();
      boolean pressed = this.isKeyPressed(this.activationBind);
      if (!pressed) {
         this.activationPressed = false;
      } else {
         if (!this.activationPressed && this.canStart()) {
            this.pauseAuraIfNeeded();
            this.jumpRequested = true;
         }

         this.activationPressed = true;
      }
   }

   @Override
   public void onDisable() {
      this.jumpRequested = false;
      this.silentRotationActive = false;
      this.activationPressed = false;
      this.resumeAuraOnPostSync = false;
      this.restorePendingSlot();
      this.resumeAura();
      this.restoreJumpInput();
   }

   @EventHandler(priority = -200)
   private void onSync(EventSync event) {
      this.restorePendingSlot();
      if (this.jumpRequested) {
         this.jumpRequested = false;
         if (this.canUseWindCharge() && mc.player.method_24828()) {
            this.pauseAuraIfNeeded();
            this.silentRotationActive = true;
            mc.player.method_36457(90.0F);
            this.performWindJump();
         } else {
            this.resetRequest();
         }
      }
   }

   @EventHandler
   private void onPostSync(EventPostSync event) {
      if (this.resumeAuraOnPostSync) {
         this.resumeAuraOnPostSync = false;
         this.resumeAura();
      }
   }

   private boolean canStart() {
      return this.pendingRestoreSlot == -1 && !this.silentRotationActive && this.canUseWindCharge() && mc.player.method_24828();
   }

   private boolean canUseWindCharge() {
      return !fullNullCheck() && mc.currentScreen == null && InventoryUtility.findItemInHotBar(Items.WIND_CHARGE).found();
   }

   private void performWindJump() {
      SearchInvResult windCharge = InventoryUtility.findItemInHotBar(Items.WIND_CHARGE);
      if (!windCharge.found()) {
         this.endSilentRotation();
      } else {
         int previousSlot = mc.player.method_31548().selectedSlot;
         boolean switched = false;

         try {
            InventoryUtility.switchTo(windCharge.slot());
            switched = true;
            this.performWindChargeAction();
         } finally {
            if (switched && previousSlot != windCharge.slot()) {
               this.pendingRestoreSlot = previousSlot;
            }

            this.endSilentRotation();
         }
      }
   }

   private void performWindChargeAction() {
      this.pressJumpInput();
      if (mc.interactionManager != null) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.player.method_6104(Hand.MAIN_HAND);
      }
   }

   private void endSilentRotation() {
      this.silentRotationActive = false;
      if (this.pendingRestoreSlot == -1) {
         this.resumeAuraOnPostSync = true;
      }
   }

   private void restorePendingSlot() {
      if (this.pendingRestoreSlot != -1 && !fullNullCheck()) {
         InventoryUtility.switchTo(this.pendingRestoreSlot);
         this.pendingRestoreSlot = -1;
         this.resumeAuraOnPostSync = false;
         this.resumeAura();
      }
   }

   private void pressJumpInput() {
      this.previousJumpPressed = mc.options.jumpKey.isPressed();
      mc.options.jumpKey.setPressed(true);
      mc.player.input.jumping = true;
      if (mc.player.method_24828()) {
         mc.player.method_6043();
      }

      this.restoreJumpInput = true;
   }

   private void restoreJumpInput() {
      if (this.restoreJumpInput) {
         mc.options.jumpKey.setPressed(this.previousJumpPressed);
         if (mc.player != null) {
            mc.player.input.jumping = this.previousJumpPressed;
         }

         this.restoreJumpInput = false;
      }
   }

   private void resetRequest() {
      this.jumpRequested = false;
      this.silentRotationActive = false;
      this.resumeAuraOnPostSync = false;
      this.resumeAura();
   }

   private void pauseAuraIfNeeded() {
      if (!this.auraPausedByWindJump) {
         if (this.shouldPauseAura()) {
            this.previousAuraPause = ModuleManager.aura.externalPause;
            ModuleManager.aura.externalPause = true;
            ModuleManager.moveFix.fixRotation = Float.NaN;
            this.auraPausedByWindJump = true;
         }
      }
   }

   private void resumeAura() {
      if (this.auraPausedByWindJump && ModuleManager.aura != null) {
         ModuleManager.aura.externalPause = this.previousAuraPause;
         this.auraPausedByWindJump = false;
         this.previousAuraPause = false;
      } else {
         this.auraPausedByWindJump = false;
         this.previousAuraPause = false;
      }
   }

   private boolean shouldPauseAura() {
      return ModuleManager.aura != null && ModuleManager.aura.isEnabled() && Aura.target != null && ModuleManager.aura.rotationMode.not(Aura.Mode.None);
   }
}
