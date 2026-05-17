package vcore.utility.interfaces;

import vcore.features.modules.combat.Aura;

public interface IOtherClientPlayerEntity {
   void resolve(Aura.Resolver var1);

   void releaseResolver();
}
