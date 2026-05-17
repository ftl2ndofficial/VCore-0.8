package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.Field;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import vcore.core.Managers;
import vcore.core.manager.client.MacroManager;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.MacroArgumentType;

public class MacroCommand extends Command {
   public MacroCommand() {
      super("macro", "Manage chat/command macros.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(
         literal("list")
            .executes(
               context -> {
                  sendMessage("Macros:");
                  sendMessage(" ");
                  Managers.MACRO
                     .getMacros()
                     .forEach(
                        macro -> sendMessage(
                           macro.getName() + (macro.getBind() != -1 ? " [" + this.toString(macro.getBind()) + "]" : "") + " {" + macro.getText() + "}"
                        )
                     );
                  return 1;
               }
            )
      );
      builder.then(literal("remove").then(arg("macro", MacroArgumentType.create()).executes(context -> {
         MacroManager.Macro macro = (MacroManager.Macro)context.getArgument("macro", MacroManager.Macro.class);
         if (macro == null) {
            sendMessage("Wrong macro name!");
            return 1;
         } else {
            Managers.MACRO.removeMacro(macro);
            sendMessage("Removed macro " + macro.getName());
            return 1;
         }
      })));
      builder.then(
         literal("add")
            .then(
               arg("name", StringArgumentType.word())
                  .then(arg("bind", StringArgumentType.word()).then(arg("args", StringArgumentType.greedyString()).executes(context -> {
                     String name = (String)context.getArgument("name", String.class);
                     String bind = ((String)context.getArgument("bind", String.class)).toUpperCase();
                     String args = (String)context.getArgument("args", String.class);
                     if (InputUtil.fromTranslationKey("key.keyboard." + bind.toLowerCase()).getCode() == -1) {
                        sendMessage("Wrong bind!");
                        return 1;
                     } else {
                        MacroManager.Macro macro = new MacroManager.Macro(
                           name, args, InputUtil.fromTranslationKey("key.keyboard." + bind.toLowerCase()).getCode()
                        );
                        MacroManager.addMacro(macro);
                        sendMessage("Added macro " + name + " to " + this.toString(macro.getBind()));
                        return 1;
                     }
                  })))
            )
      );
      builder.executes(context -> {
         sendMessage(this.usage());
         return 1;
      });
   }

   public String toString(int key) {
      String kn = key > 0 ? GLFW.glfwGetKeyName(key, GLFW.glfwGetKeyScancode(key)) : "None";
      if (kn == null) {
         try {
            for (Field declaredField : GLFW.class.getDeclaredFields()) {
               if (declaredField.getName().startsWith("GLFW_KEY_")) {
                  int a = (Integer)declaredField.get(null);
                  if (a == key) {
                     String nb = declaredField.getName().substring("GLFW_KEY_".length());
                     kn = nb.substring(0, 1).toUpperCase() + nb.substring(1).toLowerCase();
                  }
               }
            }
         } catch (Exception ignored) {
            kn = "unknown." + key;
         }
      }

      return key == -1 ? "None" : (kn + "").toUpperCase();
   }

   String usage() {
      return "macro add/remove/list (macro add name key text), (macro remove name)";
   }
}
