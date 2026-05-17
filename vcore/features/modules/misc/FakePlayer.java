package vcore.features.modules.misc;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttack;
import vcore.events.impl.EventSync;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.TotemPopEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.world.ExplosionUtility;

public class FakePlayer extends Module {
   public static OtherClientPlayerEntity fakePlayer;
   private final Setting<Boolean> copyInventory = new Setting<>("CopyInventory", true);
   private final Setting<Boolean> autoTotem = new Setting<>("AutoTotem", true);
   private final Setting<String> name = new Setting<>("Name", "Fake Player");
   private int deathTime;

   public FakePlayer() {
      super("FakePlayer", "Fake player for testing PvP modules.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.fromString("66123666-6666-6666-6666-666666666600"), this.name.getValue()));
         this.copyPlayerState();
         if (this.copyInventory.getValue()) {
            fakePlayer.method_6122(Hand.MAIN_HAND, mc.player.method_6047().copy());
            fakePlayer.method_6122(Hand.OFF_HAND, mc.player.method_6079().copy());
            fakePlayer.method_31548().method_5447(36, mc.player.method_31548().method_5438(36).copy());
            fakePlayer.method_31548().method_5447(37, mc.player.method_31548().method_5438(37).copy());
            fakePlayer.method_31548().method_5447(38, mc.player.method_31548().method_5438(38).copy());
            fakePlayer.method_31548().method_5447(39, mc.player.method_31548().method_5438(39).copy());
         }

         mc.world.addEntity(fakePlayer);
         fakePlayer.method_6092(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 2));
         fakePlayer.method_6092(new StatusEffectInstance(StatusEffects.ABSORPTION, 9999, 4));
         fakePlayer.method_6092(new StatusEffectInstance(StatusEffects.RESISTANCE, 9999, 1));
      } else {
         this.disable();
      }
   }

   private void copyPlayerState() {
      fakePlayer.method_5719(mc.player);
      fakePlayer.field_5982 = mc.player.field_5982;
      fakePlayer.field_6004 = mc.player.field_6004;
      fakePlayer.field_6241 = mc.player.field_6241;
      fakePlayer.field_6259 = mc.player.field_6259;
      fakePlayer.field_6283 = mc.player.field_6283;
      fakePlayer.field_6220 = mc.player.field_6220;
      fakePlayer.method_18380(mc.player.method_18376());
      fakePlayer.method_5660(mc.player.method_5715());
      fakePlayer.method_5728(mc.player.method_5624());
      fakePlayer.method_5796(mc.player.method_5681());
      fakePlayer.method_24830(mc.player.method_24828());
      fakePlayer.method_18799(mc.player.method_18798());
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof ExplosionS2CPacket explosion && fakePlayer != null && fakePlayer.field_6235 == 0) {
         fakePlayer.method_48922(mc.world.method_48963().generic());
         fakePlayer.method_6033(
            fakePlayer.method_6032()
               + fakePlayer.method_6067()
               - ExplosionUtility.getAutoCrystalDamage(new Vec3d(explosion.getX(), explosion.getY(), explosion.getZ()), fakePlayer, 0, false)
         );
         if (fakePlayer.method_29504() && fakePlayer.method_6095(mc.world.method_48963().generic())) {
            this.handleTotemPop();
         }
      }
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (fakePlayer != null) {
         if (this.autoTotem.getValue() && fakePlayer.method_6079().getItem() != Items.TOTEM_OF_UNDYING) {
            fakePlayer.method_6122(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         }

         if (fakePlayer.method_29504()) {
            this.deathTime++;
            if (this.deathTime > 10) {
               this.disable();
            }
         }
      }
   }

   @EventHandler
   public void onAttack(EventAttack e) {
      if (fakePlayer != null && e.getEntity() == fakePlayer && fakePlayer.field_6235 == 0 && !e.isPre()) {
         mc.world
            .method_43128(
               mc.player,
               fakePlayer.method_23317(),
               fakePlayer.method_23318(),
               fakePlayer.method_23321(),
               SoundEvents.ENTITY_PLAYER_HURT,
               SoundCategory.PLAYERS,
               1.0F,
               1.0F
            );
         if (mc.player.field_6017 > 0.0F || ModuleManager.criticals.isEnabled()) {
            mc.world
               .method_43128(
                  mc.player,
                  fakePlayer.method_23317(),
                  fakePlayer.method_23318(),
                  fakePlayer.method_23321(),
                  SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                  SoundCategory.PLAYERS,
                  1.0F,
                  1.0F
               );
         }

         fakePlayer.method_48922(mc.world.method_48963().generic());
         if (ModuleManager.aura.getAttackCooldown() >= 0.85) {
            fakePlayer.method_6033(fakePlayer.method_6032() + fakePlayer.method_6067() - InventoryUtility.getHitDamage(mc.player.method_6047(), fakePlayer));
         } else {
            fakePlayer.method_6033(fakePlayer.method_6032() + fakePlayer.method_6067() - 1.0F);
         }

         if (fakePlayer.method_29504() && fakePlayer.method_6095(mc.world.method_48963().generic())) {
            this.handleTotemPop();
         }
      }
   }

   private void handleTotemPop() {
      fakePlayer.method_6033(10.0F);
      this.applyTotemStatus();
      String playerName = fakePlayer.method_5477().getString();
      int pops = Managers.COMBAT.popList.merge(playerName, 1, Integer::sum);
      Vcore.EVENT_BUS.post(new TotemPopEvent(fakePlayer, pops));
   }

   private void applyTotemStatus() {
      if (mc.player != null && mc.player.networkHandler != null) {
         new EntityStatusS2CPacket(fakePlayer, (byte)35).apply(mc.player.networkHandler);
      }
   }

   @Override
   public void onDisable() {
      if (fakePlayer != null) {
         Managers.COMBAT.popList.remove(fakePlayer.method_5477().getString());
         fakePlayer.method_5768();
         fakePlayer.method_31745(RemovalReason.KILLED);
         fakePlayer.method_36209();
         fakePlayer = null;
         this.deathTime = 0;
      }
   }
}
