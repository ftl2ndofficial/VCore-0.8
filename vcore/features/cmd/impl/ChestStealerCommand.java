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

public class ChestStealerCommand extends Command {
   public ChestStealerCommand() {
      super("cheststealer", "Manage ChestStealer whitelist.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("reset").executes(context -> {
         ModuleManager.chestStealer.items.getValue().getItemsById().clear();
         sendMessage("ChestStealer got reset.");
         return 1;
      }));
      builder.then(literal("add").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("item", String.class);
         String result = getRegistered(blockName);
         if (result != null) {
            ModuleManager.chestStealer.items.getValue().getItemsById().add(result);
            sendMessage(Formatting.GREEN + blockName + " added to ChestStealer");
         } else {
            sendMessage(Formatting.RED + "There is no such item!");
         }

         return 1;
      })));
      builder.then(literal("del").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("item", String.class);
         String result = getRegistered(blockName);
         if (result != null) {
            ModuleManager.chestStealer.items.getValue().getItemsById().remove(result);
            sendMessage(Formatting.GREEN + blockName + " removed from ChestStealer");
         } else {
            sendMessage(Formatting.RED + "There is no such item!");
         }

         return 1;
      })));
      builder.executes(context -> {
         if (ModuleManager.chestStealer.items.getValue().getItemsById().isEmpty()) {
            sendMessage("ChestStealer list empty");
         } else {
            StringBuilder f = new StringBuilder("ChestStealer list: ");

            for (String name : ModuleManager.chestStealer.items.getValue().getItemsById()) {
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
