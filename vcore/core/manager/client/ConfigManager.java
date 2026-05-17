package vcore.core.manager.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.IManager;
import vcore.features.cmd.Command;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClickGui;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.EnumConverter;
import vcore.setting.impl.ItemBindSetting;
import vcore.setting.impl.ItemSelectSetting;
import vcore.setting.impl.PositionSetting;
import vcore.setting.impl.SettingGroup;

public class ConfigManager implements IManager {
   public static final String CONFIG_FOLDER_NAME = "Vcore";
   public static final File MAIN_FOLDER = new File(mc.runDirectory, "Vcore");
   public static final File CONFIGS_FOLDER = new File(MAIN_FOLDER, "configs");
   public static final File TEMP_FOLDER = new File(MAIN_FOLDER, "temp");
   public static final File MISC_FOLDER = new File(MAIN_FOLDER, "misc");
   public static final File SOUNDS_FOLDER = new File(MISC_FOLDER, "sounds");
   public static final File IMAGES_FOLDER = new File(MISC_FOLDER, "images");
   public static final File TABPARSER_FOLDER = new File(MISC_FOLDER, "tabparser");
   public File currentConfig = null;
   public static boolean firstLaunch = false;

   public ConfigManager() {
      firstLaunch = !MAIN_FOLDER.exists();
      this.createDirs(MAIN_FOLDER, CONFIGS_FOLDER, TEMP_FOLDER, MISC_FOLDER, SOUNDS_FOLDER, IMAGES_FOLDER, TABPARSER_FOLDER);
   }

   private void createDirs(File... dirs) {
      for (File dir : dirs) {
         if (!dir.exists()) {
            dir.mkdirs();
         }
      }
   }

