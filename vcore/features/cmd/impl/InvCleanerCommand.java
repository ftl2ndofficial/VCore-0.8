package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.client.ModuleManager;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.ChestStealerArgumentType;

public class InvCleanerCommand extends Command {
   public InvCleanerCommand() {
      super("invcleaner", "Manage InvCleaner whitelist.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("reset").executes(context -> {
         ModuleManager.inventoryCleaner.items.getValue().clear();
         sendMessage("InvCleaner got reset.");
         return 1;
      }));
      builder.then(literal("add").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("item", String.class);
         String result = getRegistered(blockName);
         if (result != null) {
            ModuleManager.inventoryCleaner.items.getValue().add(result);
            sendMessage(Formatting.GREEN + blockName + " added to InvCleaner");
         } else {
            sendMessage(Formatting.RED + "There is no such item!");
         }

         return 1;
      })));
      builder.then(literal("del").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("item", String.class);
         String result = getRegistered(blockName);
         if (result != null) {
            ModuleManager.inventoryCleaner.items.getValue().remove(result);
            sendMessage(Formatting.GREEN + blockName + " removed from InvCleaner");
         } else {
            sendMessage(Formatting.RED + "There is no such item!");
         }

         return 1;
      })));
      builder.executes(context -> {
         if (ModuleManager.inventoryCleaner.items.getValue().getItemsById().isEmpty()) {
            sendMessage("InvCleaner list empty");
         } else {
            StringBuilder f = new StringBuilder("InvCleaner list: ");

            for (String name : ModuleManager.inventoryCleaner.items.getValue().getItemsById()) {
               try {
                  f.append(name).append(", ");
               } catch (Exception var5) {
               }
            }

            sendMessage(f.toString());
         }

         return 1;
      });
   }

   public static String getRegistered(String Name) {
      for (Block block : Registries.BLOCK) {
         if (block.getTranslationKey().replace("block.minecraft.", "").equalsIgnoreCase(Name)) {
            return block.getTranslationKey().replace("block.minecraft.", "");
         }
      }

      for (Item item : Registries.ITEM) {
         if (item.getTranslationKey().replace("item.minecraft.", "").equalsIgnoreCase(Name)) {
            return item.getTranslationKey().replace("item.minecraft.", "");
         }
      }

      return null;
   }
}
