package vcore.features.modules.player;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import vcore.core.manager.client.ModuleManager;
import vcore.features.modules.Module;
import vcore.gui.clickui.ClickGUI;
import vcore.setting.Setting;
import vcore.utility.player.InventoryUtility;
import vcore.utility.player.MovementUtility;

public class AutoArmor extends Module {
   private final Setting<AutoArmor.EnchantPriority> head = new Setting<>("Head", AutoArmor.EnchantPriority.Protection);
   private final Setting<AutoArmor.EnchantPriority> body = new Setting<>("Body", AutoArmor.EnchantPriority.Protection);
   private final Setting<AutoArmor.EnchantPriority> tights = new Setting<>("Tights", AutoArmor.EnchantPriority.Protection);
   private final Setting<AutoArmor.EnchantPriority> feet = new Setting<>("Feet", AutoArmor.EnchantPriority.Protection);
   private final Setting<AutoArmor.ElytraPriority> elytraPriority = new Setting<>("ElytraPriority", AutoArmor.ElytraPriority.Ignore);
   private final Setting<Integer> delay = new Setting<>("Delay", 5, 0, 10);
   private final Setting<Boolean> oldVersion = new Setting<>("OldVersion", false);
   private final Setting<Boolean> pauseInventory = new Setting<>("PauseInventory", false);
   private final Setting<Boolean> noMove = new Setting<>("NoMove", false);
   private final Setting<Boolean> ignoreCurse = new Setting<>("IgnoreCurse", true);
   private final Setting<Boolean> strict = new Setting<>("Strict", false);
   private int tickDelay = 0;
   List<AutoArmor.ArmorData> armorList = Arrays.asList(
      new AutoArmor.ArmorData(EquipmentSlot.FEET, 36, -1, -1, -1),
      new AutoArmor.ArmorData(EquipmentSlot.LEGS, 37, -1, -1, -1),
      new AutoArmor.ArmorData(EquipmentSlot.CHEST, 38, -1, -1, -1),
      new AutoArmor.ArmorData(EquipmentSlot.HEAD, 39, -1, -1, -1)
   );

