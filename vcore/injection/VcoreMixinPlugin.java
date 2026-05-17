package vcore.injection;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class VcoreMixinPlugin implements IMixinConfigPlugin {
   private static final String MIXIN_WORLD_RENDERER_CHUNK_ANIM = "MixinWorldRendererChunkAnimations";
   private static final String MIXIN_SODIUM_DEFAULT_CHUNK_RENDERER = "MixinSodiumDefaultChunkRenderer";
   private static final String MIXIN_SODIUM_RENDER_SECTION_MANAGER_CHUNK_ANIM = "MixinSodiumRenderSectionManagerChunkAnimations";
   private static final String MIXIN_SODIUM_BLOCK_OCCLUSION_CACHE = "MixinSodiumBlockOcclusionCache";

   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      boolean sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
      if (mixinClassName.endsWith("MixinWorldRendererChunkAnimations")) {
         return !sodiumLoaded;
      } else if (mixinClassName.endsWith("MixinSodiumDefaultChunkRenderer")) {
         return sodiumLoaded;
      } else {
         return !mixinClassName.endsWith("MixinSodiumRenderSectionManagerChunkAnimations") && !mixinClassName.endsWith("MixinSodiumBlockOcclusionCache")
            ? true
            : sodiumLoaded;
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
