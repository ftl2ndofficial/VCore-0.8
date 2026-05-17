package vcore.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import vcore.utility.render.chunk.ChunkAnimations;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
public class MixinSodiumDefaultChunkRenderer {
   @ModifyArgs(
      method = "setModelMatrixUniforms(Lnet/caffeinemc/mods/sodium/client/render/chunk/shader/ChunkShaderInterface;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;)V",
      at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/shader/ChunkShaderInterface;setRegionOffset(FFF)V", remap = false),
      remap = false,
      require = 0
   )
   private static void modifyRegionOffset(Args args) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.gameRenderer != null) {
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         if (cameraPos != null) {
            float regionOffsetX = (Float)args.get(0);
            float regionOffsetY = (Float)args.get(1);
            float regionOffsetZ = (Float)args.get(2);
            BlockPos origin = new BlockPos(
               (int)Math.round(regionOffsetX + cameraPos.x), (int)Math.round(regionOffsetY + cameraPos.y), (int)Math.round(regionOffsetZ + cameraPos.z)
            );
            Vec3d offset = ChunkAnimations.INSTANCE.getOffset(origin);
            if (offset != Vec3d.ZERO) {
               args.set(0, regionOffsetX + (float)offset.x);
               args.set(1, regionOffsetY + (float)offset.y);
               args.set(2, regionOffsetZ + (float)offset.z);
            }
         }
      }
   }
}
