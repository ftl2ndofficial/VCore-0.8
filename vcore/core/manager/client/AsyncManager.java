package vcore.core.manager.client;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import vcore.core.Managers;
import vcore.core.manager.IManager;
import vcore.events.impl.EventPostTick;
import vcore.events.impl.EventSync;
import vcore.events.impl.EventTick;
import vcore.features.cmd.Command;
import vcore.features.modules.Module;

public class AsyncManager implements IManager {
   private AsyncManager.ClientService clientService = new AsyncManager.ClientService();
   public static ExecutorService executor = Executors.newCachedThreadPool();
   private volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
   private volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
   public final AtomicBoolean ticking = new AtomicBoolean(false);

   public static void sleep(int delay) {
      try {
         Thread.sleep(delay);
      } catch (Exception var2) {
      }
   }

   @EventHandler(priority = -200)
   public void onPostTick(EventPostTick e) {
      if (mc.world != null) {
         this.threadSafeEntityList = Lists.newArrayList(mc.world.getEntities());
         this.threadSafePlayersList = Lists.newArrayList(mc.world.method_18456());
         this.ticking.set(false);
      }
   }

   public Iterable<Entity> getAsyncEntities() {
      return this.threadSafeEntityList;
   }

   public List<AbstractClientPlayerEntity> getAsyncPlayers() {
      return this.threadSafePlayersList;
   }

   public AsyncManager() {
      this.clientService.setName("Vcore-AsyncProcessor");
      this.clientService.setDaemon(true);
      this.clientService.start();
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (!this.clientService.isAlive()) {
         this.clientService = new AsyncManager.ClientService();
         this.clientService.setName("Vcore-AsyncProcessor");
         this.clientService.setDaemon(true);
         this.clientService.start();
      }
   }

   @EventHandler(priority = 200)
   public void onTick(EventTick e) {
      this.ticking.set(true);
   }

   public void run(Runnable runnable, long delay) {
      executor.execute(() -> {
         try {
            Thread.sleep(delay);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }

         runnable.run();
      });
   }

   public void run(Runnable r) {
      executor.execute(r);
   }

   public static class ClientService extends Thread {
      @Override
      public void run() {
         while (!Thread.currentThread().isInterrupted()) {
            try {
               if (!Module.fullNullCheck()) {
                  Managers.MODULE.modules.forEach(m -> {
                     if (m.isEnabled()) {
                        m.onThread();
                     }
                  });
                  Thread.sleep(100L);
               } else {
                  Thread.yield();
               }
            } catch (Exception exception) {
               exception.printStackTrace();
               Command.sendMessage(exception.getMessage());
            }
         }
      }
   }
}
