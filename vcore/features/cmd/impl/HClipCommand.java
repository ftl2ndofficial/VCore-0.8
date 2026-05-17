package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;

public class HClipCommand extends Command {
   public HClipCommand() {
      super("hclip", "Clip horizontally in facing direction.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(
         literal("s")
            .executes(
               context -> {
                  double x = -(MathHelper.sin(mc.player.method_36454() * (float) (Math.PI / 180.0)) * 0.8);
                  double z = MathHelper.cos(mc.player.method_36454() * (float) (Math.PI / 180.0)) * 0.8;

                  for (int i = 0; i < 10; i++) {
                     mc.player
                        .networkHandler
                        .method_52787(new PositionAndOnGround(mc.player.method_23317() + x, mc.player.method_23318(), mc.player.method_23321() + z, false));
                  }

                  mc.player.method_5814(mc.player.method_23317() + x, mc.player.method_23318(), mc.player.method_23321() + z);
                  return 1;
               }
            )
      );
      builder.then(
         arg("count", DoubleArgumentType.doubleArg())
            .executes(
               context -> {
                  double speed = (Double)context.getArgument("count", Double.class);

                  try {
                     sendMessage(Formatting.GREEN + "Clipping by " + speed + " blocks.");
                     mc.player
                        .method_5814(
                           mc.player.method_23317() - MathHelper.sin(mc.player.method_36454() * (float) (Math.PI / 180.0)) * speed,
                           mc.player.method_23318(),
                           mc.player.method_23321() + MathHelper.cos(mc.player.method_36454() * (float) (Math.PI / 180.0)) * speed
                        );
                  } catch (Exception var4) {
                  }

                  return 1;
               }
            )
      );
      builder.executes(context -> {
         sendMessage("Try .hclip <number>, .hclip s");
         return 1;
      });
   }
}
