package vcore.utility.render;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;

public final class SettingControlRenderer {
   public static final float CHECKBOX_SIZE = 10.0F;
   public static final float CHECKBOX_WIDTH = 10.0F;
   public static final float CHECKBOX_HEIGHT = 10.0F;
   private static final float CHECKBOX_RADIUS = 2.2F;
   private static final float CHECKBOX_THICKNESS = 0.58F;
   private static final float CHECKBOX_OFF_ALPHA = 0.5F;
   private static final Color CHECKBOX_COLOR = Color.WHITE;

   private SettingControlRenderer() {
   }

   public static void drawCheckbox(MatrixStack matrices, float x, float y, float progress) {
      float clampedProgress = clamp(progress);
      float alpha = 0.5F + 0.5F * clampedProgress;
      Render2DEngine.drawCheckbox(matrices, x, y, 10.0F, 10.0F, 2.2F, 0.58F, clampedProgress, withAlpha(CHECKBOX_COLOR, alpha));
   }

   private static Color withAlpha(Color color, float alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255.0F * clamp(alpha)));
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
