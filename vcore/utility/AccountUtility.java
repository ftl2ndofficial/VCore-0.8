package vcore.utility;

import com.mojang.authlib.minecraft.UserApiService;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.util.Uuids;
import vcore.injection.accesors.IMinecraftClient;

public final class AccountUtility {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final String USERNAME_RESOURCE_PATH = "/resources/liquidbounce/data/usernames/";
   private static final int MIN_RANDOM_NAME_LENGTH = 8;
   private static final int MAX_MINECRAFT_NAME_LENGTH = 16;
   private static final int MIN_SECOND_WORD_LENGTH = 3;
   private static final List<String>[] ADJECTIVE_LISTS_BY_SIZE = buildShorterThanList(loadLines("adjectives.txt"));
   private static final List<String>[] ANIMAL_LISTS_BY_SIZE = buildShorterThanList(loadLines("animals.txt"));

   private AccountUtility() {
   }

   public static String login(String name) {
      String username = normalizeMinecraftName(name);
      Session session = new Session(username, Uuids.getOfflinePlayerUuid(username), "", Optional.empty(), Optional.empty(), AccountType.MOJANG);
      setSession(session);
      return username;
   }

   public static String loginRandomAlt() {
      return login(randomMinecraftName());
   }

   public static String randomMinecraftName() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      return randomMinecraftName(random.nextInt(8, 17), random);
   }

   private static String randomMinecraftName(int maxLength, ThreadLocalRandom random) {
      List<String>[] firstWordList;
      List<String>[] secondWordList;
      if (random.nextBoolean()) {
         firstWordList = ADJECTIVE_LISTS_BY_SIZE;
         secondWordList = ANIMAL_LISTS_BY_SIZE;
      } else {
         firstWordList = ANIMAL_LISTS_BY_SIZE;
         secondWordList = ADJECTIVE_LISTS_BY_SIZE;
      }

      String firstWord = randomWordShorterOrEqual(firstWordList, maxLength - 3, random);
      String secondWord = randomWordShorterOrEqual(secondWordList, maxLength - firstWord.length(), random);
      List<String> elements = new ArrayList<>();
      elements.add(firstWord);
      elements.add(secondWord);
      int currentLength = elements.stream().mapToInt(String::length).sum();
      if (currentLength + 1 < maxLength && random.nextInt(20) != 0) {
         int until = Math.min(maxLength - currentLength, 3);
         int digits = until <= 2 ? until : random.nextInt(2, until);
         elements.add(Integer.toString(random.nextInt(pow10(digits))));
      }

      int allowedDelimiters = maxLength - elements.stream().mapToInt(String::length).sum();
      int currentDelimiters = random.nextInt(4);

      while (Integer.bitCount(currentDelimiters) > Math.max(allowedDelimiters, 0)) {
         currentDelimiters = random.nextInt(4);
      }

      StringBuilder output = new StringBuilder(elements.getFirst());

      for (int i = 1; i < elements.size(); i++) {
         if ((currentDelimiters & 1) == 1) {
            output.append('_');
         }

         currentDelimiters >>= 1;
         output.append(elements.get(i));
      }

      return leetRandomly(output.toString(), random.nextInt(3), random);
   }

   private static void setSession(Session session) {
      IMinecraftClient accessor = (IMinecraftClient)mc;
      accessor.setSessionT(session);
      mc.getGameProfile().getProperties().clear();
      UserApiService apiService = UserApiService.OFFLINE;
      accessor.setUserApiService(apiService);
      accessor.setSocialInteractionsManagerT(new SocialInteractionsManager(mc, apiService));
      accessor.setProfileKeys(ProfileKeys.create(apiService, session, mc.runDirectory.toPath()));
      accessor.setAbuseReportContextT(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
      accessor.invokeUpdateWindowTitle();
   }

   private static List<String> loadLines(String name) {
      String resourceName = "/resources/liquidbounce/data/usernames/" + name;
      InputStream inputStream = AccountUtility.class.getResourceAsStream(resourceName);
      if (inputStream == null) {
         throw new IllegalStateException("Failed to load resource " + resourceName);
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
         return reader.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
      } catch (Exception exception) {
         throw new IllegalStateException("Failed to read resource " + resourceName, exception);
      }
   }

   private static String randomWordShorterOrEqual(List<String>[] wordListsBySize, int maxLength, ThreadLocalRandom random) {
      List<String> words = findWordsShorterOrEqual(wordListsBySize, maxLength);
      return words.get(random.nextInt(words.size()));
   }

   private static List<String> findWordsShorterOrEqual(List<String>[] wordListsBySize, int maxLength) {
      int index = Math.clamp(maxLength, 0, wordListsBySize.length - 1);
      return wordListsBySize[index];
   }

   private static List<String>[] buildShorterThanList(List<String> words) {
      List<String> sortedWords = new ArrayList<>(words);
      sortedWords.sort(Comparator.comparingInt(String::length));
      int maxLength = sortedWords.getLast().length();
      List<String>[] wordListsBySize = new List[maxLength];

      for (int i = 0; i < wordListsBySize.length; i++) {
         wordListsBySize[i] = List.of();
      }

      int lastLength = 0;

      for (int i = 0; i < sortedWords.size(); i++) {
         String word = sortedWords.get(i);
         if (word.length() != lastLength) {
            wordListsBySize[lastLength] = List.copyOf(sortedWords.subList(0, i));
            lastLength = word.length();
         }
      }

      for (int i = 1; i < wordListsBySize.length; i++) {
         if (wordListsBySize[i].isEmpty()) {
            wordListsBySize[i] = wordListsBySize[i - 1];
         }
      }

      return wordListsBySize;
   }

   private static String leetRandomly(String name, int leetReplacements, ThreadLocalRandom random) {
      char[] characters = name.toCharArray();
      List<Integer> positions = new ArrayList<>();

      for (int i = 0; i < characters.length; i++) {
         if (getLeetChar(characters[i]) != 0) {
            positions.add(i);
         }
      }

      for (int i = positions.size() - 1; i > 0; i--) {
         int swapIndex = random.nextInt(i + 1);
         int position = positions.get(i);
         positions.set(i, positions.get(swapIndex));
         positions.set(swapIndex, position);
      }

      for (int i = 0; i < Math.min(leetReplacements, positions.size()); i++) {
         int position = positions.get(i);
         characters[position] = getLeetChar(characters[position]);
      }

      return new String(characters);
   }

   private static char getLeetChar(char character) {
      return switch (character) {
         case 'a' -> '4';
         case 'b' -> '8';
         default -> '\u0000';
         case 'e' -> '3';
         case 'g' -> '6';
         case 'i' -> '1';
         case 'o' -> '0';
         case 's' -> '5';
         case 't' -> '7';
         case 'z' -> '2';
      };
   }

   private static int pow10(int exponent) {
      int result = 1;

      for (int i = 0; i < exponent; i++) {
         result *= 10;
      }

      return result;
   }

   private static String normalizeMinecraftName(String name) {
      StringBuilder builder = new StringBuilder(16);

      for (int i = 0; i < name.length() && builder.length() < 16; i++) {
         char character = name.charAt(i);
         if (isMinecraftNameCharacter(character)) {
            builder.append(character);
         }
      }

      while (!builder.isEmpty() && builder.charAt(0) == '_') {
         builder.deleteCharAt(0);
      }

      while (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '_') {
         builder.deleteCharAt(builder.length() - 1);
      }

      while (builder.length() < 3) {
         builder.append(ThreadLocalRandom.current().nextInt(10));
      }

      return builder.toString();
   }

   private static boolean isMinecraftNameCharacter(char character) {
      return character == '_' || character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9';
   }
}
