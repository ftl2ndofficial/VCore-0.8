package vcore.utility.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import vcore.Vcore;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;

public class Render3DEngine {
   public static List<Render3DEngine.FillAction> FILLED_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.OutlineAction> OUTLINE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.FadeAction> FADE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.FillSideAction> FILLED_SIDE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.OutlineSideAction> OUTLINE_SIDE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.DebugLineAction> DEBUG_LINE_QUEUE = new ArrayList<>();
   public static List<Render3DEngine.LineAction> LINE_QUEUE = new ArrayList<>();
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();
   private static float prevCircleStep;
   private static float circleStep;

   public static void onRender3D(MatrixStack stack) {
      if (!FILLED_QUEUE.isEmpty() || !FADE_QUEUE.isEmpty() || !FILLED_SIDE_QUEUE.isEmpty()) {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferBuilder = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         RenderSystem.disableDepthTest();
         setupRender();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         FILLED_QUEUE.forEach(action -> setFilledBoxVertexes(bufferBuilder, stack.peek().getPositionMatrix(), action.box(), action.color()));
         FADE_QUEUE.forEach(action -> setFilledFadePoints(action.box(), bufferBuilder, stack.peek().getPositionMatrix(), action.color(), action.color2()));
         FILLED_SIDE_QUEUE.forEach(action -> setFilledSidePoints(bufferBuilder, stack.peek().getPositionMatrix(), action.box, action.color(), action.side()));
         Render2DEngine.endBuilding(bufferBuilder);
         endRender();
         RenderSystem.enableDepthTest();
         FADE_QUEUE.clear();
         FILLED_SIDE_QUEUE.clear();
         FILLED_QUEUE.clear();
      }

      if (!OUTLINE_QUEUE.isEmpty() || !OUTLINE_SIDE_QUEUE.isEmpty()) {
         setupRender();
         Tessellator tessellator = Tessellator.getInstance();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
         Set<Float> lineWidths = new LinkedHashSet<>();
         OUTLINE_QUEUE.forEach(action -> lineWidths.add(action.lineWidth()));
         OUTLINE_SIDE_QUEUE.forEach(action -> lineWidths.add(action.lineWidth()));

         for (float lineWidth : lineWidths) {
            BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.LINES);
            RenderSystem.lineWidth(lineWidth);
            OUTLINE_QUEUE.forEach(action -> {
               if (Float.compare(action.lineWidth(), lineWidth) == 0) {
                  setOutlinePoints(action.box(), matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color());
               }
            });
            OUTLINE_SIDE_QUEUE.forEach(action -> {
               if (Float.compare(action.lineWidth(), lineWidth) == 0) {
                  setSideOutlinePoints(action.box, matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color(), action.side());
               }
            });
            Render2DEngine.endBuilding(buffer);
         }

         RenderSystem.enableCull();
         RenderSystem.lineWidth(1.0F);
         RenderSystem.enableDepthTest();
         endRender();
         OUTLINE_QUEUE.clear();
         OUTLINE_SIDE_QUEUE.clear();
      }

