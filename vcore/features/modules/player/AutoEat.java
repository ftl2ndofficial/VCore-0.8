package vcore.features.modules.player;

import baritone.api.BaritoneAPI;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import vcore.Vcore;
import vcore.features.modules.Module;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;

public class AutoEat extends Module {
   public final Setting<Integer> hunger = new Setting<>("Hunger", 8, 0, 20);
   public final Setting<Boolean> gapple = new Setting<>("Gapple", false);
   public final Setting<Boolean> chorus = new Setting<>("Chorus", false);
   public final Setting<Boolean> rottenFlesh = new Setting<>("RottenFlesh", false);
   public final Setting<Boolean> spiderEye = new Setting<>("SpiderEye", false);
   public final Setting<Boolean> pufferfish = new Setting<>("Pufferfish", false);
   public final Setting<Boolean> swapBack = new Setting<>("SwapBack", true);
   public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", true, v -> Vcore.baritone);
   private boolean eating;
   private int prevSlot;

   public AutoEat() {
      super("AutoEat", "Auto eats food when hungry.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (mc.player.method_7344().getFoodLevel() <= this.hunger.getValue()) {
         boolean found;
         if (!this.isHandGood(Hand.MAIN_HAND) && !this.isHandGood(Hand.OFF_HAND)) {
            found = this.switchToFood();
         } else {
            found = true;
         }

         if (!found) {
            if (this.eating) {
               this.stopEating();
            }

            return;
         }

         this.startEating();
      } else if (this.eating) {
         this.stopEating();
      }
   }

   public void startEating() {
      this.eating = true;
      if (mc.currentScreen != null && !mc.player.method_6115()) {
         ((IMinecraftClient)mc).idoItemUse();
      } else {
         if (this.pauseBaritone.getValue() && Vcore.baritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
         }

         mc.options.useKey.setPressed(true);
      }
   }

   public void stopEating() {
      this.eating = false;
      mc.options.useKey.setPressed(false);
      if (this.swapBack.getValue()) {
         mc.player.method_31548().selectedSlot = this.prevSlot;
      }

      if (this.pauseBaritone.getValue() && Vcore.baritone) {
         BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
      }
   }

   public boolean switchToFood() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.method_31548().method_5438(i);
         if (stack.method_57353().contains(DataComponentTypes.FOOD)
            && (this.gapple.getValue() || stack.getItem() != Items.GOLDEN_APPLE && stack.getItem() != Items.ENCHANTED_GOLDEN_APPLE)
            && (this.chorus.getValue() || stack.getItem() != Items.CHORUS_FRUIT)
            && (this.rottenFlesh.getValue() || stack.getItem() != Items.ROTTEN_FLESH)
            && (this.spiderEye.getValue() || stack.getItem() != Items.SPIDER_EYE)
            && (this.pufferfish.getValue() || stack.getItem() != Items.PUFFERFISH)) {
            this.prevSlot = mc.player.method_31548().selectedSlot;
            mc.player.method_31548().selectedSlot = i;
            this.sendPacket(new UpdateSelectedSlotC2SPacket(i));
            return true;
         }
      }

      return false;
   }

   private boolean isHandGood(Hand hand) {
      ItemStack stack = hand == Hand.MAIN_HAND ? mc.player.method_6047() : mc.player.method_6079();
      Item item = stack.getItem();
      return stack.method_57353().contains(DataComponentTypes.FOOD)
         && (this.gapple.getValue() || item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE)
         && (this.chorus.getValue() || item != Items.CHORUS_FRUIT)
         && (this.rottenFlesh.getValue() || item != Items.ROTTEN_FLESH)
         && (this.spiderEye.getValue() || item != Items.SPIDER_EYE)
         && (this.pufferfish.getValue() || item != Items.PUFFERFISH);
   }
}
