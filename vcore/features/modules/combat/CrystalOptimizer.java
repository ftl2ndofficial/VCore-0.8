package vcore.features.modules.combat;

import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public final class CrystalOptimizer extends Module {
   private static final long MIN_BLATANT_TIME_MS = 150L;
   private static final double CHAIN_CRYSTAL_RADIUS_SQ = 144.0;
   private final Setting<Boolean> blatant = new Setting<>("Blatant", true);
   private final Map<Integer, Long> inhibitedCrystals = new ConcurrentHashMap<>();

   public CrystalOptimizer() {
      super("CrystalOptimizer", "Removes attacked crystals client-side faster.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.inhibitedCrystals.clear();
   }

   @Override
   public void onLogout() {
      this.inhibitedCrystals.clear();
   }

   @Override
   public void onUpdate() {
      if (fullNullCheck()) {
         this.inhibitedCrystals.clear();
      } else {
         if (this.blatant.getValue()) {
            this.pruneInhibitedCrystals();
         } else {
            this.inhibitedCrystals.clear();
         }
      }
   }

   @EventHandler
   private void onPacketSend(PacketEvent.@NotNull Send event) {
      if (!fullNullCheck() && event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
         if (Criticals.getInteractType(packet) == Criticals.InteractType.ATTACK) {
            if (this.blatant.getValue()) {
               this.pruneInhibitedCrystals();
            }

            Entity entity = Criticals.getEntity(packet);
            if (this.blatant.getValue()) {
               this.handleBlatantAttack(event, packet, entity);
            } else {
               if (entity instanceof EndCrystalEntity crystal && !this.cantBreakCrystal()) {
                  this.removeCrystal(crystal);
               }
            }
         }
      }
   }

   private void handleBlatantAttack(PacketEvent.@NotNull Send event, @NotNull PlayerInteractEntityC2SPacket packet, Entity entity) {
      if (this.shouldBlatantInhibit(this.getEntityId(packet))) {
         if (entity instanceof EndCrystalEntity crystal) {
            this.removeCrystal(crystal);
         }

         event.cancel();
      } else if (entity instanceof EndCrystalEntity crystal) {
         if (!this.cantBreakCrystal()) {
            this.markInhibited(crystal.method_5628());
            this.markNearbyCrystalsDead(crystal.method_23317(), crystal.method_23318(), crystal.method_23321());
            this.removeCrystal(crystal);
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.@NotNull Receive event) {
      if (!fullNullCheck()) {
         if (this.blatant.getValue()) {
            this.pruneInhibitedCrystals();
            if (event.getPacket() instanceof ExplosionS2CPacket explosion) {
               this.markNearbyCrystalsDead(explosion.getX(), explosion.getY(), explosion.getZ());
            }
         }
      }
   }

   private void removeCrystal(@NotNull Entity entity) {
      entity.kill();
      entity.method_31745(RemovalReason.KILLED);
      entity.onRemoved();
   }

   public boolean isAttackInhibited(Entity entity) {
      if (!fullNullCheck() && !this.isOff() && this.blatant.getValue() && entity instanceof EndCrystalEntity crystal) {
         this.pruneInhibitedCrystals();
         if (!this.isInhibited(crystal.method_5628())) {
            return false;
         }

         this.removeCrystal(crystal);
         return true;
      } else {
         return false;
      }
   }

   public boolean shouldIgnoreForPlacement(Entity entity) {
      if (!fullNullCheck() && !this.isOff() && this.blatant.getValue() && entity instanceof EndCrystalEntity crystal) {
         this.pruneInhibitedCrystals();
         return this.isInhibited(crystal.method_5628());
      } else {
         return false;
      }
   }

   private void markNearbyCrystalsDead(double x, double y, double z) {
      long time = System.currentTimeMillis();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity crystal && crystal.method_5649(x, y, z) <= 144.0 && !this.isInhibited(crystal.method_5628())) {
            this.inhibitedCrystals.put(crystal.method_5628(), time);
         }
      }
   }

   private void markInhibited(int entityId) {
      this.inhibitedCrystals.put(entityId, System.currentTimeMillis());
   }

   private void pruneInhibitedCrystals() {
      long now = System.currentTimeMillis();
      this.inhibitedCrystals.entrySet().removeIf(entry -> now - entry.getValue() > this.getBlatantTimeMs());
   }

   private boolean shouldBlatantInhibit(int entityId) {
      return this.blatant.getValue() && this.isInhibited(entityId);
   }

   private boolean isInhibited(int entityId) {
      return this.inhibitedCrystals.containsKey(entityId);
   }

   private long getBlatantTimeMs() {
      return Math.max(150L, Managers.SERVER.getPing() * 2L);
   }

   private int getEntityId(@NotNull PlayerInteractEntityC2SPacket packet) {
      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      packet.write(packetBuf);
      return packetBuf.readVarInt();
   }

   private boolean cantBreakCrystal() {
      StatusEffectInstance weakness = mc.player.method_6112(StatusEffects.WEAKNESS);
      StatusEffectInstance strength = mc.player.method_6112(StatusEffects.STRENGTH);
      return weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier()) && !this.isTool(mc.player.method_6047());
   }

   private boolean isTool(@NotNull ItemStack stack) {
      return stack.getItem() instanceof SwordItem
         || stack.getItem() instanceof PickaxeItem
         || stack.getItem() instanceof AxeItem
         || stack.getItem() instanceof ShovelItem;
   }
}
