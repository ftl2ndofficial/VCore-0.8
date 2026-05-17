package vcore.features.modules.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import vcore.Vcore;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventHeldItemRenderer;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Aura;
import vcore.injection.accesors.IHeldItemRenderer;
import vcore.setting.Setting;

public class Animations extends Module {
   private static final float SELF_OLD_MAIN_HAND_X_OFFSET = 0.12F;
   private static final float SELF_OLD_MAIN_HAND_Y_OFFSET = 0.1F;
   private final Setting<Boolean> onlyaura = new Setting<>("OnlyAura", false);
   public Setting<Boolean> oldAnimationsM = new Setting<>("DisableSwapMain", true);
   public Setting<Boolean> oldAnimationsOff = new Setting<>("DisableSwapOff", true);
   public static Setting<Boolean> slowAnimation = new Setting<>("SlowAnimation", true);
   private final Setting<Animations.Mode> mode = new Setting<>("Mode", Animations.Mode.Smooth);
   private final Setting<Integer> offset = new Setting<>(
      "Offset", 0, 0, 10, v -> this.mode.getValue() == Animations.Mode.Self || this.mode.getValue() == Animations.Mode.Old
   );
   private final Setting<Integer> power = new Setting<>("Power", 6, 1, 10);
   public static Setting<Integer> slowAnimationVal = new Setting<>("SlowValue", 12, 1, 20, v -> slowAnimation.getValue());
   public boolean flip;

   public Animations() {
      super("Animations", "Custom hand animations.", Module.Category.RENDER);
   }

   public boolean shouldAnimate(ItemStack item) {
      return this.isEnabled() && (!this.onlyaura.getValue() || ModuleManager.aura.isEnabled() && Aura.target != null);
   }

   public boolean shouldChangeAnimationDuration() {
      return this.isEnabled() && (!this.onlyaura.getValue() || ModuleManager.aura.isEnabled() && Aura.target != null);
   }

