package vcore.gui.notification;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import vcore.core.manager.client.NotificationManager;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.animation.EaseOutBack;

public class Notification {
   private static final float DEFAULT_TEXT_X = 30.0F;
   private static final float DEFAULT_TITLE_Y = 6.0F;
   private static final float DEFAULT_MESSAGE_Y = 15.0F;
   private static final float DEFAULT_TEXT_RIGHT_PADDING = 1.5F;
   private static final float DEFAULT_TITLE_FADE_WIDTH = 8.0F;
   private static final float CROSSHAIR_TEXT_X = 16.0F;
   private static final float CROSSHAIR_TEXT_Y = 5.0F;
   private final String message;
   private final String title;
   private final String icon;
   private final int lifeTime;
   public final EaseOutBack animation;
   private float y;
   private float width;
   private float animationX;
   private float height = 25.0F;
   private boolean direction = false;
   private boolean framePrepared = false;
   private final Timer timer = new Timer();

   public Notification(String title, String message, Notification.Type type, int time) {
      this.lifeTime = time;
      this.title = title;
      this.message = message;
      switch (type) {
         case INFO:
            this.icon = "J";
            break;
         case WARNING:
            this.icon = "L";
            break;
         case ERROR:
         case DISABLED:
            this.icon = "I";
            break;
         case ENABLED:
            this.icon = "K";
            break;
         default:
            this.icon = "H";
      }

      this.width = NotificationManager.isDefault()
         ? FontRenderers.sf_bold_mini.getStringWidth(message) + 38.0F
         : FontRenderers.sf_bold_micro.getStringWidth(title + " " + message) + 20.0F;
      this.height = NotificationManager.isDefault() ? 25.0F : 13.0F;
      this.animation = new EaseOutBack(NotificationManager.isDefault() ? 10 : 20);
      this.animationX = this.width;
      if (NotificationManager.isDefault()) {
         this.y = Module.mc.getWindow().getScaledHeight() - this.height;
      } else {
         this.y = Module.mc.getWindow().getScaledHeight() / 2.0F + 10.0F;
      }
   }

   public void render(MatrixStack matrix, float getY) {
      this.prepareFrame(getY);
      int contentAlpha = this.getContentAlpha();
      Color color = new Color(170, 170, 170, contentAlpha);
      Color textColor = Render2DEngine.injectAlpha(HudEditor.getTextColor(), contentAlpha);
      if (NotificationManager.isDefault()) {
         float x = Module.mc.getWindow().getScaledWidth() - 6 - this.width + this.animationX;
         if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            Render2DEngine.verticalGradient(matrix, x + 25.0F, this.y + 1.0F, x + 25.5F, this.y + 12.0F, Render2DEngine.injectAlpha(textColor, 0), textColor);
            Render2DEngine.verticalGradient(matrix, x + 25.0F, this.y + 11.0F, x + 25.5F, this.y + 22.0F, textColor, Render2DEngine.injectAlpha(textColor, 0));
         } else {
            Render2DEngine.drawRect(
               matrix,
               x + 25.0F,
               this.y + 2.0F,
               0.5F,
               20.0F,
               Render2DEngine.injectAlpha(new Color(1157627903, true), Math.round(68.0F * contentAlpha / 255.0F))
            );
         }

         float textX = x + 30.0F;
         float titleWidth = Math.max(0.0F, this.width - 30.0F - 1.5F);
         float titleClipTop = this.y + 6.0F - 2.0F;
         float titleClipBottom = titleClipTop + FontRenderers.sf_bold_mini.getStringHeight(this.title) + 4.0F;
         Render2DEngine.addWindow(matrix, new Render2DEngine.Rectangle(textX, titleClipTop, textX + titleWidth, titleClipBottom));
         FontRenderers.sf_bold_mini.drawStringWithHorizontalFade(matrix, this.title, textX, this.y + 6.0F, textColor.getRGB(), titleWidth, 8.0F);
         Render2DEngine.popWindow();
         FontRenderers.sf_bold_mini.drawString(matrix, this.message, textX, this.y + 15.0F, color.getRGB());
         FontRenderers.mid_icons.drawString(matrix, this.icon, x + 5.0F, this.y + 7.0F, color.getRGB());
      } else {
         float x = Module.mc.getWindow().getScaledWidth() / 2.0F - this.width / 2.0F;
         if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            Render2DEngine.verticalGradient(matrix, x + 13.0F, this.y + 1.0F, x + 13.5F, this.y + 6.0F, Render2DEngine.injectAlpha(color, 0), color);
            Render2DEngine.verticalGradient(matrix, x + 13.0F, this.y + 6.0F, x + 13.5F, this.y + 11.0F, color, Render2DEngine.injectAlpha(color, 0));
         } else {
            Render2DEngine.drawRect(
               matrix,
               x + 13.0F,
               this.y + 1.0F,
               0.5F,
               10.0F,
               Render2DEngine.injectAlpha(new Color(1157627903, true), Math.round(68.0F * contentAlpha / 255.0F))
            );
         }

