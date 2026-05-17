package vcore.utility.render.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import vcore.features.modules.Module;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.render.HudEditor;
import vcore.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import vcore.utility.render.shaders.satin.api.managed.ShaderEffectManager;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform1i;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import vcore.utility.render.shaders.satin.api.managed.uniform.Uniform3f;

public class MainMenuProgram {
   private static final float OFFSCREEN_MOUSE = -1000.0F;
   private static final float ENABLED = 1.0F;
   private static final float ANIMATION_SPEED = 1.0F;
   private static final float LINE_DISTANCE = 0.05F;
   private static final float LINE_COUNT = 6.0F;
   private static final float MOUSE_DAMPING = 0.05F;
   private static final float PARALLAX_STRENGTH_VALUE = 0.2F;
   private static final float GLOW_STRENGTH_VALUE = 0.55F;
   private final long startTimeNanos = System.nanoTime();
   private float targetMouseX = -1000.0F;
   private float targetMouseY = -1000.0F;
   private float smoothMouseX = -1000.0F;
   private float smoothMouseY = -1000.0F;
   private float bendInfluenceValue;
   private float targetParallaxX;
   private float targetParallaxY;
   private float smoothParallaxX;
   private float smoothParallaxY;
   private Uniform1f iTime;
   private Uniform3f iResolution;
   private Uniform1i mode;
   private Uniform1f animationSpeed;
   private Uniform1f enableTop;
   private Uniform1f enableMiddle;
   private Uniform1f enableBottom;
   private Uniform1f topLineCount;
   private Uniform1f middleLineCount;
   private Uniform1f bottomLineCount;
   private Uniform1f topLineDistance;
   private Uniform1f middleLineDistance;
   private Uniform1f bottomLineDistance;
   private Uniform3f topWavePosition;
   private Uniform3f middleWavePosition;
   private Uniform3f bottomWavePosition;
   private Uniform2f iMouse;
   private Uniform1f interactive;
   private Uniform1f bendRadius;
   private Uniform1f bendStrength;
   private Uniform1f bendInfluence;
   private Uniform1f parallax;
   private Uniform2f parallaxOffset;
   private Uniform3f middleLineColor;
   private Uniform3f sideLineColor;
   private Uniform1f glowStrength;
   public static final ManagedCoreShader MAIN_MENU = ShaderEffectManager.getInstance()
      .manageCoreShader(Identifier.of("vcore", "mainmenu"), VertexFormats.POSITION);

   public MainMenuProgram() {
      this.setup();
   }

