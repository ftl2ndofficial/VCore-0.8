package vcore.features.modules.combat;

import io.netty.buffer.Unpooled;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IClientPlayerEntity;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.render.Render3DEngine;

public final class Criticals extends Module {
   public final Setting<Criticals.Mode> mode = new Setting<>("Mode", Criticals.Mode.UpdatedNCP);
   public final Setting<Integer> blinkDelayMin = new Setting<>("DelayMin", 300, 0, 1000, v -> this.mode.is(Criticals.Mode.Blink));
   public final Setting<Integer> blinkDelayMax = new Setting<>("DelayMax", 600, 0, 1000, v -> this.mode.is(Criticals.Mode.Blink));
   public final Setting<Float> blinkRange = new Setting<>("Range", 4.0F, 0.0F, 10.0F, v -> this.mode.is(Criticals.Mode.Blink)).step(0.1F);
   public final Setting<Boolean> blinkVisual = new Setting<>("Visual", true, v -> this.mode.is(Criticals.Mode.Blink));
   public final Setting<ColorSetting> blinkLineColor = new Setting<>(
      "Line", new ColorSetting(new Color(0, 128, 255, 255)), v -> this.mode.is(Criticals.Mode.Blink) && this.blinkVisual.getValue()
   );
   public final Setting<ColorSetting> blinkBoxColor = new Setting<>(
      "BoxColor", new ColorSetting(new Color(36, 32, 147, 87)), v -> this.mode.is(Criticals.Mode.Blink) && this.blinkVisual.getValue()
   );
   public static boolean cancelCrit;
   private static final boolean CANCEL_PACKETS = true;
   private Vec3d freezePos = Vec3d.ZERO;
   private final Deque<Criticals.QueuedPacket> blinkPackets = new ArrayDeque<>();
   private final Object blinkPacketLock = new Object();
   private int nextBlinkDelay = this.getRandomBlinkDelay();
   private boolean blinkEnemyInRange;
   private boolean blinkInState;

   public Criticals() {
      super("Criticals", "Makes every hit critical.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.resetAirStuck();
      this.blinkEnemyInRange = false;
      this.blinkInState = false;
      this.flushBlinkPackets();
   }

   @Override
   public void onLogout() {
      this.resetAirStuck();
      this.blinkEnemyInRange = false;
      this.blinkInState = false;
      this.clearBlinkPackets();
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.mode.is(Criticals.Mode.Blink) && this.blinkVisual.getValue() && this.hasBlinkPackets() && mc.player != null) {
         this.renderBlinkLine();
         this.renderBlinkEsp(stack);
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.@NotNull Send event) {
      if (event.getPacket() instanceof PlayerMoveC2SPacket && this.isAirStuckActive()) {
         event.cancel();
      } else if (!this.handleBlinkPacket(event)) {
         if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && getInteractType(packet) == Criticals.InteractType.ATTACK) {
            Entity ent = getEntity(packet);
            if (ent == null || ent instanceof EndCrystalEntity || cancelCrit) {
               return;
            }

            this.doCrit();
         }
      }
   }

   @EventHandler
   public void onTick(EventTick event) {
      if (fullNullCheck()) {
         this.blinkEnemyInRange = false;
         this.blinkInState = false;
         this.clearBlinkPackets();
         this.resetAirStuck();
      } else {
         this.updateBlinkState();
         if (!this.shouldAirStuck()) {
            this.resetAirStuck();
         } else {
            if (this.freezePos == Vec3d.ZERO) {
               this.freezePos = mc.player.method_19538();
            }

            ClientPlayerEntity player = mc.player;
            if (player == null) {
               this.resetAirStuck();
            } else {
               player.method_18800(0.0, 0.0, 0.0);
               if (player.input != null) {
                  player.input.movementForward = 0.0F;
                  player.input.movementSideways = 0.0F;
               }

               player.method_23327(player.method_23317(), this.freezePos.y, player.method_23321());
            }
         }
      }
   }

