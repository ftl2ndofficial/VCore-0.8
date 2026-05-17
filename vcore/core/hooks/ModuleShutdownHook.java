package vcore.core.hooks;

import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.misc.UnHook;

public class ModuleShutdownHook extends Thread {
   @Override
   public void run() {
      if (UnHook.isActive()) {
         ModuleManager.unHook.disable();
      }
   }
}
