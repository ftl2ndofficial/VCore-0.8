package vcore.core.manager.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.CopyOnWriteArrayList;
import vcore.core.manager.IManager;

public class MacroManager implements IManager {
   private static CopyOnWriteArrayList<MacroManager.Macro> macros = new CopyOnWriteArrayList<>();

   public static void addMacro(MacroManager.Macro macro) {
      if (!macros.contains(macro)) {
         macros.add(macro);
      }
   }

   public void onLoad() {
      macros = new CopyOnWriteArrayList<>();

      try {
         File file = new File("Vcore/misc/macro.txt");
         if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
               while (reader.ready()) {
                  String[] nameKey = reader.readLine().split(":");
                  String name = nameKey[0];
                  String key = nameKey[1];
                  String command = nameKey[2];
                  addMacro(new MacroManager.Macro(name, command, Integer.parseInt(key)));
               }
            }
         }
      } catch (Exception var9) {
      }
   }

   public void saveMacro() {
      File file = new File("Vcore/misc/macro.txt");

      try {
         if (new File("Vcore").mkdirs()) {
            file.createNewFile();
         }
      } catch (Exception var6) {
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
         for (MacroManager.Macro macro : macros) {
            writer.write(macro.name + ":" + macro.bind + ":" + macro.text + "\n");
         }
      } catch (Exception var8) {
      }
   }

   public void removeMacro(MacroManager.Macro macro) {
      macros.remove(macro);
   }

   public CopyOnWriteArrayList<MacroManager.Macro> getMacros() {
      return macros;
   }

   public MacroManager.Macro getMacroByName(String n) {
      for (MacroManager.Macro m : this.getMacros()) {
         if (m.name.equalsIgnoreCase(n)) {
            return m;
         }
      }

      return null;
   }

   public static class Macro {
      private String name;
      private String text;
      private int bind;

      public Macro(String name, String text, int bind) {
         this.name = name;
         this.text = text;
         this.bind = bind;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public String getText() {
         return this.text;
      }

      public void setText(String text) {
         this.text = text;
      }

      public int getBind() {
         return this.bind;
      }

      public void setBind(int bind) {
         this.bind = bind;
      }

      public void runMacro() {
         if (IManager.mc.player != null) {
            if (this.text.contains("/")) {
               IManager.mc.player.networkHandler.sendChatCommand(this.text.replace("/", ""));
            } else {
               IManager.mc.player.networkHandler.sendChatMessage(this.text);
            }
         }
      }
   }
}
