package vcore.setting.impl;

import java.lang.reflect.Field;
import org.lwjgl.glfw.GLFW;

public class Bind {
   private final int key;
   private boolean hold;
   private final boolean mouse;

   public Bind(int key, boolean mouse, boolean hold) {
      this.key = key;
      this.mouse = mouse;
      this.hold = hold;
   }

   public int getKey() {
      return this.key;
   }

   public String getBind() {
      if (this.mouse) {
         return "M" + this.key;
      }

      String kn = this.key > 0 ? GLFW.glfwGetKeyName(this.key, 0) : "None";
      if (kn == null) {
         try {
            for (Field declaredField : GLFW.class.getDeclaredFields()) {
               if (declaredField.getName().startsWith("GLFW_KEY_")) {
                  int a = (Integer)declaredField.get(null);
                  if (a == this.key) {
                     String nb = declaredField.getName().substring("GLFW_KEY_".length());
                     kn = nb.substring(0, 1).toUpperCase() + nb.substring(1).toLowerCase();
                  }
               }
            }
         } catch (Exception ignore) {
            kn = "unknown." + this.key;
         }
      }

      return this.key == -1 ? "None" : (kn + "").toUpperCase();
   }

   public boolean isHold() {
      return this.hold;
   }

   public boolean isMouse() {
      return this.mouse;
   }

   public void setHold(boolean hold) {
      this.hold = hold;
   }
}