   @NotNull
   public static String getConfigDate(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".vc");
      return !file.exists() ? "none" : new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date(file.lastModified()));
   }

   public void load(String name, String category) {
      File file = new File(CONFIGS_FOLDER, name + ".vc");
      if (!file.exists()) {
         Command.sendMessage("Config " + name + " does not exist!");
      } else {
         if (this.currentConfig != null) {
            this.save(this.currentConfig);
         }

         Managers.MODULE.onUnload(category);
         this.load(file, category);
         Managers.MODULE.onLoad(category);
      }
   }

   public void loadBinds(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".vc");
      if (!file.exists()) {
         Command.sendMessage("Config " + name + " does not exist!");
      } else {
         if (this.currentConfig != null) {
            this.save(this.currentConfig);
         }

         this.loadBinds(file);
      }
   }

   private void loadBinds(@NotNull File config) {
      if (!config.exists()) {
         this.save(config);
      }

      try (FileReader reader = new FileReader(config, StandardCharsets.UTF_8)) {
         JsonObject modulesObject = JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject();
         JsonArray modules = modulesObject.getAsJsonArray("Modules");
         if (modules != null) {
            for (JsonElement element : modules) {
               this.parseBinds(element.getAsJsonObject());
            }
         }

         Command.sendMessage("Loaded bind from config: " + config.getName());
      } catch (IOException e) {
         LogUtils.getLogger().warn(e.getMessage());
      }

      this.saveCurrentConfig();
   }

   public void load(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".vc");
      if (!file.exists()) {
         Command.sendMessage("Config " + name + " does not exist!");
      } else {
         if (this.currentConfig != null) {
            this.save(this.currentConfig);
         }

         Managers.MODULE.onUnload("none");
         this.load(file);
         Managers.MODULE.onLoad("none");
      }
   }

   public void loadModuleOnly(String name, Module module) {
      File file = new File(CONFIGS_FOLDER, name + ".vc");
      if (!file.exists()) {
         Command.sendMessage("Config " + name + " does not exist!");
      } else {
         if (module.isEnabled()) {
            Vcore.EVENT_BUS.unsubscribe(module);
            module.setEnabled(false);
         }

         this.loadModuleOnly(file, module);
         if (module.isEnabled()) {
            Vcore.EVENT_BUS.subscribe(module);
         }
      }
   }

   public void load(@NotNull File config) {
      this.load(config, "none");
   }

   private void load(@NotNull File config, String category) {
      if (!config.exists()) {
         this.save(config);
      }

      try (FileReader reader = new FileReader(config, StandardCharsets.UTF_8)) {
         JsonObject modulesObject = JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject();
         JsonArray modules = modulesObject.getAsJsonArray("Modules");
         if (modules != null) {
            for (JsonElement element : modules) {
               try {
                  this.parseModule(element.getAsJsonObject(), category);
               } catch (Exception var10) {
               }
            }
         }

         Command.sendMessage("Loaded " + config.getName());
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (Objects.equals(category, "none")) {
         this.currentConfig = config;
      }

      this.saveCurrentConfig();
   }

   public void loadModuleOnly(File config, Module module) {
      try (FileReader reader = new FileReader(config)) {
         JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
         JsonObject modulesObject = array.get(0).getAsJsonObject();
         JsonArray modules = modulesObject.getAsJsonArray("Modules");
         if (modules != null) {
            for (JsonElement element : modules) {
               JsonObject moduleObject = element.getAsJsonObject();
               Module loadedModule = Managers.MODULE.modules.stream().filter(m -> moduleObject.getAsJsonObject(m.getName()) != null).findFirst().orElse(null);
               if (loadedModule != null && Objects.equals(module.getName(), loadedModule.getName())) {
                  this.parseModule(moduleObject, "none");
               }
            }
         }

         Command.sendMessage("Loaded " + module.getName() + " from " + config.getName());
      } catch (IOException e) {
         LogUtils.getLogger().warn(e.getMessage());
      }
   }

   public void save(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".vc");
      if (file.exists()) {
         Command.sendMessage("Overwriting " + name + "...");
         file.delete();
      } else {
         Command.sendMessage("Config " + name + " successfully saved!");
      }

      this.save(file);
   }

   public void save(@NotNull File config) {
      try {
         if (!config.exists()) {
            config.createNewFile();
         }

         JsonArray array = new JsonArray();
         JsonObject modulesObj = new JsonObject();
         modulesObj.add("Modules", this.getModuleArray());
         array.add(modulesObj);

         try (FileWriter writer = new FileWriter(config, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(array, writer);
         }
      } catch (IOException e) {
         LogUtils.getLogger().warn(e.getMessage());
      }
   }

   private void parseModule(JsonObject object, String category) throws NullPointerException {
      Module module = Managers.MODULE.modules.stream().filter(m -> object.getAsJsonObject(m.getName()) != null).findFirst().orElse(null);
      if (module != null) {
         if (Objects.equals(category, "none") || module.getCategory().getName().equalsIgnoreCase(category)) {
            JsonObject mobject = object.getAsJsonObject(module.getName());

            for (Setting setting : module.getSettings()) {
               try {
                  if (!(setting.getValue() instanceof SettingGroup)) {
                     if (setting.getValue() instanceof Boolean) {
                        JsonElement el = mobject.get(setting.getName());
                        if (el != null && el.isJsonPrimitive()) {
                           setting.setValue(el.getAsJsonPrimitive().getAsBoolean());
                        }
                     } else if (setting.getValue() instanceof Float) {
                        JsonElement el = mobject.get(setting.getName());
                        if (el != null && el.isJsonPrimitive()) {
                           setting.setValue(el.getAsJsonPrimitive().getAsFloat());
                        }
                     } else if (setting.getValue() instanceof Integer) {
                        JsonElement el = mobject.get(setting.getName());
                        if (el != null && el.isJsonPrimitive()) {
                           setting.setValue(el.getAsJsonPrimitive().getAsInt());
                        }
                     } else if (setting.getValue() instanceof String) {
                        JsonElement el = mobject.get(setting.getName());
                        if (el != null && el.isJsonPrimitive()) {
                           setting.setValue(el.getAsJsonPrimitive().getAsString().replace("%%", " ").replace("++", "/"));
                        }
                     } else if (setting.getValue() instanceof Bind) {
                        JsonArray array = mobject.getAsJsonArray(setting.getName());
                        if (array.get(0).getAsString().contains("M")) {
                           setting.setValue(new Bind(Integer.parseInt(array.get(0).getAsString().replace("M", "")), true, array.get(1).getAsBoolean()));
                        } else {
                           setting.setValue(new Bind(Integer.parseInt(array.get(0).getAsString()), false, array.get(1).getAsBoolean()));
                        }
                     } else if (setting.getValue() instanceof ColorSetting colorSetting) {
                        JsonElement el = mobject.get(setting.getName());
                        if (el != null && el.isJsonArray()) {
                           JsonArray array = el.getAsJsonArray();
                           if (array.size() >= 2) {
                              colorSetting.setColor(array.get(0).getAsInt());
                              colorSetting.setRainbow(array.get(1).getAsBoolean());
                           }
                        }
                     } else if (setting.getValue() instanceof PositionSetting posSetting) {
                        JsonArray array = mobject.getAsJsonArray(setting.getName());
                        posSetting.setX(array.get(0).getAsFloat());
                        posSetting.setY(array.get(1).getAsFloat());
                     } else if (setting.getValue() instanceof BooleanSettingGroup bGroup) {
                        bGroup.setEnabled(mobject.getAsJsonPrimitive(setting.getName()).getAsBoolean());
                     } else if (setting.getValue() instanceof ItemSelectSetting iSetting) {
                        JsonArray array = mobject.getAsJsonArray(setting.getName());

                        for (int i = 0; i < array.size(); i++) {
                           if (!iSetting.getItemsById().contains(array.get(i).getAsString())) {
                              iSetting.getItemsById().add(array.get(i).getAsString());
                           }
                        }
                     } else if (setting.getValue() instanceof ItemBindSetting) {
                        JsonArray array = mobject.getAsJsonArray(setting.getName());
                        if (array != null && array.size() >= 3) {
                           String itemId = array.get(0).getAsString();
                           String bindRaw = array.get(1).getAsString();
                           boolean hold = array.get(2).getAsBoolean();
                           Bind bind;
                           if (bindRaw.startsWith("M")) {
                              bind = new Bind(Integer.parseInt(bindRaw.replace("M", "")), true, hold);
                           } else {
                              bind = new Bind(Integer.parseInt(bindRaw), false, hold);
                           }

                           setting.setValue(new ItemBindSetting(itemId, bind));
                        }
                     } else if (setting.getValue().getClass().isEnum()) {
                        JsonElement el = mobject.get(setting.getName());
                        if (el != null && el.isJsonPrimitive()) {
                           Enum value = new EnumConverter((Class<? extends Enum>)((Enum)setting.getValue()).getClass()).doBackward(el.getAsJsonPrimitive());
                           setting.setValue(value == null ? setting.getDefaultValue() : value);
                        }
                     }
                  }
               } catch (Exception e) {
                  LogUtils.getLogger().warn("[Vcore] Module: " + module.getName() + " Setting: " + setting.getName() + " Error: ");
                  e.printStackTrace();
               }
            }

            if (module instanceof ClickGui clickGui) {
               try {
                  if (MinecraftClient.getInstance().getWindow() != null) {
                     clickGui.applyFontSettings();
                  }
               } catch (Exception var16) {
               }
            }
         }
      }
   }

   private void parseBinds(JsonObject object) throws NullPointerException {
      Module module = Managers.MODULE.modules.stream().filter(m -> object.getAsJsonObject(m.getName()) != null).findFirst().orElse(null);
      if (module != null) {
         JsonObject mobject = object.getAsJsonObject(module.getName());

         for (Setting setting : module.getSettings()) {
            try {
               if (setting.getValue() instanceof Bind) {
                  JsonArray array = mobject.getAsJsonArray(setting.getName());
                  if (array.get(0).getAsString().contains("M")) {
                     setting.setValue(new Bind(Integer.parseInt(array.get(0).getAsString().replace("M", "")), true, array.get(1).getAsBoolean()));
                  } else {
                     setting.setValue(new Bind(Integer.parseInt(array.get(0).getAsString()), false, array.get(1).getAsBoolean()));
                  }
               }
            } catch (Exception e) {
               LogUtils.getLogger().warn("[Vcore] Module: " + module.getName() + " Setting: " + setting.getName() + " Error: ");
               e.printStackTrace();
            }
         }
      }
   }

   @NotNull
   private JsonArray getModuleArray() {
      JsonArray modulesArray = new JsonArray();

      for (Module m : Managers.MODULE.modules) {
         modulesArray.add(this.getModuleObject(m));
      }

      return modulesArray;
   }

   public JsonObject getModuleObject(@NotNull Module m) {
      JsonObject attribs = new JsonObject();
      JsonParser jp = new JsonParser();

      for (Setting setting : m.getSettings()) {
         if (setting.getValue() instanceof ColorSetting color) {
            JsonArray array = new JsonArray();
            array.add(new JsonPrimitive(color.getRawColor()));
            array.add(new JsonPrimitive(color.isRainbow()));
            attribs.add(setting.getName(), array);
         } else if (setting.getValue() instanceof PositionSetting pos) {
            JsonArray array = new JsonArray();
            array.add(new JsonPrimitive(pos.getX()));
            array.add(new JsonPrimitive(pos.getY()));
            attribs.add(setting.getName(), array);
         } else if (setting.getValue() instanceof BooleanSettingGroup bGroup) {
            attribs.add(setting.getName(), jp.parse(String.valueOf(bGroup.isEnabled())));
         } else if (setting.getValue() instanceof Bind b) {
            JsonArray array = new JsonArray();
            if (b.isMouse()) {
               array.add(jp.parse(b.getBind()));
            } else {
               array.add(new JsonPrimitive(b.getKey()));
            }

            array.add(new JsonPrimitive(b.isHold()));
            attribs.add(setting.getName(), array);
         } else if (setting.getValue() instanceof String str) {
            try {
               attribs.add(setting.getName(), jp.parse(str.replace(" ", "%%").replace("/", "++")));
            } catch (Exception var17) {
            }
         } else if (!(setting.getValue() instanceof ItemSelectSetting iSelect)) {
            if (setting.getValue() instanceof ItemBindSetting iBind) {
               JsonArray array = new JsonArray();
               array.add(new JsonPrimitive(iBind.getItemId()));
               Bind b = iBind.getBind();
               if (b.isMouse()) {
                  array.add(new JsonPrimitive("M" + b.getKey()));
               } else {
                  array.add(new JsonPrimitive(b.getKey()));
               }

               array.add(new JsonPrimitive(b.isHold()));
               attribs.add(setting.getName(), array);
            } else if (setting.isEnumSetting()) {
               attribs.add(
                  setting.getName(), new EnumConverter((Class<? extends Enum>)((Enum)setting.getValue()).getClass()).doForward((Enum)setting.getValue())
               );
            } else {
               try {
                  attribs.add(setting.getName(), jp.parse(setting.getSerializedValue()));
               } catch (Exception var16) {
               }
            }
         } else {
            JsonArray array = new JsonArray();

            for (String id : iSelect.getItemsById()) {
               array.add(new JsonPrimitive(id));
            }

            attribs.add(setting.getName(), array);
         }
      }

      JsonObject moduleObject = new JsonObject();
      moduleObject.add(m.getName(), attribs);
      return moduleObject;
   }

   public void delete(@NotNull File file) {
      file.delete();
   }

   public void delete(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".vc");
      if (file.exists()) {
         this.delete(file);
      }
   }

   public List<String> getConfigList() {
      if (MAIN_FOLDER.exists() && MAIN_FOLDER.listFiles() != null) {
         List<String> list = new ArrayList<>();
         if (CONFIGS_FOLDER.listFiles() != null) {
            for (File file : Arrays.stream(Objects.requireNonNull(CONFIGS_FOLDER.listFiles())).filter(f -> f.getName().endsWith(".vc")).toList()) {
               list.add(file.getName().replace(".vc", ""));
            }
         }

         return list;
      } else {
         return null;
      }
   }

   public void saveCurrentConfig() {
      if (this.currentConfig != null) {
         File file = new File(MISC_FOLDER, "currentcfg.txt");

         try {
            if (!file.exists()) {
               file.createNewFile();
            }

            try (FileWriter writer = new FileWriter(file)) {
               writer.write(this.currentConfig.getName().replace(".vc", ""));
            }
         } catch (Exception e) {
            LogUtils.getLogger().warn(e.getMessage());
         }
      }
   }

   public File getCurrentConfig() {
      File file = new File(MISC_FOLDER, "currentcfg.txt");
      String name = "config";

      try {
         if (file.exists()) {
            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {
               name = reader.nextLine();
            }

            reader.close();
         }
      } catch (Exception e) {
         LogUtils.getLogger().warn(e.getMessage());
      }

      this.currentConfig = new File(CONFIGS_FOLDER, name + ".vc");
      return this.currentConfig;
   }
}