   public AutoArmor() {
      super("AutoArmor", "Auto equips best armor.", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (mc.currentScreen == null || !this.pauseInventory.getValue() || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGUI) {
         if (this.tickDelay-- <= 0) {
            this.armorList.forEach(AutoArmor.ArmorData::reset);

            for (int i = 0; i < 36; i++) {
               ItemStack stack = mc.player.method_31548().method_5438(i);
               int prot = this.getProtection(stack);
               if (prot > 0) {
                  for (AutoArmor.ArmorData e : this.armorList) {
                     if (e.getEquipmentSlot() == (stack.getItem() instanceof ArmorItem ai ? ai.method_7685() : EquipmentSlot.CHEST)
                        && prot > e.getPrevProt()
                        && prot > e.getNewProtection()) {
                        e.setNewSlot(i);
                        e.setNewProtection(prot);
                     }
                  }
               }
            }

            for (AutoArmor.ArmorData armorPiece : this.armorList) {
               int slot = armorPiece.getNewSlot();
               if (slot != -1) {
                  if ((armorPiece.getPrevProt() == -1 || !this.oldVersion.getValue()) && slot < 9) {
                     InventoryUtility.saveAndSwitchTo(slot);
                     this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.method_36454(), mc.player.method_36455()));
                     InventoryUtility.returnSlot();
                  } else {
                     if (MovementUtility.isMoving() && this.noMove.getValue()) {
                        return;
                     }

                     int newArmorSlot = slot < 9 ? 36 + slot : slot;
                     if (this.strict.getValue()) {
                        this.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                     }

                     clickSlot(newArmorSlot);
                     clickSlot(armorPiece.getArmorSlot() - 34 + (39 - armorPiece.getArmorSlot()) * 2);
                     if (armorPiece.getPrevProt() != -1) {
                        clickSlot(newArmorSlot);
                     }

                     this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.field_7512.syncId));
                  }

                  this.tickDelay = this.delay.getValue();
                  return;
               }
            }
         }
      }
   }

   private int getProtection(ItemStack is) {
      if (is.getItem() instanceof ArmorItem || is.getItem() instanceof ElytraItem) {
         int prot = 0;
         EquipmentSlot slot = is.getItem() instanceof ArmorItem ai ? ai.method_7685() : EquipmentSlot.BODY;
         if (is.getItem() instanceof ElytraItem) {
            if (!ElytraItem.isUsable(is)) {
               return 0;
            }

            boolean ePlus = this.elytraPriority.is(AutoArmor.ElytraPriority.ElytraPlus) && ModuleManager.elytraPlus.isEnabled();
            boolean ignore = this.elytraPriority.is(AutoArmor.ElytraPriority.Ignore)
               && mc.player.method_31548().method_5438(38).getItem() instanceof ElytraItem;
            if (ePlus || ignore || this.elytraPriority.is(AutoArmor.ElytraPriority.Always)) {
               prot = 999;
            }
         }

         int blastMultiplier = 1;
         int protectionMultiplier = 1;
         switch (slot) {
            case HEAD:
               if (this.head.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case BODY:
               if (this.body.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case LEGS:
               if (this.tights.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case FEET:
               if (this.feet.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
         }

         if (is.hasEnchantments()) {
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(is);
            if (enchants.getEnchantments()
               .contains(mc.world.method_30349().get(Enchantments.PROTECTION.getRegistryRef()).getEntry(Enchantments.PROTECTION).get())) {
               prot += enchants.getLevel(
                     (RegistryEntry)mc.world.method_30349().get(Enchantments.PROTECTION.getRegistryRef()).getEntry(Enchantments.PROTECTION).get()
                  )
                  * protectionMultiplier;
            }

            if (enchants.getEnchantments()
               .contains(mc.world.method_30349().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BLAST_PROTECTION).get())) {
               prot += enchants.getLevel(
                     (RegistryEntry)mc.world.method_30349().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BLAST_PROTECTION).get()
                  )
                  * blastMultiplier;
            }

            if (enchants.getEnchantments()
                  .contains(mc.world.method_30349().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BINDING_CURSE).get())
               && this.ignoreCurse.getValue()) {
               prot = -999;
            }
         }

         return (is.getItem() instanceof ArmorItem armorItem ? (armorItem.getProtection() + (int)Math.ceil(armorItem.getToughness())) * 10 : 0) + prot;
      } else {
         return !is.isEmpty() ? 0 : -1;
      }
   }

   public class ArmorData {
      private EquipmentSlot equipmentSlot;
      private int armorSlot;
      private int prevProtection;
      private int newSlot;
      private int newProtection;

      public ArmorData(EquipmentSlot equipmentSlot, int armorSlot, int prevProtection, int newSlot, int newProtection) {
         this.equipmentSlot = equipmentSlot;
         this.armorSlot = armorSlot;
         this.prevProtection = prevProtection;
         this.newSlot = newSlot;
         this.newProtection = newProtection;
      }

      public int getArmorSlot() {
         return this.armorSlot;
      }

      public int getPrevProt() {
         return this.prevProtection;
      }

      public void setPrevProt(int prevProtection) {
         this.prevProtection = prevProtection;
      }

      public int getNewSlot() {
         return this.newSlot;
      }

      public void setNewSlot(int newSlot) {
         this.newSlot = newSlot;
      }

      public int getNewProtection() {
         return this.newProtection;
      }

      public void setNewProtection(int newProtection) {
         this.newProtection = newProtection;
      }

      public EquipmentSlot getEquipmentSlot() {
         return this.equipmentSlot;
      }

      public void reset() {
         this.setPrevProt(AutoArmor.this.getProtection(Module.mc.player.method_31548().method_5438(this.getArmorSlot())));
         this.setNewSlot(-1);
         this.setNewProtection(-1);
      }
   }

   private enum ElytraPriority {
      None,
      Always,
      ElytraPlus,
      Ignore;
   }

   private enum EnchantPriority {
      Blast,
      Protection;
   }
}