   @Override
   public void onUpdate() {
      if (!fullNullCheck()) {
         if (this.oldAnimationsM.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).getEquippedProgressMainHand() <= 1.0F) {
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setEquippedProgressMainHand(1.0F);
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setItemStackMainHand(mc.player.method_6047());
         }

         if (this.oldAnimationsOff.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).getEquippedProgressOffHand() <= 1.0F
            )
          {
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setEquippedProgressOffHand(1.0F);
            ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getHeldItemRenderer()).setItemStackOffHand(mc.player.method_6079());
         }
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (e.getPacket() instanceof HandSwingC2SPacket) {
         this.flip = !this.flip;
      }
   }

   private void renderSwordAnimation(MatrixStack matrices, float f, float swingProgress, float equipProgress, Arm arm, Hand hand, Animations.Mode animMode) {
      if (arm != Arm.LEFT || animMode != Animations.Mode.Self && animMode != Animations.Mode.Old) {
         switch (animMode) {
            case Default:
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.translateToViewModel(matrices);
               float sin2_def = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2_def * -(this.power.getValue().intValue() * 10.0F)));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2_def * 45.0F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2_def * 15.0F));
               this.translateBack(matrices);
               break;
            case Smooth:
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.translateToViewModel(matrices);
               float s_f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
               float s_sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
               float s_f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s_sin2 * (45.0F + s_f * -20.0F)));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s_sin2 * s_f1 * -20.0F));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(s_f1 * -(this.power.getValue().intValue() * 10.0F)));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s_sin2 * -45.0F));
               this.translateBack(matrices);
               break;
            case Fade:
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.translateToViewModel(matrices);
               int handDir = arm == Arm.RIGHT ? 1 : -1;
               float sinSq = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
               float sinSqrt = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
               float sinLinear = MathHelper.sin(swingProgress * (float) Math.PI);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handDir * sinSqrt * (45.0F + sinSq * -5.0F)));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handDir * sinSqrt * sinLinear * -20.0F));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinLinear * -((this.power.getValue().intValue() + 2.0F) * 8.0F)));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handDir * sinSqrt * -45.0F));
               this.translateBack(matrices);
               break;
            case Self: {
               float anim = MathHelper.sin(swingProgress * (float) Math.PI);
               this.applyEquipOffset(matrices, arm, 0.0F);
               this.translateToSelfOldViewModel(matrices, hand, animMode);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F));
               float baseX = -90.0F + this.offset.getValue().intValue() * 10.0F;
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(baseX - this.power.getValue().intValue() * 10.0F * anim));
               this.translateBackSelfOldViewModel(matrices, hand, animMode);
               break;
            }
            case Old: {
               float anim = MathHelper.sin(swingProgress * (float) Math.PI);
               this.applyEquipOffset(matrices, arm, 0.0F);
               this.translateToSelfOldViewModel(matrices, hand, animMode);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
               float baseX = -90.0F + this.offset.getValue().intValue() * 10.0F;
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(baseX - this.power.getValue().intValue() * 10.0F * anim));
               this.translateBackSelfOldViewModel(matrices, hand, animMode);
            }
         }
      } else {
         float viewModelX = this.getSelfOldViewModelX(hand, animMode);
         float viewModelY = this.getSelfOldViewModelY(hand, animMode);
         float viewModelZ = this.getViewModelZ();
         this.applyEquipOffset(matrices, arm, equipProgress);
         matrices.translate(-viewModelX, viewModelY, viewModelZ);
         this.applySwingOffset(matrices, arm, swingProgress);
         matrices.translate(viewModelX, -viewModelY, -viewModelZ);
      }
   }

   public void renderFirstPersonItemCustom(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light
   ) {
      if (!player.method_31550()) {
         if (this.onlyaura.getValue() && (!ModuleManager.aura.isEnabled() || Aura.target == null)) {
            return;
         }

         boolean bl = hand == Hand.MAIN_HAND;
         Arm arm = bl ? player.method_6068() : player.method_6068().getOpposite();
         matrices.push();
         float f = 0.0F;
         if (item.isOf(Items.CROSSBOW)) {
            boolean bl2 = CrossbowItem.isCharged(item);
            boolean bl3 = arm == Arm.RIGHT;
            int i = bl3 ? 1 : -1;
            if (player.method_6115() && player.method_6014() > 0 && player.method_6058() == hand) {
               this.applyEquipOffset(matrices, arm, equipProgress);
               matrices.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -9.785F));
               f = item.getMaxUseTime(mc.player) - (mc.player.method_6014() - tickDelta + 1.0F);
               float g = f / CrossbowItem.getPullTime(item, mc.player);
               if (g > 1.0F) {
                  g = 1.0F;
               }

               if (g > 0.1F) {
                  float h = MathHelper.sin((f - 0.1F) * 1.3F);
                  float j = g - 0.1F;
                  float k = h * j;
                  matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
               }

               matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
               matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
            } else {
               f = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
               float g = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
               float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
               matrices.translate(i * f, g, h);
               this.applyEquipOffset(matrices, arm, equipProgress);
               this.applySwingOffset(matrices, arm, swingProgress);
               if (bl2 && swingProgress < 0.001F && bl) {
                  matrices.translate(i * -0.641864F, 0.0F, 0.0F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
               }
            }

            this.renderItem(
               player,
               item,
               bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
               !bl3,
               matrices,
               vertexConsumers,
               light
            );
         } else if (this.shouldAnimate(item)) {
            boolean bl2 = arm == Arm.RIGHT;
            float m = 0.0F;
            Animations.Mode chosenMode = Animations.Mode.Fade;
            if (this.isSword(item.getItem()) || this.isAxe(item.getItem()) || this.isPickaxe(item.getItem())) {
               chosenMode = this.mode.getValue();
            }

            if (player.method_6115() && player.method_6014() > 0 && player.method_6058() == hand) {
               int l = bl2 ? 1 : -1;
               switch (item.getUseAction()) {
                  case NONE:
                  case BLOCK:
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     break;
                  case EAT:
                  case DRINK:
                     this.applyEatOrDrinkTransformationCustom(matrices, tickDelta, arm, item);
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     break;
                  case BOW:
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     matrices.translate(l * -0.2785682F, 0.18344387F, 0.15731531F);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 35.3F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * -9.785F));
                     m = item.getMaxUseTime(mc.player) - (mc.player.method_6014() - tickDelta + 1.0F);
                     f = m / 20.0F;
                     f = (f * f + f * 2.0F) / 3.0F;
                     if (f > 1.0F) {
                        f = 1.0F;
                     }

                     if (f > 0.1F) {
                        float g = MathHelper.sin((m - 0.1F) * 1.3F);
                        float h = f - 0.1F;
                        float j = g * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.translate(f * 0.0F, f * 0.0F, f * 0.04F);
                     matrices.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(l * 45.0F));
                     break;
                  case SPEAR:
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     matrices.translate(l * -0.5F, 0.7F, 0.1F);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 35.3F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * -9.785F));
                     m = item.getMaxUseTime(mc.player) - (mc.player.method_6014() - tickDelta + 1.0F);
                     f = m / 10.0F;
                     if (f > 1.0F) {
                        f = 1.0F;
                     }

                     if (f > 0.1F) {
                        float g = MathHelper.sin((m - 0.1F) * 1.3F);
                        float h = f - 0.1F;
                        float j = g * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.translate(0.0F, 0.0F, f * 0.2F);
                     matrices.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(l * 45.0F));
                     break;
                  case BRUSH:
                     this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                     break;
                  default:
                     this.applyEquipOffset(matrices, arm, equipProgress);
               }
            } else if (player.method_6123()) {
               this.applyEquipOffset(matrices, arm, equipProgress);
               int l = bl2 ? 1 : -1;
               matrices.translate(l * -0.4F, 0.8F, 0.3F);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 65.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * -85.0F));
            } else {
               this.renderSwordAnimation(matrices, f, swingProgress, equipProgress, arm, hand, chosenMode);
            }

            EventHeldItemRenderer event = new EventHeldItemRenderer(hand, item, equipProgress, matrices);
            Vcore.EVENT_BUS.post(event);
            this.applySelfOldMainHandOffset(matrices, hand, chosenMode);
            this.renderItem(
               player,
               item,
               bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
               !bl2,
               matrices,
               vertexConsumers,
               light
            );
         } else {
            boolean isRight = arm == Arm.RIGHT;
            this.renderItem(
               player,
               item,
               isRight ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
               !isRight,
               matrices,
               vertexConsumers,
               light
            );
         }

         matrices.pop();
      }
   }

   private void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, @NotNull ItemStack stack, float equipProgress) {
      this.applyEquipOffset(matrices, arm, equipProgress);
      float f = mc.player.method_6014() - tickDelta + 1.0F;
      float g = 1.0F - f / stack.getMaxUseTime(mc.player);
      float m = -15.0F + 75.0F * MathHelper.cos(g * 45.0F * (float) Math.PI);
      if (arm != Arm.RIGHT) {
         matrices.translate(0.1, 0.83, 0.35);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(m));
         matrices.translate(-0.3, 0.22, 0.35);
      } else {
         matrices.translate(-0.25, 0.22, 0.35);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(m));
      }
   }

   private void applyEquipOffset(@NotNull MatrixStack matrices, Arm arm, float equipProgress) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(@NotNull MatrixStack matrices, Arm arm, float swingProgress) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * -20.0F)));
      float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
   }

   public void renderItem(
      LivingEntity entity,
      ItemStack stack,
      ModelTransformationMode renderMode,
      boolean leftHanded,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light
   ) {
      if (!stack.isEmpty()) {
         mc.getItemRenderer()
            .renderItem(
               entity,
               stack,
               renderMode,
               leftHanded,
               matrices,
               vertexConsumers,
               entity.method_37908(),
               light,
               OverlayTexture.DEFAULT_UV,
               entity.method_5628() + renderMode.ordinal()
            );
      }
   }

   private void applyEatOrDrinkTransformationCustom(MatrixStack matrices, float tickDelta, Arm arm, @NotNull ItemStack stack) {
      float f = mc.player.method_6014() - tickDelta + 1.0F;
      float g = f / stack.getMaxUseTime(mc.player);
      if (g < 0.8F) {
         float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.005F);
         matrices.translate(0.0F, h, 0.0F);
      }

      float h = 1.0F - (float)Math.pow(g, 27.0);
      int i = arm == Arm.RIGHT ? 1 : -1;
      matrices.translate(h * 0.6F * i * ModuleManager.viewModel.getEatXFactor(), h * -0.5F * ModuleManager.viewModel.eatY.getValue(), h * 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * h * 90.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * h * 30.0F));
   }

   private void translateToViewModel(MatrixStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            ModuleManager.viewModel.positionMainX.getValue(),
            ModuleManager.viewModel.positionMainY.getValue(),
            ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private void translateToViewModelOff(MatrixStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            -ModuleManager.viewModel.positionMainX.getValue(),
            ModuleManager.viewModel.positionMainY.getValue(),
            ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private void translateBack(MatrixStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            -ModuleManager.viewModel.positionMainX.getValue(),
            -ModuleManager.viewModel.positionMainY.getValue(),
            -ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private void translateToSelfOldViewModel(MatrixStack matrices, Hand hand, Animations.Mode animMode) {
      matrices.translate(this.getSelfOldViewModelX(hand, animMode), this.getSelfOldViewModelY(hand, animMode), this.getViewModelZ());
   }

   private void translateBackSelfOldViewModel(MatrixStack matrices, Hand hand, Animations.Mode animMode) {
      matrices.translate(-this.getSelfOldViewModelX(hand, animMode), -this.getSelfOldViewModelY(hand, animMode), -this.getViewModelZ());
   }

   private float getSelfOldViewModelX(Hand hand, Animations.Mode animMode) {
      float x = ModuleManager.viewModel.isEnabled() ? ModuleManager.viewModel.positionMainX.getValue() : 0.0F;
      return x + this.getSelfOldMainHandXOffset(hand, animMode);
   }

   private float getSelfOldViewModelY(Hand hand, Animations.Mode animMode) {
      return this.getViewModelY() + this.getSelfMainHandYOffset(hand, animMode);
   }

   private float getViewModelY() {
      return ModuleManager.viewModel.isEnabled() ? ModuleManager.viewModel.positionMainY.getValue() : 0.0F;
   }

   private float getViewModelZ() {
      return ModuleManager.viewModel.isEnabled() ? ModuleManager.viewModel.positionMainZ.getValue() : 0.0F;
   }

   private void applySelfOldMainHandOffset(MatrixStack matrices, Hand hand, Animations.Mode animMode) {
      float xOffset = this.getSelfOldMainHandXOffset(hand, animMode);
      float yOffset = this.getSelfMainHandYOffset(hand, animMode);
      if (xOffset != 0.0F || yOffset != 0.0F) {
         matrices.translate(xOffset, yOffset, 0.0F);
      }
   }

   private float getSelfOldMainHandXOffset(Hand hand, Animations.Mode animMode) {
      return hand != Hand.MAIN_HAND || animMode != Animations.Mode.Self && animMode != Animations.Mode.Old ? 0.0F : 0.12F;
   }

   private float getSelfMainHandYOffset(Hand hand, Animations.Mode animMode) {
      return hand != Hand.MAIN_HAND || animMode != Animations.Mode.Self && animMode != Animations.Mode.Old ? 0.0F : 0.1F;
   }

   private void translateBacklOff(MatrixStack matrices) {
      if (ModuleManager.viewModel.isEnabled()) {
         matrices.translate(
            ModuleManager.viewModel.positionMainX.getValue(),
            -ModuleManager.viewModel.positionMainY.getValue(),
            -ModuleManager.viewModel.positionMainZ.getValue()
         );
      }
   }

   private boolean isSword(Item i) {
      return i == Items.WOODEN_SWORD
         || i == Items.STONE_SWORD
         || i == Items.IRON_SWORD
         || i == Items.GOLDEN_SWORD
         || i == Items.DIAMOND_SWORD
         || i == Items.NETHERITE_SWORD;
   }

   private boolean isPickaxe(Item i) {
      return i == Items.WOODEN_PICKAXE
         || i == Items.STONE_PICKAXE
         || i == Items.IRON_PICKAXE
         || i == Items.GOLDEN_PICKAXE
         || i == Items.DIAMOND_PICKAXE
         || i == Items.NETHERITE_PICKAXE;
   }

   private boolean isAxe(Item i) {
      return i == Items.WOODEN_AXE
         || i == Items.STONE_AXE
         || i == Items.IRON_AXE
         || i == Items.GOLDEN_AXE
         || i == Items.DIAMOND_AXE
         || i == Items.NETHERITE_AXE;
   }

   private enum Mode {
      Default,
      Smooth,
      Fade,
      Self,
      Old;
   }
}
