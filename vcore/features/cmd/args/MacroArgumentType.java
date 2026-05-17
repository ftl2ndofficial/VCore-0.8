package vcore.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import vcore.core.Managers;
import vcore.core.manager.client.MacroManager;

public class MacroArgumentType implements ArgumentType<MacroManager.Macro> {
   private static final Collection<String> EXAMPLES = Managers.MACRO.getMacros().stream().map(MacroManager.Macro::getName).limit(5L).toList();

   public static MacroArgumentType create() {
      return new MacroArgumentType();
   }

   public MacroManager.Macro parse(StringReader reader) throws CommandSyntaxException {
      MacroManager.Macro macro = Managers.MACRO.getMacroByName(reader.readString());
      if (macro == null) {
         throw new DynamicCommandExceptionType(name -> Text.literal("Macro with name " + name + " does not exists(")).create(reader.readString());
      } else {
         return macro;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(Managers.MACRO.getMacros().stream().map(MacroManager.Macro::getName), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
