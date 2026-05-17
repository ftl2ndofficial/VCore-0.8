package vcore.features.modules.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.IOUtils;
import vcore.core.Managers;
import vcore.features.hud.impl.StaffBoard;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.Timer;

public class Spammer extends Module {
   public static ArrayList<String> SpamList = new ArrayList<>();
   private int scriptIndex = 0;
   private float customScriptDelay = -1.0F;
   public Setting<Spammer.Messages> messages = new Setting<>("messages", Spammer.Messages.File);
   public Setting<Spammer.Mode> mode = new Setting<>("mode", Spammer.Mode.Chat, v -> this.messages.getValue() != Spammer.Messages.Script);
   public Setting<Spammer.WhisperPrefix> whisper_prefix = new Setting<>(
      "prefix", Spammer.WhisperPrefix.W, v -> this.mode.getValue() == Spammer.Mode.Whispers && this.messages.getValue() != Spammer.Messages.Script
   );
   public Setting<Boolean> global = new Setting<>(
      "global", true, v -> this.mode.getValue() == Spammer.Mode.Chat && this.messages.getValue() != Spammer.Messages.Script
   );
   public Setting<Boolean> antiSpam = new Setting<>("AntiSpam", false, v -> this.messages.getValue() != Spammer.Messages.Script);
   public Setting<Float> delay = new Setting<>("delay", 5.0F, 0.0F, 30.0F, v -> this.messages.getValue() != Spammer.Messages.Script);
   public Setting<Float> scriptDefaultDelay = new Setting<>("Default delay", 0.2F, 0.0F, 30.0F, v -> this.messages.getValue() == Spammer.Messages.Script);
   public Setting<Boolean> autoReload = new Setting<>("AutoReload", true, v -> this.messages.getValue() == Spammer.Messages.Script);
   private final Timer timer_delay = new Timer();
   private final Random random = new Random();
   private String fact;

   public Spammer() {
      super("Spammer", "Spams messages.", Module.Category.MISC);
   }

   public static void loadSpammer(Spammer.Messages messagesMode) {
      try {
         String fileName = messagesMode == Spammer.Messages.Script ? "script.txt" : "spammer.txt";
         File file = new File("Vcore/misc/" + fileName);
         if (!file.exists()) {
            file.createNewFile();
         }

         new Thread(() -> {
            try {
               FileInputStream fis = new FileInputStream(file);
               InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
               BufferedReader reader = new BufferedReader(isr);
               ArrayList<String> lines = new ArrayList<>();

               String line;
               while ((line = reader.readLine()) != null) {
                  lines.add(line);
               }

               boolean newline = false;

               for (String l : lines) {
                  if (l.equals("")) {
                     newline = true;
                     break;
                  }
               }

               SpamList.clear();
               ArrayList<String> spamList = new ArrayList<>();
               if (!newline) {
                  spamList.addAll(lines);
               } else {
                  StringBuilder spamChunk = new StringBuilder();

                  for (String l : lines) {
                     if (l.equals("")) {
                        if (!spamChunk.isEmpty()) {
                           spamList.add(spamChunk.toString());
                           spamChunk = new StringBuilder();
                        }
                     } else {
                        spamChunk.append(l).append(" ");
                     }
                  }

                  spamList.add(spamChunk.toString());
               }

               SpamList = spamList;
            } catch (Exception var11) {
            }
         }).start();
      } catch (IOException var3) {
      }
   }

   public String getPlayerName() {
      try {
         List<String> list = StaffBoard.getOnlinePlayer();
         return list.isEmpty() ? "" : list.get(this.random.nextInt(0, list.size() - 1));
      } catch (NullPointerException e) {
         return null;
      }
   }

   private void changeFact() {
      Managers.ASYNC.run(() -> {
         try {
            String jsonResponse = IOUtils.toString(new URL("https://catfact.ninja/fact?max_length=200"), StandardCharsets.UTF_8);
            JsonObject jsonObject = new JsonParser().parse(jsonResponse).getAsJsonObject();
            this.fact = jsonObject.get("fact").getAsString();
         } catch (IOException e) {
            this.disable("Failed to load the fact, can you turn on the Internet?");
         }
      });
   }

   public static String generateRandomSymbol() {
      Random random = new Random();
      String randomSymbol = "[";
      randomSymbol = randomSymbol + (char)(random.nextInt(26) + 97);
      randomSymbol = randomSymbol + random.nextInt(10);
      randomSymbol = randomSymbol + (char)(random.nextInt(26) + 97);
      return randomSymbol + "]";
   }

