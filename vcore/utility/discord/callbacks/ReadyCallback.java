package vcore.utility.discord.callbacks;

import com.sun.jna.Callback;
import vcore.utility.discord.DiscordUser;

public interface ReadyCallback extends Callback {
   void apply(DiscordUser var1);
}
