package vcore.injection;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Nullables;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.features.modules.Module;

@Mixin(PlayerListHud.class)
public class MixinPlayerListHud {
   private static final Comparator<Object> ENTRY_ORDERING = Comparator.<Object>comparingInt(
         entry -> ((PlayerListEntry)entry).getGameMode() == GameMode.SPECTATOR ? 1 : 0
      )
      .thenComparing(entry -> (String)Nullables.mapOrElse(((PlayerListEntry)entry).getScoreboardTeam(), Team::method_1197, ""))
      .thenComparing(entry -> ((PlayerListEntry)entry).getProfile().getName(), String::compareToIgnoreCase);

   @Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
   private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
      if (!Vcore.isFuturePresent()) {
         cir.setReturnValue(Module.mc.player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).limit(80L).toList());
      }
   }
}
