package vcore.injection;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vcore.core.Core;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.features.modules.render.Tooltips;
import vcore.gui.misc.PeekScreen;
import vcore.utility.Timer;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.TextureStorage;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
   @Unique
   private static final float INVENTORY_ANIM_DURATION = 250.0F;
   @Unique
   private final Timer delayTimer = new Timer();
   @Unique
   private Runnable postRender;
   @Unique
   private long openTime;
   @Unique
   private boolean inventoryAnimActive;
   @Shadow
   @Nullable
   protected Slot field_2787;
   @Shadow
   protected int field_2776;
   @Shadow
   protected int field_2800;
   private static final ItemStack[] ITEMS = new ItemStack[27];
   private Map<Render2DEngine.Rectangle, Integer> clickableRects = new HashMap<>();

   protected MixinHandledScreen(Text title) {
      super(title);
   }

   @Shadow
   protected abstract boolean method_2387(Slot var1, double var2, double var4);

   @Shadow
   protected abstract void method_2383(Slot var1, int var2, int var3, SlotActionType var4);

   @Inject(method = "init", at = @At("RETURN"))
   private void onInit(CallbackInfo ci) {
      this.openTime = System.currentTimeMillis();
   }

   @Inject(
      method = "renderBackground",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
         shift = Shift.BEFORE
      )
   )
   private void drawScreenBackgroundHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.isInventoryScreen() && this.isSurvivalMode()) {
         float progress = this.getOpenProgress();
         if (progress < 1.0F) {
            float anim = this.getBackOutScale(progress);
            int centerX = context.getScaledWindowWidth() / 2;
            int centerY = context.getScaledWindowHeight() / 2;
            context.getMatrices().push();
            context.getMatrices().translate(centerX, centerY, 0.0F);
            context.getMatrices().scale(anim, anim, 1.0F);
            context.getMatrices().translate(-centerX, -centerY, 0.0F);
            this.inventoryAnimActive = true;
         } else {
            this.inventoryAnimActive = false;
         }
      } else {
         this.inventoryAnimActive = false;
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void drawScreenHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         for (int i1 = 0; i1 < Module.mc.player.field_7512.slots.size(); i1++) {
            Slot slot = (Slot)Module.mc.player.field_7512.slots.get(i1);
            if (this.method_2387(slot, mouseX, mouseY)
               && slot.isEnabled()
               && ModuleManager.itemScroller.isEnabled()
               && this.shit()
               && this.attack()
               && this.delayTimer.passedMs(ModuleManager.itemScroller.delay.getValue().intValue())) {
               this.method_2383(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
               this.delayTimer.reset();
            }
         }
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void drawScreenHookTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.inventoryAnimActive) {
         context.getMatrices().pop();
         this.inventoryAnimActive = false;
      }
   }

   private boolean shit() {
      return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 340)
         || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 344);
   }

   private boolean attack() {
      return Core.hold_mouse0;
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (this.field_2787 != null && !this.field_2787.getStack().isEmpty() && this.field_22787.player.field_7498.method_34255().isEmpty()) {
            if (Tooltips.hasItems(this.field_2787.getStack()) && Tooltips.storage.getValue()) {
               this.renderShulkerToolTip(context, mouseX, mouseY, 0, 0, this.field_2787.getStack());
            } else if (this.field_2787.getStack().getItem() == Items.FILLED_MAP && Tooltips.maps.getValue()) {
               this.drawMapPreview(context, this.field_2787.getStack(), mouseX, mouseY);
            }
         }

         int xOffset = 0;
         int yOffset = 20;
         int stage = 0;
         if (ModuleManager.tooltips.isEnabled() && ModuleManager.tooltips.shulkerRegear.getValue()) {
            this.clickableRects.clear();

            for (int i1 = 0; i1 < Module.mc.player.field_7512.slots.size(); i1++) {
               Slot slot = (Slot)Module.mc.player.field_7512.slots.get(i1);
               if (!slot.getStack().isEmpty()
                  && slot.getStack().getItem() instanceof BlockItem bi
                  && bi.getBlock() instanceof ShulkerBoxBlock
                  && this.renderShulkerToolTip(context, xOffset, yOffset + 67, mouseX, mouseY, slot.getStack())) {
                  this.clickableRects.put(new Render2DEngine.Rectangle(xOffset, yOffset, xOffset + 176, yOffset + 67), slot.id);
                  yOffset += 67;
                  if (stage == 0) {
                     if (yOffset + 67 >= Module.mc.getWindow().getScaledHeight()) {
                        yOffset = 20;
                        xOffset = Module.mc.getWindow().getScaledWidth() - 176;
                        stage = 1;
                     }
                  } else if (stage == 1) {
                     if (yOffset + 67 >= Module.mc.getWindow().getScaledHeight()) {
                        yOffset = 20;
                        xOffset = 170;
                        stage = 2;
                     }
                  } else if (yOffset + 67 >= Module.mc.getWindow().getScaledHeight()) {
                     yOffset = 20;
                     xOffset = Module.mc.getWindow().getScaledWidth() - 352;
                     stage = 0;
                  }
               }
            }

            if (this.postRender != null) {
               this.postRender.run();
               this.postRender = null;
            }
         }
      }
   }

   public boolean renderShulkerToolTip(DrawContext context, int offsetX, int offsetY, int mouseX, int mouseY, ItemStack stack) {
      try {
         ContainerComponent compoundTag = (ContainerComponent)stack.method_57824(DataComponentTypes.CONTAINER);
         if (compoundTag == null) {
            return false;
         }

         float[] colors = new float[]{1.0F, 1.0F, 1.0F};
         if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
            try {
               Color c = new Color(Objects.requireNonNull(ShulkerBoxBlock.getColor(stack.getItem())).getEntityColor());
               colors = new float[]{c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getRed() / 255.0F, c.getAlpha() / 255.0F};
            } catch (NullPointerException npe) {
               colors = new float[]{1.0F, 1.0F, 1.0F};
            }
         }

         this.draw(context, compoundTag.stream().toList(), offsetX, offsetY, mouseX, mouseY, colors);
         return true;
      } catch (Exception ignore) {
         return false;
      }
   }

   @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
   private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (this.field_2787 != null
            && !this.field_2787.getStack().isEmpty()
            && this.field_22787.player.field_7498.method_34255().isEmpty()
            && this.field_2787.getStack().getItem() == Items.FILLED_MAP
            && Tooltips.maps.getValue()) {
            ci.cancel();
         }
      }
   }

   @Unique
   private void draw(DrawContext context, List<ItemStack> itemStacks, int offsetX, int offsetY, int mouseX, int mouseY, float[] colors) {
      RenderSystem.disableDepthTest();
      GL11.glClear(256);
      offsetX += 8;
      offsetY -= 82;
      boolean shulkerAnimPushed = false;
      if (this.isSurvivalMode()) {
         float progress = this.getOpenProgress();
         if (progress < 1.0F) {
            float anim = this.getBackOutScale(progress);
            float centerX = offsetX + 88.0F;
            float centerY = offsetY + 33.5F;
            context.getMatrices().push();
            context.getMatrices().translate(centerX, centerY, 0.0F);
            context.getMatrices().scale(anim, anim, 1.0F);
            context.getMatrices().translate(-centerX, -centerY, 0.0F);
            shulkerAnimPushed = true;
         }
      }

      this.drawBackground(context, offsetX, offsetY, colors);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      DiffuseLighting.enableGuiDepthLighting();
      int row = 0;
      int i = 0;

      for (ItemStack itemStack : itemStacks) {
         context.drawItem(itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);
         context.drawItemInSlot(Module.mc.textRenderer, itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);
         if (mouseX > offsetX + 8 + i * 18 && mouseX < offsetX + 28 + i * 18 && mouseY > offsetY + 7 + row * 18 && mouseY < offsetY + 27 + row * 18) {
            this.postRender = () -> context.drawTooltip(this.field_22793, method_25408(Module.mc, itemStack), itemStack.getTooltipData(), mouseX, mouseY);
         }

         if (++i >= 9) {
            i = 0;
            row++;
         }
      }

      DiffuseLighting.disableGuiDepthLighting();
      RenderSystem.enableDepthTest();
      if (shulkerAnimPushed) {
         context.getMatrices().pop();
      }
   }

   @Unique
   private boolean isInventoryScreen() {
      return this instanceof InventoryScreen;
   }

   @Unique
   private boolean isSurvivalMode() {
      return this.field_22787 != null
         && this.field_22787.interactionManager != null
         && this.field_22787.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL;
   }

   @Unique
   private float getOpenProgress() {
      return Math.min(1.0F, (float)(System.currentTimeMillis() - this.openTime) / 250.0F);
   }

   @Unique
   private float getBackOutScale(float progress) {
      float x = progress - 1.0F;
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(x, 3.0) + c1 * (float)Math.pow(x, 2.0);
   }

   private void drawBackground(DrawContext context, int x, int y, float[] colors) {
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1.0F);
      RenderSystem.texParameter(3553, 10240, 9729);
      RenderSystem.texParameter(3553, 10241, 9987);
      context.drawTexture(TextureStorage.container, x, y, 0.0F, 0.0F, 176, 67, 176, 67);
      RenderSystem.enableBlend();
   }

   private void drawMapPreview(DrawContext context, ItemStack stack, int x, int y) {
      RenderSystem.enableBlend();
      context.getMatrices().push();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int y1 = y - 12;
      int x1 = x + 8;
      int z = 300;
      MapState mapState = FilledMapItem.getMapState(stack, this.field_22787.world);
      if (mapState != null) {
         mapState.getPlayerSyncData(this.field_22787.player);
         x1 += 8;
         y1 += 8;
         int var14 = 310;
         double scale = 0.65625;
         context.getMatrices().translate(x1, y1, var14);
         context.getMatrices().scale((float)scale, (float)scale, 0.0F);
         Immediate consumer = this.field_22787.getBufferBuilders().getEntityVertexConsumers();
         this.field_22787
            .gameRenderer
            .getMapRenderer()
            .draw(context.getMatrices(), consumer, (MapIdComponent)stack.method_57824(DataComponentTypes.MAP_ID), mapState, false, 15728880);
      }

      context.getMatrices().pop();
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (!Module.fullNullCheck()) {
         if (button == 2 && this.field_2787 != null && !this.field_2787.getStack().isEmpty() && this.field_22787.player.field_7498.method_34255().isEmpty()) {
            ItemStack itemStack = this.field_2787.getStack();
            if (Tooltips.hasItems(itemStack) && Tooltips.middleClickOpen.getValue()) {
               Arrays.fill(ITEMS, ItemStack.EMPTY);
               ContainerComponent nbt = (ContainerComponent)itemStack.method_57824(DataComponentTypes.CONTAINER);
               if (nbt != null) {
                  List<ItemStack> list = nbt.stream().toList();

                  for (int i = 0; i < list.size(); i++) {
                     ITEMS[i] = list.get(i);
                  }
               }

               this.field_22787
                  .setScreen(
                     new PeekScreen(
                        new ShulkerBoxScreenHandler(0, this.field_22787.player.method_31548(), new SimpleInventory(ITEMS)),
                        this.field_22787.player.method_31548(),
                        this.field_2787.getStack().getName(),
                        ((BlockItem)this.field_2787.getStack().getItem()).getBlock()
                     )
                  );
               cir.setReturnValue(true);
            }
         }

         for (Render2DEngine.Rectangle rect : this.clickableRects.keySet()) {
            if (rect.contains(mouseX, mouseY)) {
               if (ModuleManager.tooltips.shulkerRegearShiftMode.getValue()) {
                  Module.mc
                     .interactionManager
                     .clickSlot(Module.mc.player.field_7512.syncId, this.clickableRects.get(rect), 0, SlotActionType.QUICK_MOVE, Module.mc.player);
               } else {
                  Module.mc
                     .interactionManager
                     .clickSlot(Module.mc.player.field_7512.syncId, this.clickableRects.get(rect), 0, SlotActionType.PICKUP, Module.mc.player);
               }
            }
         }
      }
   }
}
