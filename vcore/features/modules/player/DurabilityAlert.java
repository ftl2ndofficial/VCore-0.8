package vcore.features.modules.player;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import vcore.core.Managers;
import vcore.features.modules.Module;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.render.TextureStorage;

public class DurabilityAlert extends Module {
   private final Setting<Boolean> friends = new Setting<>("Friend message", false);
   private final Setting<Integer> percent = new Setting<>("Percent", 20, 1, 100);
   private static final Color WARNING_COLOR = new Color(255, 92, 92);
   private static final int ICON_RENDER_SIZE = 50;
   private static final int ICON_TEXTURE_SIZE = 200;
   private boolean need_alert = false;
   private final Timer timer = new Timer();

   public DurabilityAlert() {
      super("DurabilityAlert", "Alerts when armor is low.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (this.friends.getValue()) {
         for (PlayerEntity player : mc.world.method_18456()) {
            if (Managers.FRIEND.isFriend(player) && player != mc.player) {
               for (ItemStack stack : player.getInventory().armor) {
                  if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem && getDurability(stack) < this.percent.getValue() && this.timer.passedMs(30000L)
                     )
                   {
                     mc.player.networkHandler.sendChatCommand("msg " + player.method_5477().getString() + " Your armor is about to break!");
                     this.timer.reset();
                  }
               }
            }
         }
      }

      boolean flag = false;

      for (ItemStack stack : mc.player.method_31548().armor) {
         if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem && getDurability(stack) < this.percent.getValue()) {
            this.need_alert = true;
            flag = true;
         }
      }

      if (!flag && this.need_alert) {
         this.need_alert = false;
      }
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (this.need_alert) {
         FontRenderers.sf_bold
            .drawCenteredString(
               context.getMatrices(),
               "Armor about to break!",
               mc.getWindow().getScaledWidth() / 2.0F,
               mc.getWindow().getScaledHeight() / 3.0F - 60.0F,
               WARNING_COLOR.getRGB()
            );
         RenderSystem.setShaderColor(WARNING_COLOR.getRed() / 255.0F, WARNING_COLOR.getGreen() / 255.0F, WARNING_COLOR.getBlue() / 255.0F, 1.0F);
         RenderSystem.setShaderTexture(0, TextureStorage.brokenShield);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         context.drawTexture(
            TextureStorage.brokenShield,
            (int)(mc.getWindow().getScaledWidth() / 2.0F - 25.0F),
            (int)(mc.getWindow().getScaledHeight() / 3.0F - 120.0F),
            50,
            50,
            0.0F,
            0.0F,
            200,
            200,
            200,
            200
         );
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public static int getDurability(ItemStack stack) {
      return (int)((stack.getMaxDamage() - stack.getDamage()) / Math.max(0.1, stack.getMaxDamage()) * 100.0);
   }
}