   public void doCrit() {
      if (!this.isDisabled() && mc.player != null && mc.world != null) {
         if (!this.mode.is(Criticals.Mode.AirStuck) && !this.mode.is(Criticals.Mode.Blink)) {
            if ((mc.player.method_24828() || mc.player.method_31549().flying || this.mode.is(Criticals.Mode.Grim))
               && !mc.player.method_5771()
               && !mc.player.method_5869()) {
               switch ((Criticals.Mode)this.mode.getValue()) {
                  case Grim:
                     if (!mc.player.method_24828()) {
                        this.critPacket(-1.0E-6, true);
                     }
                  case AirStuck:
                  case Blink:
                  default:
                     break;
                  case UpdatedNCP:
                     this.critPacket(2.71875E-7, false);
                     this.critPacket(0.0, false);
               }
            }
         }
      }
   }

   private void critPacket(double yDelta, boolean full) {
      if (!full) {
         this.sendPacket(new PositionAndOnGround(mc.player.method_23317(), mc.player.method_23318() + yDelta, mc.player.method_23321(), false));
      } else {
         this.sendPacket(
            new Full(
               mc.player.method_23317(),
               mc.player.method_23318() + yDelta,
               mc.player.method_23321(),
               ((IClientPlayerEntity)mc.player).getLastYaw(),
               ((IClientPlayerEntity)mc.player).getLastPitch(),
               false
            )
         );
      }
   }

   public static Entity getEntity(@NotNull PlayerInteractEntityC2SPacket packet) {
      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      packet.write(packetBuf);
      return mc.world.method_8469(packetBuf.readVarInt());
   }

   public static Criticals.InteractType getInteractType(@NotNull PlayerInteractEntityC2SPacket packet) {
      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      packet.write(packetBuf);
      packetBuf.readVarInt();
      return (Criticals.InteractType)packetBuf.readEnumConstant(Criticals.InteractType.class);
   }

   private boolean shouldAirStuck() {
      if (!this.mode.is(Criticals.Mode.AirStuck) || mc.player == null || mc.world == null) {
         return false;
      } else if (!ModuleManager.aura.isEnabled() || Aura.target == null) {
         return false;
      } else if (ModuleManager.aura.pauseWhileEating.getValue() && this.isConsumingItem()) {
         return false;
      } else if (mc.player.method_6128()) {
         return false;
      } else if (mc.player.method_24828()) {
         return false;
      } else {
         return mc.player.field_6017 <= 0.0F ? false : mc.player.method_5739(Aura.target) <= ModuleManager.aura.attackRange.getValue();
      }
   }

   private boolean isAirStuckActive() {
      return this.mode.is(Criticals.Mode.AirStuck) && this.freezePos != Vec3d.ZERO;
   }

   private void resetAirStuck() {
      this.freezePos = Vec3d.ZERO;
   }

   private boolean isConsumingItem() {
      if (mc.player != null && mc.player.method_6115()) {
         UseAction useAction = mc.player.method_6030().getUseAction();
         return useAction == UseAction.EAT || useAction == UseAction.DRINK;
      } else {
         return false;
      }
   }

   private void updateBlinkState() {
      if (this.mode.is(Criticals.Mode.Blink) && mc.player != null && mc.world != null) {
         this.blinkEnemyInRange = this.hasBlinkEnemyInRange();
         boolean shouldQueue = this.shouldBlinkQueuePackets();
         boolean delayExpired = shouldQueue && this.isBlinkDelayExpired();
         this.blinkInState = shouldQueue && !delayExpired;
         if (!shouldQueue) {
            if (this.hasBlinkPackets()) {
               this.flushBlinkPackets();
            }
         } else {
            if (delayExpired) {
               this.nextBlinkDelay = this.getRandomBlinkDelay();
               if (this.hasBlinkPackets()) {
                  this.flushBlinkPackets();
               }

               this.blinkInState = false;
            }
         }
      } else {
         this.blinkEnemyInRange = false;
         this.blinkInState = false;
         if (this.hasBlinkPackets()) {
            this.flushBlinkPackets();
         }
      }
   }

