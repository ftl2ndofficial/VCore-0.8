package vcore.features.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AntiBot;
import vcore.features.modules.combat.HitBox;
import vcore.features.modules.misc.FixHP;
import vcore.features.modules.misc.NameProtect;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class ESP extends Module {
   private final Map<LivingEntity, Vector4f> projectedBounds = new HashMap<>();
   private static final Color ENEMY_COLOR = new Color(-1, true);
   private static final Color FRIEND_COLOR = new Color(-2147437568, true);
   private static final Color TAG_FILL_COLOR = new Color(Integer.MIN_VALUE, true);
   private static final Color TEXT_COLOR = new Color(-1, true);
   private static final Color HEALTH_TOP_COLOR = new Color(0, 255, 0);
   private static final Color HEALTH_BOTTOM_COLOR = new Color(255, 0, 0);
   private static final Color HEALTH_BACKGROUND = new Color(0, 0, 0, 255);
   private static final Color HEALTH_MISSING_OVERLAY = new Color(0, 0, 0, 255);
   private final Setting<Boolean> players = new Setting<>("Players", true);
   private final Setting<Boolean> nakedOnly = new Setting<>("Naked", true, v -> this.players.getValue());
   private final Setting<Boolean> showSelf = new Setting<>("Self", false, v -> this.players.getValue());
   private final Setting<Boolean> animals = new Setting<>("Animals", false);
   private final Setting<Boolean> mobs = new Setting<>("Mobs", false);
   private final Setting<Boolean> villagers = new Setting<>("Villagers", false);
   private final Setting<Boolean> box = new Setting<>("Box", false);
   private final Setting<Boolean> drawTag = new Setting<>("NameTag", true);
   private final Setting<Boolean> drawHpTag = new Setting<>("HP", true, v -> this.drawTag.getValue());
   private final Setting<Boolean> drawHealth = new Setting<>("HealthBar", false);
   private final Setting<Boolean> drawArmor = new Setting<>("Armor", false);
   public final Setting<Boolean> syncColor = new Setting<>("SyncColor", false);
   public final Setting<ColorSetting> boxColor = new Setting<>(
      "Color", new ColorSetting(new Color(255, 255, 255, 255).getRGB()), v -> !this.syncColor.getValue()
   );

   public ESP() {
      super("ESP", "Highlights specific entities.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      this.projectedBounds.clear();
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (fullNullCheck()) {
         this.projectedBounds.clear();
      } else {
         this.projectedBounds.clear();
         this.collectTargets().forEach(entity -> {
            Vector4f rect = this.project(entity);
            if (rect != null) {
               this.projectedBounds.put(entity, rect);
            }
         });
         this.projectedBounds.forEach((entity, rect) -> this.renderEntity(context, entity, rect));
      }
   }

   private List<LivingEntity> collectTargets() {
      if (mc.player != null && mc.world != null) {
         double size = 256.0;
         Box search = Box.of(mc.player.method_19538(), size * 2.0, size * 2.0, size * 2.0);
         return mc.world.method_8390(LivingEntity.class, search, this::isTargetValid);
      } else {
         return List.of();
      }
   }

   private boolean isTargetValid(LivingEntity entity) {
      if (entity == null || !entity.method_5805()) {
         return false;
      }

      if (entity == mc.player && !this.shouldRenderSelf()) {
         return false;
      }

      if (entity instanceof PlayerEntity player) {
         if (ModuleManager.antiBot.isEnabled() && ModuleManager.antiBot.mode.getValue() == AntiBot.Mode.Matrix && AntiBot.isBot(player)) {
            return false;
         } else if (player.method_7325()) {
            return false;
         } else if (!this.players.getValue()) {
            return false;
         } else if (entity == mc.player && this.showSelf.getValue()) {
            return true;
         } else {
            return this.nakedOnly.getValue() ? true : !this.isNaked(player);
         }
      } else if (entity instanceof VillagerEntity) {
         return this.villagers.getValue();
      } else if (entity instanceof AnimalEntity) {
         return this.animals.getValue();
      } else {
         return entity instanceof HostileEntity ? this.mobs.getValue() : false;
      }
   }

   private boolean isNaked(PlayerEntity player) {
      for (ItemStack stack : player.getInventory().armor) {
         if (!stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void renderEntity(DrawContext context, LivingEntity entity, Vector4f rect) {
      MatrixStack matrices = context.getMatrices();
      if (this.box.getValue()) {
         this.drawBox(matrices, rect, entity);
      }

      if (this.drawHealth.getValue()) {
         this.drawHealthBar(matrices, rect, entity);
      }

      if (this.drawArmor.getValue() && entity instanceof PlayerEntity player) {
         this.drawArmorStrip(context, rect, player);
      }

      if (this.drawTag.getValue()) {
         this.drawNameTag(context, rect, entity);
      }
   }

   private void drawBox(MatrixStack matrices, Vector4f rect, LivingEntity entity) {
      float left = rect.x;
      float top = rect.y;
      float right = rect.z;
      float bottom = rect.w;
      float width = right - left;
      float height = bottom - top;
      float thin = 0.5F;
      Color baseColor;
      if (entity instanceof PlayerEntity player && Managers.FRIEND.isFriend(player)) {
         baseColor = FRIEND_COLOR;
      } else if (this.syncColor.getValue()) {
         baseColor = HudEditor.getColor(entity.field_6012);
      } else {
         baseColor = this.boxColor.getValue().getColorObject();
      }

      int alpha = ENEMY_COLOR.getAlpha();
      Color color = Render2DEngine.injectAlpha(baseColor, alpha);
      float horizontal = width * 0.3F;
      float vertical = height * 0.3F;
      float corner = thin + 0.2F;
      Render2DEngine.drawRect(matrices, left, top, horizontal, corner, color);
      Render2DEngine.drawRect(matrices, right - horizontal, top, horizontal, corner, color);
      Render2DEngine.drawRect(matrices, left, bottom - corner, horizontal, corner, color);
      Render2DEngine.drawRect(matrices, right - horizontal, bottom - corner, horizontal, corner, color);
      Render2DEngine.drawRect(matrices, left, top, corner, vertical, color);
      Render2DEngine.drawRect(matrices, right - corner, top, corner, vertical, color);
      Render2DEngine.drawRect(matrices, left, bottom - vertical, corner, vertical, color);
      Render2DEngine.drawRect(matrices, right - corner, bottom - vertical, corner, vertical, color);
   }

   private void drawHealthBar(MatrixStack matrices, Vector4f rect, LivingEntity entity) {
      float barWidth = 1.2F;
      float left = rect.x - (barWidth + 2.0F);
      float top = rect.y;
      float height = rect.w - rect.y;
      Render2DEngine.drawRect(matrices, left, top, barWidth, height, HEALTH_BACKGROUND);
      float maxHealth = Math.max(entity.getMaxHealth(), 1.0F);
      float currentHealth = entity instanceof PlayerEntity player && ModuleManager.fixHP.isEnabled()
         ? FixHP.getHealth(player)
         : entity.getHealth() + entity.getAbsorptionAmount();
      float health = MathHelper.clamp(currentHealth, 0.0F, maxHealth);
      float ratio = MathHelper.clamp(health / maxHealth, 0.0F, 1.0F);
      float filled = height * ratio;
      Render2DEngine.verticalGradient(matrices, left, top, left + barWidth, top + height, HEALTH_TOP_COLOR, HEALTH_BOTTOM_COLOR);
      if (ratio < 1.0F) {
         float missingHeight = height - filled;
         Render2DEngine.drawRect(matrices, left, top, barWidth, missingHeight, HEALTH_MISSING_OVERLAY);
      }
   }

   private void drawNameTag(DrawContext context, Vector4f rect, LivingEntity entity) {
      if (mc.player != null) {
         String name;
         if (entity instanceof PlayerEntity player) {
            name = NameProtect.getDisplayName(player);
         } else {
            Text display = entity.method_5476();
            name = display != null ? display.getString() : "";
         }

         float hpVal = entity instanceof PlayerEntity player && ModuleManager.fixHP.isEnabled()
            ? FixHP.getHealth(player)
            : entity.getHealth() + entity.getAbsorptionAmount();
         float nameWidth = FontRenderers.sf_medium.getStringWidth(name);
         String openBracket = " [";
         String healthValueStr = Math.round(hpVal) + " HP";
         String closeBracket = "]";
         float openWidth = this.drawHpTag.getValue() ? FontRenderers.sf_medium.getStringWidth(openBracket) : 0.0F;
         float healthValueWidth = this.drawHpTag.getValue() ? FontRenderers.sf_medium.getStringWidth(healthValueStr) : 0.0F;
         float closeWidth = this.drawHpTag.getValue() ? FontRenderers.sf_medium.getStringWidth(closeBracket) : 0.0F;
         float textWidth = nameWidth + openWidth + healthValueWidth + closeWidth;
         float textHeight = FontRenderers.sf_medium.getStringHeight(name + (this.drawHpTag.getValue() ? openBracket + healthValueStr + closeBracket : ""));
         float paddingX = 4.0F;
         float paddingY = 0.8F;
         float bgWidth = textWidth + paddingX * 2.0F;
         float bgHeight = textHeight + paddingY * 2.0F;
         float centerX = (rect.x + rect.z) / 2.0F;
         float bgX = centerX - bgWidth / 2.0F;
         float bgY = rect.y - bgHeight - 2.0F;
         float bgCenterY = bgY + bgHeight / 2.0F;
         float scale = 0.75F;
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         matrices.translate(centerX, bgCenterY, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         matrices.translate(-centerX, -bgCenterY, 0.0F);
         Color fillColor = TAG_FILL_COLOR;
         if (entity instanceof PlayerEntity player && Managers.FRIEND.isFriend(player)) {
            fillColor = FRIEND_COLOR;
         }

         Render2DEngine.drawRect(matrices, bgX, bgY, bgWidth, bgHeight, fillColor);
         float textX = bgX + (bgWidth - textWidth) / 2.0F;
         float textY = bgY + (bgHeight - textHeight) / 2.0F;
         matrices.push();
         matrices.translate(0.0F, 3.0F, 0.0F);
         FontRenderers.sf_medium.drawString(matrices, name, textX, textY, TEXT_COLOR.getRGB());
         if (this.drawHpTag.getValue()) {
            float hpStartX = textX + nameWidth;
            FontRenderers.sf_medium.drawString(matrices, openBracket, hpStartX, textY, TEXT_COLOR.getRGB());
            FontRenderers.sf_medium.drawString(matrices, healthValueStr, hpStartX + openWidth, textY, this.getHealthColor(entity));
            FontRenderers.sf_medium.drawString(matrices, closeBracket, hpStartX + openWidth + healthValueWidth, textY, TEXT_COLOR.getRGB());
         }

         matrices.pop();
         matrices.pop();
      }
   }

   private int getHealthColor(LivingEntity entity) {
      float maxHealth = Math.max(entity.getMaxHealth(), 1.0F);
      float current = entity instanceof PlayerEntity player && ModuleManager.fixHP.isEnabled()
         ? FixHP.getHealth(player)
         : entity.getHealth() + entity.getAbsorptionAmount();
      current = MathHelper.clamp(current, 0.0F, maxHealth);
      float ratio = current / maxHealth;
      Color color = Render2DEngine.interpolateColorC(new Color(240, 20, 0), new Color(0, 240, 20), ratio);
      return color.getRGB();
   }

   private void drawArmorStrip(DrawContext context, Vector4f rect, PlayerEntity player) {
      List<ItemStack> stacks = new ArrayList<>();
      stacks.add(player.method_6079());
      stacks.addAll(player.getInventory().armor);
      stacks.add(player.method_6047());
      stacks.removeIf(ItemStack::isEmpty);
      if (!stacks.isEmpty()) {
         float totalWidth = stacks.size() * 18.0F;
         float startX = rect.x + (rect.z - rect.x - totalWidth) / 2.0F;
         float y = rect.w + 1.0F;
         float scale = 0.55F;
         float centerX = rect.x + (rect.z - rect.x) / 2.0F;
         float centerY = y + 7.0F;
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         matrices.translate(centerX, centerY, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         matrices.translate(-centerX, -centerY, 0.0F);

         for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            float x = startX + i * 18.0F;
            context.drawItem(stack, (int)x, (int)y);
            context.drawItemInSlot(mc.textRenderer, stack, (int)x, (int)y);
         }

         matrices.pop();
      }
   }

   @Nullable
   private Vector4f project(LivingEntity entity) {
      if (mc.getEntityRenderDispatcher() == null) {
         return null;
      }

      double interpolatedX = entity.field_6014 + (entity.method_23317() - entity.field_6014) * Render3DEngine.getTickDelta();
      double interpolatedY = entity.field_6036 + (entity.method_23318() - entity.field_6036) * Render3DEngine.getTickDelta();
      double interpolatedZ = entity.field_5969 + (entity.method_23321() - entity.field_5969) * Render3DEngine.getTickDelta();
      Box box = HitBox.getBaseBoundingBox(entity)
         .offset(interpolatedX - entity.method_23317(), interpolatedY - entity.method_23318(), interpolatedZ - entity.method_23321());
      Vector4f bounds = null;

      for (int i = 0; i < 8; i++) {
         double x = (i & 1) == 0 ? box.minX : box.maxX;
         double y = (i & 2) == 0 ? box.minY : box.maxY;
         double z = (i & 4) == 0 ? box.minZ : box.maxZ;
         Vec3d screen = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(x, y, z));
         if (!Double.isNaN(screen.x) && !Double.isNaN(screen.y) && !(screen.z <= 0.0) && !(screen.z >= 1.0)) {
            if (bounds == null) {
               bounds = new Vector4f((float)screen.x, (float)screen.y, (float)screen.x, (float)screen.y);
            } else {
               bounds.x = Math.min(bounds.x, (float)screen.x);
               bounds.y = Math.min(bounds.y, (float)screen.y);
               bounds.z = Math.max(bounds.z, (float)screen.x);
               bounds.w = Math.max(bounds.w, (float)screen.y);
            }
         }
      }

      return bounds;
   }

   public boolean shouldHideVanillaNameTag(LivingEntity entity) {
      return !this.drawTag.getValue() || entity == null || mc.player == null ? false : this.isTargetValid(entity);
   }

   private boolean shouldRenderSelf() {
      return !this.showSelf.getValue() ? false : !this.isInFirstPersonView() || this.isFreeCamActive();
   }

   private boolean isInFirstPersonView() {
      return mc.options != null && mc.options.getPerspective().isFirstPerson();
   }

   private boolean isFreeCamActive() {
      return ModuleManager.freeCam != null && ModuleManager.freeCam.isEnabled();
   }
}
