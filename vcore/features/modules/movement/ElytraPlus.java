package vcore.features.modules.movement;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import vcore.Vcore;
import vcore.core.Managers;
import vcore.events.impl.EventMove;
import vcore.events.impl.PacketEvent;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.features.modules.combat.Criticals;
import vcore.features.modules.player.ElytraSwap;
import vcore.features.modules.render.HudEditor;
import vcore.gui.font.FontRenderers;
import vcore.setting.Setting;
import vcore.setting.impl.Bind;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.utility.Timer;
import vcore.utility.math.MathUtility;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;
import vcore.utility.player.PlayerUtility;
import vcore.utility.render.Render2DEngine;
import vcore.utility.render.Render3DEngine;

public class ElytraPlus extends Module {
   public final Setting<ElytraPlus.Mode> mode = new Setting<>("Mode", ElytraPlus.Mode.FireWork);
   private final Setting<Integer> disablerDelay = new Setting<>("DisablerDelay", 1, 0, 10, v -> this.mode.is(ElytraPlus.Mode.SunriseOld));
   private final Setting<Boolean> twoBee = new Setting<>("2b2t", false, v -> this.mode.is(ElytraPlus.Mode.Boost));
   private final Setting<Boolean> onlySpace = new Setting<>("OnlySpace", true, v -> this.mode.is(ElytraPlus.Mode.Boost) && this.twoBee.getValue());
   private final Setting<Boolean> stopOnGround = new Setting<>("StopOnGround", false, v -> this.mode.is(ElytraPlus.Mode.Packet));
   private final Setting<Boolean> infDurability = new Setting<>("InfDurability", true, v -> this.mode.is(ElytraPlus.Mode.Packet));
   private final Setting<Boolean> vertical = new Setting<>("Vertical", false, v -> this.mode.is(ElytraPlus.Mode.Packet));
   private final Setting<ElytraPlus.NCPStrict> ncpStrict = new Setting<>("NCPStrict", ElytraPlus.NCPStrict.Off, v -> this.mode.is(ElytraPlus.Mode.Packet));
   private final Setting<ElytraPlus.AntiKick> antiKick = new Setting<>(
      "AntiKick", ElytraPlus.AntiKick.Jitter, v -> this.mode.is(ElytraPlus.Mode.FireWork) || this.mode.is(ElytraPlus.Mode.SunriseOld)
   );
   private final Setting<Float> xzSpeed = new Setting<>(
      "XZSpeed", 1.55F, 0.1F, 10.0F, v -> !this.mode.is(ElytraPlus.Mode.Boost) && this.mode.getValue() != ElytraPlus.Mode.Pitch40Infinite
   );
   private final Setting<Float> ySpeed = new Setting<>(
      "YSpeed",
      0.47F,
      0.0F,
      2.0F,
      v -> this.mode.is(ElytraPlus.Mode.FireWork)
         || this.mode.getValue() == ElytraPlus.Mode.SunriseOld
         || this.mode.is(ElytraPlus.Mode.Packet) && this.vertical.getValue()
   );
   private final Setting<Integer> fireSlot = new Setting<>("FireSlot", 1, 1, 9, v -> this.mode.is(ElytraPlus.Mode.FireWork));
   private final Setting<BooleanSettingGroup> accelerate = new Setting<>(
      "Acceleration", new BooleanSettingGroup(false), v -> this.mode.is(ElytraPlus.Mode.Control) || this.mode.is(ElytraPlus.Mode.Packet)
   );
   private final Setting<Float> accelerateFactor = new Setting<>(
         "AccelerateFactor", 9.0F, 0.0F, 100.0F, v -> this.mode.is(ElytraPlus.Mode.Control) || this.mode.is(ElytraPlus.Mode.Packet)
      )
      .addToGroup(this.accelerate);
   private final Setting<Float> fireDelay = new Setting<>("FireDelay", 1.5F, 0.0F, 1.5F, v -> this.mode.is(ElytraPlus.Mode.FireWork));
   private final Setting<BooleanSettingGroup> grim = new Setting<>("Grim", new BooleanSettingGroup(false), v -> this.mode.is(ElytraPlus.Mode.FireWork));
   private final Setting<Boolean> rotate = new Setting<>("Rotate", true, v -> this.mode.is(ElytraPlus.Mode.FireWork)).addToGroup(this.grim);
   private final Setting<Boolean> fireWorkExtender = new Setting<>("FireWorkExtender", true, v -> this.mode.is(ElytraPlus.Mode.FireWork)).addToGroup(this.grim);
   private final Setting<Boolean> stayMad = new Setting<>("GroundSafe", false, v -> this.mode.is(ElytraPlus.Mode.FireWork));
   private final Setting<Boolean> keepFlying = new Setting<>("KeepFlying", false, v -> this.mode.is(ElytraPlus.Mode.FireWork));
   private final Setting<Boolean> disableOnFlag = new Setting<>("DisableOnFlag", false, v -> this.mode.is(ElytraPlus.Mode.FireWork));
   private final Setting<Boolean> allowFireSwap = new Setting<>("AllowFireSwap", false, v -> this.mode.is(ElytraPlus.Mode.FireWork));
   private final Setting<Boolean> bowBomb = new Setting<>(
      "BowBomb", false, v -> this.mode.is(ElytraPlus.Mode.FireWork) || this.mode.getValue() == ElytraPlus.Mode.SunriseOld
   );
   private final Setting<Bind> bombKey = new Setting<>("BombKey", new Bind(-1, false, false), v -> this.mode.getValue() == ElytraPlus.Mode.SunriseOld);
   private final Setting<Boolean> instantFly = new Setting<>(
      "InstantFly", true, v -> this.mode.is(ElytraPlus.Mode.Boost) && !this.twoBee.getValue() || this.mode.is(ElytraPlus.Mode.Control)
   );
   private final Setting<Boolean> cruiseControl = new Setting<>("CruiseControl", false, v -> this.mode.is(ElytraPlus.Mode.Boost));
   private final Setting<Float> factor = new Setting<>("Factor", 1.5F, 0.1F, 50.0F, v -> this.mode.is(ElytraPlus.Mode.Boost));
   private final Setting<Float> upSpeed = new Setting<>(
      "UpSpeed", 1.0F, 0.01F, 5.0F, v -> this.mode.is(ElytraPlus.Mode.Boost) && !this.twoBee.getValue() || this.mode.is(ElytraPlus.Mode.Control)
   );
   private final Setting<Float> downFactor = new Setting<>(
      "Glide", 1.0F, 0.0F, 2.0F, v -> this.mode.is(ElytraPlus.Mode.Boost) && !this.twoBee.getValue() || this.mode.is(ElytraPlus.Mode.Control)
   );
   private final Setting<Boolean> stopMotion = new Setting<>("StopMotion", true, v -> this.mode.is(ElytraPlus.Mode.Boost) && !this.twoBee.getValue());
   private final Setting<Float> minUpSpeed = new Setting<>(
      "MinUpSpeed", 0.5F, 0.1F, 5.0F, v -> this.mode.is(ElytraPlus.Mode.Boost) && this.cruiseControl.getValue()
   );
   private final Setting<Boolean> forceHeight = new Setting<>("ForceHeight", false, v -> this.mode.is(ElytraPlus.Mode.Boost) && this.cruiseControl.getValue());
   private final Setting<Integer> manualHeight = new Setting<>("Height", 121, 1, 256, v -> this.mode.is(ElytraPlus.Mode.Boost) && this.forceHeight.getValue());
   private final Setting<Float> sneakDownSpeed = new Setting<>("DownSpeed", 1.0F, 0.01F, 5.0F, v -> this.mode.is(ElytraPlus.Mode.Control));
   private final Setting<Boolean> speedLimit = new Setting<>("SpeedLimit", true, v -> this.mode.is(ElytraPlus.Mode.Boost));
   private final Setting<Float> maxSpeed = new Setting<>("MaxSpeed", 2.5F, 0.1F, 10.0F, v -> this.mode.is(ElytraPlus.Mode.Boost));
   private final Setting<Float> redeployInterval = new Setting<>(
      "RedeployInterval", 1.0F, 0.1F, 5.0F, v -> this.mode.is(ElytraPlus.Mode.Boost) && !this.twoBee.getValue()
   );
   private final Setting<Float> redeployTimeOut = new Setting<>(
      "RedeployTimeout", 5.0F, 0.1F, 20.0F, v -> this.mode.is(ElytraPlus.Mode.Boost) && !this.twoBee.getValue()
   );
   private final Setting<Float> redeployDelay = new Setting<>(
      "RedeployDelay", 0.5F, 0.1F, 1.0F, v -> this.mode.is(ElytraPlus.Mode.Boost) && !this.twoBee.getValue()
   );
   private final Setting<Float> infiniteMaxSpeed = new Setting<>(
      "InfiniteMaxSpeed", 150.0F, 50.0F, 170.0F, v -> this.mode.getValue() == ElytraPlus.Mode.Pitch40Infinite
   );
   private final Setting<Float> infiniteMinSpeed = new Setting<>(
      "InfiniteMinSpeed", 25.0F, 10.0F, 70.0F, v -> this.mode.getValue() == ElytraPlus.Mode.Pitch40Infinite
   );
   private final Setting<Integer> infiniteMaxHeight = new Setting<>(
      "InfiniteMaxHeight", 200, 50, 360, v -> this.mode.getValue() == ElytraPlus.Mode.Pitch40Infinite
   );
   private final Timer startTimer = new Timer();
   private final Timer redeployTimer = new Timer();
   private final Timer strictTimer = new Timer();
   private final Timer pingTimer = new Timer();
   private boolean infiniteFlag;
   private boolean hasTouchedGround;
   private boolean elytraEquiped;
   private boolean flying;
   private boolean started;
   private float acceleration;
   private float accelerationY;
   private float height;
   private float prevClientPitch;
   private float infinitePitch;
   private float lastInfinitePitch;
   private ItemStack prevArmorItemCopy;
   private ItemStack getStackInSlotCopy;
   private Item prevArmorItem = Items.AIR;
   private Item prevItemInHand = Items.AIR;
   private Vec3d flightZonePos;
   private int prevElytraSlot = -1;
   private int disablerTicks;
   private int slotWithFireWorks = -1;
   private long lastFireworkTime;

