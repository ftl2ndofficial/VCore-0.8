package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Objects;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.ModuleArgumentType;
import vcore.features.hud.impl.KeyBinds;
import vcore.features.modules.Module;
import vcore.setting.impl.Bind;

public class BindCommand extends Command {
   public BindCommand() {
      super("bind", "Bind a module to a key.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(arg("module", ModuleArgumentType.create()).then(arg("key", StringArgumentType.word()).executes(context -> {
         Module module = (Module)context.getArgument("module", Module.class);
         String stringKey = (String)context.getArgument("key", String.class);
         if (stringKey == null) {
            sendMessage(module.getName() + " is bound to " + Formatting.GRAY + module.getBind().getBind());
            return 1;
         }

         int key;
         if (!stringKey.equalsIgnoreCase("none") && !stringKey.equalsIgnoreCase("null")) {
            try {
               key = InputUtil.fromTranslationKey("key.keyboard." + stringKey.toLowerCase()).getCode();
            } catch (NumberFormatException e) {
               sendMessage("There is no such button");
               return 1;
            }
         } else {
            key = -1;
         }

         if (key == 0) {
            sendMessage("Unknown key '" + stringKey + "'!");
            return 1;
         } else {
            module.setBind(key, !stringKey.equals("M") && stringKey.contains("M"), false);
            sendMessage("Bind for " + Formatting.GREEN + module.getName() + Formatting.WHITE + " set to " + Formatting.GRAY + stringKey.toUpperCase());
            return 1;
         }
      })));
      builder.then(
         literal("list")
            .executes(
               context -> {
                  StringBuilder binds = new StringBuilder("Binds: ");

                  for (Module feature : Managers.MODULE.modules) {
                     if (!Objects.equals(feature.getBind().getBind(), "None")) {
                        binds.append("\n- ")
                           .append(feature.getName())
                           .append(" -> ")
                           .append(KeyBinds.getShortKeyName(feature))
                           .append(feature.getBind().isHold() ? "[hold]" : "");
                     }
                  }

                  sendMessage(binds.toString());
                  return 1;
               }
            )
      );
      builder.then(literal("clear").executes(context -> {
         for (Module mod : Managers.MODULE.modules) {
            mod.setBind(new Bind(-1, false, false));
         }

         return 1;
      }));
      builder.then(literal("reset").executes(context -> {
         for (Module mod : Managers.MODULE.modules) {
            mod.setBind(new Bind(-1, false, false));
         }

         sendMessage("Done!");
         return 1;
      }));
   }
}
