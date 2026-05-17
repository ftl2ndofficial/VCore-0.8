package vcore.features.modules.combat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import vcore.events.impl.EventEntityRemoved;
import vcore.events.impl.EventEntitySpawn;
import vcore.events.impl.EventSync;
import vcore.features.modules.Module;
import vcore.features.modules.misc.FakePlayer;
import vcore.setting.Setting;

public final class AntiBot extends Module {
   public final Setting<AntiBot.Mode> mode = new Setting<>("Mode", AntiBot.Mode.Matrix);
   public final Setting<Boolean> enabledRemove = new Setting<>("Remove", true);
   public final Setting<Boolean> onlyAura = new Setting<>("OnlyAura", false);
   public static final List<PlayerEntity> bots = new CopyOnWriteArrayList<>();
   private static final Set<UUID> botIds = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Integer> pendingChecks = new ConcurrentHashMap<>();
   private static final int MAX_SPAWN_CHECKS = 6;

   public AntiBot() {
      super("AntiBot", "Removes matrix bots.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.world != null) {
         for (PlayerEntity p : mc.world.method_18456()) {
            this.detect(p);
         }
      }
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (this.mode.getValue() == AntiBot.Mode.Matrix) {
         if (mc.world != null && !pendingChecks.isEmpty()) {
            for (PlayerEntity p : mc.world.method_18456()) {
               try {
                  Integer rem = pendingChecks.get(p.method_5667());
                  if (rem != null) {
                     this.detect(p);
                     if (!isBot(p) && rem - 1 > 0) {
                        pendingChecks.put(p.method_5667(), rem - 1);
                     } else {
                        pendingChecks.remove(p.method_5667());
                     }
                  }
               } catch (Exception var6) {
               }
            }
         }

         if (!this.onlyAura.getValue()) {
            for (PlayerEntity p : mc.world.method_18456()) {
               this.detect(p);
            }
         } else if (Aura.target instanceof PlayerEntity ent) {
            this.detect((PlayerEntity)Aura.target);
         }

         if (this.enabledRemove.getValue()) {
            for (PlayerEntity p : bots) {
               try {
                  if (FakePlayer.fakePlayer == null || p != FakePlayer.fakePlayer) {
                     mc.world.removeEntity(p.method_5628(), RemovalReason.KILLED);
                  }
               } catch (Exception var5) {
               }
            }
         }
      }
   }

   @EventHandler
   public void onEntitySpawn(EventEntitySpawn e) {
      if (this.mode.getValue() == AntiBot.Mode.Matrix) {
         if (e != null && e.getEntity() != null) {
            if (e.getEntity() instanceof PlayerEntity p) {
               this.detect(p);

               try {
                  if (!isBot(p)) {
                     pendingChecks.put(p.method_5667(), 6);
                  }
               } catch (Exception var4) {
               }
            }
         }
      }
   }

   @EventHandler
   public void onEntityRemoved(EventEntityRemoved e) {
      if (e != null && e.getEntity() != null) {
         if (e.getEntity() instanceof PlayerEntity p) {
            try {
               bots.removeIf(b -> b == null || b.method_5667().equals(p.method_5667()));
               botIds.remove(p.method_5667());
               pendingChecks.remove(p.method_5667());
            } catch (Exception var4) {
            }
         }
      }
   }

   private void detect(PlayerEntity ent) {
      if (ent != null && ent != mc.player) {
         if (FakePlayer.fakePlayer == null || ent != FakePlayer.fakePlayer) {
            if (ent instanceof OtherClientPlayerEntity) {
               if (!isBot(ent)) {
                  boolean visualArmor = false;

                  try {
                     ItemStack head = ent.method_6118(EquipmentSlot.HEAD);
                     ItemStack chest = ent.method_6118(EquipmentSlot.CHEST);
                     ItemStack legs = ent.method_6118(EquipmentSlot.LEGS);
                     ItemStack feet = ent.method_6118(EquipmentSlot.FEET);
                     if (head != null && !head.isEmpty()
                        || chest != null && !chest.isEmpty()
                        || legs != null && !legs.isEmpty()
                        || feet != null && !feet.isEmpty()) {
                        visualArmor = true;
                     }
                  } catch (Exception var9) {
                  }

                  boolean invArmor = false;

                  try {
                     for (ItemStack s : ent.getInventory().armor) {
                        if (s != null && !s.isEmpty()) {
                           invArmor = true;
                           break;
                        }
                     }
                  } catch (Exception var8) {
                  }

                  try {
                     if (visualArmor && (!invArmor || ent.method_6096() == 0)) {
                        this.addBot(ent);
                     }
                  } catch (Exception ignored) {
                     if (visualArmor && !invArmor) {
                        this.addBot(ent);
                     }
                  }
               }
            }
         }
      }
   }

   private void addBot(PlayerEntity p) {
      if (p != null) {
         if (FakePlayer.fakePlayer == null || p != FakePlayer.fakePlayer) {
            try {
               bots.add(p);
               botIds.add(p.method_5667());
               pendingChecks.remove(p.method_5667());
               this.sendMessage(p.method_5477().getString() + " is a bot (Matrix)!");
            } catch (Exception var3) {
            }
         }
      }
   }

   public static boolean isBot(Entity e) {
      if (e == null) {
         return false;
      }

      try {
         if (FakePlayer.fakePlayer != null && e == FakePlayer.fakePlayer) {
            return false;
         }
      } catch (Exception var3) {
      }

      try {
         if (e instanceof PlayerEntity pl) {
            if (bots.contains(pl)) {
               return true;
            }

            return botIds.contains(pl.method_5667());
         }
      } catch (Exception var2) {
      }

      return false;
   }

   @Override
   public String getDisplayInfo() {
      return String.valueOf(bots.size());
   }

   public enum Mode {
      Matrix;
   }
}
