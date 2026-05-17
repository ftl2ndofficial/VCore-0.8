package vcore.features.modules.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import vcore.features.modules.Module;

public class FixHP extends Module {
   public FixHP() {
      super("FixHP", "Accurate HP for Matrix AntiCheat.", Module.Category.MISC);
   }

   public static float getHealth(PlayerEntity ent) {
      if (ent == null) {
         return 0.0F;
      }

      if (ent == mc.player) {
         return ent.method_6032();
      }

      if (mc.world != null && mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
         try {
            Scoreboard scoreboard = ent.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (objective == null) {
               return ent.method_6032();
            }

            ReadableScoreboardScore score = scoreboard.getScore(ent, objective);
            return score.getScore();
         } catch (Exception ignored) {
            return ent.method_6032();
         }
      } else {
         return ent.method_6032();
      }
   }
}
