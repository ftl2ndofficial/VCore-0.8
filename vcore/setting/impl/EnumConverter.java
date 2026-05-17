package vcore.setting.impl;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public class EnumConverter extends Converter<Enum, JsonElement> {
   private final Class<? extends Enum> clazz;

   public EnumConverter(Class<? extends Enum> clazz) {
      this.clazz = clazz;
   }

   public static int currentEnum(Enum clazz) {
      for (int i = 0; i < ((Enum[])clazz.getClass().getEnumConstants()).length; i++) {
         Enum e = ((Enum[])clazz.getClass().getEnumConstants())[i];
         if (e.name().equalsIgnoreCase(clazz.name())) {
            return i;
         }
      }

      return -1;
   }

   public static Enum increaseEnum(Enum clazz) {
      int index = currentEnum(clazz);

      for (int i = 0; i < ((Enum[])clazz.getClass().getEnumConstants()).length; i++) {
         Enum e = ((Enum[])clazz.getClass().getEnumConstants())[i];
         if (i == index + 1) {
            return e;
         }
      }

      return ((Enum[])clazz.getClass().getEnumConstants())[0];
   }

   public static Enum<?> setEnumInt(@NotNull Enum<?> clazz, int id) {
      for (int i = 0; i < ((Enum[])clazz.getClass().getEnumConstants()).length; i++) {
         Enum<?> e = ((Enum[])clazz.getClass().getEnumConstants())[i];
         if (i == id) {
            return e;
         }
      }

      return ((Enum[])clazz.getClass().getEnumConstants())[0];
   }

   public static String[] getNames(Enum clazz) {
      return Arrays.stream((Enum[])clazz.getClass().getEnumConstants()).map(Enum::toString).toArray(String[]::new);
   }

   public static String getProperName(Enum clazz) {
      return clazz.toString();
   }

   public JsonElement doForward(Enum anEnum) {
      return new JsonPrimitive(anEnum.toString());
   }

   public Enum doBackward(JsonElement jsonElement) {
      String raw = jsonElement.getAsString();

      try {
         return Enum.valueOf(this.clazz, raw);
      } catch (IllegalArgumentException ignored) {
         for (Enum e : this.clazz.getEnumConstants()) {
            if (e.toString().equalsIgnoreCase(raw)) {
               return e;
            }
         }

         return null;
      }
   }
}
