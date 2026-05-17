package vcore.gui.font;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.awt.Color;
import java.awt.Font;
import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import vcore.core.manager.IManager;
import vcore.features.modules.render.HudEditor;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;

public class FontRenderer implements Closeable {
   private static final Char2IntArrayMap colorCodes = new Char2IntArrayMap() {
      {
         this.put('0', 0);
         this.put('1', 170);
         this.put('2', 43520);
         this.put('3', 43690);
         this.put('4', 11141120);
         this.put('5', 11141290);
         this.put('6', 16755200);
         this.put('7', 11184810);
         this.put('8', 5592405);
         this.put('9', 5592575);
         this.put('A', 5635925);
         this.put('B', 5636095);
         this.put('C', 16733525);
         this.put('D', 16733695);
         this.put('E', 16777045);
         this.put('F', 16777215);
      }
   };
   private static final ExecutorService ASYNC_WORKER = Executors.newCachedThreadPool();
   private final Object2ObjectMap<Identifier, ObjectList<FontRenderer.DrawEntry>> GLYPH_PAGE_CACHE = new Object2ObjectOpenHashMap();
   private final float originalSize;
   private final ObjectList<GlyphMap> maps = new ObjectArrayList();
   private final Char2ObjectArrayMap<Glyph> allGlyphs = new Char2ObjectArrayMap();
   private final int charsPerPage;
   private final int padding;
   private final String prebakeGlyphs;
   private int scaleMul = 0;
   private Font font;
   private int previousGameScale = -1;
   private Future<Void> prebakeGlyphsFuture;
   private boolean initialized;

   public FontRenderer(Font font, float sizePx, int charactersPerPage, int paddingBetweenCharacters, @Nullable String prebakeCharacters) {
      this.originalSize = sizePx;
      this.charsPerPage = charactersPerPage;
      this.padding = paddingBetweenCharacters;
      this.prebakeGlyphs = prebakeCharacters;
      this.init(font, sizePx);
   }

   public FontRenderer(Font font, float sizePx) {
      this(font, sizePx, 256, 5, null);
   }

   private static int floorNearestMulN(int x, int n) {
      return n * (int)Math.floor((double)x / n);
   }

   public static String stripControlCodes(String text) {
      char[] chars = text.toCharArray();
      StringBuilder f = new StringBuilder();

      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (c == 167) {
            i++;
         } else {
            f.append(c);
         }
      }

