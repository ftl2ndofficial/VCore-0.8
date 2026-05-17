package vcore.utility.discord;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
import vcore.utility.discord.callbacks.DisconnectedCallback;
import vcore.utility.discord.callbacks.ErroredCallback;
import vcore.utility.discord.callbacks.JoinGameCallback;
import vcore.utility.discord.callbacks.JoinRequestCallback;
import vcore.utility.discord.callbacks.ReadyCallback;
import vcore.utility.discord.callbacks.SpectateGameCallback;

public class DiscordEventHandlers extends Structure {
   public DisconnectedCallback disconnected;
   public JoinRequestCallback joinRequest;
   public SpectateGameCallback spectateGame;
   public ReadyCallback ready;
   public ErroredCallback errored;
   public JoinGameCallback joinGame;

   protected List<String> getFieldOrder() {
      return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }
}