   private boolean handleBlinkPacket(PacketEvent.@NotNull Send event) {
      if (!this.mode.is(Criticals.Mode.Blink)) {
         this.blinkInState = false;
         if (this.hasBlinkPackets()) {
            this.flushBlinkPackets();
         }

         return false;
      } else if (mc.player != null && mc.world != null) {
         boolean shouldQueue = this.shouldBlinkQueuePackets();
         if (!shouldQueue) {
            this.blinkInState = false;
            if (this.hasBlinkPackets()) {
               this.flushBlinkPackets();
            }

            return false;
         } else if (this.isBlinkDelayExpired()) {
            this.nextBlinkDelay = this.getRandomBlinkDelay();
            this.blinkInState = false;
            if (this.hasBlinkPackets()) {
               this.flushBlinkPackets();
            }

            return false;
         } else {
            Packet<?> packet = event.getPacket();
            if (this.shouldPassBlinkPacket(packet)) {
               this.blinkInState = true;
               return false;
            } else {
               this.blinkInState = true;
               event.cancel();
               this.queueBlinkPacket(new Criticals.QueuedPacket(packet, System.currentTimeMillis()));
               return true;
            }
         }
      } else {
         this.blinkInState = false;
         this.blinkEnemyInRange = false;
         this.clearBlinkPackets();
         return false;
      }
   }

   private boolean shouldBlinkQueuePackets() {
      return this.mode.is(Criticals.Mode.Blink) && this.blinkEnemyInRange && !this.wouldDoBlinkCriticalHit(true);
   }

   private boolean wouldDoBlinkCriticalHit(boolean ignoreSprint) {
      return this.canDoBlinkCriticalHit(false, ignoreSprint) && mc.player.field_6017 > 0.0F;
   }

   private boolean canDoBlinkCriticalHit(boolean ignoreOnGround, boolean ignoreSprint) {
      return this.allowsBlinkCriticalHit(ignoreOnGround) && mc.player.method_7261(0.5F) > 0.9F && (!mc.player.method_5624() || ignoreSprint);
   }

   private boolean allowsBlinkCriticalHit(boolean ignoreOnGround) {
      return mc.player == null
         ? false
         : !ModuleManager.flight.isEnabled()
            && !mc.player.method_5771()
            && !mc.player.method_5799()
            && !mc.player.method_5869()
            && !mc.player.method_5765()
            && !Managers.PLAYER.isInWeb()
            && !mc.player.method_6059(StatusEffects.LEVITATION)
            && !mc.player.method_6059(StatusEffects.BLINDNESS)
            && !mc.player.method_6059(StatusEffects.SLOW_FALLING)
            && !mc.player.method_6101()
            && !mc.player.method_5740()
            && !mc.player.method_6115()
            && !mc.player.method_31549().flying
            && (ignoreOnGround || !mc.player.method_24828());
   }

