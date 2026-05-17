package vcore.features.cmd.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class SettingArgumentType implements ArgumentType<String> {
   public static SettingArgumentType create() {
      return new SettingArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      return reader.readString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(getSettings((Module)context.getArgument("module", Module.class)), builder);
   }

   public static List<String> getSettings(Module module) {
      List<String> result = new ArrayList<>();

      for (Setting<?> setting : module.getSettings()) {
         result.add(setting.getName());
      }

      return result;
   }
}
