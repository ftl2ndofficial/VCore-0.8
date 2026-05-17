package vcore.features.modules.movement.freelook;

public final class FreeLookState {
   public static boolean active;
   private static boolean manualActive;
   private static boolean autoFarmActive;

   private FreeLookState() {
   }

   public static void setManualActive(boolean value) {
      manualActive = value;
      updateActive();
   }

   public static void setAutoFarmActive(boolean value) {
      autoFarmActive = value;
      updateActive();
   }

   public static boolean isManualActive() {
      return manualActive;
   }

   private static void updateActive() {
      active = manualActive || autoFarmActive;
   }
}
