package vcore.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InputBlocker {
   private static volatile long blockedUntil = 0L;
   private static volatile long useBlockedUntil = 0L;
   private static final Set<String> movementBlockOwners = ConcurrentHashMap.newKeySet();

   public static void block() {
      blockedUntil = Long.MAX_VALUE;
   }

   public static void blockFor(long ms) {
      blockedUntil = System.currentTimeMillis() + ms;
   }

   public static void block(String owner) {
      if (owner != null && !owner.isEmpty()) {
         movementBlockOwners.add(owner);
      }
   }

   public static void blockUseFor(long ms) {
      useBlockedUntil = System.currentTimeMillis() + ms;
   }

   public static void unblock() {
      blockedUntil = 0L;
   }

   public static void unblock(String owner) {
      if (owner != null) {
         movementBlockOwners.remove(owner);
      }
   }

   public static void unblockUse() {
      useBlockedUntil = 0L;
   }

   public static boolean isBlocked() {
      return System.currentTimeMillis() < blockedUntil || !movementBlockOwners.isEmpty();
   }

   public static boolean isUseBlocked() {
      return System.currentTimeMillis() < useBlockedUntil;
   }
}
