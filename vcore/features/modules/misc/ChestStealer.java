package vcore.features.modules.misc;

import java.util.ArrayList;
import java.util.Random;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.features.modules.render.StorageEsp;
import vcore.setting.Setting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;

public class ChestStealer extends Module {
   public final Setting<ItemSelectSetting> items = new Setting<>("Items", new ItemSelectSetting(new ArrayList<>()));
   private final Setting<Integer> delay = new Setting<>("Delay", 50, 0, 1000);
   private final Setting<Boolean> random = new Setting<>("Random", false);
   private final Setting<Boolean> close = new Setting<>("Close", false);
   private final Setting<Boolean> autoMyst = new Setting<>("AutoMyst", false);
   private final Setting<ChestStealer.Sort> sort = new Setting<>("Sort", ChestStealer.Sort.None);
   private final Timer autoMystDelay = new Timer();
   private final Timer timer = new Timer();
   private final Random rnd = new Random();

   public ChestStealer() {
      super("ChestStealer", "Steals items from chests.", Module.Category.MISC);
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (mc.player.field_7512 instanceof GenericContainerScreenHandler chest) {
         for (int i = 0; i < chest.getInventory().size(); i++) {
            Slot slot = chest.method_7611(i);
            if (slot.hasStack()
               && this.isAllowed(slot.getStack())
               && this.timer
                  .every(this.delay.getValue() + (this.random.getValue() && this.delay.getValue() != 0 ? this.rnd.nextInt(this.delay.getValue()) : 0))
               && !mc.currentScreen.getTitle().getString().toLowerCase().contains("auction")
               && !mc.currentScreen.getTitle().getString().toLowerCase().contains("purchase")
               && !mc.currentScreen.getTitle().getString().contains("Аукцион")
               && !mc.currentScreen.getTitle().getString().contains("покупки")) {
               mc.interactionManager.clickSlot(mc.player.field_7512.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
               this.autoMystDelay.reset();
            }
         }

         if (this.isContainerEmpty(chest) && this.close.getValue()) {
            mc.player.method_7346();
         }
      }
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (this.autoMyst.getValue() && mc.currentScreen == null && this.autoMystDelay.passedMs(3000L)) {
         for (BlockEntity be : StorageEsp.getBlockEntities()) {
            if (be instanceof EnderChestBlockEntity && !(mc.player.method_5707(be.getPos().toCenterPos()) > 39.0)) {
               mc.interactionManager
                  .interactBlock(
                     mc.player,
                     Hand.MAIN_HAND,
                     new BlockHitResult(
                        be.getPos().toCenterPos().add(MathUtility.random(-0.4, 0.4), 0.375, MathUtility.random(-0.4, 0.4)), Direction.UP, be.getPos(), false
                     )
                  );
               mc.player.method_6104(Hand.MAIN_HAND);
               break;
            }
         }
      }
   }

   private boolean isAllowed(ItemStack stack) {
      boolean allowed = this.items.getValue().contains(stack.getItem().getTranslationKey().replace("block.minecraft.", "").replace("item.minecraft.", ""));

      return switch ((ChestStealer.Sort)this.sort.getValue()) {
         case None -> true;
         case WhiteList -> allowed;
         default -> !allowed;
      };
   }

   private boolean isContainerEmpty(GenericContainerScreenHandler container) {
      for (int i = 0; i < (container.getInventory().size() == 90 ? 54 : 27); i++) {
         if (container.method_7611(i).hasStack()) {
            return false;
         }
      }

      return true;
   }

   private enum Sort {
      None,
      WhiteList,
      BlackList;
   }
}