      if (!DEBUG_LINE_QUEUE.isEmpty()) {
         setupRender();
         RenderSystem.disableDepthTest();
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.LINES);
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
         DEBUG_LINE_QUEUE.forEach(
            action -> {
               MatrixStack matrices = matrixFrom(action.start.method_10216(), action.start.method_10214(), action.start.method_10215());
               vertexLine(
                  matrices,
                  buffer,
                  0.0F,
                  0.0F,
                  0.0F,
                  (float)(action.end.method_10216() - action.start.method_10216()),
                  (float)(action.end.method_10214() - action.start.method_10214()),
                  (float)(action.end.method_10215() - action.start.method_10215()),
                  action.color
               );
            }
         );
         Render2DEngine.endBuilding(buffer);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         endRender();
         DEBUG_LINE_QUEUE.clear();
      }

      if (!LINE_QUEUE.isEmpty()) {
         setupRender();
         Tessellator tessellator = Tessellator.getInstance();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
         RenderSystem.lineWidth(2.5F);
         RenderSystem.disableDepthTest();
         BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.LINES);
         LINE_QUEUE.forEach(
            action -> {
               MatrixStack matrices = matrixFrom(action.start.method_10216(), action.start.method_10214(), action.start.method_10215());
               vertexLine(
                  matrices,
                  buffer,
                  0.0F,
                  0.0F,
                  0.0F,
                  (float)(action.end.method_10216() - action.start.method_10216()),
                  (float)(action.end.method_10214() - action.start.method_10214()),
                  (float)(action.end.method_10215() - action.start.method_10215()),
                  action.color
               );
            }
         );
         Render2DEngine.endBuilding(buffer);
         RenderSystem.enableCull();
         RenderSystem.lineWidth(1.0F);
         RenderSystem.enableDepthTest();
         endRender();
         LINE_QUEUE.clear();
      }
   }

   @Deprecated
   public static void drawFilledBox(MatrixStack stack, Box box, Color c) {
      FILLED_QUEUE.add(new Render3DEngine.FillAction(box, c));
   }

   public static void setFilledBoxVertexes(@NotNull BufferBuilder bufferBuilder, Matrix4f m, @NotNull Box box, @NotNull Color c) {
      float minX = (float)(box.minX - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216());
      float minY = (float)(box.minY - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214());
      float minZ = (float)(box.minZ - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215());
      float maxX = (float)(box.maxX - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216());
      float maxY = (float)(box.maxY - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214());
      float maxZ = (float)(box.maxZ - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215());
      bufferBuilder.method_22918(m, minX, minY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, minY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, minY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, minY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, minY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, maxY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, maxY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, minY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, minY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, maxY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, minY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, minY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, minY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, minY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, minY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, maxY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, maxY, minZ).color(c.getRGB());
      bufferBuilder.method_22918(m, minX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, maxY, maxZ).color(c.getRGB());
      bufferBuilder.method_22918(m, maxX, maxY, minZ).color(c.getRGB());
   }

   @NotNull
   public static Box interpolateBox(@NotNull Box from, @NotNull Box to, float delta) {
      double X = Render2DEngine.interpolate(from.maxX, to.maxX, delta);
      double Y = Render2DEngine.interpolate(from.maxY, to.maxY, delta);
      double Z = Render2DEngine.interpolate(from.maxZ, to.maxZ, delta);
      double X1 = Render2DEngine.interpolate(from.minX, to.minX, delta);
      double Y1 = Render2DEngine.interpolate(from.minY, to.minY, delta);
      double Z1 = Render2DEngine.interpolate(from.minZ, to.minZ, delta);
      return new Box(X1, Y1, Z1, X, Y, Z);
   }

   @Deprecated
   public static void drawFilledSide(MatrixStack stack, @NotNull Box box, Color c, Direction dir) {
      FILLED_SIDE_QUEUE.add(new Render3DEngine.FillSideAction(box, c, dir));
   }

   public static void setFilledSidePoints(BufferBuilder buffer, Matrix4f matrix, Box box, Color c, Direction dir) {
      float minX = (float)(box.minX - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216());
      float minY = (float)(box.minY - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214());
      float minZ = (float)(box.minZ - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215());
      float maxX = (float)(box.maxX - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216());
      float maxY = (float)(box.maxY - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214());
      float maxZ = (float)(box.maxZ - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215());
      if (dir == Direction.DOWN) {
         buffer.method_22918(matrix, minX, minY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, minY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, minY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, minX, minY, maxZ).color(c.getRGB());
      }

      if (dir == Direction.NORTH) {
         buffer.method_22918(matrix, minX, minY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, minX, maxY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, maxY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, minY, minZ).color(c.getRGB());
      }

      if (dir == Direction.EAST) {
         buffer.method_22918(matrix, maxX, minY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, maxY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, maxY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, minY, maxZ).color(c.getRGB());
      }

      if (dir == Direction.SOUTH) {
         buffer.method_22918(matrix, minX, minY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, minY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, maxY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, minX, maxY, maxZ).color(c.getRGB());
      }

      if (dir == Direction.WEST) {
         buffer.method_22918(matrix, minX, minY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, minX, minY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, minX, maxY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, minX, maxY, minZ).color(c.getRGB());
      }

      if (dir == Direction.UP) {
         buffer.method_22918(matrix, minX, maxY, minZ).color(c.getRGB());
         buffer.method_22918(matrix, minX, maxY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, maxY, maxZ).color(c.getRGB());
         buffer.method_22918(matrix, maxX, maxY, minZ).color(c.getRGB());
      }
   }

   public static void drawTextIn3D(String text, @NotNull Vec3d pos, double offX, double offY, double textOffset, @NotNull Color color) {
      MatrixStack matrices = new MatrixStack();
      Camera camera = Module.mc.gameRenderer.getCamera();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(pos.method_10216() - camera.getPos().x, pos.method_10214() - camera.getPos().y, pos.method_10215() - camera.getPos().z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      setupRender();
      matrices.translate(offX, offY - 0.1, -0.01);
      matrices.scale(-0.025F, -0.025F, 0.0F);
      FontRenderers.sf_medium.drawCenteredString(matrices, text, textOffset, 0.0, color.getRGB());
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      endRender();
   }

   @NotNull
   public static Vec3d worldSpaceToScreenSpace(@NotNull Vec3d pos) {
      Camera camera = Module.mc.getEntityRenderDispatcher().camera;
      int displayHeight = Module.mc.getWindow().getHeight();
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      Vector3f target = new Vector3f();
      double deltaX = pos.x - camera.getPos().x;
      double deltaY = pos.y - camera.getPos().y;
      double deltaZ = pos.z - camera.getPos().z;
      Vector4f transformedCoordinates = new Vector4f((float)deltaX, (float)deltaY, (float)deltaZ, 1.0F).mul(lastWorldSpaceMatrix);
      Matrix4f matrixProj = new Matrix4f(lastProjMat);
      Matrix4f matrixModel = new Matrix4f(lastModMat);
      matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3d(target.x / getScaleFactor(), (displayHeight - target.y) / getScaleFactor(), target.z);
   }

   public static double getScaleFactor() {
      return Module.mc.getWindow().getScaleFactor();
   }

   @Deprecated
   public static void drawFilledFadeBox(@NotNull MatrixStack stack, @NotNull Box box, @NotNull Color c, @NotNull Color c1) {
      FADE_QUEUE.add(new Render3DEngine.FadeAction(box, c, c1));
   }

   public static void setFilledFadePoints(Box box, BufferBuilder buffer, Matrix4f posMatrix, Color c, Color c1) {
      float minX = (float)(box.minX - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216());
      float minY = (float)(box.minY - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214());
      float minZ = (float)(box.minZ - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215());
      float maxX = (float)(box.maxX - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216());
      float maxY = (float)(box.maxY - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214());
      float maxZ = (float)(box.maxZ - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215());
      buffer.method_22918(posMatrix, minX, minY, minZ).color(c.getRGB());
      buffer.method_22918(posMatrix, minX, maxY, minZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, maxX, minY, minZ).color(c.getRGB());
      buffer.method_22918(posMatrix, maxX, minY, minZ).color(c.getRGB());
      buffer.method_22918(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, maxX, minY, maxZ).color(c.getRGB());
      buffer.method_22918(posMatrix, minX, minY, maxZ).color(c.getRGB());
      buffer.method_22918(posMatrix, maxX, minY, maxZ).color(c.getRGB());
      buffer.method_22918(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, minX, minY, minZ).color(c.getRGB());
      buffer.method_22918(posMatrix, minX, minY, maxZ).color(c.getRGB());
      buffer.method_22918(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, minX, maxY, minZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, minX, maxY, minZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
      buffer.method_22918(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
   }

   public static void drawLine(@NotNull Vec3d start, @NotNull Vec3d end, @NotNull Color color) {
      LINE_QUEUE.add(new Render3DEngine.LineAction(start, end, color));
   }

   @Deprecated
   public static void drawBoxOutline(@NotNull Box box, Color color, float lineWidth) {
      OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(box, color, lineWidth));
   }

   public static void setOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color) {
      box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
      vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
      vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
      vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
      vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
      vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
      vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
      vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
      vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
      vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
      vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
      vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
   }

   @Deprecated
   public static void drawSideOutline(@NotNull Box box, Color color, float lineWidth, Direction dir) {
      OUTLINE_SIDE_QUEUE.add(new Render3DEngine.OutlineSideAction(box, color, lineWidth, dir));
   }

   public static void setSideOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color, Direction dir) {
      box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      switch (dir) {
         case UP:
            vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
            vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
            vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
            vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
            break;
         case DOWN:
            vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
            vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
            vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
            vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
            break;
         case EAST:
            vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
            vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
            vertexLine(matrices, buffer, x2, y2, z2, x2, y2, z1, color);
            vertexLine(matrices, buffer, x2, y1, z2, x2, y1, z1, color);
            break;
         case WEST:
            vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
            vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
            vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
            vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
            break;
         case NORTH:
            vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
            vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
            vertexLine(matrices, buffer, x2, y1, z1, x1, y1, z1, color);
            vertexLine(matrices, buffer, x2, y2, z1, x1, y2, z1, color);
            break;
         case SOUTH:
            vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
            vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
            vertexLine(matrices, buffer, x1, y1, z2, x2, y1, z2, color);
            vertexLine(matrices, buffer, x1, y2, z2, x2, y2, z2, color);
      }
   }

   public static void drawHoleOutline(@NotNull Box box, Color color, float lineWidth) {
      setupRender();
      MatrixStack matrices = matrixFrom(box.minX, box.minY, box.minZ);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.LINES);
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
      RenderSystem.lineWidth(lineWidth);
      box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float y2 = (float)box.maxY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float z2 = (float)box.maxZ;
      vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
      vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
      vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
      vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
      vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
      vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
      vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
      vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
      Render2DEngine.endBuilding(buffer);
      RenderSystem.enableCull();
      endRender();
   }

   public static void vertexLine(
      @NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor
   ) {
      Matrix4f model = matrices.peek().getPositionMatrix();
      Entry entry = matrices.peek();
      Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);
      buffer.vertex(model, x1, y1, z1)
         .color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha())
         .normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
      buffer.vertex(model, x2, y2, z2)
         .color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha())
         .normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
   }

   @NotNull
   public static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
      float xNormal = x2 - x1;
      float yNormal = y2 - y1;
      float zNormal = z2 - z1;
      float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
      return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
   }

   @NotNull
   public static MatrixStack matrixFrom(double x, double y, double z) {
      MatrixStack matrices = new MatrixStack();
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);
      return matrices;
   }

   public static void setupRender() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void endRender() {
      RenderSystem.disableBlend();
   }

   public static void drawTargetEsp(MatrixStack stack, @NotNull Entity target) {
      ArrayList<Vec3d> vecs = new ArrayList<>();
      ArrayList<Vec3d> vecs1 = new ArrayList<>();
      ArrayList<Vec3d> vecs2 = new ArrayList<>();
      double x = target.prevX + (target.getX() - target.prevX) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216();
      double y = target.prevY + (target.getY() - target.prevY) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214();
      double z = target.prevZ + (target.getZ() - target.prevZ) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215();
      double height = target.getHeight();

      for (int i = 0; i <= 361; i++) {
         double v = Math.sin(Math.toRadians(i));
         double u = Math.cos(Math.toRadians(i));
         Vec3d vec = new Vec3d((float)(u * 0.5), height, (float)(v * 0.5));
         vecs.add(vec);
         double v1 = Math.sin(Math.toRadians((i + 120) % 360));
         double u1 = Math.cos(Math.toRadians(i + 120) % 360.0);
         Vec3d vec1 = new Vec3d((float)(u1 * 0.5), height, (float)(v1 * 0.5));
         vecs1.add(vec1);
         double v2 = Math.sin(Math.toRadians((i + 240) % 360));
         double u2 = Math.cos(Math.toRadians((i + 240) % 360));
         Vec3d vec2 = new Vec3d((float)(u2 * 0.5), height, (float)(v2 * 0.5));
         vecs2.add(vec2);
         height -= 0.004F;
      }

      stack.push();
      stack.translate(x, y, z);
      setupRender();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      Matrix4f matrix = stack.peek().getPositionMatrix();

      for (int j = 0; j < vecs.size() - 1; j++) {
         float alpha = 1.0F - (j + (float)(System.currentTimeMillis() - Vcore.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.method_22918(matrix, (float)vecs.get(j).x, (float)vecs.get(j).y, (float)vecs.get(j).z)
            .color(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.method_22918(matrix, (float)vecs.get(j + 1).x, (float)vecs.get(j + 1).y + 0.1F, (float)vecs.get(j + 1).z)
            .color(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
      }

      Render2DEngine.endBuilding(bufferBuilder);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int j = 0; j < vecs1.size() - 1; j++) {
         float alpha = 1.0F - (j + (float)(System.currentTimeMillis() - Vcore.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.method_22918(matrix, (float)vecs1.get(j).x, (float)vecs1.get(j).y, (float)vecs1.get(j).z)
            .color(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.method_22918(matrix, (float)vecs1.get(j + 1).x, (float)vecs1.get(j + 1).y + 0.1F, (float)vecs1.get(j + 1).z)
            .color(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
      }

      Render2DEngine.endBuilding(bufferBuilder);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int j = 0; j < vecs2.size() - 1; j++) {
         float alpha = 1.0F - (j + (float)(System.currentTimeMillis() - Vcore.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.method_22918(matrix, (float)vecs2.get(j).x, (float)vecs2.get(j).y, (float)vecs2.get(j).z)
            .color(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.method_22918(matrix, (float)vecs2.get(j + 1).x, (float)vecs2.get(j + 1).y + 0.1F, (float)vecs2.get(j + 1).z)
            .color(Render2DEngine.injectAlpha(HudEditor.getColor((int)(j / 20.0F)), (int)(alpha * 255.0F)).getRGB());
      }

      Render2DEngine.endBuilding(bufferBuilder);
      RenderSystem.enableCull();
      stack.translate(-x, -y, -z);
      endRender();
      RenderSystem.enableDepthTest();
      stack.pop();
   }

   public static void renderCrosses(@NotNull Box box, Color color, float lineWidth) {
      setupRender();
      MatrixStack matrices = matrixFrom(box.minX, box.minY, box.minZ);
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
      RenderSystem.lineWidth(lineWidth);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);
      box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());
      vertexLine(matrices, buffer, (float)box.maxX, (float)box.minY, (float)box.minZ, (float)box.minX, (float)box.minY, (float)box.maxZ, color);
      vertexLine(matrices, buffer, (float)box.minX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.minY, (float)box.maxZ, color);
      Render2DEngine.endBuilding(buffer);
      RenderSystem.enableCull();
      endRender();
   }

   public static void drawSphere(MatrixStack matrix, float radius, int slices, int stacks, int color) {
      float drho = (float) Math.PI / stacks;
      float dtheta = (float) (Math.PI * 2) / (slices - 1.0F);
      setupRender();

      for (int i = 1; i < stacks; i++) {
         float rho = i * drho;
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);

         for (int j = 0; j < slices; j++) {
            float theta = j * dtheta;
            float x = (float)(Math.cos(theta) * Math.sin(rho));
            float y = (float)(Math.sin(theta) * Math.sin(rho));
            float z = (float)Math.cos(rho);
            buffer.method_22918(matrix.peek().getPositionMatrix(), x * radius, y * radius, z * radius).color(color);
         }

         Render2DEngine.endBuilding(buffer);
      }

      for (int j = 0; j < slices; j++) {
         float theta = j * dtheta;
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);

         for (int var20 = 0; var20 <= stacks; var20++) {
            float rho = var20 * drho;
            float x = (float)(Math.cos(theta) * Math.sin(rho));
            float y = (float)(Math.sin(theta) * Math.sin(rho));
            float z = (float)Math.cos(rho);
            buffer.method_22918(matrix.peek().getPositionMatrix(), x * radius, y * radius, z * radius).color(color);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }

      endRender();
   }

   public static void drawCylinder(MatrixStack stack, float radius, float height, int slices, int stacks, int color) {
      float da = (float)((Math.PI * 2) / slices);
      float dz = height / stacks;
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      float y = 0.0F;

      for (int j = 0; j <= stacks; j++) {
         for (int i = 0; i <= slices; i++) {
            float x = (float)Math.cos(i * da);
            float z = (float)Math.sin(i * da);
            buffer.method_22918(stack.peek().getPositionMatrix(), x * radius, y, z * radius).color(color);
         }

         y += dz;
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);

      for (int i = 0; i <= slices; i++) {
         float x = (float)Math.cos(i * da);
         float z = (float)Math.sin(i * da);
         buffer.method_22918(stack.peek().getPositionMatrix(), x * radius, 0.0F, z * radius).color(color);
         buffer.method_22918(stack.peek().getPositionMatrix(), x * radius, height, z * radius).color(color);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   public static void drawCircle3D(MatrixStack stack, Entity ent, float radius, int color, int points, boolean hudColor, int colorOffset) {
      setupRender();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      double x = ent.prevX + (ent.getX() - ent.prevX) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216();
      double y = ent.prevY + (ent.getY() - ent.prevY) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214();
      double z = ent.prevZ + (ent.getZ() - ent.prevZ) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215();
      stack.push();
      stack.translate(x, y, z);
      Matrix4f matrix = stack.peek().getPositionMatrix();

      for (int i = 0; i <= points; i++) {
         if (hudColor) {
            color = HudEditor.getColor(i * colorOffset).getRGB();
         }

         bufferBuilder.method_22918(matrix, (float)(radius * Math.cos(i * 6.28 / points)), 0.0F, (float)(radius * Math.sin(i * 6.28 / points))).color(color);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      endRender();
      stack.translate(-x, -y, -z);
      stack.pop();
   }

   public static void drawOldTargetEsp(MatrixStack stack, Entity target) {
      double cs = prevCircleStep + (circleStep - prevCircleStep) * getTickDelta();
      double prevSinAnim = absSinAnimation(cs - 0.45F);
      double sinAnim = absSinAnimation(cs);
      double x = target.prevX + (target.getX() - target.prevX) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216();
      double y = target.prevY
         + (target.getY() - target.prevY) * getTickDelta()
         - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214()
         + prevSinAnim * target.getHeight();
      double z = target.prevZ + (target.getZ() - target.prevZ) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215();
      double nextY = target.prevY
         + (target.getY() - target.prevY) * getTickDelta()
         - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214()
         + sinAnim * target.getHeight();
      stack.push();
      setupRender();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);

      for (int i = 0; i <= 30; i++) {
         float cos = (float)(x + Math.cos(i * 6.28 / 30.0) * target.getWidth() * 0.8);
         float sin = (float)(z + Math.sin(i * 6.28 / 30.0) * target.getWidth() * 0.8);
         bufferBuilder.method_22918(stack.peek().getPositionMatrix(), cos, (float)nextY, sin)
            .color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 170).getRGB());
         bufferBuilder.method_22918(stack.peek().getPositionMatrix(), cos, (float)y, sin).color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 0).getRGB());
      }

      Render2DEngine.endBuilding(bufferBuilder);
      RenderSystem.enableCull();
      endRender();
      RenderSystem.enableDepthTest();
      stack.pop();
   }

   public static void renderGhosts(LivingEntity target, float anim, float red, float speed) {
      if (target != null && Module.mc.player != null) {
         Camera camera = Module.mc.gameRenderer.getCamera();
         Vec3d targetPos = new Vec3d(
            Render2DEngine.interpolate(target.field_6014, target.method_23317(), getTickDelta()) - camera.getPos().x,
            Render2DEngine.interpolate(target.field_6036, target.method_23318(), getTickDelta()) - camera.getPos().y,
            Render2DEngine.interpolate(target.field_5969, target.method_23321(), getTickDelta()) - camera.getPos().z
         );
         double iAge = Render2DEngine.interpolate(Module.mc.player.field_6012 - 1, Module.mc.player.field_6012, getTickDelta());
         float halfHeight = target.method_17682() / 2.0F + 0.2F;
         float baseWidth = target.method_17681() + 0.2F;
         float minY = 0.2F;
         float maxY = target.method_17682() - 0.2F;
         float hitEffect = Math.min(red * 2.0F, 2.0F);
         float acceleration = (float)Math.sin(hitEffect * Math.PI) * 0.18F;
         float bany = (float)Math.sin(hitEffect * Math.PI) * -0.04F;
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.setShaderTexture(0, TextureStorage.firefly);
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         RenderSystem.disableDepthTest();
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         int length = 10;

         for (int j = 0; j < 4; j++) {
            for (int i = 0; i <= length; i++) {
               double baseAngle = ((i / 2.0F + iAge * speed * 2.0) * length + j * 90.0F) % (length * 180.0F);
               double radians = Math.toRadians(baseAngle);
               float distanceMultiplier = 1.0F + acceleration;
               double sinQuad = Math.sin(Math.toRadians(iAge * 0.7 + i * (j + halfHeight)) * 1.1) / 2.0;
               double adjustedSin = j % 2 == 0 ? sinQuad : -sinQuad;
               double yOffset = minY + (adjustedSin + 0.5) * (maxY - minY);
               float offset = (float)(i + length) / (length + length);
               double finalWidth = baseWidth * distanceMultiplier * 1.04F;
               MatrixStack matrices = new MatrixStack();
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
               matrices.translate(targetPos.x + Math.cos(radians) * finalWidth, targetPos.y + yOffset, targetPos.z + Math.sin(radians) * finalWidth);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
               float scale = 0.6F * offset * (0.6F + speed * 0.1F) + bany;
               if (!(scale <= 0.0F)) {
                  int baseColor = HudEditor.getColor(i).getRGB();
                  int color = applyRedAndAlpha(baseColor, 1.0F + red * 10.0F, offset * anim);
                  float halfScale = scale / 2.0F;
                  Matrix4f matrix = matrices.peek().getPositionMatrix();
                  buffer.method_22918(matrix, -halfScale, halfScale, 0.0F).texture(0.0F, 1.0F).color(color);
                  buffer.method_22918(matrix, halfScale, halfScale, 0.0F).texture(1.0F, 1.0F).color(color);
                  buffer.method_22918(matrix, halfScale, -halfScale, 0.0F).texture(1.0F, 0.0F).color(color);
                  buffer.method_22918(matrix, -halfScale, -halfScale, 0.0F).texture(0.0F, 0.0F).color(color);
               }
            }
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private static int applyRedAndAlpha(int color, float redMultiplier, float alpha) {
      Color base = new Color(color, true);
      int r = MathHelper.clamp((int)(base.getRed() * redMultiplier), 0, 255);
      int a = MathHelper.clamp((int)(base.getAlpha() * alpha), 0, 255);
      return new Color(r, base.getGreen(), base.getBlue(), a).getRGB();
   }

   public static void updateTargetESP() {
      prevCircleStep = circleStep;
      circleStep += 0.15F;
   }

   public static double absSinAnimation(double input) {
      return Math.abs(1.0 + Math.sin(input)) / 2.0;
   }

   public static Vec3d interpolatePos(float prevposX, float prevposY, float prevposZ, float posX, float posY, float posZ) {
      double x = prevposX + (posX - prevposX) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10216();
      double y = prevposY + (posY - prevposY) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10214();
      double z = prevposZ + (posZ - prevposZ) * getTickDelta() - Module.mc.getEntityRenderDispatcher().camera.getPos().method_10215();
      return new Vec3d(x, y, z);
   }

   public static void drawLineDebug(Vec3d start, Vec3d end, Color color) {
      DEBUG_LINE_QUEUE.add(new Render3DEngine.DebugLineAction(start, end, color));
   }

   public static float getTickDelta() {
      return Module.mc.getRenderTickCounter().getTickDelta(true);
   }

   public record DebugLineAction(Vec3d start, Vec3d end, Color color) {
   }

   public record FadeAction(Box box, Color color, Color color2) {
   }

   public record FillAction(Box box, Color color) {
   }

   public record FillSideAction(Box box, Color color, Direction side) {
   }

   public record LineAction(Vec3d start, Vec3d end, Color color) {
   }

   public record OutlineAction(Box box, Color color, float lineWidth) {
   }

   public record OutlineSideAction(Box box, Color color, float lineWidth, Direction side) {
   }
}
