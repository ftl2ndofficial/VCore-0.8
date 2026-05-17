package vcore.core;

import vcore.Vcore;
import vcore.core.manager.client.AsyncManager;
import vcore.core.manager.client.CommandManager;
import vcore.core.manager.client.ConfigManager;
import vcore.core.manager.client.MacroManager;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.client.NotificationManager;
import vcore.core.manager.client.ServerManager;
import vcore.core.manager.client.SoundManager;
import vcore.core.manager.player.CombatManager;
import vcore.core.manager.player.FriendManager;
import vcore.core.manager.player.PlayerManager;

public class Managers {
   public static final CombatManager COMBAT = new CombatManager();
   public static final FriendManager FRIEND = new FriendManager();
   public static final PlayerManager PLAYER = new PlayerManager();
   public static final AsyncManager ASYNC = new AsyncManager();
   public static final ModuleManager MODULE = new ModuleManager();
   public static final ConfigManager CONFIG = new ConfigManager();
   public static final MacroManager MACRO = new MacroManager();
   public static final NotificationManager NOTIFICATION = new NotificationManager();
   public static final ServerManager SERVER = new ServerManager();
   public static final SoundManager SOUND = new SoundManager();
   public static final CommandManager COMMAND = new CommandManager();

   public static void init() {
      CONFIG.load(CONFIG.getCurrentConfig());
      MODULE.onLoad("none");
      FRIEND.loadFriends();
      MACRO.onLoad();
      SOUND.registerSounds();
   }

   public static void subscribe() {
      Vcore.EVENT_BUS.subscribe(NOTIFICATION);
      Vcore.EVENT_BUS.subscribe(SERVER);
      Vcore.EVENT_BUS.subscribe(PLAYER);
      Vcore.EVENT_BUS.subscribe(COMBAT);
      Vcore.EVENT_BUS.subscribe(ASYNC);
   }
}
