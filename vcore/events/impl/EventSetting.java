package vcore.events.impl;

import vcore.events.Event;
import vcore.setting.Setting;

public class EventSetting extends Event {
   final Setting<?> setting;

   public EventSetting(Setting<?> setting) {
      this.setting = setting;
   }

   public Setting<?> getSetting() {
      return this.setting;
   }
}
