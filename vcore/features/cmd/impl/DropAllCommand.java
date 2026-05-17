package vcore.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.NotNull;
import vcore.core.Managers;
import vcore.features.cmd.Command;

public class DropAllCommand extends Command {
   public DropAllCommand() {
      super("dropall", "Drop all items from inventory.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("legit").executes(context -> {
         Managers.ASYNC.run(() -> {
            for (int i = 5; i <= 45; i++) {
               mc.interactionManager.clickSlot(mc.player.field_7512.syncId, i, 1, SlotActionType.THROW, mc.player);

               try {
                  Thread.sleep(70L);
               } catch (InterruptedException e) {
                  throw new RuntimeException(e);
               }
            }

            mc.player.networkHandler.method_52787(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
         }, 1L);
         sendMessage("ok");
         return 1;
      }));
      builder.executes(context -> {
         for (int i = 5; i <= 45; i++) {
            mc.interactionManager.clickSlot(mc.player.field_7512.syncId, i, 1, SlotActionType.THROW, mc.player);
         }

         mc.player.networkHandler.method_52787(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
         sendMessage("ok");
         return 1;
      });
   }
}
