package vcore.gui.misc;

import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import vcore.features.modules.render.Tooltips;

public class PeekScreen extends ShulkerBoxScreen {
   private static final ItemStack[] ITEMS = new ItemStack[27];

   public PeekScreen(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title, Block block) {
      super(handler, inventory, title);
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (button == 2 && this.field_2787 != null && !this.field_2787.getStack().isEmpty() && this.field_22787.player.field_7498.method_34255().isEmpty()) {
         ItemStack itemStack = this.field_2787.getStack();
         if (Tooltips.hasItems(itemStack) && Tooltips.middleClickOpen.getValue()) {
            Arrays.fill(ITEMS, ItemStack.EMPTY);
            ContainerComponent nbt = (ContainerComponent)itemStack.method_57824(DataComponentTypes.CONTAINER);
            if (nbt != null) {
               List<ItemStack> list = nbt.stream().toList();

               for (int i = 0; i < list.size(); i++) {
                  ITEMS[i] = list.get(i);
               }
            }

            this.field_22787
               .setScreen(
                  new PeekScreen(
                     new ShulkerBoxScreenHandler(0, this.field_22787.player.method_31548(), new SimpleInventory(ITEMS)),
                     this.field_22787.player.method_31548(),
                     this.field_2787.getStack().getName(),
                     ((BlockItem)this.field_2787.getStack().getItem()).getBlock()
                  )
               );
            return true;
         }
      }

      return false;
   }

   public boolean method_25406(double mouseX, double mouseY, int button) {
      return false;
   }
}
