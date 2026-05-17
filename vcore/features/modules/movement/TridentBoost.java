package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.MovementType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.events.impl.UseTridentEvent;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class TridentBoost extends Module {
   private final Setting<TridentBoost.Mode> mode = new Setting<>("Mode", TridentBoost.Mode.Motion);
   private final Setting<Float> factor = new Setting<>("Factor", 1.0F, 0.1F, 20.0F);
   public final Setting<Integer> cooldown = new Setting<>("Cooldown", 10, 0, 20);
   public final Setting<Boolean> anyWeather = new Setting<>("AnyWeather", true);

   public TridentBoost() {
      super("TridentBoost", "Boost with trident.", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onUseTrident(UseTridentEvent e) {
      if (mc.player.method_6048() >= this.cooldown.getValue()) {
         float j = EnchantmentHelper.getTridentSpinAttackStrength(mc.player.method_6030(), mc.player);
         if ((this.anyWeather.getValue() || mc.player.method_5721()) && j > 0.0F) {
            float f = mc.player.method_36454();
            float g = mc.player.method_36455();
            float speedX = -MathHelper.sin(f * (float) (Math.PI / 180.0)) * MathHelper.cos(g * (float) (Math.PI / 180.0));
            float speedY = -MathHelper.sin(g * (float) (Math.PI / 180.0));
            float speedZ = MathHelper.cos(f * (float) (Math.PI / 180.0)) * MathHelper.cos(g * (float) (Math.PI / 180.0));
            float plannedSpeed = MathHelper.sqrt(speedX * speedX + speedY * speedY + speedZ * speedZ);
            float n = this.mode.is(TridentBoost.Mode.Factor) ? this.factor.getValue() * 3.0F * ((1.0F + j) / 4.0F) : this.factor.getValue();
            speedX *= n / plannedSpeed;
            speedY *= n / plannedSpeed;
            speedZ *= n / plannedSpeed;
            mc.player.method_5762(speedX, speedY, speedZ);
            mc.player.method_40126(20, 8.0F, mc.player.method_6030());
            if (mc.player.method_24828()) {
               mc.player.method_5784(MovementType.SELF, new Vec3d(0.0, 1.1999999F, 0.0));
            }

            RegistryEntry<SoundEvent> registryEntry = EnchantmentHelper.getEffect(mc.player.method_6030(), EnchantmentEffectComponentTypes.TRIDENT_SOUND)
               .orElse(SoundEvents.ITEM_TRIDENT_THROW);
            mc.world.method_43129(null, mc.player, (SoundEvent)registryEntry.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
         }
      }

      e.cancel();
   }

   private enum Mode {
      Motion,
      Factor;
   }
}
