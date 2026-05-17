package vcore.gui.font;

import net.minecraft.util.Identifier;

public class Texture {
   final Identifier id;

   public Texture(String path) {
      this.id = Identifier.of("vcore", this.validatePath(path));
   }

   public Texture(Identifier i) {
      this.id = Identifier.of(i.getNamespace(), i.getPath());
   }

   String validatePath(String path) {
      if (Identifier.isPathValid(path)) {
         return path;
      }

      StringBuilder ret = new StringBuilder();

      for (char c : path.toLowerCase().toCharArray()) {
         if (Identifier.isPathCharacterValid(c)) {
            ret.append(c);
         }
      }

      return ret.toString();
   }

   public Identifier getId() {
      return this.id;
   }
}
