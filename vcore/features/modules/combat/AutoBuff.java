package vcore.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAfterRotate;
import vcore.events.impl.EventPostSync;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.Timer;

public final class AutoBuff extends Module {
   private final Setting<Boolean> strength = new Setting<>("Strength", true);
   private final Setting<Boolean> speed = new Setting<>("Speed", true);
   private final Setting<Boolean> fire = new Setting<>("FireResistance", true);
   private final Setting<BooleanSettingGroup> heal = new Setting<>("InstantHealing", new BooleanSettingGroup(false));
   private final Setting<Integer> healthH = new Setting<>("Health", 8, 0, 20).addToGroup(this.heal);
   private final Setting<BooleanSettingGroup> regen = new Setting<>("Regeneration", new BooleanSettingGroup(false));
   private final Setting<AutoBuff.TriggerOn> triggerOn = new Setting<>("Trigger", AutoBuff.TriggerOn.LackOfRegen).addToGroup(this.regen);
   private final Setting<Integer> healthR = new Setting<>("HP", 8, 0, 20, v -> this.triggerOn.is(AutoBuff.TriggerOn.Health)).addToGroup(this.regen);
   private final Setting<Boolean> onDaGround = new Setting<>("OnlyOnGround", true);
   private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);
   public Timer timer = new Timer();
   private boolean spoofed = false;

   public AutoBuff() {
      super("AutoBuff", "Auto throws potions.", Module.Category.COMBAT);
   }

   public static int getPotionSlot(AutoBuff.Potions potion) {
      for (int i = 0; i < 9; i++) {
         if (isStackPotion(mc.player.method_31548().method_5438(i), potion)) {
            return i;
         }
      }

      return -1;
   }

   public static boolean isPotionOnHotBar(AutoBuff.Potions potions) {
      return getPotionSlot(potions) != -1;
   }

   public static boolean isStackPotion(ItemStack stack, AutoBuff.Potions potion) {
      if (stack == null) {
         return false;
      }

      if (stack.getItem() instanceof SplashPotionItem) {
         PotionContentsComponent potionContentsComponent = (PotionContentsComponent)stack.method_57825(
            DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
         );
         RegistryEntry<StatusEffect> id = null;
         switch (potion) {
            case STRENGTH:
               id = StatusEffects.STRENGTH;
               break;
            case SPEED:
               id = StatusEffects.SPEED;
               break;
            case FIRERES:
               id = StatusEffects.FIRE_RESISTANCE;
               break;
            case HEAL:
               id = StatusEffects.INSTANT_HEALTH;
               break;
            case REGEN:
               id = StatusEffects.REGENERATION;
         }

         for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
            if (effect.getEffectType() == id) {
               return true;
            }
         }
      }

      return false;
   }

   @EventHandler
   public void onPostRotationSet(EventAfterRotate event) {
      if (Aura.target == null || !(mc.player.method_7261(1.0F) > 0.5F)) {
         if (mc.player.field_6012 > 80 && this.shouldThrow()) {
            mc.player.method_36457(90.0F);
            this.spoofed = true;
         }
      }
   }

   private boolean shouldThrow() {
      return !mc.player.method_6059(StatusEffects.SPEED) && isPotionOnHotBar(AutoBuff.Potions.SPEED) && this.speed.getValue()
         || !mc.player.method_6059(StatusEffects.STRENGTH) && isPotionOnHotBar(AutoBuff.Potions.STRENGTH) && this.strength.getValue()
         || !mc.player.method_6059(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(AutoBuff.Potions.FIRERES) && this.fire.getValue()
         || mc.player.method_6032() + mc.player.method_6067() < this.healthH.getValue().intValue()
            && isPotionOnHotBar(AutoBuff.Potions.HEAL)
            && this.heal.getValue().isEnabled()
         || !mc.player.method_6059(StatusEffects.REGENERATION)
            && this.triggerOn.is(AutoBuff.TriggerOn.LackOfRegen)
            && isPotionOnHotBar(AutoBuff.Potions.REGEN)
            && this.regen.getValue().isEnabled()
         || mc.player.method_6032() + mc.player.method_6067() < this.healthR.getValue().intValue()
            && this.triggerOn.is(AutoBuff.TriggerOn.Health)
            && isPotionOnHotBar(AutoBuff.Potions.REGEN)
            && this.regen.getValue().isEnabled();
   }

   @EventHandler
   public void onPostSync(EventPostSync e) {
      if (Aura.target == null || !(mc.player.method_7261(1.0F) > 0.5F)) {
         if (!this.onDaGround.getValue() || mc.player.method_24828()) {
            if (mc.player.field_6012 > 80 && this.shouldThrow() && this.timer.passedMs(1000L) && this.spoofed) {
               if (!mc.player.method_6059(StatusEffects.SPEED) && isPotionOnHotBar(AutoBuff.Potions.SPEED) && this.speed.getValue()) {
                  this.throwPotion(AutoBuff.Potions.SPEED);
               }

               if (!mc.player.method_6059(StatusEffects.STRENGTH) && isPotionOnHotBar(AutoBuff.Potions.STRENGTH) && this.strength.getValue()) {
                  this.throwPotion(AutoBuff.Potions.STRENGTH);
               }

               if (!mc.player.method_6059(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(AutoBuff.Potions.FIRERES) && this.fire.getValue()) {
                  this.throwPotion(AutoBuff.Potions.FIRERES);
               }

               if (mc.player.method_6032() + mc.player.method_6067() < this.healthH.getValue().intValue()
                  && this.heal.getValue().isEnabled()
                  && isPotionOnHotBar(AutoBuff.Potions.HEAL)) {
                  this.throwPotion(AutoBuff.Potions.HEAL);
               }

               if ((
                     !mc.player.method_6059(StatusEffects.REGENERATION) && this.triggerOn.is(AutoBuff.TriggerOn.LackOfRegen)
                        || mc.player.method_6032() + mc.player.method_6067() < this.healthR.getValue().intValue()
                           && this.triggerOn.is(AutoBuff.TriggerOn.Health)
                  )
                  && isPotionOnHotBar(AutoBuff.Potions.REGEN)
                  && this.regen.getValue().isEnabled()) {
                  this.throwPotion(AutoBuff.Potions.REGEN);
               }

               this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.method_31548().selectedSlot));
               this.timer.reset();
               this.spoofed = false;
            }
         }
      }
   }

   public void throwPotion(AutoBuff.Potions potion) {
      if (this.pauseAura.getValue()) {
         ModuleManager.aura.pause();
      }

      this.sendPacket(new UpdateSelectedSlotC2SPacket(getPotionSlot(potion)));
      this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.method_36454(), mc.player.method_36455()));
   }

   public enum Potions {
      STRENGTH,
      SPEED,
      FIRERES,
      HEAL,
      REGEN;
   }

   public enum TriggerOn {
      LackOfRegen,
      Health;
   }
}
