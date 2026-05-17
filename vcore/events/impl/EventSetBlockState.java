package vcore.events.impl;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import vcore.events.Event;

public class EventSetBlockState extends Event {
   private final BlockPos pos;
   private final BlockState state;
   private final BlockState prevState;

   public EventSetBlockState(BlockPos pos, BlockState state, BlockState prevState) {
      this.pos = pos;
      this.state = state;
      this.prevState = prevState;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }

   public BlockState getPrevState() {
      return this.prevState;
   }
}
