package vcore.features.modules.misc;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import vcore.core.Managers;
import vcore.core.manager.client.ModuleManager;
import vcore.events.impl.EventSync;
import vcore.events.impl.PlayerUpdateEvent;
import vcore.features.modules.Module;
import vcore.features.modules.movement.AutoSprint;
import vcore.injection.accesors.IMinecraftClient;
import vcore.setting.Setting;
import vcore.utility.Timer;
import vcore.utility.player.InteractionUtility;
import vcore.utility.player.InventoryUtility;

public class AutoDungeon extends Module {
   private static final double MAX_HEIGHT_DIFFERENCE = 3.0;
   private static final double STOP_MOVE_THRESHOLD = 0.25;
   private static final double WALK_SPRINT_THRESHOLD = 1.5;
   private static final float ATTACK_RANGE = 3.0F;
   private static final long ATTACK_DELAY_MS = 510L;
   private static final long RANDOM_ATTACK_DELAY_MAX_MS = 650L;
   private static final float RANDOM_ATTACK_RANGE_MIN = 2.0F;
   private static final int AUTO_EAT_HUNGER_THRESHOLD = 6;
   private static final float YAW_ROTATION_STEP = 150.0F;
   private static final float PITCH_ROTATION_STEP = 8.0F;
   private static final long STUCK_CHECK_INTERVAL_MS = 250L;
   private static final double STUCK_MOVEMENT_THRESHOLD = 0.05;
   private static final float STUCK_STRAFE_YAW = 45.0F;
   private final Setting<Boolean> autoSprint = new Setting<>("AutoSprint", true);
   private final Setting<Boolean> autoJump = new Setting<>("AutoJump", true);
   private final Setting<Boolean> autoEat = new Setting<>("AutoEat", true);
   private final Setting<Boolean> autoWeapon = new Setting<>("AutoWeapon", true);
   private final Setting<Boolean> ramdonAttack = new Setting<>("RamdonAttack", false);
   private final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", true);
   private final Timer attackTimer = new Timer();
   private MobEntity target;
   private float rotationYaw;
   private float rotationPitch;
   private float movementYawOffset;
   private boolean pendingAttack;
   private boolean autoEating;
   private boolean controllingMovement;
   private boolean stuck;
   private boolean strafingRight;
   private Vec3d lastPosition;
   private long lastMoveTime;
   private long nextAttackDelayMs = 510L;
   private float nextAttackRange = 3.0F;
   private boolean randomAttackPattern;

   public AutoDungeon() {
      super("AutoDungeon", "Automatically hunts nearby mobs in dungeons.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.resetState(true);
      this.attackTimer.setMs(this.getAttackDelayMs());
   }

   @Override
   public void onDisable() {
      this.resetState(true);
   }

   @Override
   public void onLogout() {
      this.resetState(true);
   }

   @Override
   public String getDisplayInfo() {
      return this.target != null ? this.target.method_5477().getString() : null;
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (!fullNullCheck() && mc.player != null && !mc.player.method_7325()) {
         this.syncAttackPattern();
         this.handleAutoEat();
         this.target = this.findNearestTarget();
         this.pendingAttack = false;
         if (this.target == null) {
            this.resetState(false);
         } else {
            this.handleAutoWeapon();
            if (!this.canOperateWithCurrentWeapon()) {
               this.resetStuckState();
               ModuleManager.moveFix.fixRotation = Float.NaN;
               if (this.controllingMovement) {
                  this.releaseMovement();
               }
            } else {
               this.updateStuckState(this.shouldMoveFast(this.target));
               this.updateRotation(this.target);
               ModuleManager.moveFix.fixRotation = this.rotationYaw;
               this.handleMovement(this.target);
               this.pendingAttack = this.shouldAttack(this.target);
            }
         }
      } else {
         this.resetState(false);
      }
   }

   @EventHandler
   public void onSync(EventSync event) {
      if (mc.player != null && this.target != null && this.canOperateWithCurrentWeapon()) {
         mc.player.method_36456(this.rotationYaw);
         mc.player.method_36457(this.rotationPitch);
         if (this.pendingAttack) {
            MobEntity queuedTarget = this.target;
            Runnable previous = event.getPostAction();
            event.addPostAction(() -> {
               if (previous != null) {
                  previous.run();
               }

               this.attackTarget(queuedTarget);
            });
            this.pendingAttack = false;
         }
      }
   }

   private MobEntity findNearestTarget() {
      MobEntity bestTarget = null;
      double bestDistance = Double.MAX_VALUE;

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof MobEntity mob && this.isValidTarget(mob)) {
            double distance = mc.player.method_33571().squaredDistanceTo(this.getTargetAimPos(mob));
            if (!(distance >= bestDistance)) {
               bestDistance = distance;
               bestTarget = mob;
            }
         }
      }