   @Override
   public void onEnable() {
      loadSpammer(this.messages.getValue());
      if (this.messages.getValue() == Spammer.Messages.Script) {
         if (this.autoReload.getValue()) {
            this.customScriptDelay = -1.0F;
         }
      } else {
         this.customScriptDelay = -1.0F;
      }
   }

   @Override
   public void onUpdate() {
      if (this.messages.getValue() == Spammer.Messages.Script && this.mode.getValue() != Spammer.Mode.Chat) {
         this.mode.setValue(Spammer.Mode.Chat);
      }

      float currentDelay = this.messages.getValue() == Spammer.Messages.Script ? this.scriptDefaultDelay.getValue() : this.delay.getValue();
      if (this.messages.getValue() == Spammer.Messages.Script && this.customScriptDelay > 0.0F) {
         currentDelay = this.customScriptDelay;
      }

      if (this.timer_delay.passedMs((long)(currentDelay * 1000.0F))) {
         this.customScriptDelay = -1.0F;
         String c;
         if (this.messages.getValue() == Spammer.Messages.File) {
            if (SpamList.isEmpty()) {
               this.disable("The spammer file is empty!");
               return;
            }

            c = SpamList.get(new Random().nextInt(SpamList.size()));
         } else if (this.messages.getValue() == Spammer.Messages.Script) {
            if (SpamList.isEmpty()) {
               this.disable("The script file is empty!");
               return;
            }

            c = this.getNextScriptMessage();
            if (c == null) {
               return;
            }
         } else {
            if (this.fact == null) {
               return;
            }

            c = this.fact;
            this.changeFact();
         }

         if (this.messages.getValue() != Spammer.Messages.Script && this.antiSpam.getValue()) {
            c = c + generateRandomSymbol();
         }

         if (this.mode.getValue() == Spammer.Mode.Chat) {
            if (c.charAt(0) == '/') {
               c = c.replace("/", "");
               mc.player.networkHandler.sendCommand(c);
            } else if (this.messages.getValue() != Spammer.Messages.Script) {
               mc.player.networkHandler.sendChatMessage(this.global.getValue() ? "!" + c : c);
            } else {
               mc.player.networkHandler.sendChatMessage(c);
            }
         } else {
            try {
               String prefix = this.whisper_prefix.getValue().prefix;
               mc.player.networkHandler.sendCommand(prefix + this.getPlayerName() + " " + c);
            } catch (NullPointerException var4) {
            }
         }

         if (this.messages.getValue() == Spammer.Messages.Script) {
            this.processTrailingScriptCommands();
         }

         this.timer_delay.reset();
      }
   }

   private String getNextScriptMessage() {
      if (SpamList.isEmpty()) {
         return null;
      }

      int steps = 0;

      while (steps < SpamList.size()) {
         String entry = SpamList.get(this.scriptIndex);
         this.advanceScriptIndex();
         steps++;
         String trimmed = entry.trim();
         if (!trimmed.isEmpty()) {
            if (trimmed.startsWith("!delay ")) {
               this.applyDelayCommand(trimmed, true);
               return null;
            }

            return entry;
         }
      }

      return null;
   }

   private void processTrailingScriptCommands() {
      if (!SpamList.isEmpty()) {
         int steps = 0;

         while (steps < SpamList.size()) {
            String entry = SpamList.get(this.scriptIndex);
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
               this.advanceScriptIndex();
               steps++;
            } else {
               if (!trimmed.startsWith("!delay ")) {
                  break;
               }

               this.advanceScriptIndex();
               this.applyDelayCommand(trimmed, false);
               steps++;
            }
         }
      }
   }

   private void applyDelayCommand(String trimmed, boolean resetTimerImmediately) {
      String[] parts = trimmed.split("\\s+");
      if (parts.length >= 2) {
         try {
            float parsedDelay = Float.parseFloat(parts[1]);
            if (parsedDelay >= 0.0F) {
               this.customScriptDelay = parsedDelay;
            }
         } catch (NumberFormatException var5) {
         }
      }

      if (resetTimerImmediately) {
         this.timer_delay.reset();
      }
   }

   private void advanceScriptIndex() {
      this.scriptIndex++;
      if (this.scriptIndex >= SpamList.size()) {
         this.scriptIndex = 0;
      }
   }

   private enum Messages {
      File,
      Script,
      CatFacts;
   }

   private enum Mode {
      Chat,
      Whispers;
   }

   private enum WhisperPrefix {
      W("w "),
      Msg("msg "),
      Tell("tell ");

      final String prefix;

      WhisperPrefix(String p) {
         this.prefix = p;
      }
   }
}