   public ElytraPlus() {
      super("Elytra+", "Enhanced elytra flight.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.player.method_23318() < this.infiniteMaxHeight.getValue().intValue() && this.mode.getValue() == ElytraPlus.Mode.Pitch40Infinite) {
         this.disable("Elytra's about to break!");
         mc.world
            .method_8465(
               mc.player,
               mc.player.method_23317(),
               mc.player.method_23318(),
               mc.player.method_23321(),
               SoundEvents.ENTITY_GENERIC_EXPLODE,
               SoundCategory.AMBIENT,
               10.0F,
               1.0F,
               0L
            );
      }
   }

   private void doSunriseNew() {
      if (mc.player.field_5976) {
         this.acceleration = 0.0F;
      }

      int elytra = InventoryUtility.getElytra();
      if (elytra != -1) {
         if (mc.player.method_24828()) {
            mc.player.method_6043();
            this.acceleration = 0.0F;
         } else if (!(mc.player.field_6017 <= 0.0F)) {
            if (!mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
               this.takeOnChestPlate();
               if (mc.player.field_6012 % 8 == 0) {
                  this.matrixDisabler(elytra);
               }

               MovementUtility.setMotion(Math.min((this.acceleration = this.acceleration + 8.0F / this.xzSpeed.getValue()) / 100.0F, this.xzSpeed.getValue()));
               if (!MovementUtility.isMoving()) {
                  this.acceleration = 0.0F;
               }

               mc.player.method_18800(mc.player.method_18798().method_10216(), -0.005F, mc.player.method_18798().method_10215());
            } else {
               this.acceleration = 0.0F;
               this.takeOnElytra();
            }
         }
      }
   }

