package vcore.features.cmd.impl;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import vcore.features.cmd.Command;
import vcore.utility.player.InventoryUtility;

public class EClipCommand extends Command {
   public EClipCommand() {
      super("eclip", "Elytra clip vertically.");
   }

   @Override
   public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(((LiteralArgumentBuilder)literal("bedrock").executes(context -> {
         this.execute(-((float)mc.player.method_23318()) - 3.0F);
         return 1;
      })).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
         float y = -((float)mc.player.method_23318()) - 3.0F;
         if (y == 0.0F) {
            y = (Float)context.getArgument("number", Float.class);
         }

         this.execute(y);
         return 1;
      })));
      builder.then(((LiteralArgumentBuilder)literal("down").executes(context -> {
         float y = 0.0F;

         for (int i = 1; i < 255; i++) {
            if (mc.world.method_8320(BlockPos.ofFloored(mc.player.method_19538()).add(0, -i, 0)) == Blocks.AIR.getDefaultState()) {
               y = -i - 1;
               break;
            }

            if (mc.world.method_8320(BlockPos.ofFloored(mc.player.method_19538()).add(0, -i, 0)) == Blocks.BEDROCK.getDefaultState()) {
               sendMessage(Formatting.RED + "You can only teleport under bedrock");
               sendMessage(Formatting.RED + " eclip bedrock");
               return 1;
            }
         }

         this.execute(y);
         return 1;
      })).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
         float y = 0.0F;

         for (int i = 1; i < 255; i++) {
            if (mc.world.method_8320(BlockPos.ofFloored(mc.player.method_19538()).add(0, -i, 0)) == Blocks.AIR.getDefaultState()) {
               y = -i - 1;
               break;
            }

            if (mc.world.method_8320(BlockPos.ofFloored(mc.player.method_19538()).add(0, -i, 0)) == Blocks.BEDROCK.getDefaultState()) {
               sendMessage(Formatting.RED + "You can only teleport under bedrock");
               sendMessage(Formatting.RED + " eclip bedrock");
               return 1;
            }
         }

         if (y == 0.0F) {
            y = (Float)context.getArgument("number", Float.class);
         }

         this.execute(y);
         return 1;
      })));
      builder.then(((LiteralArgumentBuilder)literal("up").executes(context -> {
         float y = 0.0F;

         for (int i = 4; i < 255; i++) {
            if (mc.world.method_8320(BlockPos.ofFloored(mc.player.method_19538()).add(0, i, 0)) == Blocks.AIR.getDefaultState()) {
               y = i + 1;
               break;
            }
         }

         this.execute(y);
         return 1;
      })).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
         float y = 0.0F;

         for (int i = 4; i < 255; i++) {
            if (mc.world.method_8320(BlockPos.ofFloored(mc.player.method_19538()).add(0, i, 0)) == Blocks.AIR.getDefaultState()) {
               y = i + 1;
               break;
            }
         }

         if (y == 0.0F) {
            y = (Float)context.getArgument("number", Float.class);
         }

         this.execute(y);
         return 1;
      })));
   }

   private void execute(float y) {
      int elytra;
      if ((elytra = InventoryUtility.findItemInInventory(Items.ELYTRA).slot()) == -1) {
         sendMessage(Formatting.RED + "You need an elytra in your inventory");
      } else {
         if (elytra != -2) {
            mc.interactionManager.clickSlot(0, elytra, 1, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
         }

         mc.player.networkHandler.method_52787(new PositionAndOnGround(mc.player.method_23317(), mc.player.method_23318(), mc.player.method_23321(), false));
         mc.player.networkHandler.method_52787(new PositionAndOnGround(mc.player.method_23317(), mc.player.method_23318(), mc.player.method_23321(), false));
         mc.player.networkHandler.method_52787(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         mc.player
            .networkHandler
            .method_52787(new PositionAndOnGround(mc.player.method_23317(), mc.player.method_23318() + y, mc.player.method_23321(), false));
         mc.player.networkHandler.method_52787(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         if (elytra != -2) {
            mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, elytra, 1, SlotActionType.PICKUP, mc.player);
         }

         mc.player.method_5814(mc.player.method_23317(), mc.player.method_23318() + y, mc.player.method_23321());
      }
   }
}
