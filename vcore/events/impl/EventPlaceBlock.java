package vcore.events.impl;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import vcore.events.Event;

public class EventPlaceBlock extends Event {
   private final BlockPos blockPos;
   private final Block block;

   public EventPlaceBlock(BlockPos blockPos, Block block) {
      this.blockPos = blockPos;
      this.block = block;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public Block getBlock() {
      return this.block;
   }
}