      return f.toString();
   }

   private void sizeCheck() {
      int gs = (int)IManager.mc.getWindow().getScaleFactor();
      if (gs != this.previousGameScale) {
         this.close();
         this.init(this.font, this.originalSize);
      }
   }

   private void init(Font font, float sizePx) {
      if (this.initialized) {
         throw new IllegalStateException("Double call to init()");
      }

      this.initialized = true;
      this.previousGameScale = (int)IManager.mc.getWindow().getScaleFactor();
      this.scaleMul = this.previousGameScale;
      this.font = font.deriveFont(sizePx * this.scaleMul);
      if (this.prebakeGlyphs != null && !this.prebakeGlyphs.isEmpty()) {
         this.prebakeGlyphsFuture = this.prebake();
      }
   }

   private Future<Void> prebake() {
      return ASYNC_WORKER.submit(() -> {
         for (char c : this.prebakeGlyphs.toCharArray()) {
            if (Thread.interrupted()) {
               break;
            }

            this.locateGlyph1(c);
         }

         return null;
      });
   }

   private GlyphMap generateMap(char from, char to) {
      GlyphMap gm = new GlyphMap(from, to, this.font, randomIdentifier(), this.padding);
      this.maps.add(gm);
      return gm;
   }

   private Glyph locateGlyph0(char glyph) {
      ObjectListIterator base = this.maps.iterator();

      while (base.hasNext()) {
         GlyphMap map = (GlyphMap)base.next();
         if (map.contains(glyph)) {
            return map.getGlyph(glyph);
         }
      }

      int basex = floorNearestMulN(glyph, this.charsPerPage);
      GlyphMap glyphMap = this.generateMap((char)basex, (char)(basex + this.charsPerPage));
      return glyphMap.getGlyph(glyph);
   }

   @Nullable
   private Glyph locateGlyph1(char glyph) {
      return (Glyph)this.allGlyphs.computeIfAbsent(glyph, this::locateGlyph0);
   }

   public void drawString(MatrixStack stack, String s, double x, double y, int color) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      this.drawString(stack, s, (float)x, (float)y, r, g, b, a);
   }

   public void drawString(MatrixStack stack, String s, double x, double y, Color color) {
      this.drawString(stack, s, (float)x, (float)y, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha());
   }

   public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
      this.drawString(stack, s, x, y, r, g, b, a, false, 0);
   }

   public void drawStringWithHorizontalFade(MatrixStack stack, String s, float x, float y, int color, float visibleWidth, float fadeWidth) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      this.drawStringWithHorizontalFade(stack, s, x, y, r, g, b, a, visibleWidth, fadeWidth);
   }

   public void drawStringWithHorizontalFade(
      MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, float visibleWidth, float fadeWidth
   ) {
      if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone()) {
         try {
            this.prebakeGlyphsFuture.get();
         } catch (InterruptedException | ExecutionException var46) {
         }
      }

      this.sizeCheck();
      float scaledVisibleWidth = Math.max(0.0F, visibleWidth) * this.scaleMul;
      float scaledFadeWidth = Math.max(0.0F, Math.min(fadeWidth, visibleWidth)) * this.scaleMul;
      float fadeStart = Math.max(0.0F, scaledVisibleWidth - scaledFadeWidth);
      float r2 = r;
      float g2 = g;
      float b2 = b;
      stack.push();
      y -= 3.0F;
      stack.translate(MathUtility.roundToDecimal(x, 1), MathUtility.roundToDecimal(y, 1), 0.0);
      stack.scale(1.0F / this.scaleMul, 1.0F / this.scaleMul, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      Matrix4f mat = stack.peek().getPositionMatrix();
      char[] chars = s.toCharArray();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      boolean inSel = false;
      int lineStart = 0;
      synchronized (this.GLYPH_PAGE_CACHE) {
         for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (inSel) {
               inSel = false;
               char c1 = Character.toUpperCase(c);
               if (colorCodes.containsKey(c1)) {
                  int ii = colorCodes.get(c1);
                  int[] col = RGBIntToRGB(ii);
                  r2 = col[0] / 255.0F;
                  g2 = col[1] / 255.0F;
                  b2 = col[2] / 255.0F;
               } else if (c1 == 'R') {
                  r2 = r;
                  g2 = g;
                  b2 = b;
               }
            } else if (c == 167) {
               inSel = true;
            } else if (c == '\n') {
               yOffset += this.getStringHeight(s.substring(lineStart, i)) * this.scaleMul;
               xOffset = 0.0F;
               lineStart = i + 1;
            } else {
               Glyph glyph = this.locateGlyph1(c);
               if (glyph != null) {
                  if (glyph.value() != ' ') {
                     Identifier i1 = glyph.owner().bindToTexture;
                     FontRenderer.DrawEntry entry = new FontRenderer.DrawEntry(xOffset, yOffset, r2, g2, b2, glyph);
                     ((ObjectList)this.GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList())).add(entry);
                  }

                  xOffset += glyph.width();
               }
            }
         }

         ObjectIterator var49 = this.GLYPH_PAGE_CACHE.keySet().iterator();

         while (var49.hasNext()) {
            Identifier identifier = (Identifier)var49.next();
            RenderSystem.setShaderTexture(0, identifier);
            List<FontRenderer.DrawEntry> objects = (List<FontRenderer.DrawEntry>)this.GLYPH_PAGE_CACHE.get(identifier);
            BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (FontRenderer.DrawEntry object : objects) {
               float xo = object.atX;
               float yo = object.atY;
               float cr = object.r;
               float cg = object.g;
               float cb = object.b;
               Glyph glyph = object.toDraw;
               GlyphMap owner = glyph.owner();
               float w = glyph.width();
               float h = glyph.height();
               float u1 = (float)glyph.u() / owner.width;
               float v1 = (float)glyph.v() / owner.height;
               float u2 = (float)(glyph.u() + glyph.width()) / owner.width;
               float v2 = (float)(glyph.v() + glyph.height()) / owner.height;
               float leftAlpha = this.computeHorizontalFadeAlpha(xo, scaledVisibleWidth, fadeStart, a);
               float rightAlpha = this.computeHorizontalFadeAlpha(xo + w, scaledVisibleWidth, fadeStart, a);
               if (!(leftAlpha <= 0.0F) || !(rightAlpha <= 0.0F)) {
                  bb.method_22918(mat, xo, yo + h, 0.0F).texture(u1, v2).color(cr, cg, cb, leftAlpha);
                  bb.method_22918(mat, xo + w, yo + h, 0.0F).texture(u2, v2).color(cr, cg, cb, rightAlpha);
                  bb.method_22918(mat, xo + w, yo, 0.0F).texture(u2, v1).color(cr, cg, cb, rightAlpha);
                  bb.method_22918(mat, xo, yo, 0.0F).texture(u1, v1).color(cr, cg, cb, leftAlpha);
               }
            }

            Render2DEngine.endBuilding(bb);
         }

         this.GLYPH_PAGE_CACHE.clear();
      }

      stack.pop();
   }

   public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean gradient, int offset) {
      if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone()) {
         try {
            this.prebakeGlyphsFuture.get();
         } catch (InterruptedException | ExecutionException var41) {
         }
      }

      this.sizeCheck();
      float r2 = r;
      float g2 = g;
      float b2 = b;
      stack.push();
      y -= 3.0F;
      stack.translate(MathUtility.roundToDecimal(x, 1), MathUtility.roundToDecimal(y, 1), 0.0);
      stack.scale(1.0F / this.scaleMul, 1.0F / this.scaleMul, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      Matrix4f mat = stack.peek().getPositionMatrix();
      char[] chars = s.toCharArray();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      boolean inSel = false;
      int lineStart = 0;
      synchronized (this.GLYPH_PAGE_CACHE) {
         for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (inSel) {
               inSel = false;
               char c1 = Character.toUpperCase(c);
               if (colorCodes.containsKey(c1)) {
                  int ii = colorCodes.get(c1);
                  int[] col = RGBIntToRGB(ii);
                  r2 = col[0] / 255.0F;
                  g2 = col[1] / 255.0F;
                  b2 = col[2] / 255.0F;
               } else if (c1 == 'R') {
                  r2 = r;
                  g2 = g;
                  b2 = b;
               }
            } else {
               if (gradient) {
                  Color color = HudEditor.getColor(i * offset);
                  r2 = color.getRed() / 255.0F;
                  g2 = color.getGreen() / 255.0F;
                  b2 = color.getBlue() / 255.0F;
                  a = color.getAlpha() / 255.0F;
               }

               if (c == 167) {
                  inSel = true;
               } else if (c == '\n') {
                  yOffset += this.getStringHeight(s.substring(lineStart, i)) * this.scaleMul;
                  xOffset = 0.0F;
                  lineStart = i + 1;
               } else {
                  Glyph glyph = this.locateGlyph1(c);
                  if (glyph != null) {
                     if (glyph.value() != ' ') {
                        Identifier i1 = glyph.owner().bindToTexture;
                        FontRenderer.DrawEntry entry = new FontRenderer.DrawEntry(xOffset, yOffset, r2, g2, b2, glyph);
                        ((ObjectList)this.GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList())).add(entry);
                     }

                     xOffset += glyph.width();
                  }
               }
            }
         }

         ObjectIterator var44 = this.GLYPH_PAGE_CACHE.keySet().iterator();

         while (var44.hasNext()) {
            Identifier identifier = (Identifier)var44.next();
            RenderSystem.setShaderTexture(0, identifier);
            List<FontRenderer.DrawEntry> objects = (List<FontRenderer.DrawEntry>)this.GLYPH_PAGE_CACHE.get(identifier);
            BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (FontRenderer.DrawEntry object : objects) {
               float xo = object.atX;
               float yo = object.atY;
               float cr = object.r;
               float cg = object.g;
               float cb = object.b;
               Glyph glyph = object.toDraw;
               GlyphMap owner = glyph.owner();
               float w = glyph.width();
               float h = glyph.height();
               float u1 = (float)glyph.u() / owner.width;
               float v1 = (float)glyph.v() / owner.height;
               float u2 = (float)(glyph.u() + glyph.width()) / owner.width;
               float v2 = (float)(glyph.v() + glyph.height()) / owner.height;
               bb.method_22918(mat, xo + 0.0F, yo + h, 0.0F).texture(u1, v2).color(cr, cg, cb, a);
               bb.method_22918(mat, xo + w, yo + h, 0.0F).texture(u2, v2).color(cr, cg, cb, a);
               bb.method_22918(mat, xo + w, yo + 0.0F, 0.0F).texture(u2, v1).color(cr, cg, cb, a);
               bb.method_22918(mat, xo + 0.0F, yo + 0.0F, 0.0F).texture(u1, v1).color(cr, cg, cb, a);
            }

            Render2DEngine.endBuilding(bb);
         }

         this.GLYPH_PAGE_CACHE.clear();
      }

      stack.pop();
   }

   public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      this.drawString(stack, s, (float)(x - this.getStringWidth(s) / 2.0F), (float)y, r, g, b, a);
   }

   public void drawCenteredString(MatrixStack stack, String s, double x, double y, Color color) {
      this.drawString(
         stack,
         s,
         (float)(x - this.getStringWidth(s) / 2.0F),
         (float)y,
         color.getRed() / 255.0F,
         color.getGreen() / 255.0F,
         color.getBlue() / 255.0F,
         color.getAlpha() / 255.0F
      );
   }

   public void drawCenteredString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
      this.drawString(stack, s, x - this.getStringWidth(s) / 2.0F, y, r, g, b, a);
   }

   public float getStringWidth(String text) {
      char[] c = stripControlCodes(text).toCharArray();
      float currentLine = 0.0F;
      float maxPreviousLines = 0.0F;

      for (char c1 : c) {
         if (c1 == '\n') {
            maxPreviousLines = Math.max(currentLine, maxPreviousLines);
            currentLine = 0.0F;
         } else {
            Glyph glyph = this.locateGlyph1(c1);
            currentLine += glyph == null ? 0.0F : (float)glyph.width() / this.scaleMul;
         }
      }

      return Math.max(currentLine, maxPreviousLines);
   }

   public float getStringHeight(String text) {
      char[] c = stripControlCodes(text).toCharArray();
      if (c.length == 0) {
         c = new char[]{' '};
      }

      float currentLine = 0.0F;
      float previous = 0.0F;

      for (char c1 : c) {
         if (c1 == '\n') {
            if (currentLine == 0.0F) {
               currentLine = this.locateGlyph1(' ') == null ? 0.0F : (float)Objects.requireNonNull(this.locateGlyph1(' ')).height() / this.scaleMul;
            }

            previous += currentLine;
            currentLine = 0.0F;
         } else {
            Glyph glyph = this.locateGlyph1(c1);
            currentLine = Math.max(glyph == null ? 0.0F : (float)glyph.height() / this.scaleMul, currentLine);
         }
      }

      return currentLine + previous;
   }

   private float computeHorizontalFadeAlpha(float x, float visibleWidth, float fadeStart, float baseAlpha) {
      if (x <= fadeStart) {
         return baseAlpha;
      }

      if (x >= visibleWidth) {
         return 0.0F;
      }

      if (visibleWidth <= fadeStart) {
         return 0.0F;
      }

      float progress = (x - fadeStart) / (visibleWidth - fadeStart);
      progress = Math.max(0.0F, Math.min(1.0F, progress));
      return baseAlpha * (float)Math.pow(1.0F - progress, 2.35F);
   }

   @Override
   public void close() {
      try {
         if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone() && !this.prebakeGlyphsFuture.isCancelled()) {
            this.prebakeGlyphsFuture.cancel(true);
            this.prebakeGlyphsFuture.get();
            this.prebakeGlyphsFuture = null;
         }

         ObjectListIterator var1 = this.maps.iterator();

         while (var1.hasNext()) {
            GlyphMap map = (GlyphMap)var1.next();
            map.destroy();
         }

         this.maps.clear();
         this.allGlyphs.clear();
         this.initialized = false;
      } catch (Exception var3) {
      }
   }

   @Contract(value = "-> new", pure = true)
   @NotNull
   public static Identifier randomIdentifier() {
      return Identifier.of("vcore", "temp/" + randomString());
   }

   private static String randomString() {
      return IntStream.range(0, 32).mapToObj(operand -> String.valueOf((char)new Random().nextInt(97, 123))).collect(Collectors.joining());
   }

   @Contract(value = "_ -> new", pure = true)
   public static int @NotNull [] RGBIntToRGB(int in) {
      int red = in >> 16 & 0xFF;
      int green = in >> 8 & 0xFF;
      int blue = in & 0xFF;
      return new int[]{red, green, blue};
   }

   public float getFontHeight(String str) {
      return this.getStringHeight(str);
   }

   public void drawGradientString(MatrixStack stack, String s, float x, float y, int offset) {
      this.drawString(stack, s, x, y, 255.0F, 255.0F, 255.0F, 255.0F, true, offset);
   }

   public void drawGradientCenteredString(MatrixStack matrices, String s, float x, float y, int i) {
      this.drawGradientString(matrices, s, x - this.getStringWidth(s) / 2.0F, y, i);
   }

   record DrawEntry(float atX, float atY, float r, float g, float b, Glyph toDraw) {
   }
}
