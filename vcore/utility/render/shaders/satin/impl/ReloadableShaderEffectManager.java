package vcore.utility.render.shaders.satin.impl;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import vcore.utility.render.WindowResizeCallback;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.ManagedShaderEffect;
import vcore.utility.render.shaders.satin.api.managed.ShaderEffectManager;

public final class ReloadableShaderEffectManager implements ShaderEffectManager {
   public static final ReloadableShaderEffectManager INSTANCE = new ReloadableShaderEffectManager();
   private final Set<ResettableManagedShaderBase<?>> managedShaders = new ReferenceOpenHashSet();

   public ReloadableShaderEffectManager() {
      WindowResizeCallback.EVENT
         .register((WindowResizeCallback)(client, window) -> this.onResolutionChanged(window.getFramebufferWidth(), window.getFramebufferHeight()));
   }

   @Override
   public ManagedShaderEffect manage(Identifier location) {
      return this.manage(location, s -> {});
   }

   @Override
   public ManagedShaderEffect manage(Identifier location, Consumer<ManagedShaderEffect> initCallback) {
      ResettableManagedShaderEffect ret = new ResettableManagedShaderEffect(location, initCallback);
      this.managedShaders.add(ret);
      return ret;
   }

   @Override
   public ManagedCoreShader manageCoreShader(Identifier location) {
      return this.manageCoreShader(location, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
   }

   @Override
   public ManagedCoreShader manageCoreShader(Identifier location, VertexFormat vertexFormat) {
      return this.manageCoreShader(location, vertexFormat, s -> {});
   }

   @Override
   public ManagedCoreShader manageCoreShader(Identifier location, VertexFormat vertexFormat, Consumer<ManagedCoreShader> initCallback) {
      ResettableManagedCoreShader ret = new ResettableManagedCoreShader(location, vertexFormat, initCallback);
      this.managedShaders.add(ret);
      return ret;
   }

   public void reload(ResourceFactory shaderResources) {
      for (ResettableManagedShaderBase<?> ss : this.managedShaders) {
         ss.initializeOrLog(shaderResources);
      }
   }

   public void onResolutionChanged(int newWidth, int newHeight) {
      this.runShaderSetup(newWidth, newHeight);
   }

   private void runShaderSetup(int newWidth, int newHeight) {
      if (!this.managedShaders.isEmpty()) {
         for (ResettableManagedShaderBase<?> ss : this.managedShaders) {
            if (ss.isInitialized()) {
               ss.setup(newWidth, newHeight);
            }
         }
      }
   }
}
