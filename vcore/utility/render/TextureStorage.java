package vcore.utility.render;

import net.minecraft.util.Identifier;

public final class TextureStorage {
   public static final Identifier star = id("textures/particles/star.png");
   public static final Identifier heart = id("textures/particles/heart.png");
   public static final Identifier dollar = id("textures/particles/dollar.png");
   public static final Identifier snowflake = id("textures/particles/snowflake.png");
   public static final Identifier firefly = id("textures/particles/firefly.png");
   public static final Identifier arrow = id("textures/markers/triangle.png");
   public static final Identifier capture = id("textures/markers/capture.png");
   public static final Identifier bubble = id("textures/particles/hit_bubble.png");
   public static final Identifier defaultCircle = id("textures/particles/circle.png");
   public static final Identifier container = id("textures/hud/container.png");
   public static final Identifier guiArrow = id("textures/gui/arrow.png");
   public static final Identifier donation = id("textures/gui/donation_alerts.png");
   public static final Identifier setting = id("textures/gui/settings.png");
   public static final Identifier brokenShield = id("textures/hud/broken_shield.png");
   public static final Identifier miniLogo = id("textures/branding/mini_logo.png");
   public static final Identifier playerIcon = id("textures/gui/player.png");
   public static final Identifier speedometerIcon = id("textures/hud/speedometer.png");
   public static final Identifier lagIcon = id("textures/hud/lag.png");
   public static final Identifier fpsIcon = id("textures/hud/fps.png");
   public static final Identifier pingIcon = id("textures/hud/ping.png");
   public static final Identifier tpsIcon = id("textures/hud/tps.png");
   public static final Identifier thLogo = id("textures/branding/th_logo.png");
   public static final Identifier coordsIcon = id("textures/hud/coords.png");
   public static final Identifier themeHudIcon = id("textures/gui/theme_hud.png");
   public static final Identifier starCape = id("textures/capes/starcape.png");

   private TextureStorage() {
   }

   private static Identifier id(String path) {
      return Identifier.of("vcore", path);
   }
}
