package vcore.features.modules.misc;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.player.FriendManager;
import vcore.features.modules.Module;
import vcore.setting.Setting;

public class NameProtect extends Module {
   public static Setting<String> newName = new Setting<>("name", "V-Core User");
   public static Setting<Boolean> hideFriends = new Setting<>("Hide friends", true);

   public NameProtect() {
      super("NameProtect", "Hides your nickname.", Module.Category.MISC);
   }

   public static String getCustomName() {
      return ModuleManager.nameProtect.isEnabled() ? newName.getValue().replaceAll("&", "§") : mc.getGameProfile().getName();
   }

   public static String protectText(String text) {
      if (text == null || text.isEmpty()) {
         return text;
      }

      if (ModuleManager.nameProtect.isEnabled() && mc.player != null) {
         Map<String, String> replacements = new LinkedHashMap<>();
         String customName = getCustomName();
         addReplacement(replacements, mc.getSession().getUsername(), customName);
         addReplacement(replacements, mc.getGameProfile().getName(), customName);
         addReplacement(replacements, mc.player.method_5477().getString(), customName);
         if (hideFriends.getValue()) {
            for (String friend : FriendManager.friends) {
               addReplacement(replacements, friend, "Friend");
            }
         }

         return replacements.isEmpty() ? text : replaceTokens(text, replacements);
      } else {
         return text;
      }
   }

   private static String replaceTokens(String input, Map<String, String> replacements) {
      StringBuilder result = null;
      int copyStart = 0;
      int tokenStart = -1;
      int tokenEnd = -1;
      StringBuilder token = new StringBuilder();
      int i = 0;

      while (i < input.length()) {
         if (isFormattingCode(input, i)) {
            i += 2;
         } else {
            int codePoint = input.codePointAt(i);
            int codePointLength = Character.charCount(codePoint);
            char folded = foldIdentifierCodePoint(codePoint);
            if (folded != 0) {
               if (tokenStart == -1) {
                  tokenStart = i;
                  token.setLength(0);
               }

               token.append(folded);
               tokenEnd = i + codePointLength;
            } else if (tokenStart != -1) {
               String replacement = replacements.get(token.toString());
               if (replacement != null) {
                  if (result == null) {
                     result = new StringBuilder(input.length() + 16);
                  }

                  result.append(input, copyStart, tokenStart).append(replacement);
                  copyStart = tokenEnd;
               }

               tokenStart = -1;
               tokenEnd = -1;
            }

            i += codePointLength;
         }
      }

      if (tokenStart != -1) {
         String replacement = replacements.get(token.toString());
         if (replacement != null) {
            if (result == null) {
               result = new StringBuilder(input.length() + 16);
            }

            result.append(input, copyStart, tokenStart).append(replacement);
            copyStart = tokenEnd;
         }
      }

      if (result == null) {
         return input;
      }

      result.append(input, copyStart, input.length());
      return result.toString();
   }

   private static void addReplacement(Map<String, String> replacements, String sourceName, String targetName) {
      if (sourceName != null && !sourceName.isEmpty()) {
         String normalized = normalizeName(sourceName);
         if (!normalized.isEmpty()) {
            replacements.putIfAbsent(normalized, targetName);
         }
      }
   }

   private static String normalizeName(String name) {
      StringBuilder out = new StringBuilder(name.length());
      int i = 0;

      while (i < name.length()) {
         int codePoint = name.codePointAt(i);
         char folded = foldIdentifierCodePoint(codePoint);
         if (folded != 0) {
            out.append(folded);
         }

         i += Character.charCount(codePoint);
      }

      return out.toString();
   }

   private static char foldIdentifierCodePoint(int codePoint) {
      int mappedCodePoint = mapFontVariant(codePoint);
      if (mappedCodePoint <= 127) {
         char ascii = Character.toLowerCase((char)mappedCodePoint);
         return isIdentifierCharacter(ascii) ? ascii : '\u0000';
      }

      String normalized = Normalizer.normalize(new String(Character.toChars(mappedCodePoint)), Form.NFKD);
      int i = 0;

      while (i < normalized.length()) {
         int cp = normalized.codePointAt(i);
         i += Character.charCount(cp);
         if (!isIgnoredCodePoint(cp)) {
            cp = mapFontVariant(cp);
            if (cp <= 127) {
               char ascii = Character.toLowerCase((char)cp);
               if (isIdentifierCharacter(ascii)) {
                  return ascii;
               }
            } else if (Character.isDigit(cp)) {
               int digit = Character.getNumericValue(cp);
               if (digit >= 0 && digit <= 9) {
                  return (char)(48 + digit);
               }
            }
         }
      }

      return '\u0000';
   }

   private static boolean isFormattingCode(String input, int index) {
      return index + 1 < input.length() && input.charAt(index) == 167;
   }

   private static boolean isIgnoredCodePoint(int codePoint) {
      int type = Character.getType(codePoint);
      return type == 6 || type == 8 || type == 7 || type == 16;
   }

   private static boolean isIdentifierCharacter(char c) {
      return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
   }

   private static int mapFontVariant(int codePoint) {
      return switch (codePoint) {
         case 491 -> 113;
         case 610 -> 103;
         case 618 -> 105;
         case 628 -> 110;
         case 640 -> 114;
         case 655 -> 121;
         case 665 -> 98;
         case 668 -> 104;
         case 671 -> 108;
         case 913, 945 -> 97;
         case 914 -> 98;
         case 917, 949 -> 101;
         case 921, 953 -> 105;
         case 922, 954 -> 107;
         case 924, 956 -> 109;
         case 925, 957 -> 110;
         case 927, 959 -> 111;
         case 929, 961 -> 112;
         case 932, 964 -> 116;
         case 933, 965 -> 121;
         case 935, 967 -> 120;
         case 1030, 1110 -> 105;
         case 1032, 1112 -> 106;
         case 1040, 1072 -> 97;
         case 1042, 1074 -> 98;
         case 1045, 1077 -> 101;
         case 1050, 1082 -> 107;
         case 1052, 1084 -> 109;
         case 1053, 1085 -> 104;
         case 1054, 1086 -> 111;
         case 1056, 1088 -> 112;
         case 1057, 1089 -> 99;
         case 1058, 1090 -> 116;
         case 1059, 1091 -> 121;
         case 1061, 1093 -> 120;
         case 1171, 42800 -> 102;
         case 7424 -> 97;
         case 7428 -> 99;
         case 7429 -> 100;
         case 7431 -> 101;
         case 7434 -> 106;
         case 7435 -> 107;
         case 7437 -> 109;
         case 7439 -> 111;
         case 7448 -> 112;
         case 7451 -> 116;
         case 7452 -> 117;
         case 7456 -> 118;
         case 7457 -> 119;
         case 7458 -> 122;
         case 42801 -> 115;
         default -> codePoint;
      };
   }

   public static String getDisplayName(PlayerEntity player) {
      if (ModuleManager.nameProtect.isEnabled() && hideFriends.getValue()) {
         try {
            if (Managers.FRIEND.isFriend(player)) {
               return "Friend";
            }
         } catch (Exception var2) {
         }
      }

      return ModuleManager.nameProtect.isEnabled() && player == mc.player ? newName.getValue().replaceAll("&", "§") : player.method_5477().getString();
   }
}
