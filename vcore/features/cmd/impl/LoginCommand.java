package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;
import vcore.utility.AccountUtility;

public class LoginCommand extends Command {
   public LoginCommand() {
      super("login", "Switch offline session to a nickname.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(arg("name", StringArgumentType.word()).executes(context -> {
         this.login((String)context.getArgument("name", String.class));
         sendMessage("Switched account to: " + mc.getSession().getUsername());
         return 1;
      }));
      builder.executes(context -> {
         sendMessage("Usage: .login <nickname>");
         return 1;
      });
   }

   public void login(String name) {
      try {
         AccountUtility.login(name);
      } catch (Exception exception) {
         sendMessage("Incorrect username! " + exception);
      }
   }
}
