package vcore.utility.player;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import vcore.features.modules.Module;

public class PlayerEntityCopy extends OtherClientPlayerEntity {
   public PlayerEntityCopy() {
      super(Objects.requireNonNull(Module.mc.world), Objects.requireNonNull(Module.mc.player).method_7334());
      this.method_5878(Module.mc.player);
      this.method_3123();
      this.field_6011.set(field_7518, (Byte)Module.mc.player.method_5841().get(field_7518));
      this.method_5826(UUID.randomUUID());
   }

   public void spawn() {
      if (Module.mc.world != null) {
         this.method_31482();
         Module.mc.world.addEntity(this);
      }
   }

   public void deSpawn() {
      if (Module.mc.world != null) {
         Module.mc.world.removeEntity(this.method_5628(), RemovalReason.DISCARDED);
      }
   }
}
