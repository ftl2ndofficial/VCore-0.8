package vcore.setting.impl;

public class BooleanSettingGroup {
   private boolean enabled;
   private boolean extended;

   public BooleanSettingGroup(boolean enabled) {
      this.enabled = enabled;
      this.extended = false;
   }

   public boolean isExtended() {
      return this.extended;
   }

   public void setExtended(boolean extended) {
      this.extended = extended;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
