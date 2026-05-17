package vcore.features.modules.render;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import vcore.events.impl.EventMouse;
import vcore.events.impl.TotemPopEvent;
import vcore.features.hud.HudElement;
import vcore.features.hud.impl.ArmorHud;
import vcore.features.hud.impl.Coords;
import vcore.features.hud.impl.InventoryHud;
import vcore.features.hud.impl.KeyBinds;
import vcore.features.hud.impl.PingHud;
import vcore.features.hud.impl.PotionHud;
import vcore.features.hud.impl.Speedometer;
import vcore.features.hud.impl.StaffBoard;
import vcore.features.hud.impl.TPSCounter;
import vcore.features.hud.impl.TargetHud;
import vcore.features.hud.impl.TotemCounter;
import vcore.features.hud.impl.WaterMark;
import vcore.features.modules.Module;
import vcore.setting.Setting;
import vcore.setting.impl.ColorSetting;
import vcore.setting.impl.PositionSetting;
import vcore.utility.render.Render2DEngine;

public final class HudEditor extends Module {
   public static final boolean STICKY = true;
   public static final boolean GLOW_ENABLED = false;
   public static final float COLOR_SPEED = 10.0F;
   public static final Color TEXT_COLOR = new Color(-1, true);
   public static final Color TEXT_COLOR2 = new Color(-1, true);
   public static final float HUD_ROUND = 4.0F;
   public static final float BLEND = 20.0F;
   public static final float OUTLINE = 0.5F;
   public static final float GLOW_STRENGTH = 0.1F;
   public static final float BLUR_STRENGTH = 5.0F;
   private static final float MIN_ALPHA = 0.2F;
   private static final float MAX_ALPHA = 0.85F;
   private static final float ALPHA_STEP = 0.05F;
   public static final Setting<HudEditor.HudStyle> hudStyle = new Setting<>("HudStyle", HudEditor.HudStyle.Blurry);
   public static final Setting<ColorSetting> plateColor = new Setting<>("PlateColor", new ColorSetting(new Color(10, 10, 10, 255).getRGB()));
   public static final Setting<ColorSetting> blurColor = new Setting<>("BlurColor", new ColorSetting(new Color(-16777216, true).getRGB()).withoutAlpha());
   public static final Setting<Float> alpha = new Setting<>("Alpha", 0.65F, 0.2F, 0.85F).step(0.05F);
   private static final Setting<Integer> themeIndexSetting = new Setting<>("ThemeIndex", 1, 0, 8, v -> false);
   private final Setting<Boolean> armorHudEnabled = new Setting<>("ArmorHud", false);
   private final Setting<Boolean> coordsEnabled = new Setting<>("Coords", false);
   private final Setting<Boolean> inventoryHudEnabled = new Setting<>("InventoryHud", false);
   private final Setting<Boolean> keyBindsEnabled = new Setting<>("KeyBinds", false);
   private final Setting<Boolean> pingHudEnabled = new Setting<>("PingHud", false);
   private final Setting<Boolean> potionHudEnabled = new Setting<>("PotionHud", false);
   private final Setting<Boolean> speedometerEnabled = new Setting<>("Speedometer", false);
   private final Setting<Boolean> staffBoardEnabled = new Setting<>("StaffBoard", false);
   private final Setting<Boolean> targetHudEnabled = new Setting<>("TargetHud", false);
   private final Setting<Boolean> totemCounterEnabled = new Setting<>("TotemCounter", false);
   private final Setting<Boolean> tpsCounterEnabled = new Setting<>("TPSCounter", false);
   private final Setting<Boolean> waterMarkEnabled = new Setting<>("WaterMark", false);
   private final Setting<PositionSetting> armorHudPos = new Setting<>("ArmorHudPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> coordsPos = new Setting<>("CoordsPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> inventoryHudPos = new Setting<>("InventoryHudPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> keyBindsPos = new Setting<>("KeyBindsPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> pingHudPos = new Setting<>("PingHudPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> potionHudPos = new Setting<>("PotionHudPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> speedometerPos = new Setting<>("SpeedometerPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> staffBoardPos = new Setting<>("StaffBoardPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> targetHudPos = new Setting<>("TargetHudPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> totemCounterPos = new Setting<>("TotemCounterPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> tpsCounterPos = new Setting<>("TPSCounterPos", new PositionSetting(0.5F, 0.5F));
   private final Setting<PositionSetting> waterMarkPos = new Setting<>("WaterMarkPos", new PositionSetting(0.5F, 0.5F));
   private final Supplier<List<HudElement>> activeHudSupplier = this::getActiveElements;
   private final ArmorHud armorHud = new ArmorHud(this.armorHudPos.getValue(), this.activeHudSupplier);
   private final Coords coords = new Coords(this.coordsPos.getValue(), this.activeHudSupplier);
   private final InventoryHud inventoryHud = new InventoryHud(this.inventoryHudPos.getValue(), this.activeHudSupplier);
   private final KeyBinds keyBinds = new KeyBinds(this.keyBindsPos.getValue(), this.activeHudSupplier);
   private final PingHud pingHud = new PingHud(this.pingHudPos.getValue(), this.activeHudSupplier);
   private final PotionHud potionHud = new PotionHud(this.potionHudPos.getValue(), this.activeHudSupplier);
   private final Speedometer speedometer = new Speedometer(this.speedometerPos.getValue(), this.activeHudSupplier);
   private final StaffBoard staffBoard = new StaffBoard(this.staffBoardPos.getValue(), this.activeHudSupplier);
   private final TargetHud targetHud = new TargetHud(this.targetHudPos.getValue(), this.activeHudSupplier);
   private final TotemCounter totemCounter = new TotemCounter(this.totemCounterPos.getValue(), this.activeHudSupplier);
   private final TPSCounter tpsCounter = new TPSCounter(this.tpsCounterPos.getValue(), this.activeHudSupplier);
   private final WaterMark waterMark = new WaterMark(this.waterMarkPos.getValue(), this.activeHudSupplier);
   private final List<HudEditor.HudEntry> hudEntries = List.of(
      new HudEditor.HudEntry("ArmorHud", this.armorHudEnabled, this.armorHud),
      new HudEditor.HudEntry("Coords", this.coordsEnabled, this.coords),
      new HudEditor.HudEntry("InventoryHud", this.inventoryHudEnabled, this.inventoryHud),
      new HudEditor.HudEntry("KeyBinds", this.keyBindsEnabled, this.keyBinds),
      new HudEditor.HudEntry("PingHud", this.pingHudEnabled, this.pingHud),
      new HudEditor.HudEntry("PotionHud", this.potionHudEnabled, this.potionHud),
      new HudEditor.HudEntry("Speedometer", this.speedometerEnabled, this.speedometer),
      new HudEditor.HudEntry("StaffBoard", this.staffBoardEnabled, this.staffBoard),
      new HudEditor.HudEntry("TargetHud", this.targetHudEnabled, this.targetHud),
      new HudEditor.HudEntry("TotemCounter", this.totemCounterEnabled, this.totemCounter),
      new HudEditor.HudEntry("TPSCounter", this.tpsCounterEnabled, this.tpsCounter),
      new HudEditor.HudEntry("WaterMark", this.waterMarkEnabled, this.waterMark)
   );
   public static final List<HudEditor.Theme> advancecolor = Arrays.asList(
      new HudEditor.Theme("theme1", new Color(16051455), new Color(7756453)),
      new HudEditor.Theme("theme2", new Color(471946), new Color(30975)),
      new HudEditor.Theme("theme3", new Color(14926847), new Color(6815724)),
      new HudEditor.Theme("theme4", new Color(16761600), new Color(16734208)),
      new HudEditor.Theme("theme5", new Color(2899536), new Color(16610412)),
      new HudEditor.Theme("theme6", new Color(3422264), new Color(24427)),
      new HudEditor.Theme("theme7", new Color(16595514), new Color(3815994)),
      new HudEditor.Theme("theme8", new Color(16051455), new Color(7782111)),
      new HudEditor.Theme("theme9", new Color(3619652), new Color(4359924))
   );
   private static int themeIndex = 0;

   public HudEditor() {
      super("HUD", "HUD element editor.", Module.Category.RENDER);
   }

   public static Color getColor(int count) {
      HudEditor.Theme theme = getCurrentTheme();
      return Render2DEngine.TwoColoreffect(theme.color1, theme.color2, 10.0, count);
   }

   public static HudEditor.Theme getCurrentTheme() {
      int idx = themeIndexSetting.getValue();
      idx = Math.max(0, Math.min(idx, advancecolor.size() - 1));
      themeIndex = idx;
      return advancecolor.get(idx);
   }

   public static void setThemeIndex(int idx) {
      int clamped = Math.max(0, Math.min(idx, advancecolor.size() - 1));
      themeIndex = clamped;
      if (!Objects.equals(themeIndexSetting.getValue(), clamped)) {
         themeIndexSetting.setValueSilent(clamped);
      }
   }

   public static Color getTextColor() {
      return TEXT_COLOR;
   }

   public static Color getTextColor2() {
      return TEXT_COLOR2;
   }

   public static float getAlpha() {
      return alpha.getValue();
   }

   public static float getBlurOpacity() {
      return getAlpha();
   }

   public static Color getBlurColor() {
      Color color = blurColor.getValue().getColorObject();
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
   }

   @Override
   public void onUpdate() {
      setThemeIndex(themeIndexSetting.getValue());
      this.hudEntries.stream().filter(this::isEnabled).forEach(entry -> entry.element.tick());
   }

   @Override
   public void onRender2D(DrawContext context) {
      HudElement.anyHovered = false;
      this.hudEntries.stream().filter(this::isEnabled).map(entry -> entry.element).forEach(element -> element.render(context));
   }

   @EventHandler
   public void onMouse(EventMouse event) {
      this.hudEntries.stream().filter(this::isEnabled).map(entry -> entry.element).forEach(element -> element.handleMouse(event));
   }

   @EventHandler
   public void onTotemPop(TotemPopEvent event) {
      if (this.totemCounterEnabled.getValue()) {
         this.totemCounter.onTotemPop(event);
      }
   }

   public boolean isHudEnabled(String name) {
      return this.isEnabled() && this.hudEntries.stream().anyMatch(entry -> entry.name.equalsIgnoreCase(name) && entry.toggle.getValue());
   }

   public List<HudElement> getActiveElements() {
      return this.isDisabled() ? List.of() : this.hudEntries.stream().filter(this::isEnabled).map(entry -> entry.element).collect(Collectors.toList());
   }

   private boolean isEnabled(HudEditor.HudEntry entry) {
      return entry.toggle.getValue();
   }

   public enum ColorA {
      theme1,
      theme2,
      theme3,
      theme4,
      theme5,
      theme6,
      theme7,
      theme8,
      theme9;
   }

   private record HudEntry(String name, Setting<Boolean> toggle, HudElement element) {
   }

   public enum HudStyle {
      Blurry,
      Glowing;
   }

   public static class Theme {
      public final String name;
      public final Color color1;
      public final Color color2;

      public Theme(String name, Color color1, Color color2) {
         this.name = name;
         this.color1 = color1;
         this.color2 = color2;
      }

      public Theme(String name, Color color) {
         this.name = name;
         this.color1 = color;
         this.color2 = color;
      }
   }
}
