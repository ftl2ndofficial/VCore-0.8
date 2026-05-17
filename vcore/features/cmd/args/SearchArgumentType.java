package vcore.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class SearchArgumentType implements ArgumentType<String> {
   private static final List<String> EXAMPLES = getRegisteredBlocks().stream().limit(5L).toList();

   public static SearchArgumentType create() {
      return new SearchArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      String blockName = reader.readString();
      if (!getRegisteredBlocks().contains(blockName)) {
         throw new DynamicCommandExceptionType(name -> Text.literal("There is no such block!")).create(blockName);
      } else {
         return blockName;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(getRegisteredBlocks(), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static List<String> getRegisteredBlocks() {
      List<String> result = new ArrayList<>();

      for (Block block : Registries.BLOCK) {
         result.add(block.getTranslationKey().replace("block.minecraft.", ""));
      }

      return result;
   }
}
