package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;

public class GarbageCleanerCommand extends Command {
   public GarbageCleanerCommand() {
      super("clearram", "Run GC to free RAM.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(context -> {
         sendMessage("Cleaning RAM..");
         System.gc();
         sendMessage("Successfully cleaned RAM!");
         return 1;
      });
   }
}
