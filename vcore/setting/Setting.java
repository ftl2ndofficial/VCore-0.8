package vcore.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import vcore.Vcore;
import vcore.events.impl.EventSetting;
import vcore.features.modules.Module;
import vcore.setting.impl.Bind;
import vcore.setting.impl.BooleanSettingGroup;
import vcore.setting.impl.EnumConverter;
import vcore.setting.impl.ItemSelectSetting;
import vcore.setting.impl.PositionSetting;
import vcore.setting.impl.SettingGroup;

public class Setting<T> {
   private static final AtomicLong NEXT_CREATION_ORDER = new AtomicLong();
   private final String name;
   private final T defaultValue;
   private final long creationOrder = NEXT_CREATION_ORDER.getAndIncrement();
   private T value;
   private T plannedValue;
   private T min;
   private T max;
   public Setting<?> group = null;
   private boolean hasRestriction;
   private float step;
   private Predicate<T> visibility;
   private Module module;

   public Setting(String name, T defaultValue) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.plannedValue = defaultValue;
   }

   public Setting(String name, T defaultValue, T min, T max) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.min = min;
      this.max = max;
      this.plannedValue = defaultValue;
      this.hasRestriction = true;
   }

   public Setting(String name, T defaultValue, T min, T max, Predicate<T> visibility) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.min = min;
      this.max = max;
      this.plannedValue = defaultValue;
      this.visibility = visibility;
      this.hasRestriction = true;
   }

   public Setting(String name, T defaultValue, Predicate<T> visibility) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.visibility = visibility;
      this.plannedValue = defaultValue;
   }

   public static Enum get(Enum clazz) {
      int index = EnumConverter.currentEnum(clazz);

      for (int i = 0; i < ((Enum[])clazz.getClass().getEnumConstants()).length; i++) {
         Enum e = ((Enum[])clazz.getClass().getEnumConstants())[i];
         if (i == index + 1) {
            return e;
         }
      }

      return ((Enum[])clazz.getClass().getEnumConstants())[0];
   }

   public String getName() {
      return this.name;
   }

   public T getValue() {
      return this.value;
   }

   public void setValue(T value) {
      this.setValueSilent(value);
      Vcore.EVENT_BUS.post(new EventSetting(this));
   }

   public void setValueSilent(T value) {
      this.setPlannedValue(value);
      if (this.plannedValue instanceof Number numberValue) {
         this.plannedValue = this.normalizeNumberValue(numberValue);
      } else if (this.hasRestriction) {
         if (((Number)this.min).floatValue() > ((Number)value).floatValue()) {
            this.setPlannedValue(this.min);
         }

         if (((Number)this.max).floatValue() < ((Number)value).floatValue()) {
            this.setPlannedValue(this.max);
         }
      }

      this.value = this.plannedValue;
   }

   public float getPow2Value() {
      if (this.value instanceof Float) {
         return (Float)this.value * (Float)this.value;
      } else {
         return this.value instanceof Integer ? (Integer)this.value * (Integer)this.value : 0.0F;
      }
   }

   public void setPlannedValue(T value) {
      this.plannedValue = value;
   }

   public T getMin() {
      return this.min;
   }

   public void setMin(T min) {
      this.min = min;
   }

   public T getMax() {
      return this.max;
   }

   public void setMax(T max) {
      this.max = max;
   }

   public Module getModule() {
      return this.module;
   }

   public void setModule(Module module) {
      this.module = module;
   }

   public String currentEnumName() {
      return EnumConverter.getProperName((Enum)this.value);
   }

   public String[] getModes() {
      return EnumConverter.getNames((Enum)this.value);
   }

   public void setEnum(Enum mod) {
      this.plannedValue = (T)mod;
   }

   public void increaseEnum() {
      this.plannedValue = (T)EnumConverter.increaseEnum((Enum)this.value);
      this.value = this.plannedValue;
      Vcore.EVENT_BUS.post(new EventSetting(this));
   }

   public void setEnumByNumber(int id) {
      this.plannedValue = (T)EnumConverter.setEnumInt((Enum<?>)this.value, id);
      this.value = this.plannedValue;
      Vcore.EVENT_BUS.post(new EventSetting(this));
   }

   public boolean isNumberSetting() {
      return this.value instanceof Double
         || this.value instanceof Integer
         || this.value instanceof Short
         || this.value instanceof Long
         || this.value instanceof Float;
   }

   public boolean isInteger() {
      return this.value instanceof Integer;
   }

   public boolean isFloat() {
      return this.value instanceof Float;
   }

   public boolean isEnumSetting() {
      return this.value.getClass().isEnum();
   }

   public boolean isBindSetting() {
      return this.value instanceof Bind;
   }

   public boolean isStringSetting() {
      return this.value instanceof String;
   }

   public boolean isItemSelectSetting() {
      return this.value instanceof ItemSelectSetting;
   }

   public boolean isPositionSetting() {
      return this.value instanceof PositionSetting;
   }

   public T getDefaultValue() {
      return this.defaultValue;
   }

   public long getCreationOrder() {
      return this.creationOrder;
   }

   public String getDisplayValue() {
      return this.formatValueObject(this.value, true);
   }

   public String getMinDisplay() {
      return this.formatValueObject(this.min, true);
   }

   public String getMaxDisplay() {
      return this.formatValueObject(this.max, true);
   }

   public String getSerializedValue() {
      return this.formatValueObject(this.value, false);
   }

   public Setting<T> step(float step) {
      this.step = Math.max(0.0F, step);
      this.setValueSilent(this.value);
      return this;
   }

   public float getStep() {
      return this.step;
   }

   public boolean hasStep() {
      return this.step > 0.0F;
   }

   public boolean hasRestriction() {
      return this.hasRestriction;
   }

   public Setting<T> addToGroup(Setting<?> group) {
      this.group = group;
      return this;
   }

   public boolean isVisible() {
      if (!this.isGroupChainVisible()) {
         return false;
      } else {
         return this.visibility == null ? true : this.visibility.test(this.getValue());
      }
   }

   public int getGroupDepth() {
      int depth = 0;

      for (Setting<?> currentGroup = this.group; currentGroup != null; currentGroup = currentGroup.group) {
         depth++;
      }

      return depth;
   }

   private boolean isGroupChainVisible() {
      for (Setting<?> currentGroup = this.group; currentGroup != null; currentGroup = currentGroup.group) {
         if (!currentGroup.isVisible()) {
            return false;
         }

         if (currentGroup.getValue() instanceof BooleanSettingGroup bp && !bp.isExtended()) {
            return false;
         }

         if (currentGroup.getValue() instanceof SettingGroup p && !p.isExtended()) {
            return false;
         }
      }

      return true;
   }

   public boolean is(T v) {
      return this.value == v;
   }

   public boolean not(T v) {
      return this.value != v;
   }

   private T normalizeNumberValue(Number numberValue) {
      double value = numberValue.doubleValue();
      boolean restrictedNumber = this.hasRestriction && this.min instanceof Number && this.max instanceof Number;
      if (restrictedNumber) {
         double minValue = ((Number)this.min).doubleValue();
         double maxValue = ((Number)this.max).doubleValue();
         value = Math.max(minValue, Math.min(maxValue, value));
      }

      if (this.hasStep()) {
         BigDecimal stepDecimal = getStepDecimal(this.step);
         BigDecimal baseDecimal = restrictedNumber ? BigDecimal.valueOf(((Number)this.min).doubleValue()) : BigDecimal.ZERO;
         value = BigDecimal.valueOf(value)
            .subtract(baseDecimal)
            .divide(stepDecimal, 0, RoundingMode.HALF_UP)
            .multiply(stepDecimal)
            .add(baseDecimal)
            .doubleValue();
         if (restrictedNumber) {
            double minValue = ((Number)this.min).doubleValue();
            double maxValue = ((Number)this.max).doubleValue();
            value = Math.max(minValue, Math.min(maxValue, value));
         }

         value = BigDecimal.valueOf(value).setScale(getStepScale(this.step), RoundingMode.HALF_UP).doubleValue();
      }

      if (numberValue instanceof Integer) {
         return (T)(int)Math.round(value);
      } else if (numberValue instanceof Float) {
         return (T)(float)value;
      } else if (numberValue instanceof Long) {
         return (T)Math.round(value);
      } else if (numberValue instanceof Short) {
         return (T)(short)Math.round(value);
      } else {
         return (T)(numberValue instanceof Byte ? (byte)Math.round(value) : value);
      }
   }

   private static int getStepScale(float step) {
      return Math.max(0, getStepDecimal(step).stripTrailingZeros().scale());
   }

   private static BigDecimal getStepDecimal(float step) {
      return new BigDecimal(Float.toString(step));
   }

   private String formatValueObject(Object value, boolean displayValue) {
      if (value instanceof Number number) {
         if (!(number instanceof Float) && !(number instanceof Double)) {
            return String.valueOf(value);
         }

         if (!this.hasStep()) {
            return displayValue ? formatRoundedDisplay(number.doubleValue(), 2) : String.valueOf(value);
         }

         int scale = getStepScale(this.step);
         BigDecimal decimal = BigDecimal.valueOf(number.doubleValue()).setScale(scale, RoundingMode.HALF_UP);
         return displayValue ? decimal.stripTrailingZeros().toPlainString() : decimal.toPlainString();
      } else {
         return String.valueOf(value);
      }
   }

   private static String formatRoundedDisplay(double value, int scale) {
      String formatted = BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
      return formatted.contains(".") ? formatted : formatted + ".0";
   }
}
