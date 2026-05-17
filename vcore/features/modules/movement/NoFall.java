package vcore.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import vcore.events.impl.EventSync;
import vcore.events.impl.EventTick;
import vcore.events.impl.PacketEvent;
import vcore.features.modules.Module;
import vcore.injection.accesors.IPlayerMoveC2SPacket;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.InteractionUtility;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.SearchInvResult;

public class NoFall extends Module {
   public final Setting<NoFall.Mode> mode = new Setting<>("Mode", NoFall.Mode.Rubberband);
   public final Setting<NoFall.FallDistance> fallDistance = new Setting<>("FallDistance", NoFall.FallDistance.Calc);
   public final Setting<Integer> fallDistanceValue = new Setting<>(
      "FallDistanceVal", 10, 2, 100, v -> this.fallDistance.getValue() == NoFall.FallDistance.Custom
   );
   private final Setting<Boolean> powderSnowBucket = new Setting<>("PowderSnowBucket", true, v -> this.mode.getValue() == NoFall.Mode.Items);
   private final Setting<Boolean> waterBucket = new Setting<>("WaterBucket", true, v -> this.mode.getValue() == NoFall.Mode.Items);
   private final Setting<Boolean> retrieve = new Setting<>("Retrieve", true, v -> this.mode.getValue() == NoFall.Mode.Items && this.waterBucket.getValue());
   private final Setting<Boolean> enderPearl = new Setting<>("EnderPearl", true, v -> this.mode.getValue() == NoFall.Mode.Items);
   private final Setting<Boolean> cobweb = new Setting<>("Cobweb", true, v -> this.mode.getValue() == NoFall.Mode.Items);
   private final Setting<Boolean> twistingVines = new Setting<>("TwistingVines", true, v -> this.mode.getValue() == NoFall.Mode.Items);
   private Timer pearlCooldown = new Timer();
   private boolean retrieveFlag;
   private boolean cancelGround = false;

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (!fullNullCheck()) {
         if (this.isFalling()) {
            switch ((NoFall.Mode)this.mode.getValue()) {
               case Rubberband:
                  this.sendPacket(new OnGroundOnly(true));
                  break;
               case Items:
                  BlockPos playerPos = BlockPos.ofFloored(mc.player.method_19538());
                  SearchInvResult snowResult = InventoryUtility.findItemInHotBar(Items.POWDER_SNOW_BUCKET);
                  SearchInvResult pearlResult = InventoryUtility.findItemInHotBar(Items.ENDER_PEARL);
                  SearchInvResult webResult = InventoryUtility.findItemInHotBar(Items.COBWEB);
                  SearchInvResult vinesResult = InventoryUtility.findItemInHotBar(Items.TWISTING_VINES);
                  SearchInvResult waterResult = InventoryUtility.findItemInHotBar(Items.WATER_BUCKET);
                  if (waterResult.found() && this.waterBucket.getValue()) {
                     mc.player.method_36457(90.0F);
                     this.doWaterDrop(waterResult, playerPos);
                  } else if (pearlResult.found() && this.enderPearl.getValue()) {
                     mc.player.method_36457(90.0F);
                     this.doPearlDrop(pearlResult);
                  } else if (webResult.found() && this.cobweb.getValue()) {
                     mc.player.method_36457(90.0F);
                     this.doWebDrop(webResult, playerPos);
                  } else if (vinesResult.found() && this.twistingVines.getValue()) {
                     mc.player.method_36457(90.0F);
                     this.doVinesDrop(vinesResult, playerPos);
                  } else if (snowResult.found() && this.powderSnowBucket.getValue()) {
                     mc.player.method_36457(90.0F);
                     this.doSnowDrop(snowResult, playerPos);
                  }
                  break;
               case MatrixOffGround:
               case Vanilla:
                  this.cancelGround = true;
            }
         } else if (this.retrieveFlag) {
            InventoryUtility.saveSlot();
            SearchInvResult waterResult = InventoryUtility.findItemInHotBar(Items.BUCKET);
            waterResult.switchTo();
            mc.player.method_36457(90.0F);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.method_6104(Hand.MAIN_HAND);
            InventoryUtility.returnSlot();
            this.retrieveFlag = false;
         }
      }
   }

   @EventHandler
   public void onTick(EventTick e) {
      if (this.mode.is(NoFall.Mode.Grim2b2t) && this.isFalling()) {
         this.sendPacket(
            new Full(
               mc.player.method_23317(), mc.player.method_23318() + 1.0E-9, mc.player.method_23321(), mc.player.method_36454(), mc.player.method_36455(), false
            )
         );
         mc.player.method_38785();
      }
   }

   private void doWaterDrop(SearchInvResult waterResult, BlockPos playerPos) {
      if (mc.world.method_8320(playerPos.down()).method_51367() || mc.world.method_8320(playerPos.down().down()).method_51367()) {
         InventoryUtility.saveSlot();
         waterResult.switchTo();
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.player.method_6104(Hand.MAIN_HAND);
         InventoryUtility.returnSlot();
         this.retrieveFlag = this.retrieve.getValue();
      }
   }

   private void doPearlDrop(SearchInvResult pearlResult) {
      if (this.pearlCooldown.passedMs(5000L)) {
         InventoryUtility.saveSlot();
         pearlResult.switchTo();
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.player.method_6104(Hand.MAIN_HAND);
         InventoryUtility.returnSlot();
         this.pearlCooldown.reset();
      }
   }

   private void doWebDrop(SearchInvResult webResult, BlockPos playerPos) {
      if (mc.world.method_8320(playerPos.down()).method_51367() || mc.world.method_8320(playerPos.down().down()).method_51367()) {
         InventoryUtility.saveSlot();
         if (mc.world.method_8320(playerPos.down()).method_51367()) {
            InteractionUtility.placeBlock(
               playerPos,
               InteractionUtility.Rotate.None,
               InteractionUtility.Interact.Vanilla,
               InteractionUtility.PlaceMode.Normal,
               webResult.slot(),
               false,
               true
            );
         } else {
            InteractionUtility.placeBlock(
               playerPos.down(),
               InteractionUtility.Rotate.None,
               InteractionUtility.Interact.Vanilla,
               InteractionUtility.PlaceMode.Normal,
               webResult.slot(),
               false,
               true
            );
         }

         mc.player.method_6104(Hand.MAIN_HAND);
         InventoryUtility.returnSlot();
      }
   }

   private void doVinesDrop(SearchInvResult vinesResult, BlockPos playerPos) {
      if (mc.world.method_8320(playerPos.down()).method_51367() || mc.world.method_8320(playerPos.down().down()).method_51367()) {
         InventoryUtility.saveSlot();
         if (mc.world.method_8320(playerPos.down()).method_51367()) {
            InteractionUtility.placeBlock(
               playerPos,
               InteractionUtility.Rotate.None,
               InteractionUtility.Interact.Vanilla,
               InteractionUtility.PlaceMode.Normal,
               vinesResult.slot(),
               false,
               true
            );
         } else {
            InteractionUtility.placeBlock(
               playerPos.down(),
               InteractionUtility.Rotate.None,
               InteractionUtility.Interact.Vanilla,
               InteractionUtility.PlaceMode.Normal,
               vinesResult.slot(),
               false,
               true
            );
         }

         mc.player.method_6104(Hand.MAIN_HAND);
         InventoryUtility.returnSlot();
      }
   }

   private void doSnowDrop(SearchInvResult snowResult, BlockPos playerPos) {
      if (mc.world.method_8320(playerPos.down()).method_51367() || mc.world.method_8320(playerPos.down().down()).method_51367()) {
         InventoryUtility.saveSlot();
         if (mc.world.method_8320(playerPos.down()).method_51367()) {
            InteractionUtility.placeBlock(
               playerPos,
               InteractionUtility.Rotate.None,
               InteractionUtility.Interact.Vanilla,
               InteractionUtility.PlaceMode.Normal,
               snowResult.slot(),
               false,
               true
            );
         } else {
            InteractionUtility.placeBlock(
               playerPos.down(),
               InteractionUtility.Rotate.None,
               InteractionUtility.Interact.Vanilla,
               InteractionUtility.PlaceMode.Normal,
               snowResult.slot(),
               false,
               true
            );
         }

         mc.player.method_6104(Hand.MAIN_HAND);
         InventoryUtility.returnSlot();
      }
   }

   public boolean isFalling() {
      if (mc == null || mc.player == null || mc.world == null) {
         return false;
      }

      if (mc.player.method_6128()) {
         return false;
      }

      if (this.mode.is(NoFall.Mode.Grim2b2t)) {
         return mc.player.field_6017 > 3.0F;
      }

      switch ((NoFall.FallDistance)this.fallDistance.getValue()) {
         case Calc:
            return (mc.player.field_6017 - 3.0F) / 2.0F + 3.5F > mc.player.method_6032() / 3.0F;
         case Custom:
            return mc.player.field_6017 > this.fallDistanceValue.getValue().intValue();
         default:
            return false;
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().toString() + " " + (this.isFalling() ? "Ready" : "");
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (e.getPacket() instanceof PlayerMoveC2SPacket pac && this.cancelGround) {
         ((IPlayerMoveC2SPacket)pac).setOnGround(false);
      }
   }

   @Override
   public void onEnable() {
      this.cancelGround = false;
   }

   private enum FallDistance {
      Calc,
      Custom;
   }

   private enum Mode {
      Rubberband,
      Items,
      MatrixOffGround,
      Vanilla,
      Grim2b2t;
   }
}
