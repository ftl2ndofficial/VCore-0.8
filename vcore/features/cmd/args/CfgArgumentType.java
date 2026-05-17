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

public class CfgArgumentType implements ArgumentType<String> {
   private static final Collection<String> EXAMPLES = Managers.CONFIG.getConfigList().stream().limit(5L).toList();

   public static CfgArgumentType create() {
      return new CfgArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      String config = reader.readString();
      if (!Managers.CONFIG.getConfigList().contains(config)) {
         throw new DynamicCommandExceptionType(name -> Text.literal("Config " + name + " does not exist :(")).create(config);
      } else {
         return config;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(Managers.CONFIG.getConfigList(), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
