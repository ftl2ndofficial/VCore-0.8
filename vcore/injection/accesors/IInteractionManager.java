package vcore.injection.accesors;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface IInteractionManager {
   @Accessor("currentBreakingProgress")
   float getCurBlockDamageMP();

   @Accessor("currentBreakingProgress")
   void setCurBlockDamageMP(float var1);

   @Invoker("syncSelectedSlot")
   void syncSlot();
}
