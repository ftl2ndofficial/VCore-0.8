package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;
import vcore.features.cmd.args.FriendArgumentType;
import vcore.features.cmd.args.PlayerArgumentType;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "Manage the friend list.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("reset").executes(context -> {
         Managers.FRIEND.clear();
         sendMessage("Friends got reset.");
         return 1;
      }));
      builder.then(literal("add").then(arg("player", PlayerArgumentType.create()).executes(context -> {
         PlayerListEntry player = (PlayerListEntry)context.getArgument("player", PlayerListEntry.class);
         Managers.FRIEND.addFriend(player.getProfile().getName());
         sendMessage(player.getProfile().getName() + " has been friended");
         return 1;
      })));
      builder.then(literal("remove").then(arg("player", FriendArgumentType.create()).executes(context -> {
         String nickname = (String)context.getArgument("player", String.class);
         Managers.FRIEND.removeFriend(nickname);
         sendMessage(nickname + " has been unfriended");
         return 1;
      })));
      builder.then(literal("is").then(arg("player", PlayerArgumentType.create()).executes(context -> {
         PlayerListEntry player = (PlayerListEntry)context.getArgument("player", PlayerListEntry.class);
         sendMessage(player.getProfile().getName() + (Managers.FRIEND.isFriend(player.getProfile().getName()) ? " is friended." : " isn't friended."));
         return 1;
      })));
      builder.executes(context -> {
         if (Managers.FRIEND.getFriends().isEmpty()) {
            sendMessage("Friend list empty D:");
         } else {
            StringBuilder f = new StringBuilder("Friends: ");

            for (String friend : Managers.FRIEND.getFriends()) {
               try {
                  f.append(friend).append(", ");
               } catch (Exception var5) {
               }
            }

            sendMessage(f.toString());
         }

         return 1;
      });
   }
}
