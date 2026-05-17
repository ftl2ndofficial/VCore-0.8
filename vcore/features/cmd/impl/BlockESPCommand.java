package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import vcore.core.manager.client.ModuleManager;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.SearchArgumentType;

public class BlockESPCommand extends Command {
   public BlockESPCommand() {
      super("blockesp", "Manage BlockESP highlight list.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("reset").executes(context -> {
         ModuleManager.blockESP.selectedBlocks.getValue().clear();
         sendMessage("BlockESP got reset.");
         mc.worldRenderer.reload();
         return 1;
      }));
      builder.then(literal("add").then(arg("block", SearchArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("block", String.class);
         Block result = getRegisteredBlock(blockName);
         if (result != null) {
            ModuleManager.blockESP.selectedBlocks.getValue().add(result);
            sendMessage(Formatting.GREEN + blockName + " added to BlockESP");
         } else {
            sendMessage(Formatting.RED + "There is no such block!");
         }

         mc.worldRenderer.reload();
         return 1;
      })));
      builder.then(literal("del").then(arg("block", SearchArgumentType.create()).executes(context -> {
         String blockName = (String)context.getArgument("block", String.class);
         Block result = getRegisteredBlock(blockName);
         if (result != null) {
            ModuleManager.blockESP.selectedBlocks.getValue().remove(result);
            sendMessage(Formatting.GREEN + blockName + " removed from BlockESP");
         } else {
            sendMessage(Formatting.RED + "There is no such block!");
         }

         mc.worldRenderer.reload();
         return 1;
      })));
      builder.executes(context -> {
         if (ModuleManager.blockESP.selectedBlocks.getValue().getItemsById().isEmpty()) {
            sendMessage("BlockESP list empty");
         } else {
            StringBuilder f = new StringBuilder("BlockESP list: ");

            for (String name : ModuleManager.blockESP.selectedBlocks.getValue().getItemsById()) {
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

   public static Block getRegisteredBlock(String blockName) {
      for (Block block : Registries.BLOCK) {
         if (block.getTranslationKey().replace("block.minecraft.", "").equalsIgnoreCase(blockName.replace("block.minecraft.", ""))) {
            return block;
         }
      }

      return null;
   }
}
