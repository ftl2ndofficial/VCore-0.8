package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.io.File;
import java.util.Objects;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.CfgArgumentType;
import vcore.features.cmd.args.ModuleArgumentType;
import vcore.features.modules.Module;

public class CfgCommand extends Command {
   public CfgCommand() {
      super("cfg", "Manage local configs (save/load).");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(
         context -> {
            StringBuilder configs = new StringBuilder("Configs: ");

            for (String str : Objects.requireNonNull(Managers.CONFIG.getConfigList())) {
               configs.append("\n- " + (str.equals(Managers.CONFIG.getCurrentConfig().getName().replace(".vc", "")) ? Formatting.GREEN : ""))
                  .append(str)
                  .append(Formatting.RESET);
            }

            sendMessage(configs.toString());
            return 1;
         }
      );
      builder.then(literal("dir").executes(context -> {
         try {
            Util.getOperatingSystem().open(new File("Vcore/configs/").toURI());
         } catch (Exception e) {
            e.printStackTrace();
         }

         return 1;
      }));
      builder.then(literal("save").then(arg("name", StringArgumentType.word()).executes(context -> {
         Managers.CONFIG.save((String)context.getArgument("name", String.class));
         return 1;
      })));
      builder.then(
         literal("load")
            .then(((RequiredArgumentBuilder)arg("name", CfgArgumentType.create()).then(arg("module", ModuleArgumentType.create()).executes(context -> {
               Managers.CONFIG.loadModuleOnly((String)context.getArgument("name", String.class), (Module)context.getArgument("module", Module.class));
               return 1;
            }))).executes(context -> {
               Managers.CONFIG.load((String)context.getArgument("name", String.class));
               return 1;
            }))
      );
   }
}
