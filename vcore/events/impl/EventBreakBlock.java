package vcore.events.impl;

import net.minecraft.util.math.BlockPos;
import vcore.events.Event;

public class EventBreakBlock extends Event {
   private BlockPos bp;

   public EventBreakBlock(BlockPos bp) {
      this.bp = bp;
   }

   public BlockPos getPos() {
      return this.bp;
   }
}
