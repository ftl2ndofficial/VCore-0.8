package vcore.features.modules.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import vcore.events.impl.EventHeldItemRenderer;
import vcore.events.impl.EventSetting;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.SettingGroup;

public class ViewModel extends Module {
   public final Setting<Boolean> syncHands = new Setting<>("SyncHands", true);
   public final Setting<SettingGroup> mainHand = new Setting<>("MainHand", new SettingGroup(false, 0));
   public final Setting<Float> positionMainX = new Setting<>("positionMainX", 0.0F, -3.0F, 3.0F).addToGroup(this.mainHand);
   public final Setting<Float> positionMainY = new Setting<>("positionMainY", 0.0F, -3.0F, 3.0F).addToGroup(this.mainHand);
   public final Setting<Float> positionMainZ = new Setting<>("positionMainZ", 0.0F, -3.0F, 3.0F).addToGroup(this.mainHand);
   public final Setting<Float> scaleMain = new Setting<>("ScaleMain", 1.0F, 0.1F, 1.5F).addToGroup(this.mainHand);
   public final Setting<SettingGroup> rotationMain = new Setting<>("Rotation", new SettingGroup(false, 1)).addToGroup(this.mainHand);
   public final Setting<Float> rotationMainX = new Setting<>("rotationMainX", 0.0F, -180.0F, 180.0F).addToGroup(this.rotationMain);
   public final Setting<Float> rotationMainY = new Setting<>("rotationMainY", 0.0F, -180.0F, 180.0F).addToGroup(this.rotationMain);
   public final Setting<Float> rotationMainZ = new Setting<>("rotationMainZ", 0.0F, -180.0F, 180.0F).addToGroup(this.rotationMain);
   public final Setting<SettingGroup> offHand = new Setting<>("OffHand", new SettingGroup(false, 0));
   public final Setting<Float> positionOffX = new Setting<>("positionOffX", 0.0F, -3.0F, 3.0F).addToGroup(this.offHand);
   public final Setting<Float> positionOffY = new Setting<>("positionOffY", 0.0F, -3.0F, 3.0F).addToGroup(this.offHand);
   public final Setting<Float> positionOffZ = new Setting<>("positionOffZ", 0.0F, -3.0F, 3.0F).addToGroup(this.offHand);
   public final Setting<Float> scaleOff = new Setting<>("ScaleOff", 1.0F, 0.1F, 1.5F).addToGroup(this.offHand);
   public final Setting<SettingGroup> rotationOff = new Setting<>("RotationOff", new SettingGroup(false, 1)).addToGroup(this.offHand);
   public final Setting<Float> rotationOffX = new Setting<>("rotationOffX", 0.0F, -180.0F, 180.0F).addToGroup(this.rotationOff);
   public final Setting<Float> rotationOffY = new Setting<>("rotationOffY", 0.0F, -180.0F, 180.0F).addToGroup(this.rotationOff);
   public final Setting<Float> rotationOffZ = new Setting<>("rotationOffZ", 0.0F, -180.0F, 180.0F).addToGroup(this.rotationOff);
   public final Setting<SettingGroup> eatMod = new Setting<>("Eat", new SettingGroup(false, 0));
   public final Setting<Float> eatX = new Setting<>("EatX", 1.0F, -2.0F, 2.0F).addToGroup(this.eatMod);
   public final Setting<Float> eatY = new Setting<>("EatY", 1.0F, -2.0F, 2.0F).addToGroup(this.eatMod);

   public ViewModel() {
      super("ViewModel", "Hand/weapon positioning.", Module.Category.RENDER);
   }

   @EventHandler
   public void onSettingChange(EventSetting e) {
      if (this.isEnabled()) {
         if (this.syncHands.getValue()) {
            if (e.getSetting() == this.positionMainX) {
               this.positionOffX.setValueSilent(this.positionMainX.getValue());
            }

            if (e.getSetting() == this.positionMainY) {
               this.positionOffY.setValueSilent(this.positionMainY.getValue());
            }

            if (e.getSetting() == this.positionMainZ) {
               this.positionOffZ.setValueSilent(this.positionMainZ.getValue());
            }

            if (e.getSetting() == this.positionOffX) {
               this.positionMainX.setValueSilent(this.positionOffX.getValue());
            }

            if (e.getSetting() == this.positionOffY) {
               this.positionMainY.setValueSilent(this.positionOffY.getValue());
            }

            if (e.getSetting() == this.positionOffZ) {
               this.positionMainZ.setValueSilent(this.positionOffZ.getValue());
            }

            if (e.getSetting() == this.scaleMain) {
               this.scaleOff.setValueSilent(this.scaleMain.getValue());
            }

            if (e.getSetting() == this.scaleOff) {
               this.scaleMain.setValueSilent(this.scaleOff.getValue());
            }
         }
      }
   }

   @EventHandler
   private void onHeldItemRender(EventHeldItemRenderer event) {
      if (this.isEnabled()) {
         if (!event.getItem().isOf(Items.CROSSBOW)) {
            if (event.getHand() == Hand.MAIN_HAND) {
               event.getStack().translate(this.positionMainX.getValue(), this.positionMainY.getValue(), this.positionMainZ.getValue());
               event.getStack().scale(this.scaleMain.getValue(), this.scaleMain.getValue(), this.scaleMain.getValue());
               event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotationMainX.getValue()));
               event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationMainY.getValue()));
               event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationMainZ.getValue()));
            } else {
               event.getStack().translate(-this.positionOffX.getValue(), this.positionOffY.getValue(), this.positionOffZ.getValue());
               event.getStack().scale(this.scaleOff.getValue(), this.scaleOff.getValue(), this.scaleOff.getValue());
               event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotationOffX.getValue()));
               event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationOffY.getValue()));
               event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationOffZ.getValue()));
            }
         }
      }
   }

   public float getEatXFactor() {
      return this.eatX.getValue();
   }
}
