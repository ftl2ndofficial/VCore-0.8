package vcore.injection;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class MixinScoreBoard {
   @Final
   @Shadow
   private Object2ObjectMap<String, Team> field_1427;

   @Inject(method = "removeScoreHolderFromTeam", at = @At("HEAD"), cancellable = true)
   public void removeScoreHolderFromTeamHook(String scoreHolderName, Team team, CallbackInfo ci) {
      ci.cancel();
      if (this.field_1427.get(scoreHolderName) == team) {
         this.field_1427.remove(scoreHolderName);
         team.method_1204().remove(scoreHolderName);
      }
   }
}
