package vcore.features.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class Prediction extends Module {
   private static final float TAG_SCALE = 0.85F;
   private final Setting<Boolean> renderPearls = new Setting<>("Pearl", true);
   private final Setting<Boolean> renderItems = new Setting<>("Item", false);
   private final Setting<Boolean> showTags = new Setting<>("Tag", true);
   private long cacheWorldTime = Long.MIN_VALUE;
   private boolean cachePearlSetting = this.renderPearls.getValue();
   private boolean cacheItemSetting = this.renderItems.getValue();
   private List<Prediction.PredictionResult> cachedResults = Collections.emptyList();

   public Prediction() {
      super("Prediction", "Draws impact previews for projectiles and items.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      this.cachedResults = Collections.emptyList();
      this.cacheWorldTime = Long.MIN_VALUE;
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (!fullNullCheck()) {
         for (Prediction.PredictionResult result : this.getPredictions()) {
            this.renderPath(result);
         }
      }
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (this.showTags.getValue() && !fullNullCheck() && !mc.options.hudHidden) {
         for (Prediction.PredictionResult result : this.getPredictions()) {
            this.drawTag(context, result);
         }
      }
   }

   private List<Prediction.PredictionResult> getPredictions() {
      if (!fullNullCheck() && mc.world != null) {
         long worldTime = mc.world.method_8510();
         boolean pearlsEnabled = this.renderPearls.getValue();
         boolean itemsEnabled = this.renderItems.getValue();
         if (this.cachedResults.isEmpty()
            || worldTime != this.cacheWorldTime
            || pearlsEnabled != this.cachePearlSetting
            || itemsEnabled != this.cacheItemSetting) {
            this.cachedResults = this.computePredictions(pearlsEnabled, itemsEnabled);
            this.cacheWorldTime = worldTime;
            this.cachePearlSetting = pearlsEnabled;
            this.cacheItemSetting = itemsEnabled;
         }

         return this.cachedResults;
      } else {
         return Collections.emptyList();
      }
   }

   private List<Prediction.PredictionResult> computePredictions(boolean pearlsEnabled, boolean itemsEnabled) {
      List<Prediction.PredictionResult> results = new ArrayList<>();
      if (mc.world == null) {
         return results;
      }

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EnderPearlEntity pearl && pearlsEnabled && !pearl.method_24828()) {
            Prediction.PredictionResult result = this.simulate(pearl, new ItemStack(Items.ENDER_PEARL));
            if (result != null) {
               results.add(result);
            }
         } else if (entity instanceof ItemEntity item && itemsEnabled && this.shouldSimulateItem(item)) {
            Prediction.PredictionResult result = this.simulate(item, item.getStack().copy());
            if (result != null) {
               results.add(result);
            }
         }
      }

      return results;
   }

   private boolean shouldSimulateItem(ItemEntity item) {
      return !item.method_24828() && item.method_18798().lengthSquared() > 1.0E-4;
   }

   private Prediction.PredictionResult simulate(Entity entity, ItemStack icon) {
      if (mc.world == null) {
         return null;
      }

      Vec3d position = entity.getPos();
      Vec3d motion = entity.getVelocity();
      List<Vec3d> points = new ArrayList<>();
      Vec3d landing = position;
      Vec3d display = position;
      double time = 0.0;

      for (int i = 0; i < 600; i++) {
         points.add(position);
         Vec3d next = position.add(motion);
         Vec3d start = position.add(0.0, entity.getHeight() * 0.5, 0.0);
         Vec3d end = next.add(0.0, entity.getHeight() * 0.5, 0.0);
         HitResult hit = this.findCollision(start, end);
         motion = this.updateMotion(entity, motion);
         time += 0.05;
         if (hit != null) {
            Vec3d hitPos = hit.getPos();
            Vec3d segment = hitPos.subtract(position);
            Vec3d renderEnd = hitPos;
            double segLength = segment.length();
            if (segLength > 0.001) {
               double trim = Math.min(0.5, Math.max(0.0, segLength - 0.01));
               if (trim > 0.0) {
                  renderEnd = hitPos.subtract(segment.normalize().multiply(trim));
               }
            }

            points.add(renderEnd);
            landing = hitPos;
            display = renderEnd;
            break;
         }

         if (next.y <= mc.world.method_31607()) {
            points.add(next);
            landing = next;
            display = next;
            break;
         }

         position = next;
         landing = next;
         display = next;
      }

      return points.size() < 2 ? null : new Prediction.PredictionResult(Collections.unmodifiableList(points), landing, display, time, icon);
   }

   private Vec3d updateMotion(Entity entity, Vec3d original) {
      Vec3d updated;
      if (entity.isTouchingWater()) {
         updated = original.multiply(0.8);
      } else {
         updated = original.multiply(0.99);
      }

      if ((entity.hasNoGravity() || !(entity instanceof ArrowEntity)) && !(entity instanceof ItemEntity)) {
         if (!entity.hasNoGravity() && entity instanceof EnderPearlEntity) {
            updated = updated.add(0.0, -0.03, 0.0);
         }
      } else {
         updated = updated.add(0.0, -0.05, 0.0);
      }

      return updated;
   }

   private HitResult findCollision(Vec3d start, Vec3d end) {
      if (mc.world != null && mc.player != null) {
         RaycastContext ctx = new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
         HitResult result = mc.world.method_17742(ctx);
         return result.getType() == Type.BLOCK ? result : null;
      } else {
         return null;
      }
   }

   private void renderPath(Prediction.PredictionResult result) {
      List<Vec3d> path = result.path();
      Prediction.TagLayout layout = null;
      if (this.showTags.getValue()) {
         Vec3d landingScreen = Render3DEngine.worldSpaceToScreenSpace(result.displayPos());
         if (this.isOnScreen(landingScreen)) {
            layout = this.createTagLayout(result, landingScreen);
         }
      }

      for (int i = 0; i < path.size() - 1; i++) {
         Vec3d start = path.get(i);
         Vec3d end = path.get(i + 1);
         boolean clipSegment = false;
         if (layout != null) {
            Vec3d startScreen = Render3DEngine.worldSpaceToScreenSpace(start);
            Vec3d endScreen = Render3DEngine.worldSpaceToScreenSpace(end);
            if (this.isValidScreenPoint(startScreen) && this.isValidScreenPoint(endScreen)) {
               double topY = layout.y();
               if (!(startScreen.y <= topY) || !(endScreen.y <= topY)) {
                  if (startScreen.y < topY && endScreen.y > topY) {
                     double t = MathHelper.clamp((topY - startScreen.y) / (endScreen.y - startScreen.y), 0.0, 1.0);
                     end = start.lerp(end, t);
                     clipSegment = true;
                  } else if (startScreen.y >= topY && endScreen.y >= topY) {
                     break;
                  }
               }
            }
         }

         this.drawSegment(start, end, i);
         if (clipSegment) {
            break;
         }
      }
   }

   private void drawTag(DrawContext context, Prediction.PredictionResult result) {
      Vec3d screen = Render3DEngine.worldSpaceToScreenSpace(result.displayPos());
      if (this.isOnScreen(screen)) {
         Prediction.TagLayout layout = this.createTagLayout(result, screen);
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         this.applyTagScale(matrices, layout);
         Render2DEngine.drawRound(matrices, layout.x(), layout.y(), layout.width(), layout.height(), 3.0F, new Color(0, 0, 0, 150));
         if (!result.icon().isEmpty()) {
            matrices.push();
            matrices.translate(layout.iconX(), layout.iconY(), 0.0F);
            matrices.scale(layout.iconScale(), layout.iconScale(), 1.0F);
            context.drawItem(result.icon(), 0, 0);
            context.drawItemInSlot(mc.textRenderer, result.icon(), 0, 0);
            matrices.pop();
         }

         matrices.push();
         matrices.translate(0.0F, 3.0F, 0.0F);
         FontRenderers.sf_medium.drawString(matrices, layout.text(), layout.textX(), layout.textY(), Color.WHITE.getRGB());
         matrices.pop();
         matrices.pop();
      }
   }

   private void applyTagScale(MatrixStack matrices, Prediction.TagLayout layout) {
      float centerX = layout.x() + layout.width() / 2.0F;
      float centerY = layout.y() + layout.height() / 2.0F;
      matrices.translate(centerX, centerY, 0.0F);
      matrices.scale(0.85F, 0.85F, 1.0F);
      matrices.translate(-centerX, -centerY, 0.0F);
   }

   private void drawSegment(Vec3d start, Vec3d end, int index) {
      Color color = Render2DEngine.injectAlpha(HudEditor.getColor(index * 2), 200);
      Render3DEngine.drawLine(start, end, color);
   }

   private boolean isOnScreen(Vec3d screenPos) {
      return Double.isFinite(screenPos.x) && Double.isFinite(screenPos.y) && screenPos.z > 0.0 && screenPos.z < 1.0;
   }

   private boolean isValidScreenPoint(Vec3d screenPos) {
      return Double.isFinite(screenPos.x) && Double.isFinite(screenPos.y) && screenPos.z > 0.0 && screenPos.z < 1.0;
   }

   private String formatTime(double seconds) {
      return String.format(Locale.ROOT, "%.1f s", seconds);
   }

   private Prediction.TagLayout createTagLayout(Prediction.PredictionResult result, Vec3d screen) {
      String text = this.formatTime(result.timeSeconds());
      float textWidth = FontRenderers.sf_medium.getStringWidth(text);
      float width = Math.max(35.0F, textWidth + 20.0F);
      float height = 14.0F;
      float x = (float)screen.x - width / 2.0F;
      float y = (float)screen.y + 4.0F;
      float iconScale = 0.8F;
      float iconSize = 16.0F * iconScale;
      float iconX = x + 2.0F;
      float iconY = y + (height - iconSize) / 2.0F;
      float centeredTextX = x + width / 2.0F - textWidth / 2.0F;
      float minTextX = iconX + iconSize + 2.0F;
      float textX = Math.max(minTextX, centeredTextX);
      float textHeight = FontRenderers.sf_medium.getStringHeight(text);
      float textY = y + (height - textHeight) / 2.0F;
      return new Prediction.TagLayout(text, x, y, width, height, iconX, iconY, iconScale, iconSize, textX, textY);
   }

   private record PredictionResult(List<Vec3d> path, Vec3d landingPos, Vec3d displayPos, double timeSeconds, ItemStack icon) {
   }

   private record TagLayout(
      String text, float x, float y, float width, float height, float iconX, float iconY, float iconScale, float iconSize, float textX, float textY
   ) {
   }
}