   public void setParameters(float x, float y, float width, float height) {
      ClientSettings.PanoramaMode panoramaMode = ClientSettings.panoramaMode.getValue();
      float safeWidth = Math.max(1.0F, width);
      float safeHeight = Math.max(1.0F, height);
      float scaleFactor = (float)Module.mc.getWindow().getScaleFactor();
      float pixelWidth = safeWidth * scaleFactor;
      float pixelHeight = safeHeight * scaleFactor;
      this.iResolution.set(pixelWidth, pixelHeight, 1.0F);
      this.mode.set(panoramaMode.ordinal());
      this.iTime.set((float)(System.nanoTime() - this.startTimeNanos) / 1.0E9F);
      this.animationSpeed.set(1.0F);
      HudEditor.Theme theme = HudEditor.getCurrentTheme();
      this.setColorUniform(this.middleLineColor, theme.color1);
      this.setColorUniform(this.sideLineColor, theme.color2);
      this.glowStrength.set(0.55F);
      double rawMouseX = Module.mc.mouse.getX();
      double rawMouseY = Module.mc.mouse.getY();
      double scaledMouseX = rawMouseX / scaleFactor;
      double scaledMouseY = rawMouseY / scaleFactor;
      double clampedScaledX = Math.max(0.0, Math.min(safeWidth, scaledMouseX));
      double clampedScaledY = Math.max(0.0, Math.min(safeHeight, scaledMouseY));
      double clampedRawX = Math.max(0.0, Math.min(pixelWidth, rawMouseX));
      double clampedRawY = Math.max(0.0, Math.min(pixelHeight, rawMouseY));
      boolean inside = scaledMouseX >= 0.0 && scaledMouseX <= safeWidth && scaledMouseY >= 0.0 && scaledMouseY <= safeHeight;
      boolean floatingLinesMode = panoramaMode == ClientSettings.PanoramaMode.FloatingLines;
      if (floatingLinesMode) {
         this.enableTop.set(1.0F);
         this.enableMiddle.set(1.0F);
         this.enableBottom.set(1.0F);
         this.topLineCount.set(6.0F);
         this.middleLineCount.set(6.0F);
         this.bottomLineCount.set(6.0F);
         this.topLineDistance.set(0.05F);
         this.middleLineDistance.set(0.05F);
         this.bottomLineDistance.set(0.05F);
         this.topWavePosition.set(10.0F, 0.5F, -0.4F);
         this.middleWavePosition.set(5.0F, 0.0F, 0.2F);
         this.bottomWavePosition.set(2.0F, -0.7F, -1.0F);
         this.interactive.set(1.0F);
         this.bendRadius.set(5.0F);
         this.bendStrength.set(-0.5F);
         this.parallax.set(1.0F);
         this.targetMouseX = (float)clampedRawX;
         this.targetMouseY = (float)(pixelHeight - clampedRawY);
         this.smoothMouseX = this.damp(this.smoothMouseX, this.targetMouseX);
         this.smoothMouseY = this.damp(this.smoothMouseY, this.targetMouseY);
         this.iMouse.set(this.smoothMouseX, this.smoothMouseY);
         this.bendInfluenceValue = this.damp(this.bendInfluenceValue, inside ? 1.0F : 0.0F);
         this.bendInfluence.set(this.bendInfluenceValue);
         this.targetParallaxX = (float)((clampedScaledX - safeWidth * 0.5F) / safeWidth) * 0.2F;
         this.targetParallaxY = (float)(-(clampedScaledY - safeHeight * 0.5F) / safeHeight) * 0.2F;
         this.smoothParallaxX = this.damp(this.smoothParallaxX, this.targetParallaxX);
         this.smoothParallaxY = this.damp(this.smoothParallaxY, this.targetParallaxY);
         this.parallaxOffset.set(this.smoothParallaxX, this.smoothParallaxY);
      } else {
         this.enableTop.set(0.0F);
         this.enableMiddle.set(0.0F);
         this.enableBottom.set(0.0F);
         this.topLineCount.set(0.0F);
         this.middleLineCount.set(0.0F);
         this.bottomLineCount.set(0.0F);
         this.topLineDistance.set(0.05F);
         this.middleLineDistance.set(0.05F);
         this.bottomLineDistance.set(0.05F);
         this.topWavePosition.set(10.0F, 0.5F, -0.4F);
         this.middleWavePosition.set(5.0F, 0.0F, 0.2F);
         this.bottomWavePosition.set(2.0F, -0.7F, -1.0F);
         this.interactive.set(0.0F);
         this.bendRadius.set(5.0F);
         this.bendStrength.set(-0.5F);
         this.bendInfluenceValue = this.damp(this.bendInfluenceValue, 0.0F);
         this.bendInfluence.set(this.bendInfluenceValue);
         this.parallax.set(0.0F);
         this.smoothParallaxX = this.damp(this.smoothParallaxX, 0.0F);
         this.smoothParallaxY = this.damp(this.smoothParallaxY, 0.0F);
         this.parallaxOffset.set(this.smoothParallaxX, this.smoothParallaxY);
         this.iMouse.set(-1000.0F, -1000.0F);
      }
   }

   public void use() {
      RenderSystem.setShader(MAIN_MENU::getProgram);
   }

   private void setColorUniform(Uniform3f uniform, Color color) {
      uniform.set(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
   }

   private float damp(float current, float target) {
      return current + (target - current) * 0.05F;
   }

   protected void setup() {
      this.iTime = MAIN_MENU.findUniform1f("iTime");
      this.iResolution = MAIN_MENU.findUniform3f("iResolution");
      this.mode = MAIN_MENU.findUniform1i("mode");
      this.animationSpeed = MAIN_MENU.findUniform1f("animationSpeed");
      this.enableTop = MAIN_MENU.findUniform1f("enableTop");
      this.enableMiddle = MAIN_MENU.findUniform1f("enableMiddle");
      this.enableBottom = MAIN_MENU.findUniform1f("enableBottom");
      this.topLineCount = MAIN_MENU.findUniform1f("topLineCount");
      this.middleLineCount = MAIN_MENU.findUniform1f("middleLineCount");
      this.bottomLineCount = MAIN_MENU.findUniform1f("bottomLineCount");
      this.topLineDistance = MAIN_MENU.findUniform1f("topLineDistance");
      this.middleLineDistance = MAIN_MENU.findUniform1f("middleLineDistance");
      this.bottomLineDistance = MAIN_MENU.findUniform1f("bottomLineDistance");
      this.topWavePosition = MAIN_MENU.findUniform3f("topWavePosition");
      this.middleWavePosition = MAIN_MENU.findUniform3f("middleWavePosition");
      this.bottomWavePosition = MAIN_MENU.findUniform3f("bottomWavePosition");
      this.iMouse = MAIN_MENU.findUniform2f("iMouse");
      this.interactive = MAIN_MENU.findUniform1f("interactive");
      this.bendRadius = MAIN_MENU.findUniform1f("bendRadius");
      this.bendStrength = MAIN_MENU.findUniform1f("bendStrength");
      this.bendInfluence = MAIN_MENU.findUniform1f("bendInfluence");
      this.parallax = MAIN_MENU.findUniform1f("parallax");
      this.parallaxOffset = MAIN_MENU.findUniform2f("parallaxOffset");
      this.middleLineColor = MAIN_MENU.findUniform3f("middleLineColor");
      this.sideLineColor = MAIN_MENU.findUniform3f("sideLineColor");
      this.glowStrength = MAIN_MENU.findUniform1f("glowStrength");
   }
}
