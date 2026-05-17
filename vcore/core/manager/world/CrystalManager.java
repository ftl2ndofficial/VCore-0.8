package vcore.core.manager.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import vcore.core.Managers;
import vcore.core.manager.IManager;

public class CrystalManager implements IManager {
   private final Map<Integer, Long> deadCrystals = new ConcurrentHashMap<>();
   private final Map<Integer, CrystalManager.Attempt> attackedCrystals = new ConcurrentHashMap<>();
   private final Map<BlockPos, CrystalManager.Attempt> awaitingPositions = new ConcurrentHashMap<>();

   public void onAttack(EndCrystalEntity crystal) {
      this.setDead(crystal.method_5628(), System.currentTimeMillis());
      this.addAttack(crystal);
   }

   public void reset() {
      this.deadCrystals.clear();
      this.attackedCrystals.clear();
      this.awaitingPositions.clear();
   }

   public void update() {
      long time = System.currentTimeMillis();
      this.deadCrystals.entrySet().removeIf(entry -> time - entry.getValue() > Managers.SERVER.getPing() * 2L);
      this.attackedCrystals.entrySet().removeIf(entry -> entry.getValue().shouldRemove());
      this.awaitingPositions.entrySet().removeIf(entry -> entry.getValue().shouldRemove());
   }

   public boolean isDead(Integer id) {
      return this.deadCrystals.containsKey(id);
   }

   public void setDead(Integer id, long deathTime) {
      this.deadCrystals.putIfAbsent(id, deathTime);
   }

   public boolean isBlocked(Integer id) {
      return this.attackedCrystals.containsKey(id) && this.attackedCrystals.get(id).canSetPosBlocked();
   }

   public void addAttack(EndCrystalEntity entity) {
      this.attackedCrystals.compute(entity.method_5628(), (pos, attempt) -> {
         if (attempt == null) {
            return new CrystalManager.Attempt(System.currentTimeMillis(), 1, entity.method_19538());
         }

         attempt.addAttempt();
         return (CrystalManager.Attempt)attempt;
      });
   }

   public Map<BlockPos, CrystalManager.Attempt> getAwaitingPositions() {
      return this.awaitingPositions;
   }

   public void confirmSpawn(BlockPos bp) {
      this.awaitingPositions.remove(bp);
   }

   public void addAwaitingPos(BlockPos blockPos) {
      this.awaitingPositions.compute(blockPos, (pos, attempt) -> {
         if (attempt == null) {
            return new CrystalManager.Attempt(System.currentTimeMillis(), 1, blockPos.toCenterPos());
         }

         attempt.addAttempt();
         return (CrystalManager.Attempt)attempt;
      });
   }

   public boolean isPositionBlocked(BlockPos bp) {
      return this.awaitingPositions.containsKey(bp) && this.awaitingPositions.get(bp).canSetPosBlocked();
   }

   public class Attempt {
      long time;
      int attempts;
      float distance;
      public Vec3d pos;

      Attempt(long time, int attempts, Vec3d pos) {
         this.time = time;
         this.pos = pos;
         this.attempts = attempts;
         this.distance = (float)IManager.mc.player.method_5707(pos);
      }

      public Vec3d getPos() {
         return this.pos;
      }

      public long getTime() {
         return this.time;
      }

      public float getDistance() {
         return this.distance;
      }

      public boolean shouldRemove() {
         return Math.abs(this.distance - IManager.mc.player.method_5707(this.pos)) >= 1.0;
      }

      public void addAttempt() {
         this.attempts++;
      }

      public boolean canSetPosBlocked() {
         return this.attempts >= Math.max(1.0F, Managers.SERVER.getPing() / 25.0F);
      }
   }
}
