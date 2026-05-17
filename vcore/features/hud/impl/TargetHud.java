package vcore.features.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import vcore.core.manager.client.ModuleManager;
import vcore.features.hud.HudElement;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.misc.FixHP;
import vcore.features.modules.misc.NameProtect;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.impl.PositionSetting;
import vcore.utility.math.MathUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;
import vcore.utility.render.animation.EaseOutBack;
import vcore.utility.render.animation.EaseOutCirc;

public class TargetHud extends HudElement {
   private static final float HUD_WIDTH = 137.0F;
   private static final float HUD_HEIGHT = 47.5F;
   private static final float CONTENT_X = 48.0F;
   private static final float HEALTH_BAR_Y = 32.0F;
   private static final float HEALTH_BAR_WIDTH = 85.0F;
   private static final float HEALTH_BAR_HEIGHT = 11.0F;
   private static final float ITEM_ROW_Y = -14.5F;
   private static final float ITEM_SCALE = 0.75F;
   private static final float ITEM_SPACING = 12.0F;
   private static final float NAME_Y = 9.5F;
   private static final float HP_TEXT_Y = 22.5F;
   private static final float NAME_RIGHT_PADDING = 8.0F;
   private static final float NAME_FADE_WIDTH = 4.0F;
   private static final float NAME_FADE_END_MARGIN = 1.75F;
   public EaseOutBack animation = new EaseOutBack();
   public static EaseOutCirc healthAnimation = new EaseOutCirc();
   private boolean direction = false;
   private LivingEntity target;

   public TargetHud(PositionSetting position, Supplier<List<HudElement>> activeElementsSupplier) {
      super("TargetHud", 150, 50, position, activeElementsSupplier);
   }

   @Override
   public void tick() {
      this.animation.update(this.direction);
      healthAnimation.update();
   }

   @Override
   public void render(DrawContext context) {
      super.render(context);
      this.getTarget();
      if (this.target != null) {
         float totalHealth = this.getHealth();
         float health = Math.min(this.target.getMaxHealth(), totalHealth);
         if (this.animation.getAnimationd() > 0.0) {
            float animationFactor = (float)MathUtility.clamp(this.animation.getAnimationd(), 0.0, 1.0);
            this.renderNurik(context, health, totalHealth, animationFactor);
         }
      }
   }

   private void getTarget() {
      if (Aura.target != null) {
         if (Aura.target instanceof LivingEntity) {
            this.target = (LivingEntity)Aura.target;
            this.direction = true;
         } else {
            this.target = null;
            this.direction = false;
         }
      } else if (mc.currentScreen instanceof ChatScreen) {
         this.target = mc.player;
         this.direction = true;
      } else {
         this.direction = false;
         if (this.animation.getAnimationd() < 0.02) {
            this.target = null;
         }
      }
   }

