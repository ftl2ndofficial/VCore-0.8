package meteordevelopment.orbit;

import meteordevelopment.orbit.listeners.IListener;
import meteordevelopment.orbit.listeners.LambdaListener;

public interface IEventBus {
   void registerLambdaFactory(String var1, LambdaListener.Factory var2);

   <T> T post(T var1);

   <T extends ICancellable> T post(T var1);

   void subscribe(Object var1);

   void subscribe(Class<?> var1);

   void subscribe(IListener var1);

   void unsubscribe(Object var1);

   void unsubscribe(Class<?> var1);

   void unsubscribe(IListener var1);
}
