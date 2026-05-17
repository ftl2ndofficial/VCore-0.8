package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;

public class HorseSpeedCommand extends Command {
   public HorseSpeedCommand() {
      super("horsespeed", "Show current horse speed.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(context -> {
         if (mc.player.method_5854() != null && mc.player.method_5854() instanceof HorseEntity horse) {
            if (!horse.method_6725()) {
               sendMessage(Formatting.RED + "You don't have a saddle!");
               return 1;
            }

            float speed = horse.field_6250 * 43.17F;
            float ratio = speed / 14.512F;
            String verbose = "";
            if (ratio < 0.3) {
               verbose = "Your horse is terrible :(";
            }

            if (ratio > 0.3 && ratio < 0.6) {
               verbose = "Your horse is normal";
            }

            if (ratio > 0.6) {
               verbose = "Your horse is good :)";
            }

            if (ratio > 0.9) {
               verbose = "Your horse is very good :)";
            }

            sendMessage(Formatting.GREEN + "Horse speed: " + speed + " out of 14.512. " + verbose);
         } else {
            sendMessage(Formatting.RED + "You don't have a horse!");
         }

         return 1;
      });
   }
}
