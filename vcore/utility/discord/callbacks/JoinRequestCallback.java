package vcore.utility.discord.callbacks;

import com.sun.jna.Callback;
import vcore.utility.discord.DiscordUser;

public interface JoinRequestCallback extends Callback {
   void apply(DiscordUser var1);
}