   private boolean hasBlinkEnemyInRange() {
      if (mc.player != null && mc.world != null) {
         if (this.isBlinkTargetInRange(Aura.target, this.blinkRange.getValue())) {
            return true;
         }

         for (Entity entity : mc.world.getEntities()) {
            if (this.isValidBlinkEnemy(entity) && this.isBlinkTargetInRange(entity, this.blinkRange.getValue())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isBlinkTargetInRange(Entity entity, float range) {
      if (entity != null && entity != mc.player && entity.isAlive()) {
         Vec3d eyePos = mc.player.method_33571();
         Box box = entity.method_5829();
         Vec3d closestPoint = new Vec3d(
            MathHelper.clamp(eyePos.x, box.minX, box.maxX), MathHelper.clamp(eyePos.y, box.minY, box.maxY), MathHelper.clamp(eyePos.z, box.minZ, box.maxZ)
         );
         return eyePos.squaredDistanceTo(closestPoint) <= range * range;
      } else {
         return false;
      }
   }

   private boolean isValidBlinkEnemy(Entity entity) {
      if (!(entity instanceof LivingEntity) || entity == mc.player || !entity.isAlive()) {
         return false;
      } else if (!(entity instanceof ArmorStandEntity) && !(entity instanceof CatEntity)) {
         return entity instanceof PlayerEntity player && Managers.FRIEND.isFriend(player)
            ? false
            : !(entity instanceof PlayerEntity player && player.isCreative());
      } else {
         return false;
      }
   }

   private boolean isBlinkDelayExpired() {
      Criticals.QueuedPacket firstPacket = this.peekFirstBlinkPacket();
      return firstPacket == null ? false : System.currentTimeMillis() - firstPacket.timestamp() >= this.nextBlinkDelay;
   }

   private int getRandomBlinkDelay() {
      int min = Math.min(this.blinkDelayMin.getValue(), this.blinkDelayMax.getValue());
      int max = Math.max(this.blinkDelayMin.getValue(), this.blinkDelayMax.getValue());
      return ThreadLocalRandom.current().nextInt(min, max + 1);
   }

   private boolean shouldPassBlinkPacket(Packet<?> packet) {
      return packet instanceof PlayerInteractBlockC2SPacket
         || packet instanceof PlayerActionC2SPacket
         || packet instanceof UpdateSignC2SPacket
         || packet instanceof PlayerInteractEntityC2SPacket
         || packet instanceof HandSwingC2SPacket
         || packet instanceof ResourcePackStatusC2SPacket;
   }

   private void flushBlinkPackets() {
      List<Criticals.QueuedPacket> packetsToFlush;
      synchronized (this.blinkPacketLock) {
         packetsToFlush = new ArrayList<>(this.blinkPackets);
         this.blinkPackets.clear();
      }

      for (Criticals.QueuedPacket queuedPacket : packetsToFlush) {
         this.sendPacketSilent(queuedPacket.packet());
      }
   }

   private void renderBlinkLine() {
      Color color = this.blinkLineColor.getValue().getColorObject();
      if (color.getAlpha() > 0) {
         List<Vec3d> positions = this.getQueuedBlinkPositions();
         if (positions.size() >= 2) {
            for (int i = 0; i < positions.size() - 1; i++) {
               Render3DEngine.drawLine(positions.get(i), positions.get(i + 1), color);
            }
         }
      }
   }

   private void renderBlinkEsp(MatrixStack stack) {
      Vec3d pos = this.getFirstBlinkPosition();
      if (pos != null) {
         Entity player = mc.player;
         EntityDimensions dimensions = player.getDimensions(player.getPose());
         double halfWidth = dimensions.width() / 2.0;
         Box box = new Box(pos.x - halfWidth, pos.y, pos.z - halfWidth, pos.x + halfWidth, pos.y + dimensions.height(), pos.z + halfWidth).expand(0.05);
         Render3DEngine.drawFilledBox(stack, box, this.blinkBoxColor.getValue().getColorObject());
      }
   }

   @NotNull
   private List<Vec3d> getQueuedBlinkPositions() {
      List<Vec3d> positions = new ArrayList<>();

      for (Criticals.QueuedPacket snapshot : this.getBlinkPacketSnapshot()) {
         if (snapshot.packet() instanceof PlayerMoveC2SPacket movePacket && movePacket.changesPosition()) {
            positions.add(
               new Vec3d(movePacket.getX(mc.player.method_23317()), movePacket.getY(mc.player.method_23318()), movePacket.getZ(mc.player.method_23321()))
            );
         }
      }

      return positions;
   }

   private Vec3d getFirstBlinkPosition() {
      for (Criticals.QueuedPacket snapshot : this.getBlinkPacketSnapshot()) {
         if (snapshot.packet() instanceof PlayerMoveC2SPacket movePacket && movePacket.changesPosition()) {
            return new Vec3d(movePacket.getX(mc.player.method_23317()), movePacket.getY(mc.player.method_23318()), movePacket.getZ(mc.player.method_23321()));
         }
      }

      return null;
   }

   private boolean hasBlinkPackets() {
      synchronized (this.blinkPacketLock) {
         return !this.blinkPackets.isEmpty();
      }
   }

   private void clearBlinkPackets() {
      synchronized (this.blinkPacketLock) {
         this.blinkPackets.clear();
      }
   }

   private void queueBlinkPacket(Criticals.QueuedPacket queuedPacket) {
      synchronized (this.blinkPacketLock) {
         this.blinkPackets.addLast(queuedPacket);
      }
   }

   private Criticals.QueuedPacket peekFirstBlinkPacket() {
      synchronized (this.blinkPacketLock) {
         return this.blinkPackets.peekFirst();
      }
   }

   @NotNull
   private List<Criticals.QueuedPacket> getBlinkPacketSnapshot() {
      synchronized (this.blinkPacketLock) {
         return new ArrayList<>(this.blinkPackets);
      }
   }

   public enum InteractType {
      INTERACT,
      ATTACK,
      INTERACT_AT;
   }

   public enum Mode {
      Grim,
      AirStuck,
      UpdatedNCP,
      Blink;
   }

   private record QueuedPacket(Packet<?> packet, long timestamp) {
   }
}
