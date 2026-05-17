package vcore.injection;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vcore.utility.render.chunk.ChunkAnimations;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRendererChunkAnimations {
   @ModifyArgs(method = "renderLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlUniform;set(FFF)V"), require = 0)
   private void modifyChunkOffset(Args args, RenderLayer layer, double cameraX, double cameraY, double cameraZ, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
      float x = (Float)args.get(0);
      float y = (Float)args.get(1);
      float z = (Float)args.get(2);
      BlockPos origin = new BlockPos((int)Math.round(x + cameraX), (int)Math.round(y + cameraY), (int)Math.round(z + cameraZ));
      Vec3d offset = ChunkAnimations.INSTANCE.getOffset(origin);
      args.set(0, x + (float)offset.x);
      args.set(1, y + (float)offset.y);
      args.set(2, z + (float)offset.z);
   }
}