      return bestTarget;
   }

   private boolean isValidTarget(MobEntity mob) {
      return mob != null && mob.method_5805() && !mob.method_29504()
         ? Math.abs(mob.method_23318() - mc.player.method_23318()) <= 3.0 && this.hasClearLineOfSight(mob)
         : false;
   }

   private boolean hasClearLineOfSight(MobEntity mob) {
      Vec3d start = mc.player.method_33571();
      Vec3d end = this.getTargetAimPos(mob);
      HitResult result = mc.world.method_17742(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      return result.getType() == Type.MISS;
   }

   private Vec3d getTargetAimPos(MobEntity mob) {
      return mob.method_5829().getCenter();
   }

   private void updateRotation(MobEntity mob) {
      float[] angle = InteractionUtility.calculateAngle(this.getTargetAimPos(mob));
      float targetYaw = MathHelper.wrapDegrees(angle[0] + this.movementYawOffset);
      float nextYaw = this.stuck ? targetYaw : this.smoothRotation(this.rotationYaw, targetYaw, 150.0F);
      float nextPitch = MathHelper.clamp(this.stuck ? angle[1] : this.smoothRotation(this.rotationPitch, angle[1], 8.0F), -90.0F, 90.0F);
      this.rotationYaw = this.applyGcd(nextYaw, this.rotationYaw);
      this.rotationPitch = this.applyGcd(nextPitch, this.rotationPitch);
   }

   private float smoothRotation(float current, float targetAngle, float maxStep) {
      float delta = MathHelper.wrapDegrees(targetAngle - current);
      float speed = Math.min(maxStep, Math.abs(delta) * 2.0F);
      return MathHelper.wrapDegrees(current + MathHelper.clamp(delta, -speed, speed));
   }

   private float applyGcd(float nextAngle, float previousAngle) {
      double gcdFix = Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
      return (float)(nextAngle - (nextAngle - previousAngle) % gcdFix);
   }

   private void handleMovement(MobEntity mob) {
      double horizontalDistance = this.getHorizontalDistance(mob);
      if (horizontalDistance <= 0.25) {
         if (this.controllingMovement) {
            this.releaseMovement();
         }
      } else {
         boolean shouldMoveFast = horizontalDistance > 1.5;
         boolean shouldSprint = this.autoSprint.getValue() && shouldMoveFast;
         this.controllingMovement = true;
         mc.options.forwardKey.setPressed(true);
         mc.options.backKey.setPressed(false);
         this.applyStrafeKeys();
         boolean shouldJump = this.stuck || this.autoJump.getValue() && shouldMoveFast;
         AutoSprint.setForcedDisabled(!shouldSprint);
         mc.player.method_5728(shouldSprint);
         mc.options.sprintKey.setPressed(shouldSprint);
         mc.options.jumpKey.setPressed(shouldJump);
      }
   }

   private boolean shouldAttack(MobEntity mob) {
      float attackRange = this.getAttackRange();
      if (mob == null || !this.attackTimer.passedMs(this.getAttackDelayMs())) {
         return false;
      } else if (this.isConsumingItem()) {
         return false;
      } else if (this.onlyWeapon.getValue() && !this.hasMainHandWeapon()) {
         return false;
      } else {
         return mc.player.method_33571().squaredDistanceTo(this.getTargetAimPos(mob)) > attackRange * attackRange
            ? false
            : this.hasClearLineOfSight(mob) && Managers.PLAYER.checkRtx(this.rotationYaw, this.rotationPitch, attackRange, 0.0F, mob);
      }
   }

   private void attackTarget(MobEntity mob) {
      if (mc.player != null && mc.interactionManager != null && this.shouldAttack(mob)) {
         long attackTimingMs = this.attackTimer.getPassedTimeMs();
         double attackDistance = mc.player.method_33571().distanceTo(this.getTargetAimPos(mob));
         mc.interactionManager.attackEntity(mc.player, mob);
         mc.player.method_6104(Hand.MAIN_HAND);
         this.debug("Attack timing " + attackTimingMs + "ms | Khoảng cách " + String.format(Locale.US, "%.2f", attackDistance));
         this.attackTimer.reset();
         this.updateAttackPattern();
      }
   }

   private void resetState(boolean releaseMovement) {
      this.target = null;
      this.pendingAttack = false;
      this.resetAttackPattern();
      this.stopAutoEat();
      if (mc.player != null) {
         this.rotationYaw = mc.player.method_36454();
         this.rotationPitch = mc.player.method_36455();
      }

      this.resetStuckState();
      ModuleManager.moveFix.fixRotation = Float.NaN;
      if (releaseMovement || this.controllingMovement) {
         this.releaseMovement();
      }
   }

   private void releaseMovement() {
      this.controllingMovement = false;
      AutoSprint.setForcedDisabled(false);
      if (mc.player != null) {
         mc.player.method_5728(false);
      }

      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
      }
   }

   private void updateStuckState(boolean shouldCheck) {
      if (shouldCheck && mc.player != null) {
         Vec3d currentPos = mc.player.method_19538();
         long currentTime = System.currentTimeMillis();
         if (this.lastPosition == null) {
            this.lastPosition = currentPos;
            this.lastMoveTime = currentTime;
         } else if (currentTime - this.lastMoveTime >= 250L) {
            double deltaX = Math.abs(currentPos.x - this.lastPosition.x);
            double deltaZ = Math.abs(currentPos.z - this.lastPosition.z);
            boolean barelyMoved = deltaX < 0.05 && deltaZ < 0.05;
            if (!barelyMoved && !mc.player.field_5976) {
               this.stuck = false;
               this.movementYawOffset = 0.0F;
            } else {
               this.stuck = true;
               this.strafingRight = !this.strafingRight;
               this.movementYawOffset = this.strafingRight ? -45.0F : 45.0F;
            }

            this.lastPosition = currentPos;
            this.lastMoveTime = currentTime;
         }
      } else {
         this.resetStuckState();
      }
   }

   private void applyStrafeKeys() {
      if (mc.options != null) {
         if (!this.stuck) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
         } else {
            mc.options.rightKey.setPressed(this.strafingRight);
            mc.options.leftKey.setPressed(!this.strafingRight);
         }
      }
   }

   private void resetStuckState() {
      this.stuck = false;
      this.movementYawOffset = 0.0F;
      this.lastPosition = null;
      this.lastMoveTime = 0L;
   }

   private boolean shouldMoveFast(MobEntity mob) {
      return this.getHorizontalDistance(mob) > 1.5;
   }

   private double getHorizontalDistance(MobEntity mob) {
      return Math.hypot(mob.method_23317() - mc.player.method_23317(), mob.method_23321() - mc.player.method_23321());
   }

   private void handleAutoEat() {
      if (!this.shouldAutoEat()) {
         this.stopAutoEat();
      } else {
         this.autoEating = true;
         if (mc.currentScreen != null && !mc.player.method_6115()) {
            ((IMinecraftClient)mc).idoItemUse();
         } else {
            mc.options.useKey.setPressed(true);
         }
      }
   }

   private boolean shouldAutoEat() {
      return this.autoEat.getValue() && mc.player != null && mc.player.method_7344().getFoodLevel() <= 6 && this.isOffhandFood(mc.player.method_6079());
   }

   private void stopAutoEat() {
      if (this.autoEating && mc.options != null) {
         this.autoEating = false;
         mc.options.useKey.setPressed(false);
      } else {
         this.autoEating = false;
      }
   }

   private boolean isConsumingItem() {
      if (mc.player != null && mc.player.method_6115()) {
         UseAction useAction = mc.player.method_6030().getUseAction();
         return useAction == UseAction.EAT || useAction == UseAction.DRINK;
      } else {
         return false;
      }
   }

   private boolean hasMainHandWeapon() {
      return this.isWeaponItem(mc.player.method_6047().getItem());
   }

   private boolean canOperateWithCurrentWeapon() {
      return !this.onlyWeapon.getValue() || this.hasMainHandWeapon();
   }

   private boolean isOffhandFood(ItemStack stack) {
      return !stack.isEmpty() && stack.method_57353().contains(DataComponentTypes.FOOD);
   }

   private long getAttackDelayMs() {
      return this.ramdonAttack.getValue() ? this.nextAttackDelayMs : 510L;
   }

   private float getAttackRange() {
      return this.ramdonAttack.getValue() ? this.nextAttackRange : 3.0F;
   }

   private void syncAttackPattern() {
      boolean shouldRandomize = this.ramdonAttack.getValue();
      if (shouldRandomize != this.randomAttackPattern) {
         this.randomAttackPattern = shouldRandomize;
         this.updateAttackPattern();
      }
   }

   private void resetAttackPattern() {
      this.randomAttackPattern = this.ramdonAttack.getValue();
      this.updateAttackPattern();
   }

   private void updateAttackPattern() {
      if (!this.ramdonAttack.getValue()) {
         this.nextAttackDelayMs = 510L;
         this.nextAttackRange = 3.0F;
      } else {
         this.nextAttackDelayMs = ThreadLocalRandom.current().nextLong(510L, 651L);
         this.nextAttackRange = (float)ThreadLocalRandom.current().nextDouble(2.0, Math.nextUp(3.0));
      }
   }

   private void handleAutoWeapon() {
      if (this.autoWeapon.getValue() && mc.player != null && mc.interactionManager != null && !this.isConsumingItem()) {
         int bestWeaponSlot = this.findBestWeaponSlot();
         if (bestWeaponSlot != -1) {
            int selectedSlot = mc.player.method_31548().selectedSlot;
            if (bestWeaponSlot != selectedSlot) {
               if (bestWeaponSlot < 9) {
                  InventoryUtility.switchTo(bestWeaponSlot);
               } else if (mc.player.field_7512 == mc.player.field_7498) {
                  mc.interactionManager.clickSlot(mc.player.field_7512.syncId, this.toScreenSlot(bestWeaponSlot), selectedSlot, SlotActionType.SWAP, mc.player);
               }
            }
         }
      }
   }

   private int findBestWeaponSlot() {
      int bestSwordSlot = -1;
      double bestSwordDamage = Double.NEGATIVE_INFINITY;
      int bestWeaponSlot = -1;
      double bestWeaponDamage = Double.NEGATIVE_INFINITY;

      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.method_31548().method_5438(slot);
         double damage = this.getWeaponDamage(stack);
         if (!(damage < 0.0)) {
            if (stack.getItem() instanceof SwordItem) {
               if (damage > bestSwordDamage) {
                  bestSwordDamage = damage;
                  bestSwordSlot = slot;
               }
            } else if (damage > bestWeaponDamage) {
               bestWeaponDamage = damage;
               bestWeaponSlot = slot;
            }
         }
      }

      return bestSwordSlot != -1 ? bestSwordSlot : bestWeaponSlot;
   }

   private double getWeaponDamage(ItemStack stack) {
      if (stack.isEmpty()) {
         return -1.0;
      }

      double baseDamage = this.getWeaponBaseDamage(stack.getItem());
      return baseDamage < 0.0 ? -1.0 : baseDamage + this.getSharpnessBonus(stack);
   }

   private double getWeaponBaseDamage(Item item) {
      if (item == Items.NETHERITE_SWORD) {
         return 8.0;
      } else if (item == Items.DIAMOND_SWORD) {
         return 7.0;
      } else if (item == Items.IRON_SWORD) {
         return 6.0;
      } else if (item == Items.STONE_SWORD) {
         return 5.0;
      } else if (item == Items.GOLDEN_SWORD || item == Items.WOODEN_SWORD) {
         return 4.0;
      } else if (item == Items.NETHERITE_AXE) {
         return 10.0;
      } else if (item == Items.DIAMOND_AXE || item == Items.IRON_AXE || item == Items.STONE_AXE) {
         return 9.0;
      } else if (item == Items.GOLDEN_AXE || item == Items.WOODEN_AXE) {
         return 7.0;
      } else if (item == Items.TRIDENT) {
         return 8.0;
      } else {
         return item == Items.MACE ? 5.0 : -1.0;
      }
   }

   private double getSharpnessBonus(ItemStack stack) {
      int sharpnessLevel = EnchantmentHelper.getLevel(
         (RegistryEntry)mc.world.method_30349().get(Enchantments.SHARPNESS.getRegistryRef()).getEntry(Enchantments.SHARPNESS).get(), stack
      );
      return sharpnessLevel <= 0 ? 0.0 : 0.5 + sharpnessLevel * 0.5;
   }

   private boolean isWeaponItem(Item item) {
      return this.getWeaponBaseDamage(item) >= 0.0;
   }

   private int toScreenSlot(int slot) {
      return slot < 9 ? slot + 36 : slot;
   }
}
