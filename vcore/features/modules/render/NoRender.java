package vcore.features.modules.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import vcore.core.Managers;
import vcore.events.impl.EventSync;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.gui.notification.Notification;
import vcore.setting.Setting;
import vcore.setting.impl.SettingGroup;

public class NoRender extends Module {
   public final Setting<Boolean> auto = new Setting<>("Auto", false);
   public final Setting<SettingGroup> screenGroup = new Setting<>("Screen", new SettingGroup(false, 0));
   public final Setting<Boolean> antiTitle = new Setting<>("AntiTitle", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> blindness = new Setting<>("Blindness", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> blockOverlay = new Setting<>("BlockOverlay", true).addToGroup(this.screenGroup);
   public final Setting<Boolean> bossbar = new Setting<>("Bossbar", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> fireOverlay = new Setting<>("FireOverlay", true).addToGroup(this.screenGroup);
   public final Setting<Boolean> disableGuiBackGround = new Setting<>("GuiBackGround", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> hotbarItemName = new Setting<>("HotbarItemName", true).addToGroup(this.screenGroup);
   public final Setting<Boolean> hurtCam = new Setting<>("HurtCam", true).addToGroup(this.screenGroup);
   public final Setting<Boolean> nausea = new Setting<>("Nausea", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> noBob = new Setting<>("NoBob", true).addToGroup(this.screenGroup);
   public final Setting<Boolean> noScoreBoard = new Setting<>("NoScoreBoard", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> portal = new Setting<>("Portal", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> potions = new Setting<>("Potions", false).addToGroup(this.screenGroup);
   public final Setting<Boolean> vignette = new Setting<>("Vignette", true).addToGroup(this.screenGroup);
   public final Setting<Boolean> waterOverlay = new Setting<>("WaterOverlay", true).addToGroup(this.screenGroup);
   public final Setting<SettingGroup> entitiesGroup = new Setting<>("Entities", new SettingGroup(false, 0));
   public final Setting<Boolean> armor = new Setting<>("Armor", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> arrows = new Setting<>("Arrows", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> crystals = new Setting<>("Crystals", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> eggs = new Setting<>("Eggs", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> fireworks = new Setting<>("Fireworks", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> elderGuardian = new Setting<>("Guardian", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> items = new Setting<>("Items", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> noArmorStands = new Setting<>("NoArmorStands", false).addToGroup(this.entitiesGroup);
   public final Setting<Boolean> xp = new Setting<>("Xp", false).addToGroup(this.entitiesGroup);
   public final Setting<SettingGroup> particlesGroup = new Setting<>("Particles", new SettingGroup(false, 0));
   public final Setting<Boolean> breakParticles = new Setting<>("BreakParticles", true).addToGroup(this.particlesGroup);
   public final Setting<Boolean> campFire = new Setting<>("CampFire", false).addToGroup(this.particlesGroup);
   public final Setting<Boolean> explosions = new Setting<>("Explosions", true).addToGroup(this.particlesGroup);
   public final Setting<SettingGroup> worldGroup = new Setting<>("World", new SettingGroup(false, 0));
   public final Setting<Boolean> darkness = new Setting<>("Darkness", false).addToGroup(this.worldGroup);
   public final Setting<Boolean> fireEntity = new Setting<>("FireOnEntity", false).addToGroup(this.worldGroup);
   public final Setting<Boolean> fog = new Setting<>("Fog", true).addToGroup(this.worldGroup);
   public final Setting<Boolean> noWeather = new Setting<>("NoWeather", true).addToGroup(this.worldGroup);
   public final Setting<Boolean> signText = new Setting<>("SignText", false).addToGroup(this.worldGroup);
   public final Setting<Boolean> spawnerEntity = new Setting<>("SpawnerEntity", false).addToGroup(this.worldGroup);
   public final Setting<SettingGroup> otherGroup = new Setting<>("Other", new SettingGroup(false, 0));
   public final Setting<Boolean> antiPlayerCollision = new Setting<>("AntiPlayerCollision", false).addToGroup(this.otherGroup);
   private int potionCouter;
   private int xpCounter;
   private int arrowCounter;
   private int itemsCounter;

   public NoRender() {
      super("NoRender", "Removes laggy elements.", Module.Category.RENDER);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof TitleS2CPacket && this.antiTitle.getValue()) {
         e.cancel();
      }
   }

   @EventHandler
   public void onSync(EventSync e) {
      for (Entity ent : Managers.ASYNC.getAsyncEntities()) {
         if (ent instanceof PotionEntity) {
            this.potionCouter++;
            if (this.potions.getValue()) {
               mc.world.removeEntity(ent.method_5628(), RemovalReason.KILLED);
            }
         }

         if (ent instanceof ExperienceBottleEntity) {
            this.xpCounter++;
            if (this.xp.getValue()) {
               mc.world.removeEntity(ent.method_5628(), RemovalReason.KILLED);
            }
         }

         if (ent instanceof EndCrystalEntity && this.crystals.getValue()) {
            mc.world.removeEntity(ent.method_5628(), RemovalReason.KILLED);
         }

         if (ent instanceof ArrowEntity) {
            this.arrowCounter++;
            if (this.arrows.getValue()) {
               mc.world.removeEntity(ent.method_5628(), RemovalReason.KILLED);
            }
         }

         if (ent instanceof EggEntity && this.eggs.getValue()) {
            mc.world.removeEntity(ent.method_5628(), RemovalReason.KILLED);
         }

         if (ent instanceof ItemEntity) {
            this.itemsCounter++;
            if (this.items.getValue()) {
               mc.world.removeEntity(ent.method_5628(), RemovalReason.KILLED);
            }
         }

         if (ent instanceof ArmorStandEntity && this.noArmorStands.getValue()) {
            mc.world.removeEntity(ent.method_5628(), RemovalReason.KILLED);
         }
      }

      if (this.auto.getValue()) {
         if (this.arrowCounter > 64) {
            Managers.NOTIFICATION.publicity("NoRender", "Arrows limit reached! Removing...", 3, Notification.Type.SUCCESS);
         }

         if (this.itemsCounter > 16) {
            Managers.NOTIFICATION.publicity("NoRender", "Item limit reached! Removing...", 3, Notification.Type.SUCCESS);
         }

         if (this.xpCounter > 16) {
            Managers.NOTIFICATION.publicity("NoRender", "XP orbs limit reached! Removing...", 3, Notification.Type.SUCCESS);
         }

         if (this.potionCouter > 8) {
            Managers.NOTIFICATION.publicity("NoRender", "Potions limit reached! Removing...", 3, Notification.Type.SUCCESS);
         }

         List<Integer> toRemove = new ArrayList<>();

         for (Entity ent : Managers.ASYNC.getAsyncEntities()) {
            if (ent instanceof ArrowEntity && this.arrowCounter > 64) {
               toRemove.add(ent.method_5628());
            }

            if (ent instanceof ItemEntity && this.itemsCounter > 16) {
               toRemove.add(ent.method_5628());
            }

            if (ent instanceof ExperienceBottleEntity && this.xpCounter > 16) {
               toRemove.add(ent.method_5628());
            }

            if (ent instanceof PotionEntity && this.potionCouter > 8) {
               toRemove.add(ent.method_5628());
            }
         }

         try {
            toRemove.forEach(id -> mc.world.removeEntity(id, RemovalReason.KILLED));
         } catch (Exception var5) {
         }
      }

      this.arrowCounter = 0;
      this.itemsCounter = 0;
      this.potionCouter = 0;
      this.xpCounter = 0;
   }
}
