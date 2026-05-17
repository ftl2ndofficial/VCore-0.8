package vcore.utility.render;

public class TextUtil {
   private final String[] words;
   private String currentWord = "_";
   private String currentResult = "_";
   private int arrayIndex;
   private int currentIndex;
   private int ticks;
   private boolean filip = false;

   public TextUtil(String... words) {
      this.words = words;
   }

   @Override
   public String toString() {
      return this.currentResult;
   }

   public void tick() {
      this.ticks++;
      if (this.ticks % (this.filip ? 2 : 1) == 0) {
         if (!this.currentWord.isEmpty()) {
            this.currentResult = this.currentWord.substring(0, this.currentWord.length() - Math.max(this.currentIndex, 0));
         }

         if (this.currentIndex >= this.currentWord.length()) {
            this.filip = true;
            this.arrayIndex++;
            if (this.arrayIndex >= this.words.length) {
               this.arrayIndex = 0;
            }

            this.currentWord = this.words[this.arrayIndex];
            this.currentIndex = this.currentWord.length();
         }

         if (!this.filip) {
            this.currentIndex++;
         } else {
            this.currentIndex--;
         }

         if (this.currentIndex <= -20) {
            this.filip = false;
            this.currentIndex = 0;
         }
      }
   }
}
