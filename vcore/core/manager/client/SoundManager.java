package vcore.core.manager.client;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import vcore.core.manager.IManager;

public class SoundManager implements IManager {
   public final Identifier KEYRELEASE_SOUND = Identifier.of("vcore:keyrelease");
   public SoundEvent KEYRELEASE_SOUNDEVENT = SoundEvent.of(this.KEYRELEASE_SOUND);
   public final Identifier NURSULTAN_ON_SOUND = Identifier.of("vcore:nursultan_on");
   public SoundEvent NURSULTAN_ON_SOUNDEVENT = SoundEvent.of(this.NURSULTAN_ON_SOUND);
   public final Identifier NURSULTAN_OFF_SOUND = Identifier.of("vcore:nursultan_off");
   public SoundEvent NURSULTAN_OFF_SOUNDEVENT = SoundEvent.of(this.NURSULTAN_OFF_SOUND);
   public final Identifier NEWCODE_ON_SOUND = Identifier.of("vcore:newcode_on");
   public SoundEvent NEWCODE_ON_SOUNDEVENT = SoundEvent.of(this.NEWCODE_ON_SOUND);
   public final Identifier NEWCODE_OFF_SOUND = Identifier.of("vcore:newcode_off");
   public SoundEvent NEWCODE_OFF_SOUNDEVENT = SoundEvent.of(this.NEWCODE_OFF_SOUND);
   public final Identifier CATLAVAN_ON_SOUND = Identifier.of("vcore:catlavan_on");
   public SoundEvent CATLAVAN_ON_SOUNDEVENT = SoundEvent.of(this.CATLAVAN_ON_SOUND);
   public final Identifier CATLAVAN_OFF_SOUND = Identifier.of("vcore:catlavan_off");
   public SoundEvent CATLAVAN_OFF_SOUNDEVENT = SoundEvent.of(this.CATLAVAN_OFF_SOUND);
   public final Identifier CELESTIAL_ON_SOUND = Identifier.of("vcore:celestial_on");
   public SoundEvent CELESTIAL_ON_SOUNDEVENT = SoundEvent.of(this.CELESTIAL_ON_SOUND);
   public final Identifier CELESTIAL_OFF_SOUND = Identifier.of("vcore:celestial_off");
   public SoundEvent CELESTIAL_OFF_SOUNDEVENT = SoundEvent.of(this.CELESTIAL_OFF_SOUND);
   public final Identifier SWIPEIN_SOUND = Identifier.of("vcore:swipein");
   public SoundEvent SWIPEIN_SOUNDEVENT = SoundEvent.of(this.SWIPEIN_SOUND);
   public final Identifier SWIPEOUT_SOUND = Identifier.of("vcore:swipeout");
   public SoundEvent SWIPEOUT_SOUNDEVENT = SoundEvent.of(this.SWIPEOUT_SOUND);

   public void registerSounds() {
      Registry.register(Registries.SOUND_EVENT, this.KEYRELEASE_SOUND, this.KEYRELEASE_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.SWIPEIN_SOUND, this.SWIPEIN_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.SWIPEOUT_SOUND, this.SWIPEOUT_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.NEWCODE_ON_SOUND, this.NEWCODE_ON_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.NEWCODE_OFF_SOUND, this.NEWCODE_OFF_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.CATLAVAN_ON_SOUND, this.CATLAVAN_ON_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.CATLAVAN_OFF_SOUND, this.CATLAVAN_OFF_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.CELESTIAL_ON_SOUND, this.CELESTIAL_ON_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.CELESTIAL_OFF_SOUND, this.CELESTIAL_OFF_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.NURSULTAN_ON_SOUND, this.NURSULTAN_ON_SOUNDEVENT);
      Registry.register(Registries.SOUND_EVENT, this.NURSULTAN_OFF_SOUND, this.NURSULTAN_OFF_SOUNDEVENT);
   }

   public void playEnable() {
      if (!ModuleManager.ClientSound.isDisabled()) {
         switch (ModuleManager.ClientSound.onOffSound.getValue().name()) {
            case "Newcode":
               this.playSound(this.NEWCODE_ON_SOUNDEVENT);
               break;
            case "Catlavan":
               this.playSound(this.CATLAVAN_ON_SOUNDEVENT);
               break;
            case "Celestial":
               this.playSound(this.CELESTIAL_ON_SOUNDEVENT);
               break;
            case "Nursultan":
               this.playSound(this.NURSULTAN_ON_SOUNDEVENT);
         }
      }
   }

   public void playDisable() {
      if (!ModuleManager.ClientSound.isDisabled()) {
         switch (ModuleManager.ClientSound.onOffSound.getValue().name()) {
            case "Newcode":
               this.playSound(this.NEWCODE_OFF_SOUNDEVENT);
               break;
            case "Catlavan":
               this.playSound(this.CATLAVAN_OFF_SOUNDEVENT);
               break;
            case "Celestial":
               this.playSound(this.CELESTIAL_OFF_SOUNDEVENT);
               break;
            case "Nursultan":
               this.playSound(this.NURSULTAN_OFF_SOUNDEVENT);
         }
      }
   }

   public void playSound(SoundEvent sound) {
      if (!ModuleManager.ClientSound.isDisabled()) {
         if (mc.player != null && mc.world != null) {
            mc.world
               .method_8396(
                  mc.player, mc.player.method_24515(), sound, SoundCategory.BLOCKS, ModuleManager.ClientSound.volume.getValue().intValue() / 100.0F, 1.0F
               );
         }
      }
   }

   public void playBoolean() {
      this.playSound(this.KEYRELEASE_SOUNDEVENT);
   }

   public void playSwipeIn() {
      this.playSound(this.SWIPEIN_SOUNDEVENT);
   }

   public void playSwipeOut() {
      this.playSound(this.SWIPEOUT_SOUNDEVENT);
   }
}
