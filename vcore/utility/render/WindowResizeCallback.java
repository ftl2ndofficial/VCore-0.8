package vcore.utility.render;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public interface WindowResizeCallback {
   Event<WindowResizeCallback> EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
      for (WindowResizeCallback callback : callbacks) {
         callback.onResized(client, window);
      }
   });

   void onResized(MinecraftClient var1, Window var2);
}
