package vcore.injection;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vcore.features.modules.misc.UnHook;
import vcore.utility.AccountUtility;

@Mixin(MultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {
   private static final int EDGE_PADDING = 5;
   private static final int RANDOM_ALT_BUTTON_Y = 8;
   private static final int RANDOM_ALT_BUTTON_WIDTH = 70;
   private static final int RANDOM_ALT_BUTTON_HEIGHT = 20;
   private static final int ACCOUNT_TEXT_X = 5;
   private static final int ACCOUNT_TEXT_Y = 14;
   private static final int ACCOUNT_TEXT_COLOR = -1;
   private ButtonWidget randomAltButton;

   protected MixinMultiplayerScreen(Text title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void addRandomAltButton(CallbackInfo ci) {
      if (UnHook.isActive()) {
         this.randomAltButton = null;
      } else {
         this.randomAltButton = (ButtonWidget)this.method_37063(
            ButtonWidget.builder(Text.of("Random Alt"), button -> AccountUtility.loginRandomAlt()).dimensions(this.field_22789 - 70 - 5, 8, 70, 20).build()
         );
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void removeRandomAltWhenUnhooked(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (UnHook.isActive() && this.randomAltButton != null) {
         this.method_37066(this.randomAltButton);
         this.randomAltButton = null;
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void renderAccountText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!UnHook.isActive() && this.field_22787 != null) {
         context.drawTextWithShadow(this.field_22793, "Account: " + this.field_22787.getSession().getUsername(), 5, 14, -1);
      }
   }
}
