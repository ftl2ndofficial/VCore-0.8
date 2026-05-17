package vcore.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Criticals;
import vcore.setting.Setting;

public final class AntiAttack extends Module {
   private final Setting<Boolean> friend = new Setting<>("Friend", true);
   private final Setting<Boolean> zoglin = new Setting<>("Zoglin", true);
   private final Setting<Boolean> villager = new Setting<>("Villager", false);
   private final Setting<Boolean> oneHp = new Setting<>("OneHp", false);
   private final Setting<Float> hp = new Setting<>("Hp", 1.0F, 0.0F, 20.0F, v -> this.oneHp.getValue());

   public AntiAttack() {
      super("AntiAttack", "Prevents unnecessary attacks.", Module.Category.PLAYER);
   }

   @EventHandler
   private void onPacketSend(PacketEvent.@NotNull Send e) {
      if (e.getPacket() instanceof PlayerInteractEntityC2SPacket pac) {
         Entity entity = Criticals.getEntity(pac);
         if (entity == null) {
            return;
         }

         if (Managers.FRIEND.isFriend(entity.method_5477().getString()) && this.friend.getValue()) {
            e.cancel();
         }

         if (entity instanceof ZombifiedPiglinEntity && this.zoglin.getValue()) {
            e.cancel();
         }

         if (entity instanceof VillagerEntity && this.villager.getValue()) {
            e.cancel();
         } else if (this.oneHp.getValue() && entity instanceof LivingEntity lent && lent.getHealth() <= this.hp.getValue()) {
            e.cancel();
         }
      }
   }
}