   private void renderNurik(DrawContext context, float health, float totalHealth, float animationFactor) {
      float hurtPercent = Render2DEngine.interpolateFloat(
            MathUtility.clamp(this.target.hurtTime == 0 ? 0 : this.target.hurtTime + 1, 0, 10), this.target.hurtTime, Render3DEngine.getTickDelta()
         )
         / 8.0F;
      healthAnimation.setValue(health);
      health = (float)healthAnimation.getAnimationD();
      Render2DEngine.drawHudBase2(context.getMatrices(), this.getPosX(), this.getPosY(), 137.0F, 47.5F, 9.0F, 5.0F, HudEditor.getBlurOpacity(), animationFactor);
      this.setBounds(this.getPosX(), this.getPosY(), 137.0F, 47.5F);
      if (this.target instanceof PlayerEntity) {
         RenderSystem.setShaderTexture(0, ((AbstractClientPlayerEntity)this.target).getSkinTextures().texture());
      } else {
         RenderSystem.setShaderTexture(0, mc.getEntityRenderDispatcher().getRenderer(this.target).getTexture(this.target));
      }

      context.getMatrices().push();
      context.getMatrices().translate(this.getPosX() + 3.5F + 20.0F, this.getPosY() + 3.5F + 20.0F, 0.0F);
      context.getMatrices().scale(1.0F - hurtPercent / 15.0F, 1.0F - hurtPercent / 15.0F, 1.0F);
      context.getMatrices().translate(-(this.getPosX() + 3.5F + 20.0F), -(this.getPosY() + 3.5F + 20.0F), 0.0F);
      RenderSystem.enableBlend();
      RenderSystem.colorMask(false, false, false, true);
      RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      RenderSystem.clear(16384, false);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Render2DEngine.renderRoundedQuadInternal(
         context.getMatrices().peek().getPositionMatrix(),
         animationFactor,
         animationFactor,
         animationFactor,
         animationFactor,
         this.getPosX() + 3.5F,
         this.getPosY() + 3.5F,
         this.getPosX() + 3.5F + 40.0F,
         this.getPosY() + 3.5F + 40.0F,
         7.0,
         10.0
      );
      RenderSystem.blendFunc(772, 773);
      float hurtTint = MathUtility.clamp(animationFactor - hurtPercent / 2.0F, 0.0F, 1.0F);
      RenderSystem.setShaderColor(animationFactor, hurtTint, hurtTint, animationFactor);
      Render2DEngine.renderTexture(context.getMatrices(), this.getPosX() + 3.5F, this.getPosY() + 3.5F, 40.0, 40.0, 8.0F, 8.0F, 8.0, 8.0, 64.0, 64.0);
      Render2DEngine.renderTexture(context.getMatrices(), this.getPosX() + 3.5F, this.getPosY() + 3.5F, 40.0, 40.0, 40.0F, 8.0F, 8.0, 8.0, 64.0, 64.0);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      context.getMatrices().pop();
      Render2DEngine.drawRect(context.getMatrices(), this.getPosX() + 48.0F, this.getPosY() + 32.0F, 85.0F, 11.0F, 4.0F, 0.15F * animationFactor);
      Render2DEngine.drawRect(
         context.getMatrices(),
         this.getPosX() + 48.0F,
         this.getPosY() + 32.0F,
         MathUtility.clamp(85.0F * (health / this.target.getMaxHealth()), 8.0F, 85.0F),
         11.0F,
         4.0F,
         animationFactor
      );
      this.renderTargetName(context, animationFactor);
      FontRenderers.inter_target_hp
         .drawString(
            context.getMatrices(),
            this.getHealthDisplayText(totalHealth, this.getAbsorptionHealth()),
            this.getPosX() + 48.0F,
            this.getPosY() + 22.5F,
            Render2DEngine.applyOpacity(Color.WHITE.getRGB(), animationFactor)
         );
      if (this.target instanceof PlayerEntity) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)MathUtility.clamp(this.animation.getAnimationd(), 0.0, 1.0));
         ItemStack[] items = this.getTargetItems((PlayerEntity)this.target);
         float rowWidth = (items.length - 1) * 12.0F + 12.0F;
         float xItemOffset = this.getPosX() + 48.0F + (85.0F - rowWidth) / 2.0F + 2.0F;

         for (ItemStack itemStack : items) {
            context.getMatrices().push();
            context.getMatrices().translate(xItemOffset, this.getPosY() + -14.5F, 0.0F);
            context.getMatrices().scale(0.75F, 0.75F, 0.75F);
            context.drawItem(itemStack, 0, 0);
            context.drawItemInSlot(mc.textRenderer, itemStack, 0, 0);
            context.getMatrices().pop();
            xItemOffset += 12.0F;
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public float getHealth() {
      return this.getBaseHealth() + this.getAbsorptionHealth();
   }

   private float getBaseHealth() {
      return this.target instanceof PlayerEntity ent && ModuleManager.fixHP.isEnabled() ? FixHP.getHealth(ent) : this.target.getHealth();
   }

   private float getAbsorptionHealth() {
      return this.target == null ? 0.0F : this.target.getAbsorptionAmount();
   }

   private String getTargetName() {
      return ModuleManager.nameProtect.isEnabled() && this.target == mc.player ? NameProtect.getCustomName() : this.target.method_5477().getString();
   }

   private void renderTargetName(DrawContext context, float animationFactor) {
      String targetName = this.getTargetName();
      float textX = this.getPosX() + 48.0F;
      float textY = this.getPosY() + 9.5F;
      float maxNameWidth = 81.0F;
      float clipTop = textY - 1.0F;
      float clipBottom = clipTop + FontRenderers.inter_target_name.getStringHeight(targetName) + 4.0F;
      float clipRight = textX + maxNameWidth;
      Render2DEngine.addWindow(context.getMatrices(), new Render2DEngine.Rectangle(textX, clipTop, clipRight, clipBottom));
      FontRenderers.inter_target_name
         .drawStringWithHorizontalFade(
            context.getMatrices(),
            targetName,
            textX,
            textY,
            Render2DEngine.applyOpacity(Color.WHITE.getRGB(), animationFactor),
            Math.max(0.0F, maxNameWidth - 1.75F),
            4.0F
         );
      Render2DEngine.popWindow();
   }

   private ItemStack[] getTargetItems(PlayerEntity player) {
      List<ItemStack> armor = player.getInventory().armor;
      ItemStack[] items = new ItemStack[]{player.method_6047(), armor.get(3), armor.get(2), armor.get(1), armor.get(0), player.method_6079()};
      ItemStack[] packedItems = new ItemStack[items.length];

      for (int i = 0; i < packedItems.length; i++) {
         packedItems[i] = ItemStack.EMPTY;
      }

      int writeIndex = packedItems.length - 1;

      for (int i = items.length - 1; i >= 0; i--) {
         ItemStack itemStack = items[i];
         if (!itemStack.isEmpty()) {
            packedItems[writeIndex--] = itemStack;
         }
      }

      return packedItems;
   }

   private String getHealthDisplayText(float totalHealth, float absorptionHealth) {
      String healthText = "HP: " + this.formatHealthValue(totalHealth);
      if (absorptionHealth > 0.0F) {
         healthText = healthText + " - " + this.formatHealthValue(absorptionHealth) + " AB";
      }

      return healthText;
   }

   private String formatHealthValue(float value) {
      return String.format(Locale.US, "%.1f", value);
   }

   public static void sizeAnimation(MatrixStack matrixStack, double width, double height, double animation) {
      matrixStack.translate(width, height, 0.0);
      matrixStack.scale((float)animation, (float)animation, 1.0F);
      matrixStack.translate(-width, -height, 0.0);
   }
}
