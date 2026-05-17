package vcore.gui.font;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import vcore.injection.accesors.INativeImage;

class GlyphMap {
   final char fromIncl;
   final char toExcl;
   final Font font;
   final Identifier bindToTexture;
   final int pixelPadding;
   private final Char2ObjectArrayMap<Glyph> glyphs = new Char2ObjectArrayMap();
   int width;
   int height;
   boolean generated = false;

   public GlyphMap(char from, char to, Font font, Identifier identifier, int padding) {
      this.fromIncl = from;
      this.toExcl = to;
      this.font = font;
      this.bindToTexture = identifier;
      this.pixelPadding = padding;
   }

   public Glyph getGlyph(char c) {
      if (!this.generated) {
         this.generate();
      }

      return (Glyph)this.glyphs.get(c);
   }

   public void destroy() {
      MinecraftClient.getInstance().getTextureManager().destroyTexture(this.bindToTexture);
      this.glyphs.clear();
      this.width = -1;
      this.height = -1;
      this.generated = false;
   }

   public boolean contains(char c) {
      return c >= this.fromIncl && c < this.toExcl;
   }

   private Font getFontForGlyph(char c) {
      return this.font.canDisplay(c) ? this.font : this.font;
   }

   public void generate() {
      if (!this.generated) {
         int range = this.toExcl - this.fromIncl - 1;
         int charsVert = (int)(Math.ceil(Math.sqrt(range)) * 1.5);
         this.glyphs.clear();
         int generatedChars = 0;
         int charNX = 0;
         int maxX = 0;
         int maxY = 0;
         int currentX = 0;
         int currentY = 0;
         int currentRowMaxY = 0;
         List<Glyph> glyphs1 = new ArrayList<>();
         AffineTransform af = new AffineTransform();
         FontRenderContext frc = new FontRenderContext(af, true, false);

         while (generatedChars <= range) {
            char currentChar = (char)(this.fromIncl + generatedChars);
            Font font = this.getFontForGlyph(currentChar);
            Rectangle2D stringBounds = font.getStringBounds(String.valueOf(currentChar), frc);
            int width = (int)Math.ceil(stringBounds.getWidth());
            int height = (int)Math.ceil(stringBounds.getHeight());
            generatedChars++;
            maxX = Math.max(maxX, currentX + width);
            maxY = Math.max(maxY, currentY + height);
            if (charNX >= charsVert) {
               currentX = 0;
               currentY += currentRowMaxY + this.pixelPadding;
               charNX = 0;
               currentRowMaxY = 0;
            }

            currentRowMaxY = Math.max(currentRowMaxY, height);
            glyphs1.add(new Glyph(currentX, currentY, width, height, currentChar, this));
            currentX += width + this.pixelPadding;
            charNX++;
         }

         BufferedImage bi = new BufferedImage(Math.max(maxX + this.pixelPadding, 1), Math.max(maxY + this.pixelPadding, 1), 2);
         this.width = bi.getWidth();
         this.height = bi.getHeight();
         Graphics2D g2d = bi.createGraphics();
         g2d.setColor(new Color(255, 255, 255, 0));
         g2d.fillRect(0, 0, this.width, this.height);
         g2d.setColor(Color.WHITE);
         g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
         g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

         for (Glyph glyph : glyphs1) {
            g2d.setFont(this.getFontForGlyph(glyph.value()));
            FontMetrics fontMetrics = g2d.getFontMetrics();
            g2d.drawString(String.valueOf(glyph.value()), glyph.u(), glyph.v() + fontMetrics.getAscent());
            this.glyphs.put(glyph.value(), glyph);
         }

         registerBufferedImageTexture(this.bindToTexture, bi);
         this.generated = true;
      }
   }

   public static void registerBufferedImageTexture(Identifier i, BufferedImage bi) {
      try {
         int ow = bi.getWidth();
         int oh = bi.getHeight();
         NativeImage image = new NativeImage(Format.RGBA, ow, oh, false);
         long ptr = ((INativeImage)image).getPointer();
         IntBuffer backingBuffer = MemoryUtil.memIntBuffer(ptr, image.getWidth() * image.getHeight());
         int off = 0;
         WritableRaster _ra = bi.getRaster();
         ColorModel _cm = bi.getColorModel();
         int nbands = _ra.getNumBands();
         int dataType = _ra.getDataBuffer().getDataType();

         Object _d = switch (dataType) {
            case 0 -> new byte[nbands];
            case 1 -> new short[nbands];
            default -> throw new IllegalArgumentException("Unknown data buffer type: " + dataType);
            case 3 -> new int[nbands];
            case 4 -> new float[nbands];
            case 5 -> new double[nbands];
         };

         for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
               _ra.getDataElements(x, y, _d);
               int a = _cm.getAlpha(_d);
               int r = _cm.getRed(_d);
               int g = _cm.getGreen(_d);
               int b = _cm.getBlue(_d);
               int abgr = a << 24 | b << 16 | g << 8 | r;
               backingBuffer.put(abgr);
            }
         }

         NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
         tex.upload();
         if (RenderSystem.isOnRenderThread()) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(i, tex);
         } else {
            RenderSystem.recordRenderCall(() -> MinecraftClient.getInstance().getTextureManager().registerTexture(i, tex));
         }
      } catch (Throwable e) {
         e.printStackTrace();
      }
   }
}
