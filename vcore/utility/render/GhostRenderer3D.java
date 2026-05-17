package vcore.utility.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import vcore.features.modules.Module;
import vcore.features.modules.render.HudEditor;

public class GhostRenderer3D {
   private static final float TRAIL_LENGTH = 20.5F;
   private static final double TRAIL_POINT_SPACING = 0.045;
   private static final int MAX_POINTS_PER_TICK = 10;
   public static final double TRAIL_VERTICAL_OFFSET = 0.7;
   private static final float GLOW_STRENGTH = 0.55F;
   private static final float INNER_GLOW_SCALE = 1.8F;
   private static final float OUTER_GLOW_SCALE = 2.6F;
   private static final float INNER_GLOW_ALPHA = 0.42F;
   private static final float OUTER_GLOW_ALPHA = 0.2F;
   private Vec3d prevPosition = Vec3d.ZERO;
   private Vec3d position;
   private Vec3d motion;
   private final List<Vector4f> tail = new ArrayList<>();
   private final float size;

   public GhostRenderer3D(Vec3d position, Vec3d motion, float size) {
      this.prevPosition = position;
      this.position = position;
      this.motion = motion;
      this.size = size;
   }

   public void tick() {
      this.prevPosition = this.position;
      this.position = this.position.add(this.motion);
      this.addTrailPoints(this.prevPosition, this.position);
      float fps = Math.max(Module.mc.getCurrentFps(), 5);
      float deltaY = 0.004F / fps * 300.0F;
      float deltaLife = 0.3F / fps * 300.0F;
      Iterator<Vector4f> iterator = this.tail.iterator();

      while (iterator.hasNext()) {
         Vector4f vec = iterator.next();
         vec.set(vec.x(), vec.y() + deltaY, vec.z(), vec.w() - deltaLife);
         if (vec.w() <= 0.0F) {
            iterator.remove();
         }
      }

      double damp = 0.95 / fps * 300.0;
      this.motion = this.motion.multiply(damp, damp, damp);
   }

   private void addTrailPoints(Vec3d from, Vec3d to) {
      double distance = from.distanceTo(to);
      if (distance <= 1.0E-4) {
         this.tail.add(new Vector4f((float)to.x, (float)(to.y + 0.7), (float)to.z, 20.5F));
      } else {
         int interpolatedSteps = (int)Math.ceil(distance / 0.045) + 1;
         int steps = Math.max(2, Math.min(10, interpolatedSteps));

         for (int step = 1; step <= steps; step++) {
            double delta = (double)step / steps;
            Vec3d point = from.lerp(to, delta);
            this.tail.add(new Vector4f((float)point.x, (float)(point.y + 0.7), (float)point.z, 20.5F));
         }
      }
   }

   public void render(BufferBuilder buffer, Camera camera) {
      for (Vector4f vec : this.tail) {
         if (!(vec.w() <= 0.0F)) {
            float progress = vec.w() / 20.5F;
            float miniSize = this.size * progress;
            double relX = vec.x() - camera.getPos().x;
            double relY = vec.y() - camera.getPos().y;
            double relZ = vec.z() - camera.getPos().z;
            MatrixStack matrices = new MatrixStack();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(relX, relY, relZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            int alphaValue = MathHelper.clamp((int)(progress * 255.0F), 0, 255);
            int color = Render2DEngine.injectAlpha(HudEditor.getColor(0), alphaValue).getRGB();
            int r = color >> 16 & 0xFF;
            int g = color >> 8 & 0xFF;
            int b = color & 0xFF;
            renderQuad(buffer, matrix, miniSize * 2.6F, r, g, b, scaleAlpha(alphaValue, 0.11000001F));
            renderQuad(buffer, matrix, miniSize * 1.8F, r, g, b, scaleAlpha(alphaValue, 0.23099999F));
            renderQuad(buffer, matrix, miniSize, r, g, b, alphaValue);
         }
      }
   }

   private static void renderQuad(BufferBuilder buffer, Matrix4f matrix, float size, int red, int green, int blue, int alpha) {
      if (!(size <= 0.0F) && alpha > 0) {
         float halfSize = size / 2.0F;
         buffer.method_22918(matrix, -halfSize, halfSize, 0.0F).texture(0.0F, 1.0F).color(red, green, blue, alpha);
         buffer.method_22918(matrix, halfSize, halfSize, 0.0F).texture(1.0F, 1.0F).color(red, green, blue, alpha);
         buffer.method_22918(matrix, halfSize, -halfSize, 0.0F).texture(1.0F, 0.0F).color(red, green, blue, alpha);
         buffer.method_22918(matrix, -halfSize, -halfSize, 0.0F).texture(0.0F, 0.0F).color(red, green, blue, alpha);
      }
   }

   private static int scaleAlpha(int alpha, float factor) {
      return MathHelper.clamp((int)(alpha * factor), 0, 255);
   }

   public Vec3d getPosition() {
      return this.position;
   }

   public void setPosition(Vec3d position) {
      this.prevPosition = position;
      this.position = position;
   }

   public Vec3d getMotion() {
      return this.motion;
   }

   public void setMotion(Vec3d motion) {
      this.motion = motion;
   }
}
