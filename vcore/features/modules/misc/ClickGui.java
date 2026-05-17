package vcore.features.modules.misc;

import java.awt.FontFormatException;
import java.io.IOException;
import meteordevelopment.orbit.EventHandler;
import vcore.Vcore;
import vcore.events.impl.EventSetting;
import vcore.features.modules.Module;
import vcore.gui.clickui.ClickGUI;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;

public class ClickGui extends Module {
   public final Setting<Boolean> blur = new Setting<>("Blur", false);
   public final Setting<Integer> moduleRound = new Setting<>("ModuleRound", 4, 1, 5);
   public final Setting<Integer> settingFontScale = new Setting<>("SettingFontScale", 15, 6, 20);
   public final Setting<Integer> modulesFontScale = new Setting<>("ModulesFontScale", 18, 6, 20);

   public ClickGui() {
      super("ClickGui", "Vcore main GUI.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.applyFontSettings();
      this.setGui();
   }

   @Override
   public void onDisable() {
   }

   public void setGui() {
      mc.setScreen(ClickGUI.getClickGui());
   }

   @Override
   public void onUpdate() {
      if (!(mc.currentScreen instanceof ClickGUI)) {
         this.disable();
      }
   }

   @Override
   public boolean isToggleable() {
      return false;
   }

   public void applyFontSettings() {
      try {
         FontRenderers.sf_medium_mini = FontRenderers.create(this.settingFontScale.getValue().intValue(), "sf_medium");
         FontRenderers.sf_medium_modules = FontRenderers.create(this.modulesFontScale.getValue().intValue(), "sf_medium");
      } catch (IOException | FontFormatException e) {
         Vcore.LOGGER.warn("[ClickGui] Failed to apply font settings", e);
      }
   }

   @EventHandler
   public void onSetting(EventSetting e) {
      try {
         if (e.getSetting() == this.settingFontScale) {
            this.applyFontSettings();
         }

         if (e.getSetting() == this.modulesFontScale) {
            this.applyFontSettings();
         }
      } catch (Exception var3) {
      }
   }
}
