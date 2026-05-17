package vcore.features.hud;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import vcore.events.impl.EventMouse;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class HudElement {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();
   protected static final float SCREEN_PADDING = 5.0F;
   private static final float MIN_ELEMENT_SPACING = 4.0F;
   private static final float SNAP_DISTANCE = 10.0F;
   private static final float EPSILON = 0.01F;
   private final String name;
   private final PositionSetting pos;
   private final Supplier<List<HudElement>> activeElementsSupplier;
   private boolean mouseState = false;
   private boolean mouseButton = false;
   private boolean boundsValid = false;
   private float x;
   private float y;
   private float dragX;
   private float dragY;
   private float hitX;
   private float hitY;
   private float height;
   private float width;
   public static boolean anyHovered = false;
   private static HudElement currentlyDragging;

   public HudElement(String name, int width, int height, PositionSetting pos, Supplier<List<HudElement>> activeElementsSupplier) {
      this.name = name;
      this.height = height;
      this.width = width;
      this.pos = pos;
      this.activeElementsSupplier = activeElementsSupplier;
   }

   public String getName() {
      return this.name;
   }

   public void render(DrawContext context) {
      int screenWidth = mc.getWindow().getScaledWidth();
      int screenHeight = mc.getWindow().getScaledHeight();
      this.boundsValid = false;
      float minX = 5.0F / screenWidth;
      float maxX = (screenWidth - this.width - 5.0F) / screenWidth;
      float minY = 5.0F / screenHeight;
      float maxY = (screenHeight - this.height - 5.0F) / screenHeight;
      this.y = screenHeight * this.pos.getY();
      this.x = screenWidth * this.pos.getX();
      float elementCenterX = this.getPosX() + this.getWidth() / 2.0F;
      float elementCenterY = this.getPosY() + this.getHeight() / 2.0F;
      float screenCenterX = screenWidth / 2.0F;
      float screenCenterY = screenHeight / 2.0F;
      if (this.isDraggable() && mc.currentScreen instanceof ChatScreen && this.mouseButton && this.mouseState) {
         float newX = Render2DEngine.scrollAnimate((this.normaliseX() - this.dragX) / screenWidth, this.pos.getX(), 0.1F);
         float newY = Render2DEngine.scrollAnimate((this.normaliseY() - this.dragY) / screenHeight, this.pos.getY(), 0.1F);
         float desiredPosX = Math.clamp(newX, minX, maxX) * screenWidth;
         float desiredPosY = Math.clamp(newY, minY, maxY) * screenHeight;
         float anchorOffsetX = this.x - this.hitX;
         float anchorOffsetY = this.y - this.hitY;
         float desiredHitX = this.clampHitX(desiredPosX - anchorOffsetX);
         float desiredHitY = this.clampHitY(desiredPosY - anchorOffsetY);
         float[] constrainedBounds = this.constrainDraggedBounds(desiredHitX, desiredHitY);
         this.applyResolvedBounds(constrainedBounds[0], constrainedBounds[1]);
         elementCenterX = this.getPosX() + this.getWidth() / 2.0F;
         elementCenterY = this.getPosY() + this.getHeight() / 2.0F;
         float finalX = 0.0F;
         float finalY = 0.0F;
         if (Math.abs(elementCenterX - screenCenterX) <= 10.0F) {
            finalX = screenCenterX - this.getWidth() / 2.0F;
         }

         if (Math.abs(elementCenterY - screenCenterY) <= 10.0F) {
            finalY = screenCenterY - this.getHeight() / 2.0F;
         }

         for (HudElement hudElement : this.getActiveElements()) {
            if (hudElement != this && (hudElement.getPosX() != 0.0F || hudElement.getPosY() != 0.0F)) {
               if (this.getPosX() > screenWidth / 2.0F) {
                  float snappedX = hudElement.getHitX() + hudElement.getWidth() - this.getWidth();
                  if (this.isNear(hudElement.getHitX() + hudElement.getWidth(), this.getHitX() + this.getWidth()) && this.canPlaceAt(snappedX, this.getHitY())) {
                     finalX = snappedX;
                  }
               } else {
                  float snappedX = hudElement.getHitX();
                  if (this.isNear(hudElement.getHitX(), this.getHitX()) && this.canPlaceAt(snappedX, this.getHitY())) {
                     finalX = snappedX;
                  }
               }

               float snappedY = hudElement.getHitY();
               if (this.isNear(hudElement.getHitY(), this.getHitY()) && this.canPlaceAt(this.getHitX(), snappedY)) {
                  finalY = snappedY;
               }
            }
         }

         Render2DEngine.drawLine(screenWidth / 2.0F, 0.0F, screenWidth / 2.0F, screenHeight, Color.WHITE.getRGB());
         Render2DEngine.drawLine(0.0F, screenHeight / 2.0F, screenWidth, screenHeight / 2.0F, Color.WHITE.getRGB());
         if (finalX != 0.0F || finalY != 0.0F) {
            Render2DEngine.drawRound(
               context.getMatrices(),
               finalX == 0.0F ? this.getHitX() : finalX,
               finalY == 0.0F ? this.getHitY() : finalY,
               this.width,
               this.height,
               3.0F,
               new Color(2066689839, true)
            );
         }

         if (finalX != 0.0F) {
            Render2DEngine.drawLine(finalX, 0.0F, finalX, screenHeight, -1);
         }

         if (finalY != 0.0F) {
            Render2DEngine.drawLine(0.0F, finalY, screenWidth, finalY, -1);
         }
      }

      if (!this.isDraggable() || !this.mouseButton) {
         this.mouseState = false;
      } else if (!this.mouseState && this.isHovering()) {
         this.dragX = (int)(this.normaliseX() - this.pos.getX() * screenWidth);
         this.dragY = (int)(this.normaliseY() - this.pos.getY() * screenHeight);
         this.mouseState = true;
      }

      if (this.isDraggable() && this.isHovering() && mc.currentScreen instanceof ChatScreen) {
         if (GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), this.mouseState ? GLFW.glfwCreateStandardCursor(221187) : GLFW.glfwCreateStandardCursor(221188));
         }

         anyHovered = true;
      }
   }

   public void handleMouse(@NotNull EventMouse event) {
      if (!this.isDraggable()) {
         this.mouseButton = false;
         this.mouseState = false;
         if (currentlyDragging == this) {
            currentlyDragging = null;
         }
      } else if (!(mc.currentScreen instanceof ChatScreen)) {
         if (event.getAction() == 0) {
            this.mouseButton = false;
            currentlyDragging = null;
         }
      } else {
         if (event.getAction() == 0) {
            float screenCenterX = mc.getWindow().getScaledWidth() / 2.0F;
            float screenCenterY = mc.getWindow().getScaledHeight() / 2.0F;
            float elementCenterX = this.getPosX() + this.getWidth() / 2.0F;
            float elementCenterY = this.getPosY() + this.getHeight() / 2.0F;
            if (this.mouseButton && this.mouseState) {
               if (Math.abs(elementCenterX - screenCenterX) <= 10.0F) {
                  this.pos.setX((screenCenterX - this.getWidth() / 2.0F) / mc.getWindow().getScaledWidth());
               }

               if (Math.abs(elementCenterY - screenCenterY) <= 10.0F) {
                  this.pos.setY((screenCenterY - this.getHeight() / 2.0F) / mc.getWindow().getScaledHeight());
               }

               for (HudElement hudElement : this.getActiveElements()) {
                  if (hudElement != this && (hudElement.getHitX() != 0.0F || hudElement.getHitY() != 0.0F)) {
                     float hitDifX = this.getPosX() - this.getHitX();
                     float hitDifY = this.getPosY() - this.getHitY();
                     if (this.getPosX() > mc.getWindow().getScaledWidth() / 2.0F) {
                        float snappedX = hudElement.getHitX() + hudElement.getWidth() - this.getWidth();
                        if (this.isNear(hudElement.getHitX() + hudElement.getWidth(), this.getHitX() + this.getWidth())
                           && this.canPlaceAt(snappedX, this.getHitY())) {
                           this.pos.setX((snappedX + hitDifX) / mc.getWindow().getScaledWidth());
                        }
                     } else {
                        float snappedX = hudElement.getHitX();
                        if (this.isNear(hudElement.getHitX(), this.getHitX()) && this.canPlaceAt(snappedX, this.getHitY())) {
                           this.pos.setX((snappedX + hitDifX) / mc.getWindow().getScaledWidth());
                        }
                     }

                     float snappedY = hudElement.getHitY();
                     if (this.isNear(hudElement.getHitY(), this.getHitY()) && this.canPlaceAt(this.getHitX(), snappedY)) {
                        this.pos.setY((snappedY + hitDifY) / mc.getWindow().getScaledHeight());
                     }
                  }
               }

               currentlyDragging = null;
               this.mouseButton = false;
            }
         }

         if (event.getAction() == 1 && this.isHovering() && currentlyDragging == null) {
            currentlyDragging = this;
            this.mouseButton = true;
         }
      }
   }

   public void tick() {
   }

   protected boolean isDraggable() {
      return true;
   }

   protected void setPositionPixels(float x, float y) {
      this.pos.setX(x / mc.getWindow().getScaledWidth());
      this.pos.setY(y / mc.getWindow().getScaledHeight());
   }

   public int normaliseX() {
      return (int)(mc.mouse.getX() / Render3DEngine.getScaleFactor());
   }

   public int normaliseY() {
      return (int)(mc.mouse.getY() / Render3DEngine.getScaleFactor());
   }

   public boolean isHovering() {
      return this.normaliseX() > Math.min(this.hitX, this.hitX + this.width)
         && this.normaliseX() < Math.max(this.hitX, this.hitX + this.width)
         && this.normaliseY() > Math.min(this.hitY, this.hitY + this.height)
         && this.normaliseY() < Math.max(this.hitY, this.hitY + this.height);
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setHitX(float hitX) {
      this.hitX = hitX;
   }

   public void setHitY(float hitY) {
      this.hitY = hitY;
   }

   public void setBounds(float x, float y, float w, float h) {
      this.setHitX(x);
      this.setHitY(y);
      this.setWidth(w);
      this.setHeight(h);
      this.boundsValid = true;
   }

   public float getPosX() {
      return this.x;
   }

   public float getHitX() {
      return this.hitX;
   }

   public float getHitY() {
      return this.hitY;
   }

   public float getPosY() {
      return this.y;
   }

   public float getX() {
      return this.pos.getX();
   }

   public float getY() {
      return this.pos.getY();
   }

   public float getHeight() {
      return this.height;
   }

   public float getWidth() {
      return this.width;
   }

   private List<HudElement> getActiveElements() {
      if (this.activeElementsSupplier == null) {
         return Collections.emptyList();
      }

      List<HudElement> list = this.activeElementsSupplier.get();
      return list != null ? list : Collections.emptyList();
   }

   private boolean isNear(float n1, float n2) {
      return Math.abs(n1 - n2) < 10.0F;
   }

   private float[] constrainDraggedBounds(float desiredHitX, float desiredHitY) {
      float resolvedHitX = this.clampHitX(desiredHitX);
      float resolvedHitY = this.clampHitY(desiredHitY);
      float previousValidHitX = this.hitX;
      float previousValidHitY = this.hitY;
      int maxPasses = Math.max(4, this.getActiveElements().size() * 2);

      for (int pass = 0; pass < maxPasses; pass++) {
         boolean changed = false;

         for (HudElement other : this.getActiveElements()) {
            if (other != this && other.boundsValid && this.violatesMinSpacing(resolvedHitX, resolvedHitY, other)) {
               HudElement.PlacementCandidate candidate = this.projectCandidateFromPreviousValid(
                  resolvedHitX, resolvedHitY, previousValidHitX, previousValidHitY, other
               );
               if (candidate != null && (Math.abs(candidate.hitX - resolvedHitX) > 0.01F || Math.abs(candidate.hitY - resolvedHitY) > 0.01F)) {
                  resolvedHitX = candidate.hitX;
                  resolvedHitY = candidate.hitY;
                  changed = true;
               }
            }
         }

         resolvedHitX = this.clampHitX(resolvedHitX);
         resolvedHitY = this.clampHitY(resolvedHitY);
         if (!changed || this.canPlaceAt(resolvedHitX, resolvedHitY)) {
            break;
         }
      }

      if (!this.canPlaceAt(resolvedHitX, resolvedHitY) && this.canPlaceAt(previousValidHitX, previousValidHitY)) {
         resolvedHitX = previousValidHitX;
         resolvedHitY = previousValidHitY;
      }

      return new float[]{resolvedHitX, resolvedHitY};
   }

   private void applyResolvedBounds(float targetHitX, float targetHitY) {
      float resolvedHitX = this.clampHitX(targetHitX);
      float resolvedHitY = this.clampHitY(targetHitY);
      float anchorOffsetX = this.x - this.hitX;
      float anchorOffsetY = this.y - this.hitY;
      this.x = resolvedHitX + anchorOffsetX;
      this.y = resolvedHitY + anchorOffsetY;
      this.hitX = resolvedHitX;
      this.hitY = resolvedHitY;
      this.pos.setX(this.x / mc.getWindow().getScaledWidth());
      this.pos.setY(this.y / mc.getWindow().getScaledHeight());
      this.boundsValid = true;
   }

   private float clampHitX(float targetHitX) {
      float maxHitX = mc.getWindow().getScaledWidth() - this.width - 5.0F;
      return Math.clamp(targetHitX, 5.0F, Math.max(5.0F, maxHitX));
   }

   private float clampHitY(float targetHitY) {
      float maxHitY = mc.getWindow().getScaledHeight() - this.height - 5.0F;
      return Math.clamp(targetHitY, 5.0F, Math.max(5.0F, maxHitY));
   }

   private boolean canPlaceAt(float candidateHitX, float candidateHitY) {
      float boundedHitX = this.clampHitX(candidateHitX);
      float boundedHitY = this.clampHitY(candidateHitY);

      for (HudElement other : this.getActiveElements()) {
         if (other != this && other.boundsValid && this.violatesMinSpacing(boundedHitX, boundedHitY, other)) {
            return false;
         }
      }

      return true;
   }

   private boolean violatesMinSpacing(float candidateHitX, float candidateHitY, HudElement other) {
      float boundedHitX = this.clampHitX(candidateHitX);
      float boundedHitY = this.clampHitY(candidateHitY);
      boolean verticalConflict = intervalsOverlap(boundedHitX, boundedHitX + this.width, other.hitX, other.hitX + other.width)
         && intervalGap(boundedHitY, boundedHitY + this.height, other.hitY, other.hitY + other.height) < 3.99F;
      boolean horizontalConflict = intervalsOverlap(boundedHitY, boundedHitY + this.height, other.hitY, other.hitY + other.height)
         && intervalGap(boundedHitX, boundedHitX + this.width, other.hitX, other.hitX + other.width) < 3.99F;
      return verticalConflict || horizontalConflict;
   }

   private HudElement.PlacementCandidate projectCandidateFromPreviousValid(
      float currentCandidateX, float currentCandidateY, float previousValidHitX, float previousValidHitY, HudElement other
   ) {
      float leftBarrier = other.hitX - 4.0F - this.width;
      float rightBarrier = other.hitX + other.width + 4.0F;
      float topBarrier = other.hitY - 4.0F - this.height;
      float bottomBarrier = other.hitY + other.height + 4.0F;
      boolean wasLeft = previousValidHitX <= leftBarrier + 0.01F;
      boolean wasRight = previousValidHitX >= rightBarrier - 0.01F;
      boolean wasAbove = previousValidHitY <= topBarrier + 0.01F;
      boolean wasBelow = previousValidHitY >= bottomBarrier - 0.01F;
      float projectedHitX = currentCandidateX;
      float projectedHitY = currentCandidateY;
      if (intervalsOverlap(projectedHitY, projectedHitY + this.height, other.hitY - 4.0F, other.hitY + other.height + 4.0F)) {
         if (wasLeft) {
            projectedHitX = Math.min(projectedHitX, leftBarrier);
         } else if (wasRight) {
            projectedHitX = Math.max(projectedHitX, rightBarrier);
         }
      }

      projectedHitX = this.clampHitX(projectedHitX);
      if (intervalsOverlap(projectedHitX, projectedHitX + this.width, other.hitX - 4.0F, other.hitX + other.width + 4.0F)) {
         if (wasAbove) {
            projectedHitY = Math.min(projectedHitY, topBarrier);
         } else if (wasBelow) {
            projectedHitY = Math.max(projectedHitY, bottomBarrier);
         }
      }

      projectedHitY = this.clampHitY(projectedHitY);
      return this.violatesMinSpacing(projectedHitX, projectedHitY, other) ? null : new HudElement.PlacementCandidate(projectedHitX, projectedHitY);
   }

   private static boolean intervalsOverlap(float min1, float max1, float min2, float max2) {
      return Math.max(min1, min2) < Math.min(max1, max2);
   }

   private static float intervalGap(float min1, float max1, float min2, float max2) {
      return Math.max(0.0F, Math.max(min2 - max1, min1 - max2));
   }

   private static class PlacementCandidate {
      private final float hitX;
      private final float hitY;

      private PlacementCandidate(float hitX, float hitY) {
         this.hitX = hitX;
         this.hitY = hitY;
      }
   }
}
