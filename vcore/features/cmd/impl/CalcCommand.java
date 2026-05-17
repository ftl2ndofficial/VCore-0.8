package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;

public class CalcCommand extends Command {
   public CalcCommand() {
      super("calc", "Evaluate simple math expressions.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(arg("count", StringArgumentType.string()).executes(context -> {
         String expression = (String)context.getArgument("count", String.class);

         try {
            sendMessage(evaluateExpression(expression));
            return 1;
         } catch (Exception e) {
            sendMessage("Try use operators: + - m(*) d(/)");
            return -1;
         }
      }));
   }

   public static String evaluateExpression(String expression) {
      char operator = 0;
      int operand1 = 0;
      int operand2 = 0;

      for (int i = 0; i < expression.length(); i++) {
         char ch = expression.charAt(i);
         if (ch == '+' || ch == '-' || ch == 'm' || ch == 'd') {
            operator = ch;
            operand1 = Integer.parseInt(expression.substring(0, i));
            operand2 = Integer.parseInt(expression.substring(i + 1));
            break;
         }
      }
      return switch (operator) {
         case '+' -> String.valueOf(operand1 + operand2);
         case '-' -> String.valueOf(operand1 - operand2);
         case 'd' -> String.valueOf(operand1 / operand2);
         case 'm' -> String.valueOf(operand1 * operand2);
         default -> throw new IllegalArgumentException("Wrong");
      };
   }
}
