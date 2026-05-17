package vcore.features.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.Box;
import vcore.core.manager.client.ModuleManager;
import vcore.core.manager.client.ServerManager;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public final class HitBox extends Module {
   public static final Setting<Float> XZExpand = new Setting<>("XZ Expand", 0.7F, 0.0F, 5.0F);
   public static final Setting<Float> YExpand = new Setting<>("Y Expand", 0.0F, 0.0F, 5.0F);
   public static final Setting<Boolean> affectToAura = new Setting<>("Affect To Aura", false);
   public static final Setting<Boolean> OnlyWeapon = new Setting<>("Only Weapon", false);
   public static final Setting<Boolean> RenderHitbox = new Setting<>("Render Hitbox", true);
   private static final long ANIM_DURATION_MS = 250L;
   private static final ThreadLocal<Boolean> SKIP_EXPAND = ThreadLocal.withInitial(() -> false);
   private final Map<Integer, HitBox.RenderAnimState> animStates = new HashMap<>();

   public HitBox() {
      super("HitBoxes", "Increases entity hitboxes.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.animStates.clear();
   }

   @Override
   public void onDisable() {
      this.animStates.clear();
   }

   @Override
   public String getDisplayInfo() {
      String info = "H: " + ServerManager.round2(XZExpand.getValue().floatValue()) + " V: " + ServerManager.round2(YExpand.getValue().floatValue());
      if (OnlyWeapon.getValue()) {
         info = info + " •Weapon";
      }

      if (RenderHitbox.getValue()) {
         info = info + " •Render";
      }

      return info;
   }

   public boolean shouldExpand(Entity entity) {
      if (mc == null || mc.player == null) {
         return false;
      } else if (!(entity instanceof PlayerEntity)) {
         return false;
      } else if (entity.method_5628() == mc.player.method_5628()) {
         return false;
      } else {
         return ModuleManager.aura.isEnabled() && !affectToAura.getValue() ? false : this.hasWeapon();
      }
   }

   public boolean hasWeapon() {
      if (!OnlyWeapon.getValue()) {
         return true;
      }

      if (mc.player == null) {
         return false;
      }

      Item handItem = mc.player.method_6047().getItem();
      return handItem instanceof SwordItem || handItem instanceof AxeItem || handItem instanceof MaceItem;
   }

   public static Box getBaseBoundingBox(Entity entity) {
      boolean previous = isSkippingExpand();
      SKIP_EXPAND.set(true);

      try {
         return entity.method_5829();
      } finally {
         SKIP_EXPAND.set(previous);
      }
   }

   public static boolean isSkippingExpand() {
      return Boolean.TRUE.equals(SKIP_EXPAND.get());
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (RenderHitbox.getValue() && mc != null && mc.world != null) {
         long now = System.currentTimeMillis();
         float tickDelta = Render3DEngine.getTickDelta();
         Map<Integer, HitBox.RenderAnimState> keep = new HashMap<>();
         List<HitBox.RenderAction> renderActions = new ArrayList<>();

         for (PlayerEntity player : mc.world.method_18456()) {
            if (this.shouldExpand(player)) {
               double targetExpandXZ = XZExpand.getValue().floatValue();
               double targetExpandY = YExpand.getValue().floatValue();
               HitBox.RenderAnimState state = this.animStates
                  .getOrDefault(player.method_5628(), new HitBox.RenderAnimState(0.0, 0.0, targetExpandXZ, targetExpandY, now));
               if (!sameExpand(state.toExpandXZ, state.toExpandY, targetExpandXZ, targetExpandY)) {
                  state = new HitBox.RenderAnimState(state.currentExpandXZ(now), state.currentExpandY(now), targetExpandXZ, targetExpandY, now);
               }

               keep.put(player.method_5628(), state);
               float progress = (float)Math.min(1.0, (now - state.startTime) / 250.0);
               double expandXZ = lerp(state.fromExpandXZ, state.toExpandXZ, progress);
               double expandY = lerp(state.fromExpandY, state.toExpandY, progress);
               Box baseBox = interpolateBox(player, tickDelta);
               Box renderBox = expandBox(baseBox, expandXZ, expandY);
               renderActions.add(new HitBox.RenderAction(renderBox, Color.WHITE));
            }
         }

         this.renderHitboxes(renderActions);
         this.animStates.clear();
         this.animStates.putAll(keep);
      }
   }

   private void renderHitboxes(List<HitBox.RenderAction> renderActions) {
      if (!renderActions.isEmpty()) {
         Render3DEngine.setupRender();
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
         RenderSystem.lineWidth(1.0F);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);

         for (HitBox.RenderAction action : renderActions) {
            MatrixStack matrices = Render3DEngine.matrixFrom(action.box().minX, action.box().minY, action.box().minZ);
            Render3DEngine.setOutlinePoints(action.box(), matrices, buffer, action.color());
         }

         Render2DEngine.endBuilding(buffer);
         RenderSystem.enableCull();
         RenderSystem.lineWidth(1.0F);
         RenderSystem.enableDepthTest();
         Render3DEngine.endRender();
      }
   }

   private static double lerp(double a, double b, double t) {
      return a + (b - a) * t;
   }

   private static Box interpolateBox(PlayerEntity player, float tickDelta) {
      Box box = getBaseBoundingBox(player);
      double dx = player.method_23317() - player.field_6014;
      double dy = player.method_23318() - player.field_6036;
      double dz = player.method_23321() - player.field_5969;
      double k = 1.0 - tickDelta;
      double minX = box.minX - dx * k;
      double minY = box.minY - dy * k;
      double minZ = box.minZ - dz * k;
      double maxX = box.maxX - dx * k;
      double maxY = box.maxY - dy * k;
      double maxZ = box.maxZ - dz * k;
      return new Box(minX, minY, minZ, maxX, maxY, maxZ);
   }

   private static boolean sameExpand(double axz, double ay, double bxz, double by) {
      double eps = 1.0E-4;
      return Math.abs(axz - bxz) < eps && Math.abs(ay - by) < eps;
   }

   private static Box expandBox(Box base, double expandXZ, double expandY) {
      double halfXZ = expandXZ / 2.0;
      double halfY = expandY / 2.0;
      return new Box(base.minX - halfXZ, base.minY - halfY, base.minZ - halfXZ, base.maxX + halfXZ, base.maxY + halfY, base.maxZ + halfXZ);
   }

   private static Box shrinkBox(Box expanded, double expandXZ, double expandY) {
      double halfXZ = expandXZ / 2.0;
      double halfY = expandY / 2.0;
      double minX = expanded.minX + halfXZ;
      double minY = expanded.minY + halfY;
      double minZ = expanded.minZ + halfXZ;
      double maxX = expanded.maxX - halfXZ;
      double maxY = expanded.maxY - halfY;
      double maxZ = expanded.maxZ - halfXZ;
      return !(minX >= maxX) && !(minY >= maxY) && !(minZ >= maxZ) ? new Box(minX, minY, minZ, maxX, maxY, maxZ) : expanded;
   }

   private record RenderAction(Box box, Color color) {
   }

   private record RenderAnimState(double fromExpandXZ, double fromExpandY, double toExpandXZ, double toExpandY, long startTime) {
      double currentExpandXZ(long now) {
         float progress = (float)Math.min(1.0, (now - this.startTime) / 250.0);
         return HitBox.lerp(this.fromExpandXZ, this.toExpandXZ, progress);
      }

      double currentExpandY(long now) {
         float progress = (float)Math.min(1.0, (now - this.startTime) / 250.0);
         return HitBox.lerp(this.fromExpandY, this.toExpandY, progress);
      }
   }
}
