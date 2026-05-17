package vcore.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.SettingGroup;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class ItemESP extends Module {
   public final Setting<SettingGroup> item = new Setting<>("Item", new SettingGroup(false, 0));
   private final Setting<Boolean> showNetherite = new Setting<>("NetheriteGear", true).addToGroup(this.item);
   private final Setting<Boolean> showDiamond = new Setting<>("DiamondGear", true).addToGroup(this.item);
   private final Setting<Boolean> showGold = new Setting<>("GoldGear", true).addToGroup(this.item);
   private final Setting<Boolean> showIron = new Setting<>("IronGear", true).addToGroup(this.item);
   private final Setting<Boolean> showLeather = new Setting<>("LeatherGear", true).addToGroup(this.item);
   private final Setting<Boolean> showOtherGear = new Setting<>("OtherGear", true).addToGroup(this.item);
   private final Setting<Boolean> showOtherItem = new Setting<>("OtherItem", true).addToGroup(this.item);
   public final Setting<SettingGroup> color = new Setting<>("Color", new SettingGroup(false, 0));
   private final Setting<ColorSetting> netheriteColor = new Setting<>("Netherite", new ColorSetting(new Color(16733525).getRGB())).addToGroup(this.color);
   private final Setting<ColorSetting> diamondColor = new Setting<>("Diamond", new ColorSetting(new Color(5636095).getRGB())).addToGroup(this.color);
   private final Setting<ColorSetting> goldColor = new Setting<>("Gold", new ColorSetting(new Color(16777045).getRGB())).addToGroup(this.color);
   private final Setting<ColorSetting> ironColor = new Setting<>("Iron", new ColorSetting(new Color(11184810).getRGB())).addToGroup(this.color);
   private final Setting<ColorSetting> leatherColor = new Setting<>("Leather", new ColorSetting(new Color(10506797).getRGB())).addToGroup(this.color);
   private final Setting<ColorSetting> otherGearColor = new Setting<>("OtherGearColor", new ColorSetting(new Color(11141375).getRGB())).addToGroup(this.color);
   private final Setting<ColorSetting> otherItemColor = new Setting<>("OtherItemColor", new ColorSetting(new Color(16777215).getRGB())).addToGroup(this.color);

   public ItemESP() {
      super("ItemESP", "Highlights items on ground.", Module.Category.RENDER);
   }

   private ItemESP.ItemGroup getItemGroup(ItemEntity itemEntity) {
      String name = itemEntity.getStack().getItem().getTranslationKey().toLowerCase();
      if (name.contains("netherite_helmet")
         || name.contains("netherite_chestplate")
         || name.contains("netherite_leggings")
         || name.contains("netherite_boots")
         || name.contains("netherite_sword")
         || name.contains("netherite_pickaxe")
         || name.contains("netherite_axe")) {
         return ItemESP.ItemGroup.Netherite;
      } else if (name.contains("diamond_helmet")
         || name.contains("diamond_chestplate")
         || name.contains("diamond_leggings")
         || name.contains("diamond_boots")
         || name.contains("diamond_sword")
         || name.contains("diamond_pickaxe")
         || name.contains("diamond_axe")) {
         return ItemESP.ItemGroup.Diamond;
      } else if (name.contains("golden_helmet")
         || name.contains("golden_chestplate")
         || name.contains("golden_leggings")
         || name.contains("golden_boots")
         || name.contains("golden_sword")
         || name.contains("golden_pickaxe")
         || name.contains("golden_axe")) {
         return ItemESP.ItemGroup.Gold;
      } else if (name.contains("iron_helmet")
         || name.contains("iron_chestplate")
         || name.contains("iron_leggings")
         || name.contains("iron_boots")
         || name.contains("iron_sword")
         || name.contains("iron_pickaxe")
         || name.contains("iron_axe")) {
         return ItemESP.ItemGroup.Iron;
      } else if (name.contains("leather_helmet")
         || name.contains("leather_chestplate")
         || name.contains("leather_leggings")
         || name.contains("leather_boots")
         || name.contains("leather_sword")
         || name.contains("leather_pickaxe")
         || name.contains("leather_axe")) {
         return ItemESP.ItemGroup.Leather;
      } else {
         return !name.contains("mace") && !name.contains("trident") && !name.contains("elytra") && !name.contains("enchanted_golden_apple")
            ? ItemESP.ItemGroup.OtherItem
            : ItemESP.ItemGroup.OtherGear;
      }
   }

   private int getGroupColor(ItemESP.ItemGroup group) {
      switch (group) {
         case Netherite:
            return this.showNetherite.getValue() ? this.netheriteColor.getValue().getColor() : this.otherItemColor.getValue().getColor();
         case Diamond:
            return this.showDiamond.getValue() ? this.diamondColor.getValue().getColor() : this.otherItemColor.getValue().getColor();
         case Gold:
            return this.showGold.getValue() ? this.goldColor.getValue().getColor() : this.otherItemColor.getValue().getColor();
         case Iron:
            return this.showIron.getValue() ? this.ironColor.getValue().getColor() : this.otherItemColor.getValue().getColor();
         case Leather:
            return this.showLeather.getValue() ? this.leatherColor.getValue().getColor() : this.otherItemColor.getValue().getColor();
         case OtherGear:
            return this.showOtherGear.getValue() ? this.otherGearColor.getValue().getColor() : this.otherItemColor.getValue().getColor();
         default:
            return this.otherItemColor.getValue().getColor();
      }
   }

   private boolean hasAnyGroupEnabled() {
      return this.showNetherite.getValue()
         || this.showDiamond.getValue()
         || this.showGold.getValue()
         || this.showIron.getValue()
         || this.showLeather.getValue()
         || this.showOtherGear.getValue()
         || this.showOtherItem.getValue();
   }

   private boolean shouldRender(ItemESP.ItemGroup group) {
      if (this.showOtherItem.getValue()) {
         return true;
      }

      return switch (group) {
         case Netherite -> this.showNetherite.getValue();
         case Diamond -> this.showDiamond.getValue();
         case Gold -> this.showGold.getValue();
         case Iron -> this.showIron.getValue();
         case Leather -> this.showLeather.getValue();
         case OtherGear -> this.showOtherGear.getValue();
         case OtherItem -> false;
      };
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (this.hasAnyGroupEnabled()) {
         boolean onlyOtherItem = !this.showNetherite.getValue()
            && !this.showDiamond.getValue()
            && !this.showGold.getValue()
            && !this.showIron.getValue()
            && !this.showLeather.getValue()
            && !this.showOtherGear.getValue()
            && this.showOtherItem.getValue();

         for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof ItemEntity itemEntity) {
               ItemESP.ItemGroup group = this.getItemGroup(itemEntity);
               if (this.shouldRender(group)) {
                  int groupColor = this.getGroupColor(group);
                  if (onlyOtherItem && group != ItemESP.ItemGroup.OtherItem) {
                     groupColor = this.otherItemColor.getValue().getColor();
                  }

                  Vec3d[] vectors = getPoints(itemEntity);
                  Vector4d position = null;

                  for (Vec3d vector : vectors) {
                     vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                     if (vector.z > 0.0 && vector.z < 1.0) {
                        if (position == null) {
                           position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
                        }

                        position.x = Math.min(vector.x, position.x);
                        position.y = Math.min(vector.y, position.y);
                        position.z = Math.max(vector.x, position.z);
                        position.w = Math.max(vector.y, position.w);
                     }
                  }

                  if (position != null) {
                     float posX = (float)position.x;
                     float posY = (float)position.y;
                     float endPosX = (float)position.z;
                     float diff = (endPosX - posX) / 2.0F;
                     float centerX = posX + diff;
                     String name = itemEntity.method_5476().getString();
                     float textWidthVal = FontRenderers.sf_bold_micro.getStringWidth(name);
                     float textHeight = FontRenderers.sf_bold_micro.getStringHeight(name);
                     float iconSize = 8.0F;
                     float iconBgSize = iconSize + 2.0F;
                     float iconBgPadding = (iconBgSize - iconSize) / 2.0F;
                     float textBgWidth = textWidthVal + 2.0F;
                     float textBgHeight = textHeight;
                     float gap = 1.0F;
                     float textBgX = centerX - textBgWidth / 2.0F;
                     float textBgY = posY - 2.0F - textBgHeight;
                     float iconBgX = centerX - iconBgSize / 2.0F;
                     float iconBgY = textBgY - gap - iconBgSize;
                     Render2DEngine.drawBlurredShadow(
                        context.getMatrices(), iconBgX - 2.0F, iconBgY - 2.0F, iconBgSize + 4.0F, iconBgSize + 4.0F, 8, new Color(0, 0, 0, 120)
                     );
                     Render2DEngine.drawRect(context.getMatrices(), iconBgX, iconBgY, iconBgSize, iconBgSize, new Color(0, 0, 0, 150));
                     Render2DEngine.drawBlurredShadow(
                        context.getMatrices(), textBgX - 2.0F, textBgY - 2.0F, textBgWidth + 4.0F, textBgHeight + 4.0F, 10, new Color(0, 0, 0, 120)
                     );
                     Render2DEngine.drawRect(context.getMatrices(), textBgX, textBgY, textBgWidth, textBgHeight, new Color(0, 0, 0, 150));
                     float iconDrawX = iconBgX + iconBgPadding;
                     float iconDrawY = iconBgY + iconBgPadding;
                     context.getMatrices().push();
                     context.getMatrices().translate(iconDrawX, iconDrawY, 0.0F);
                     float iconScale = iconSize / 16.0F;
                     context.getMatrices().scale(iconScale, iconScale, 1.0F);
                     context.drawItem(itemEntity.getStack(), 0, 0);
                     context.drawItemInSlot(mc.textRenderer, itemEntity.getStack(), 0, 0);
                     context.getMatrices().pop();
                     float tagXNew = centerX - textWidthVal / 2.0F;
                     float tagYNew = textBgY + (textBgHeight - textHeight) / 2.0F + 2.5F;
                     FontRenderers.sf_bold_micro.drawString(context.getMatrices(), name, tagXNew, tagYNew, groupColor);
                  }
               }
            }
         }

         boolean any = false;

         for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof ItemEntity itemEntity) {
               ItemESP.ItemGroup group = this.getItemGroup(itemEntity);
               if (this.shouldRender(group)) {
                  Vec3d[] vectors = getPoints(itemEntity);
                  Vector4d position = null;

                  for (Vec3d vector : vectors) {
                     vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                     if (vector.z > 0.0 && vector.z < 1.0) {
                        if (position == null) {
                           position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
                        }

                        position.x = Math.min(vector.x, position.x);
                        position.y = Math.min(vector.y, position.y);
                        position.z = Math.max(vector.x, position.z);
                        position.w = Math.max(vector.y, position.w);
                     }
                  }

                  if (position != null) {
                     any = true;
                  }
               }
            }
         }

         if (any) {
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            Render2DEngine.setupRender();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (Entity ent : mc.world.getEntities()) {
               if (ent instanceof ItemEntity itemEntity) {
                  ItemESP.ItemGroup group = this.getItemGroup(itemEntity);
                  if (this.shouldRender(group)) {
                     Vec3d[] vectors = getPoints(itemEntity);
                     Vector4d position = null;

                     for (Vec3d vector : vectors) {
                        vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                        if (vector.z > 0.0 && vector.z < 1.0) {
                           if (position == null) {
                              position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
                           }

                           position.x = Math.min(vector.x, position.x);
                           position.y = Math.min(vector.y, position.y);
                           position.z = Math.max(vector.x, position.z);
                           position.w = Math.max(vector.y, position.w);
                        }
                     }

                     if (position != null) {
                        float posX = (float)position.x;
                        float posY = (float)position.y;
                        float endPosX = (float)position.z;
                        float endPosY = (float)position.w;
                        this.drawRect(bufferBuilder, matrix, posX, posY, endPosX, endPosY);
                     }
                  }
               }
            }

            Render2DEngine.endBuilding(bufferBuilder);
            Render2DEngine.endRender();
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.hasAnyGroupEnabled()) {
         ;
      }
   }

   private void drawRect(BufferBuilder bufferBuilder, Matrix4f stack, float posX, float posY, float endPosX, float endPosY) {
      float width = endPosX - posX;
      float height = endPosY - posY;
      if (!(width <= 0.0F) && !(height <= 0.0F)) {
         float horizontal = Math.min(Math.max(width * 0.3F, 3.0F), width * 0.5F);
         float vertical = Math.min(Math.max(height * 0.3F, 3.0F), height * 0.5F);
         this.drawCornerBox(
            bufferBuilder, stack, posX - 0.5F, posY - 0.5F, endPosX + 0.5F, endPosY + 0.5F, horizontal + 0.5F, vertical + 0.5F, 1.5F, Color.BLACK
         );
         this.drawCornerBox(bufferBuilder, stack, posX, posY, endPosX, endPosY, horizontal, vertical, 0.7F, HudEditor.getColor(0));
      }
   }

   private void drawCornerBox(
      BufferBuilder bufferBuilder,
      Matrix4f stack,
      float left,
      float top,
      float right,
      float bottom,
      float horizontal,
      float vertical,
      float thickness,
      Color color
   ) {
      Render2DEngine.setRectPoints(bufferBuilder, stack, left, top, left + horizontal, top + thickness, color, color, color, color);
      Render2DEngine.setRectPoints(bufferBuilder, stack, right - horizontal, top, right, top + thickness, color, color, color, color);
      Render2DEngine.setRectPoints(bufferBuilder, stack, left, bottom - thickness, left + horizontal, bottom, color, color, color, color);
      Render2DEngine.setRectPoints(bufferBuilder, stack, right - horizontal, bottom - thickness, right, bottom, color, color, color, color);
      Render2DEngine.setRectPoints(bufferBuilder, stack, left, top, left + thickness, top + vertical, color, color, color, color);
      Render2DEngine.setRectPoints(bufferBuilder, stack, right - thickness, top, right, top + vertical, color, color, color, color);
      Render2DEngine.setRectPoints(bufferBuilder, stack, left, bottom - vertical, left + thickness, bottom, color, color, color, color);
      Render2DEngine.setRectPoints(bufferBuilder, stack, right - thickness, bottom - vertical, right, bottom, color, color, color, color);
   }

   @NotNull
   private static Vec3d[] getPoints(Entity ent) {
      Box axisAlignedBB = getBox(ent);
      return new Vec3d[]{
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
      };
   }

   @NotNull
   private static Box getBox(Entity ent) {
      double x = ent.prevX + (ent.getX() - ent.prevX) * Render3DEngine.getTickDelta();
      double y = ent.prevY + (ent.getY() - ent.prevY) * Render3DEngine.getTickDelta();
      double z = ent.prevZ + (ent.getZ() - ent.prevZ) * Render3DEngine.getTickDelta();
      Box axisAlignedBB2 = ent.method_5829();
      return new Box(
         axisAlignedBB2.minX - ent.getX() + x - 0.05,
         axisAlignedBB2.minY - ent.getY() + y,
         axisAlignedBB2.minZ - ent.getZ() + z - 0.05,
         axisAlignedBB2.maxX - ent.getX() + x + 0.05,
         axisAlignedBB2.maxY - ent.getY() + y + 0.15,
         axisAlignedBB2.maxZ - ent.getZ() + z + 0.05
      );
   }

   private enum ItemGroup {
      Netherite,
      Diamond,
      Gold,
      Iron,
      Leather,
      OtherGear,
      OtherItem;
   }
}
