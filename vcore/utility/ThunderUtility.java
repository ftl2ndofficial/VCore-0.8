package vcore.utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Manifest;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import vcore.Vcore;
import vcore.core.manager.client.ConfigManager;
import vcore.features.modules.Module;
import vcore.utility.math.MathUtility;

public final class ThunderUtility {
   public static List<String> changeLog = new ArrayList<>();

   @NotNull
   public static String getAuthors() {
      List<String> names = Vcore.MOD_META.getAuthors().stream().<String>map(Person::getName).toList();
      return String.join(", ", names);
   }

   public static String solveName(String notSolved) {
      AtomicReference<String> mb = new AtomicReference<>("FATAL ERROR");
      Objects.requireNonNull(Module.mc.getNetworkHandler()).getListedPlayerListEntries().forEach(player -> {
         if (notSolved.contains(player.getProfile().getName())) {
            mb.set(player.getProfile().getName());
         }
      });
      return mb.get();
   }

   public static Identifier getCustomImg(String name) throws IOException {
      return Module.mc
         .getTextureManager()
         .registerDynamicTexture(
            "th-" + name + "-" + (int)MathUtility.random(0.0F, 1000.0F),
            new NativeImageBackedTexture(NativeImage.read(new FileInputStream(ConfigManager.IMAGES_FOLDER + "/" + name + ".png")))
         );
   }

   public static String readManifestField(String fieldName) {
      try {
         Enumeration<URL> en = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");

         while (en.hasMoreElements()) {
            try {
               URL url = en.nextElement();
               InputStream is = url.openStream();
               if (is != null) {
                  String s = new Manifest(is).getMainAttributes().getValue(fieldName);
                  if (s != null) {
                     return s;
                  }
               }
            } catch (Exception var5) {
            }
         }
      } catch (Exception var6) {
      }

      return "0";
   }
}
