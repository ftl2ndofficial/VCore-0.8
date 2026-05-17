package vcore.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import vcore.core.Managers;

public class FriendArgumentType implements ArgumentType<String> {
   private static final List<String> EXAMPLES = Managers.FRIEND.getFriends().stream().limit(5L).toList();

   public static FriendArgumentType create() {
      return new FriendArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      String friend = reader.readString();
      if (!Managers.FRIEND.isFriend(friend)) {
         throw new DynamicCommandExceptionType(name -> Text.literal("Friend with name " + name + " does not exist :(")).create(friend);
      } else {
         return friend;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(Managers.FRIEND.getFriends(), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