   private void takeOnElytra() {
      int elytra = InventoryUtility.getElytra();
      if (elytra != -1) {
         elytra = elytra >= 0 && elytra < 9 ? elytra + 36 : elytra;
         if (elytra != -2) {
            clickSlot(elytra);
            clickSlot(6);
            clickSlot(elytra);
            mc.player
               .networkHandler
               .method_52787(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         }
      }
   }

   private void takeOnChestPlate() {
      int slot = ElytraSwap.getChestPlateSlot();
      if (slot != -1) {
         if (slot != -2) {
            clickSlot(slot);
            clickSlot(6);
            clickSlot(slot);
         }
      }
   }

   private float getInfinitePitch() {
      if (mc.player.method_23318() < this.infiniteMaxHeight.getValue().intValue()) {
         if (Managers.PLAYER.currentPlayerSpeed * 72.0F < this.infiniteMinSpeed.getValue() && !this.infiniteFlag) {
            this.infiniteFlag = true;
         }

         if (Managers.PLAYER.currentPlayerSpeed * 72.0F > this.infiniteMaxSpeed.getValue() && this.infiniteFlag) {
            this.infiniteFlag = false;
         }
      } else {
         this.infiniteFlag = true;
      }

      if (this.infiniteFlag) {
         this.infinitePitch += 3.0F;
      } else {
         this.infinitePitch -= 3.0F;
      }

      this.infinitePitch = MathUtility.clamp(this.infinitePitch, -40.0F, 40.0F);
      return this.infinitePitch;
   }

   @Override
   public void onDisable() {
      Vcore.TICK_TIMER = 1.0F;
      mc.player.method_31549().flying = false;
      mc.player.method_31549().setFlySpeed(0.05F);
      if (this.mode.is(ElytraPlus.Mode.FireWork)) {
         this.fireworkOnDisable();
      }
   }

   @EventHandler(priority = -200)
   public void onMove(EventMove e) {
      switch ((ElytraPlus.Mode)this.mode.getValue()) {
         case FireWork:
            this.fireworkOnMove(e);
         case SunriseOld:
         case Pitch40Infinite:
         case SunriseNew:
         default:
            break;
         case Boost:
            this.doBoost(e);
            break;
         case Control:
            this.doControl(e);
            break;
         case Packet:
            this.doMotionPacket(e);
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.SendPost event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof ClientCommandC2SPacket command
            && this.mode.is(ElytraPlus.Mode.FireWork)
            && command.getMode() == net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING) {
            this.doFireWork(false);
         }

         if (event.getPacket() instanceof PlayerInteractEntityC2SPacket p
            && this.mode.is(ElytraPlus.Mode.FireWork)
            && this.grim.getValue().isEnabled()
            && this.fireWorkExtender.getValue()
            && this.flying
            && this.flightZonePos != null
            && Criticals.getEntity(p).age < (float)this.pingTimer.getPassedTimeMs() / 50.0F) {
            this.sendMessage(Formatting.RED + "In this mode, you cannot hit entities that spawned after the module was turned on!");
         }
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof EntityTrackerUpdateS2CPacket pac
         && pac.id() == mc.player.method_5628()
         && (this.mode.is(ElytraPlus.Mode.Packet) || this.mode.is(ElytraPlus.Mode.SunriseOld))) {
         List<SerializedEntry<?>> values = pac.trackedValues();
         if (values.isEmpty()) {
            return;
         }

         for (SerializedEntry<?> value : values) {
            if (value.value().toString().equals("FALL_FLYING")
               || value.id() == 0
                  && (value.value().toString().equals("-120") || value.value().toString().equals("-128") || value.value().toString().equals("-126"))) {
               e.cancel();
            }
         }
      }

      if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.acceleration = 0.0F;
         this.accelerationY = 0.0F;
         this.pingTimer.reset();
         if (this.disableOnFlag.getValue() && this.mode.is(ElytraPlus.Mode.FireWork)) {
            this.disable("Disabled due to flag!");
         }
      }