         float textX = x + 16.0F;
         int titleColor = Render2DEngine.injectAlpha(HudEditor.getColor(1), contentAlpha).getRGB();
         FontRenderers.sf_bold_micro.drawString(matrix, this.title, textX, this.y + 5.0F, titleColor);
         FontRenderers.sf_bold_micro
            .drawString(matrix, " " + this.message, textX + FontRenderers.sf_bold_micro.getStringWidth(this.title), this.y + 5.0F, color.getRGB());
         FontRenderers.icons.drawString(matrix, this.icon, x + 3.0F, this.y + 5.5F, color.getRGB());
      }

      this.framePrepared = false;
   }

   public void onUpdate() {
      this.animation.update(this.direction);
   }

   public void renderShaders(MatrixStack matrix, float getY) {
      this.prepareFrame(getY);
      Render2DEngine.drawHudBase2(
         matrix,
         NotificationManager.isDefault()
            ? Module.mc.getWindow().getScaledWidth() - 6 - this.width + this.animationX
            : Module.mc.getWindow().getScaledWidth() / 2.0F - this.width / 2.0F,
         this.y,
         this.width,
         this.height,
         NotificationManager.isDefault() ? 5.0F : 3.0F,
         5.0F,
         HudEditor.getBlurOpacity(),
         (float)MathUtility.clamp(1.0 - this.animation.getAnimationd(), 0.0, 1.0)
      );
   }

   private void prepareFrame(float getY) {
      if (!this.framePrepared) {
         this.direction = this.isFinished();
         this.animationX = (float)(this.width * this.animation.getAnimationd());
         this.y = this.animate(this.y, getY);
         this.framePrepared = true;
      }
   }

   private int getContentAlpha() {
      return (int)MathUtility.clamp((1.0 - this.animation.getAnimationd()) * 255.0, 0.0, 255.0);
   }

   private boolean isFinished() {
      return this.timer.passedMs(this.lifeTime);
   }

   public double getHeight() {
      return this.height;
   }

   public boolean shouldDelete() {
      return this.isFinished() && this.animationX >= this.width - 5.0F;
   }

   public float animate(float value, float target) {
      return value + (target - value) / 8.0F;
   }

   public enum Type {
      SUCCESS("Success", Formatting.GREEN),
      INFO("Information", Formatting.AQUA),
      WARNING("Warning", Formatting.GOLD),
      ERROR("Error", Formatting.RED),
      ENABLED("Module enabled", Formatting.DARK_GREEN),
      DISABLED("Module disabled", Formatting.DARK_RED);

      final String name;
      final Formatting color;

      Type(String name, Formatting color) {
         this.name = name;
         this.color = color;
      }

      public String getName() {
         return this.name;
      }

      public Formatting getColor() {
         return this.color;
      }
   }
}
