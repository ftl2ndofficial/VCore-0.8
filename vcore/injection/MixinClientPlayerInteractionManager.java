package vcore.injection;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventAttackBlock;
import vcore.events.impl.EventBreakBlock;
import vcore.events.impl.EventClickSlot;
import vcore.features.modules.Module;
import vcore.features.modules.player.NoInteract;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
   @Shadow
   private int field_3716;

   @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
   private void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
      Block bs = Module.mc.world.method_8320(hitResult.getBlockPos()).method_26204();
      if (ModuleManager.noInteract.isEnabled()
         && (
            bs == Blocks.CHEST
               || bs == Blocks.TRAPPED_CHEST
               || bs == Blocks.FURNACE
               || bs == Blocks.ANVIL
               || bs == Blocks.CRAFTING_TABLE
               || bs == Blocks.HOPPER
               || bs == Blocks.JUKEBOX
               || bs == Blocks.NOTE_BLOCK
               || bs == Blocks.ENDER_CHEST
               || bs == Blocks.DISPENSER
               || bs == Blocks.DROPPER
               || bs instanceof ShulkerBoxBlock
               || bs instanceof FenceBlock
               || bs instanceof FenceGateBlock
               || bs instanceof TrapdoorBlock
         )
         && (ModuleManager.aura.isEnabled() || !NoInteract.onlyAura.getValue())) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
   private void attackBlockHook(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      if (!Module.fullNullCheck()) {
         EventAttackBlock event = new EventAttackBlock(pos, direction);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "breakBlock", at = @At("RETURN"), cancellable = true)
   public void breakBlockHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (!Module.fullNullCheck()) {
         EventBreakBlock event = new EventBreakBlock(pos);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
   public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         EventClickSlot event = new EventClickSlot(actionType, slotId, button, syncId);
         Vcore.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