      if (e.getPacket() instanceof CommonPingS2CPacket
         && this.mode.is(ElytraPlus.Mode.FireWork)
         && this.grim.getValue().isEnabled()
         && this.fireWorkExtender.getValue()
         && this.flying) {
         if (!this.pingTimer.passedMs(50000L)) {
            if (this.pingTimer.passedMs(1000L) && PlayerUtility.getSquaredDistance2D(this.flightZonePos) < 7000.0F) {
               e.cancel();
            }
         } else {
            this.pingTimer.reset();
         }
      }
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent e) {
      switch ((ElytraPlus.Mode)this.mode.getValue()) {
         case FireWork:
            this.fireWorkOnPlayerUpdate();
            break;
         case Pitch40Infinite:
            this.lastInfinitePitch = PlayerUtility.fixAngle(this.getInfinitePitch());
      }
   }

   private void doMotionPacket(EventMove e) {
      mc.player.method_31549().flying = false;
      mc.player.method_31549().setFlySpeed(0.05F);
      if ((!this.isBoxCollidingGround() || !this.stopOnGround.getValue()) && mc.player.method_31548().method_5438(38).getItem() == Items.ELYTRA) {
         mc.player.method_31549().flying = true;
         mc.player
            .method_31549()
            .setFlySpeed(
               this.xzSpeed.getValue()
                  / 15.0F
                  * (
                     this.accelerate.getValue().isEnabled()
                        ? Math.min((this.acceleration = this.acceleration + this.accelerateFactor.getValue()) / 100.0F, 1.0F)
                        : 1.0F
                  )
            );
         e.cancel();
         if (mc.player.field_6012 % 3 == 0 && this.ncpStrict.is(ElytraPlus.NCPStrict.Motion)) {
            e.setY(0.0);
            e.setX(0.0);
            e.setZ(0.0);
         } else {
            if (Math.abs(e.getX()) < 0.05) {
               e.setX(0.0);
            }

            if (Math.abs(e.getZ()) < 0.05) {
               e.setZ(0.0);
            }

            e.setY(
               this.vertical.getValue()
                  ? (mc.options.jumpKey.isPressed() ? this.ySpeed.getValue().floatValue() : (mc.options.sneakKey.isPressed() ? -this.ySpeed.getValue() : 0.0))
                  : 0.0
            );
            switch ((ElytraPlus.NCPStrict)this.ncpStrict.getValue()) {
               case Old:
                  e.setY(2.0E-4 - (mc.player.field_6012 % 2 == 0 ? 0.0 : 1.0E-6) + MathUtility.random(0.0, 9.0E-7));
                  break;
               case New:
                  e.setY(-1.0000889E-12F);
                  break;
               case Motion:
                  e.setY(-4.0003556E-12F);
            }

            if (mc.player.field_5976
               && (this.ncpStrict.is(ElytraPlus.NCPStrict.New) || this.ncpStrict.is(ElytraPlus.NCPStrict.Motion))
               && mc.player.field_6012 % 2 == 0) {
               e.setY(-0.07840000152587923);
            }

            if ((this.infDurability.getValue() || this.ncpStrict.is(ElytraPlus.NCPStrict.Motion)) && !MovementUtility.isMoving() && Math.abs(e.getX()) < 0.121) {
               float angleToRad = (float)Math.toRadians(4.5 * (mc.player.field_6012 % 80));
               e.setX(Math.sin(angleToRad) * 0.12);
               e.setZ(Math.cos(angleToRad) * 0.12);
            }
         }
      }
   }

   private void doPreLegacy() {
      if (!this.twoBee.getValue() || !this.mode.is(ElytraPlus.Mode.Boost)) {
         if (mc.player.method_24828()) {
            this.hasTouchedGround = true;
         }

         if (!this.cruiseControl.getValue()) {
            this.height = (float)mc.player.method_23318();
         }

         if (this.strictTimer.passedMs(1500L) && !this.strictTimer.passedMs(2000L)) {
            Vcore.TICK_TIMER = 1.0F;
         }

         if (!mc.player.method_6128()) {
            if (this.hasTouchedGround && !mc.player.method_24828() && mc.player.field_6017 > 0.0F && this.instantFly.getValue()) {
               Vcore.TICK_TIMER = 0.3F;
            }

            if (!mc.player.method_24828() && this.instantFly.getValue() && mc.player.method_18798().method_10214() < 0.0) {
               if (!this.startTimer.passedMs((long)(1000.0F * this.redeployDelay.getValue()))) {
                  return;
               }

               this.startTimer.reset();
               this.sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               this.hasTouchedGround = false;
               this.strictTimer.reset();
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.mode.is(ElytraPlus.Mode.FireWork)
         && this.grim.getValue().isEnabled()
         && this.fireWorkExtender.getValue()
         && this.flying
         && this.flightZonePos != null) {
         stack.push();
         Render3DEngine.setupRender();
         RenderSystem.disableCull();
         Tessellator tessellator = Tessellator.getInstance();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

         for (int i = 0; i <= 30; i++) {
            float cos = (float)(
               this.flightZonePos.method_10216() - mc.getEntityRenderDispatcher().camera.getPos().method_10216() + Math.cos(i * (Math.PI * 2) / 30.0) * 95.0
            );
            float sin = (float)(
               this.flightZonePos.method_10215() - mc.getEntityRenderDispatcher().camera.getPos().method_10215() + Math.sin(i * (Math.PI * 2) / 30.0) * 95.0
            );
            bufferBuilder.method_22918(stack.peek().getPositionMatrix(), cos, (float)(-mc.getEntityRenderDispatcher().camera.getPos().method_10214()), sin)
               .color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 255).getRGB());
            bufferBuilder.method_22918(
                  stack.peek().getPositionMatrix(), cos, (float)(128.0 - mc.getEntityRenderDispatcher().camera.getPos().method_10214()), sin
               )
               .color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 0).getRGB());
         }

         Render2DEngine.endBuilding(bufferBuilder);
         RenderSystem.enableCull();
         Render3DEngine.endRender();
         stack.pop();
      }
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (this.mode.is(ElytraPlus.Mode.FireWork)
         && this.grim.getValue().isEnabled()
         && this.fireWorkExtender.getValue()
         && this.flying
         && !this.pingTimer.passedMs(50000L)
         && this.pingTimer.passedMs(1000L)) {
         int timeS = (int)MathUtility.round2((float)(50000L - this.pingTimer.getPassedTimeMs()) / 1000.0F);
         int dist = (int)(83.0 - Math.sqrt(PlayerUtility.getSquaredDistance2D(this.flightZonePos)));
         FontRenderers.sf_bold
            .drawCenteredString(
               context.getMatrices(),
               timeS + " seconds and " + dist + " meters left",
               mc.getWindow().getScaledWidth() / 2.0F,
               mc.getWindow().getScaledHeight() / 2.0F + 30.0F,
               -1
            );
      }
   }

   private void doSunrise() {
      if (mc.player.field_5976) {
         this.acceleration = 0.0F;
      }

      if (mc.player.field_5992) {
         this.acceleration = 0.0F;
         mc.player.method_18800(mc.player.method_18798().method_10216(), 0.42F, mc.player.method_18798().method_10215());
      }

      int elytra = InventoryUtility.getElytra();
      if (elytra != -1) {
         if (mc.player.method_24828()) {
            mc.player.method_6043();
         }

         if (this.disablerTicks-- <= 0) {
            this.matrixDisabler(elytra);
         }

         if (mc.player.field_6017 > 0.25F) {
            MovementUtility.setMotion(Math.min((this.acceleration = this.acceleration + 11.0F / this.xzSpeed.getValue()) / 100.0F, this.xzSpeed.getValue()));
            if (!MovementUtility.isMoving()) {
               this.acceleration = 0.0F;
            }

            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), this.bombKey.getValue().getKey())) {
               MovementUtility.setMotion(0.8F);
               mc.player
                  .method_18800(
                     mc.player.method_18798().method_10216(), mc.player.field_6012 % 2 == 0 ? 0.42F : -0.42F, mc.player.method_18798().method_10215()
                  );
               this.acceleration = 70.0F;
            } else {
               switch ((ElytraPlus.AntiKick)this.antiKick.getValue()) {
                  case Off:
                     mc.player.method_18800(mc.player.method_18798().method_10216(), 0.0, mc.player.method_18798().method_10215());
                     break;
                  case Jitter:
                     mc.player
                        .method_18800(
                           mc.player.method_18798().method_10216(), mc.player.field_6012 % 2 == 0 ? 0.08 : -0.08, mc.player.method_18798().method_10215()
                        );
                     break;
                  case Glide:
                     mc.player
                        .method_18800(
                           mc.player.method_18798().method_10216(),
                           -0.01F - (mc.player.field_6012 % 2 == 0 ? 1.0E-4F : 0.006F),
                           mc.player.method_18798().method_10215()
                        );
               }
            }

            if (!mc.player.method_5715() && mc.options.jumpKey.isPressed()) {
               mc.player.method_18800(mc.player.method_18798().method_10216(), this.ySpeed.getValue().floatValue(), mc.player.method_18798().method_10215());
            }

            if (mc.options.sneakKey.isPressed()) {
               mc.player.method_18800(mc.player.method_18798().method_10216(), -this.ySpeed.getValue(), mc.player.method_18798().method_10215());
            }
         }
      }
   }

   private void doBoost(EventMove e) {
      if (mc.player.method_31548().method_5438(38).getItem() == Items.ELYTRA
         && mc.player.method_6128()
         && !mc.player.method_5799()
         && !mc.player.method_5771()
         && mc.player.method_6128()) {
         float moveForward = mc.player.input.movementForward;
         if (this.cruiseControl.getValue()) {
            if (mc.options.jumpKey.isPressed()) {
               this.height++;
            } else if (mc.options.sneakKey.isPressed()) {
               this.height--;
            }

            if (this.forceHeight.getValue()) {
               this.height = this.manualHeight.getValue().intValue();
            }

            if (this.twoBee.getValue()) {
               if (Managers.PLAYER.currentPlayerSpeed >= this.minUpSpeed.getValue()) {
                  mc.player
                     .method_36457(
                        (float)MathHelper.clamp(
                           MathHelper.wrapDegrees(Math.toDegrees(Math.atan2((this.height - mc.player.method_23318()) * -1.0, 10.0))), -50.0, 50.0
                        )
                     );
               } else {
                  mc.player.method_36457(0.25F);
               }
            } else {
               double heightPct = 1.0 - Math.sqrt(MathHelper.clamp(Managers.PLAYER.currentPlayerSpeed / 1.7, 0.0, 1.0));
               if (Managers.PLAYER.currentPlayerSpeed >= this.minUpSpeed.getValue()
                  && this.startTimer.passedMs((long)(2000.0F * this.redeployInterval.getValue()))) {
                  double pitch = -(44.4 * heightPct + 0.6);
                  double diff = (this.height + 1.0F - mc.player.method_23318()) * 2.0;
                  double pDist = -Math.toDegrees(Math.atan2(Math.abs(diff), Managers.PLAYER.currentPlayerSpeed * 30.0)) * Math.signum(diff);
                  mc.player.method_36457((float)(pitch + (pDist - pitch) * MathHelper.clamp(Math.abs(diff), 0.0, 1.0)));
               } else {
                  mc.player.method_36457(0.25F);
                  moveForward = 1.0F;
               }
            }
         }

         if (this.twoBee.getValue()) {
            if (mc.options.jumpKey.isPressed() || !this.onlySpace.getValue() || this.cruiseControl.getValue()) {
               double[] m = MovementUtility.forwardWithoutStrafe(this.factor.getValue() / 10.0F);
               e.setX(e.getX() + m[0]);
               e.setZ(e.getZ() + m[1]);
            }
         } else {
            Vec3d rotationVec = mc.player.method_5828(Render3DEngine.getTickDelta());
            double d6 = Math.hypot(rotationVec.x, rotationVec.z);
            double currentSpeed = Math.hypot(e.getX(), e.getZ());
            float f4 = (float)(Math.pow(Math.cos(Math.toRadians(mc.player.method_36455())), 2.0) * Math.min(1.0, rotationVec.length() / 0.4));
            e.setY(e.getY() + (-0.08 + f4 * 0.06));
            if (e.getY() < 0.0 && d6 > 0.0) {
               double ySpeed = e.getY() * -0.1 * f4;
               e.setY(e.getY() + ySpeed);
               e.setX(e.getX() + rotationVec.x * ySpeed / d6);
               e.setZ(e.getZ() + rotationVec.z * ySpeed / d6);
            }

            if (mc.player.method_36455() < 0.0F) {
               double ySpeed = currentSpeed * -Math.sin(Math.toRadians(mc.player.method_36455())) * 0.04;
               e.setY(e.getY() + ySpeed * 3.2);
               e.setX(e.getX() - rotationVec.x * ySpeed / d6);
               e.setZ(e.getZ() - rotationVec.z * ySpeed / d6);
            }

            if (d6 > 0.0) {
               e.setX(e.getX() + (rotationVec.x / d6 * currentSpeed - e.getX()) * 0.1);
               e.setZ(e.getZ() + (rotationVec.z / d6 * currentSpeed - e.getZ()) * 0.1);
            }

            if (mc.player.method_36455() > 0.0F && e.getY() < 0.0) {
               if (moveForward != 0.0F
                  && this.startTimer.passedMs((long)(2000.0F * this.redeployInterval.getValue()))
                  && this.redeployTimer.passedMs((long)(1000.0F * this.redeployTimeOut.getValue()))) {
                  if (this.stopMotion.getValue()) {
                     e.setX(0.0);
                     e.setZ(0.0);
                  }

                  this.startTimer.reset();
                  this.sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               } else if (!this.startTimer.passedMs((long)(2000.0F * this.redeployInterval.getValue()))) {
                  e.setX(e.getX() - moveForward * Math.sin(Math.toRadians(mc.player.method_36454())) * this.factor.getValue().floatValue() / 20.0);
                  e.setZ(e.getZ() + moveForward * Math.cos(Math.toRadians(mc.player.method_36454())) * this.factor.getValue().floatValue() / 20.0);
                  this.redeployTimer.reset();
               }
            }
         }

         double speed = Math.hypot(e.getX(), e.getZ());
         if (this.speedLimit.getValue() && speed > this.maxSpeed.getValue().floatValue()) {
            e.setX(e.getX() * this.maxSpeed.getValue().floatValue() / speed);
            e.setZ(e.getZ() * this.maxSpeed.getValue().floatValue() / speed);
         }

         mc.player.method_18800(e.getX(), e.getY(), e.getZ());
         e.cancel();
      }
   }

   private void doControl(EventMove e) {
      if (mc.player.method_31548().method_5438(38).getItem() == Items.ELYTRA && mc.player.method_6128()) {
         double[] dir = MovementUtility.forward(
            this.xzSpeed.getValue()
               * (
                  this.accelerate.getValue().isEnabled()
                     ? Math.min((this.acceleration = this.acceleration + this.accelerateFactor.getValue()) / 100.0F, 1.0F)
                     : 1.0F
               )
         );
         e.setX(dir[0]);
         e.setY(
            mc.options.jumpKey.isPressed()
               ? this.upSpeed.getValue().floatValue()
               : (mc.options.sneakKey.isPressed() ? -this.sneakDownSpeed.getValue() : -0.08 * this.downFactor.getValue().floatValue())
         );
         e.setZ(dir[1]);
         if (!MovementUtility.isMoving()) {
            this.acceleration = 0.0F;
         }

         mc.player.method_18800(e.getX(), e.getY(), e.getZ());
         e.cancel();
      }
   }

   public void matrixDisabler(int elytra) {
      elytra = elytra >= 0 && elytra < 9 ? elytra + 36 : elytra;
      if (elytra != -2) {
         mc.interactionManager.clickSlot(mc.player.field_7512.syncId, elytra, 1, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.field_7512.syncId, 6, 1, SlotActionType.PICKUP, mc.player);
      }

      mc.player
         .networkHandler
         .method_52787(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
      if (elytra != -2) {
         mc.interactionManager.clickSlot(mc.player.field_7512.syncId, 6, 1, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.field_7512.syncId, elytra, 1, SlotActionType.PICKUP, mc.player);
      }

      this.disablerTicks = this.disablerDelay.getValue();
   }

   private int getFireWorks(boolean hotbar) {
      return hotbar ? InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).slot() : InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
   }

   private void noFireworks() {
      this.disable("No fireworks in the hotbar!");
      this.flying = false;
   }

   private void noElytra() {
      this.disable("No elytras found in the inventory!");
      this.flying = false;
   }

   private void reset() {
      this.slotWithFireWorks = -1;
      this.prevItemInHand = Items.AIR;
      this.getStackInSlotCopy = null;
   }

   private void resetPrevItems() {
      this.prevElytraSlot = -1;
      this.prevArmorItem = Items.AIR;
      this.prevArmorItemCopy = null;
   }

   private void moveFireworksToHotbar(int n2) {
      clickSlot(n2);
      clickSlot(this.fireSlot.getValue() - 1 + 36);
      clickSlot(n2);
   }

   private void returnItem() {
      if (this.slotWithFireWorks != -1 && this.getStackInSlotCopy != null && this.prevItemInHand != Items.FIREWORK_ROCKET && this.prevItemInHand != Items.AIR) {
         int n2 = findInInventory(this.getStackInSlotCopy, this.prevItemInHand);
         n2 = n2 < 9 && n2 != -1 ? n2 + 36 : n2;
         clickSlot(n2);
         clickSlot(this.fireSlot.getValue() - 1 + 36);
         clickSlot(n2);
      }
   }

   public static int findInInventory(ItemStack stack, Item item) {
      if (stack == null) {
         return -1;
      }

      for (int i2 = 0; i2 < 45; i2++) {
         ItemStack is = mc.player.method_31548().method_5438(i2);
         if (ItemStack.areItemsEqual(is, stack) && is.getItem() == item) {
            return i2;
         }
      }

      return -1;
   }

   private int getFireworks() {
      if (mc.player.method_6079().getItem() == Items.FIREWORK_ROCKET) {
         return -2;
      }

      int firesInHotbar = this.getFireWorks(true);
      int firesInInventory = this.getFireWorks(false);
      if (firesInInventory == -1) {
         this.noFireworks();
         return -1;
      }

      if (firesInHotbar == -1) {
         if (!this.allowFireSwap.getValue()) {
            this.disable("No fireworks!");
            return this.fireSlot.getValue() - 1;
         } else {
            this.moveFireworksToHotbar(firesInInventory);
            return this.fireSlot.getValue() - 1;
         }
      } else {
         return firesInHotbar;
      }
   }

   private boolean canFly() {
      return this.shouldSwapToElytra() ? false : this.getFireworks() != -1;
   }

   private boolean shouldSwapToElytra() {
      ItemStack is = mc.player.method_6118(EquipmentSlot.CHEST);
      return is.getItem() != Items.ELYTRA || !ElytraItem.isUsable(is);
   }

   private void doFireWork(boolean started) {
      if (!started || !((float)(System.currentTimeMillis() - this.lastFireworkTime) < this.fireDelay.getValue() * 1000.0F)) {
         if (!this.grim.getValue().isEnabled()
            || !this.fireWorkExtender.getValue()
            || !started
            || !this.pingTimer.passedMs(200L)
            || this.flightZonePos == null
            || !(PlayerUtility.getSquaredDistance2D(this.flightZonePos) < 7000.0F)) {
            if (!started || mc.player.method_6128()) {
               if (started || Managers.PLAYER.ticksElytraFlying <= 1) {
                  int slot = this.getFireworks();
                  if (slot == -1) {
                     this.slotWithFireWorks = -1;
                  } else {
                     this.slotWithFireWorks = slot;
                     boolean inOffhand = mc.player.method_6079().getItem() == Items.FIREWORK_ROCKET;
                     int prevSlot = mc.player.method_31548().selectedSlot;
                     if (!inOffhand && prevSlot != slot) {
                        this.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                     }

                     this.sendSequencedPacket(
                        id -> new PlayerInteractItemC2SPacket(
                           inOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND, id, mc.player.method_36454(), mc.player.method_36455()
                        )
                     );
                     if (!inOffhand && prevSlot != mc.player.method_31548().selectedSlot) {
                        this.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
                     }

                     this.flying = true;
                     this.lastFireworkTime = System.currentTimeMillis();
                     this.pingTimer.reset();
                     this.flightZonePos = mc.player.method_19538();
                  }
               }
            }
         }
      }
   }

   private void equipElytra() {
      int elytraSlot = InventoryUtility.getElytra();
      if (elytraSlot == -1 && mc.player.field_7512.getCursorStack().getItem() != Items.ELYTRA) {
         this.noElytra();
      } else if (this.shouldSwapToElytra()) {
         if (this.prevElytraSlot == -1) {
            ItemStack is = mc.player.method_6118(EquipmentSlot.CHEST);
            this.prevElytraSlot = elytraSlot;
            this.prevArmorItem = is.getItem();
            this.prevArmorItemCopy = is.copy();
         }

         clickSlot(elytraSlot);
         clickSlot(6);
         if (this.prevElytraSlot != -1) {
            clickSlot(this.prevElytraSlot);
         }

         this.elytraEquiped = true;
      }
   }

   private void returnChestPlate() {
      if (this.prevElytraSlot != -1 && this.prevArmorItem != Items.AIR) {
         if (!this.elytraEquiped) {
            return;
         }

         ItemStack is = mc.player.method_31548().method_5438(this.prevElytraSlot);
         boolean bl2 = is != ItemStack.EMPTY && !ItemStack.areItemsEqual(is, this.prevArmorItemCopy);
         int n2 = findInInventory(this.prevArmorItemCopy, this.prevArmorItem);
         n2 = n2 < 9 && n2 != -1 ? n2 + 36 : n2;
         if (mc.player.field_7512.getCursorStack().getItem() != Items.AIR) {
            clickSlot(6);
            if (this.prevElytraSlot != -1) {
               clickSlot(this.prevElytraSlot);
            }

            return;
         }

         if (n2 == -1) {
            return;
         }

         clickSlot(n2);
         clickSlot(6);
         if (!bl2) {
            clickSlot(n2);
         } else {
            int n4 = findEmpty(false);
            if (n4 != -1) {
               clickSlot(n4);
            }
         }
      }

      this.resetPrevItems();
   }

   public static int findEmpty(boolean hotbar) {
      for (int i2 = hotbar ? 0 : 9; i2 < (hotbar ? 9 : 45); i2++) {
         if (mc.player.method_31548().method_5438(i2).isEmpty()) {
            return i2;
         }
      }

      return -1;
   }

   public void fireWorkOnPlayerUpdate() {
      boolean inAir = mc.world.method_22347(BlockPos.ofFloored(mc.player.method_19538()));
      boolean aboveLiquid = isAboveLiquid(0.1F) && inAir && mc.player.method_18798().method_10214() < 0.0;
      if ((!(mc.player.field_6017 > 0.0F) || !inAir) && !aboveLiquid) {
         if (mc.player.method_24828()) {
            this.started = false;
            return;
         }
      } else {
         this.equipElytra();
      }

      if (!MovementUtility.isMoving()) {
         this.acceleration = 0.0F;
      }

      if (this.canFly()) {
         if (!mc.player.method_6128() && !this.started && mc.player.method_18798().method_10214() < 0.0) {
            this.sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            this.started = true;
         }

         if (Managers.PLAYER.ticksElytraFlying < 4) {
            mc.options.jumpKey.setPressed(false);
         }

         this.doFireWork(true);
      }
   }

   public void fireworkOnSync() {
      if (this.grim.getValue().isEnabled() && this.rotate.getValue()) {
         if (mc.options.jumpKey.isPressed() && mc.player.method_6128() && this.flying) {
            mc.player.method_36457(-45.0F);
         }

         if (mc.options.sneakKey.isPressed() && mc.player.method_6128() && this.flying) {
            mc.player.method_36457(45.0F);
         }

         mc.player.method_36456(MovementUtility.getMoveDirection());
      }

      if (!MovementUtility.isMoving() && mc.options.jumpKey.isPressed() && mc.player.method_6128() && this.flying) {
         mc.player.method_36457(-90.0F);
      }

      if (Managers.PLAYER.ticksElytraFlying < 5 && !mc.player.method_24828()) {
         mc.player.method_36457(-45.0F);
      }
   }

   public void fireworkOnMove(EventMove e) {
      if (mc.player.method_6128() && this.flying) {
         if (mc.player.field_5976 || mc.player.field_5992) {
            this.acceleration = 0.0F;
            this.accelerationY = 0.0F;
         }

         if (Managers.PLAYER.ticksElytraFlying < 4) {
            e.setY(0.2F);
            e.cancel();
            return;
         }

         if (mc.options.jumpKey.isPressed()) {
            e.setY(this.ySpeed.getValue() * Math.min((this.accelerationY += 9.0F) / 100.0F, 1.0F));
         } else if (mc.options.sneakKey.isPressed()) {
            e.setY(-this.ySpeed.getValue() * Math.min((this.accelerationY += 9.0F) / 100.0F, 1.0F));
         } else if (this.bowBomb.getValue() && checkGround(2.0F)) {
            e.setY(mc.player.field_6012 % 2 == 0 ? 0.42F : -0.42F);
         } else {
            switch ((ElytraPlus.AntiKick)this.antiKick.getValue()) {
               case Off:
                  e.setY(0.0);
                  break;
               case Jitter:
                  e.setY(mc.player.field_6012 % 2 == 0 ? 0.08F : -0.08F);
                  break;
               case Glide:
                  e.setY(-0.08F);
            }
         }

         if (!MovementUtility.isMoving()) {
            this.acceleration = 0.0F;
         }

         if (mc.player.input.movementSideways > 0.0F) {
            mc.player.input.movementSideways = 1.0F;
         } else if (mc.player.input.movementSideways < 0.0F) {
            mc.player.input.movementSideways = -1.0F;
         }

         MovementUtility.modifyEventSpeed(e, this.xzSpeed.getValue() * Math.min((this.acceleration += 9.0F) / 100.0F, 1.0F));
         if (this.stayMad.getValue() && !checkGround(3.0F) && Managers.PLAYER.ticksElytraFlying > 10) {
            e.setY(0.42F);
         }

         e.cancel();
      }
   }

   public static boolean checkGround(float f2) {
      return mc.player.method_23318() < 0.0 ? false : !mc.world.method_20812(mc.player, mc.player.method_5829().offset(0.0, -f2, 0.0)).iterator().hasNext();
   }

   public static boolean isAboveLiquid(float offset) {
      return mc.player == null
         ? false
         : mc.world.method_8320(BlockPos.ofFloored(mc.player.method_23317(), mc.player.method_23318() - offset, mc.player.method_23321())).method_26204() instanceof FluidBlock;
   }

   public void fireworkOnEnable() {
      if (mc.player.method_6118(EquipmentSlot.CHEST).getItem() != Items.ELYTRA
         && mc.player.field_7512.getCursorStack().getItem() != Items.ELYTRA
         && InventoryUtility.getElytra() == -1) {
         this.noElytra();
      } else if (this.getFireWorks(false) == -1) {
         this.noFireworks();
      } else if (this.getFireWorks(true) == -1) {
         this.getStackInSlotCopy = mc.player.method_31548().method_5438(this.fireSlot.getValue() - 1).copy();
         this.prevItemInHand = mc.player.method_31548().method_5438(this.fireSlot.getValue() - 1).getItem();
      }
   }

   public void fireworkOnDisable() {
      this.started = false;
      if (!this.keepFlying.getValue()) {
         mc.player.method_18800(0.0, mc.player.method_18798().method_10214(), 0.0);
         new Thread(() -> {
            this.sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            Vcore.TICK_TIMER = 0.1F;
            this.returnItem();
            this.reset();

            try {
               Thread.sleep(200L);
            } catch (InterruptedException interruptedException) {
               Vcore.TICK_TIMER = 1.0F;
               interruptedException.printStackTrace();
            }

            this.returnChestPlate();
            this.resetPrevItems();
            Vcore.TICK_TIMER = 1.0F;
         }).start();
      }
   }

   private boolean isBoxCollidingGround() {
      return mc.world.method_20812(mc.player, mc.player.method_5829().expand(-0.25, 0.0, -0.25).offset(0.0, -0.3, 0.0)).iterator().hasNext();
   }

   public enum AntiKick {
      Off,
      Jitter,
      Glide;
   }

   public enum Mode {
      FireWork,
      SunriseOld,
      Boost,
      Control,
      Pitch40Infinite,
      SunriseNew,
      Packet;
   }

   public enum NCPStrict {
      Off,
      Old,
      New,
      Motion;
   }
}
