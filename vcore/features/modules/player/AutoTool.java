package vcore.features.modules.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.AirBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class AutoTool extends Module {
   public static Setting<Boolean> swapBack = new Setting<>("SwapBack", true);
   public static Setting<Boolean> saveItem = new Setting<>("SaveItem", true);
   public static Setting<Boolean> silent = new Setting<>("Silent", false);
   public static Setting<Boolean> echestSilk = new Setting<>("EchestSilk", true);
   public static int itemIndex;
   private boolean swap;
   private long swapDelay;
   private final List<Integer> lastItem = new ArrayList<>();

   public AutoTool() {
      super("AutoTool", "Auto switches to best tool.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (mc.crosshairTarget instanceof BlockHitResult result) {
         BlockPos pos = result.getBlockPos();
         if (!mc.world.method_8320(pos).method_26215()) {
            if (getTool(pos) != -1 && mc.options.attackKey.isPressed()) {
               this.lastItem.add(mc.player.method_31548().selectedSlot);
               if (silent.getValue()) {
                  mc.player.networkHandler.method_52787(new UpdateSelectedSlotC2SPacket(getTool(pos)));
               } else {
                  mc.player.method_31548().selectedSlot = getTool(pos);
               }

               itemIndex = getTool(pos);
               this.swap = true;
               this.swapDelay = System.currentTimeMillis();
            } else if (this.swap && !this.lastItem.isEmpty() && System.currentTimeMillis() >= this.swapDelay + 300L && swapBack.getValue()) {
               if (silent.getValue()) {
                  mc.player.networkHandler.method_52787(new UpdateSelectedSlotC2SPacket(this.lastItem.get(0)));
               } else {
                  mc.player.method_31548().selectedSlot = this.lastItem.get(0);
               }

               itemIndex = this.lastItem.get(0);
               this.lastItem.clear();
               this.swap = false;
            }
         }
      }
   }

   public static int getTool(BlockPos pos) {
      int index = -1;
      float CurrentFastest = 1.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.method_31548().method_5438(i);
         if (stack != ItemStack.EMPTY
            && (mc.player.method_31548().method_5438(i).getMaxDamage() - mc.player.method_31548().method_5438(i).getDamage() > 10 || !saveItem.getValue())) {
            float digSpeed = EnchantmentHelper.getLevel(
               (RegistryEntry)mc.world.method_30349().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get(), stack
            );
            float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.method_8320(pos));
            if (mc.world.method_8320(pos).method_26204() instanceof AirBlock) {
               return -1;
            }

            if (mc.world.method_8320(pos).method_26204() instanceof EnderChestBlock && echestSilk.getValue()) {
               if (EnchantmentHelper.getLevel(
                        (RegistryEntry)mc.world.method_30349().get(Enchantments.SILK_TOUCH.getRegistryRef()).getEntry(Enchantments.SILK_TOUCH).get(), stack
                     )
                     > 0
                  && digSpeed + destroySpeed > CurrentFastest) {
                  CurrentFastest = digSpeed + destroySpeed;
                  index = i;
               }
            } else if (digSpeed + destroySpeed > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }
      }

      return index;
   }
}
