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
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ChestStealerArgumentType implements ArgumentType<String> {
   private static final List<String> EXAMPLES = getRegistered().stream().limit(5L).toList();

   public static ChestStealerArgumentType create() {
      return new ChestStealerArgumentType();
   }

   public String parse(@NotNull StringReader reader) throws CommandSyntaxException {
      String blockName = reader.readString();
      if (!getRegistered().contains(blockName)) {
         throw new DynamicCommandExceptionType(name -> Text.literal("There is no such item!")).create(blockName);
      } else {
         return blockName;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(getRegistered(), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   @NotNull
   private static List<String> getRegistered() {
      List<String> result = new ArrayList<>();

      for (Block block : Registries.BLOCK) {
         result.add(block.getTranslationKey().replace("block.minecraft.", ""));
      }

      for (Item item : Registries.ITEM) {
         result.add(item.getTranslationKey().replace("item.minecraft.", ""));
      }

      return result;
   }
}
