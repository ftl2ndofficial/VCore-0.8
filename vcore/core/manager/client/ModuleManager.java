package vcore.core.manager.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import vcore.Vcore;
import vcore.core.manager.IManager;
import vcore.features.hud.HudElement;
import vcore.features.modules.Module;
import vcore.features.modules.combat.AimBot;
import vcore.features.modules.combat.AntiBot;
import vcore.features.modules.combat.Aura;
import vcore.features.modules.combat.AutoBuff;
import vcore.features.modules.combat.AutoCart;
import vcore.features.modules.combat.AutoCrystal;
import vcore.features.modules.combat.AutoGApple;
import vcore.features.modules.combat.AutoTotem;
import vcore.features.modules.combat.AutoTrap;
import vcore.features.modules.combat.AutoWeb;
import vcore.features.modules.combat.BowSpam;
import vcore.features.modules.combat.Criticals;
import vcore.features.modules.combat.CrystalOptimizer;
import vcore.features.modules.combat.EMaceHelper;
import vcore.features.modules.combat.ElytraTarget;
import vcore.features.modules.combat.HitBox;
import vcore.features.modules.combat.MaceSwap;
import vcore.features.modules.combat.Reach;
import vcore.features.modules.combat.SelfTrap;
import vcore.features.modules.combat.TargetStrafe;
import vcore.features.modules.combat.TriggerBot;
import vcore.features.modules.combat.WallsBypass;
import vcore.features.modules.misc.AntiAFK;
import vcore.features.modules.misc.AntiAttack;
import vcore.features.modules.misc.AntiCrash;
import vcore.features.modules.misc.AntiServerRP;
import vcore.features.modules.misc.AutoAuth;
import vcore.features.modules.misc.AutoDungeon;
import vcore.features.modules.misc.AutoFarm;
import vcore.features.modules.misc.AutoTpAccept;
import vcore.features.modules.misc.ChestStealer;
import vcore.features.modules.misc.ClickGui;
import vcore.features.modules.misc.ClientSettings;
import vcore.features.modules.misc.ClientSound;
import vcore.features.modules.misc.ClientSpoof;
import vcore.features.modules.misc.CombatLeave;
import vcore.features.modules.misc.FakePlayer;
import vcore.features.modules.misc.FixHP;
import vcore.features.modules.misc.LagNotifier;
import vcore.features.modules.misc.MessageAppend;
import vcore.features.modules.misc.NameProtect;
import vcore.features.modules.misc.Notifications;
import vcore.features.modules.misc.Nuker;
import vcore.features.modules.misc.RPC;
import vcore.features.modules.misc.Spammer;
import vcore.features.modules.misc.TotemPopCounter;
import vcore.features.modules.misc.UnHook;
import vcore.features.modules.movement.AirStuck;
import vcore.features.modules.movement.AntiWeb;
import vcore.features.modules.movement.AutoSprint;
import vcore.features.modules.movement.AutoWalk;
import vcore.features.modules.movement.Blink;
import vcore.features.modules.movement.ElytraBoost;
import vcore.features.modules.movement.ElytraMotion;
import vcore.features.modules.movement.ElytraPlus;
import vcore.features.modules.movement.Flight;
import vcore.features.modules.movement.FreeLook;
import vcore.features.modules.movement.GuiMove;
import vcore.features.modules.movement.MoveFix;
import vcore.features.modules.movement.NoFall;
import vcore.features.modules.movement.NoSlow;
import vcore.features.modules.movement.Phase;
import vcore.features.modules.movement.Speed;
import vcore.features.modules.movement.TridentBoost;
import vcore.features.modules.movement.Velocity;
import vcore.features.modules.movement.WaterSpeed;
import vcore.features.modules.movement.WindJump;
import vcore.features.modules.player.AntiAim;
import vcore.features.modules.player.AutoArmor;
import vcore.features.modules.player.AutoEat;
import vcore.features.modules.player.AutoRespawn;
import vcore.features.modules.player.AutoTool;
import vcore.features.modules.player.ClickAction;
import vcore.features.modules.player.DurabilityAlert;
import vcore.features.modules.player.ElytraReplace;
import vcore.features.modules.player.ElytraSwap;
import vcore.features.modules.player.FreeCam;
import vcore.features.modules.player.HotbarReplenish;
import vcore.features.modules.player.InventoryCleaner;
import vcore.features.modules.player.ItemHelper;
import vcore.features.modules.player.ItemScroller;
import vcore.features.modules.player.NoDelay;
import vcore.features.modules.player.NoEntityTrace;
import vcore.features.modules.player.NoInteract;
import vcore.features.modules.player.NoPush;
import vcore.features.modules.player.PearlChaser;
import vcore.features.modules.player.PerfectDelay;
import vcore.features.modules.player.PortalInventory;
import vcore.features.modules.player.TapeMouse;
import vcore.features.modules.render.Animations;
import vcore.features.modules.render.Arrows;
import vcore.features.modules.render.AspectRatio;
import vcore.features.modules.render.BlockESP;
import vcore.features.modules.render.ChunkAnimation;
import vcore.features.modules.render.Crosshair;
import vcore.features.modules.render.ESP;
import vcore.features.modules.render.Fullbright;
import vcore.features.modules.render.Hat;
import vcore.features.modules.render.HitParticles;
import vcore.features.modules.render.HudEditor;
import vcore.features.modules.render.ItemESP;
import vcore.features.modules.render.ItemPhysics;
import vcore.features.modules.render.JumpCircle;
import vcore.features.modules.render.LogoutSpots;
import vcore.features.modules.render.NoCameraClip;
import vcore.features.modules.render.NoRender;
import vcore.features.modules.render.Particles;
import vcore.features.modules.render.PopChams;
import vcore.features.modules.render.Prediction;
import vcore.features.modules.render.StorageEsp;
import vcore.features.modules.render.TargerESP;
import vcore.features.modules.render.Tooltips;
import vcore.features.modules.render.TotemAnimation;
import vcore.features.modules.render.ViewModel;
import vcore.features.modules.render.WorldTweaks;
import vcore.features.modules.render.XRay;
import vcore.gui.clickui.ClickGUI;

public class ModuleManager implements IManager {
   public ArrayList<Module> modules = new ArrayList<>();
   public List<Integer> activeMouseKeys = new ArrayList<>();
   public static boolean keyPearlAntiPickup = false;
   public static InventoryCleaner inventoryCleaner = new InventoryCleaner();
   public static PortalInventory portalInventory = new PortalInventory();
   public static HotbarReplenish hotbarReplenish = new HotbarReplenish();
   public static TotemPopCounter totemPopCounter = new TotemPopCounter();
   public static DurabilityAlert durabilityAlert = new DurabilityAlert();
   public static TotemAnimation totemAnimation = new TotemAnimation();
   public static ClientSettings clientSettings = new ClientSettings();
   public static MessageAppend messageAppend = new MessageAppend();
   public static ElytraReplace elytraReplace = new ElytraReplace();
   public static Notifications notifications = new Notifications();
   public static NoEntityTrace noEntityTrace = new NoEntityTrace();
   public static NoCameraClip noCameraClip = new NoCameraClip();
   public static TargetStrafe targetStrafe = new TargetStrafe();
   public static ItemScroller itemScroller = new ItemScroller();
   public static HitParticles hitParticles = new HitParticles();
   public static ChestStealer chestStealer = new ChestStealer();
   public static AutoTpAccept autoTpAccept = new AutoTpAccept();
   public static AntiServerRP antiServerRP = new AntiServerRP();
   public static PerfectDelay perfectDelay = new PerfectDelay();
   public static TridentBoost tridentBoost = new TridentBoost();
   public static WindJump windJump = new WindJump();
   public static ClientSound ClientSound = new ClientSound();
   public static PearlChaser pearlChaser = new PearlChaser();
   public static WorldTweaks worldTweaks = new WorldTweaks();
   public static NameProtect nameProtect = new NameProtect();
   public static LogoutSpots logoutSpots = new LogoutSpots();
   public static LagNotifier lagNotifier = new LagNotifier();
   public static AutoRespawn autoRespawn = new AutoRespawn();
   public static AspectRatio aspectRatio = new AspectRatio();
   public static ClientSpoof clientSpoof = new ClientSpoof();
   public static ItemPhysics itemPhysics = new ItemPhysics();
   public static WaterSpeed waterSpeed = new WaterSpeed();
   public static TriggerBot triggerBot = new TriggerBot();
   public static StorageEsp storageEsp = new StorageEsp();
   public static NoInteract noInteract = new NoInteract();
   public static JumpCircle jumpCircle = new JumpCircle();
   public static Fullbright fullbright = new Fullbright();
   public static FakePlayer fakePlayer = new FakePlayer();
   public static AutoSprint autoSprint = new AutoSprint();
   public static AutoGApple autoGApple = new AutoGApple();
   public static Animations animations = new Animations();
   public static ChunkAnimation chunkAnimation = new ChunkAnimation();
   public static AntiAttack antiAttack = new AntiAttack();
   public static Prediction prediction = new Prediction();
   public static ElytraSwap elytraSwap = new ElytraSwap();
   public static ElytraPlus elytraPlus = new ElytraPlus();
   public static ElytraTarget elytraTarget = new ElytraTarget();
   public static Particles particles = new Particles();
   public static ElytraBoost elytraBoost = new ElytraBoost();
   public static ElytraMotion elytraMotion = new ElytraMotion();
   public static TargerESP targerESP = new TargerESP();
   public static ViewModel viewModel = new ViewModel();
   public static HudEditor hudEditor = new HudEditor();
   public static Crosshair crosshair = new Crosshair();
   public static Criticals criticals = new Criticals();
   public static TapeMouse tapeMouse = new TapeMouse();
   public static AntiCrash antiCrash = new AntiCrash();
   public static ClickAction keyPearl = new ClickAction();
   public static AutoTotem autoTotem = new AutoTotem();
   public static AutoArmor autoArmor = new AutoArmor();
   public static Velocity velocity = new Velocity();
   public static Tooltips tooltips = new Tooltips();
   public static PopChams popChams = new PopChams();
   public static NoRender noRender = new NoRender();
   public static ClickGui clickGui = new ClickGui();
   public static AutoTrap autoTrap = new AutoTrap();
   public static SelfTrap selfTrap = new SelfTrap();
   public static AutoWalk autoWalk = new AutoWalk();
   public static BlockESP blockESP = new BlockESP();
   public static AirStuck airStuck = new AirStuck();
   public static MaceSwap maceSwap = new MaceSwap();
   public static EMaceHelper eMaceHelper = new EMaceHelper();
   public static WallsBypass wallsBypass = new WallsBypass();
   public static AutoTool autoTool = new AutoTool();
   public static ItemHelper itemHelper = new ItemHelper();
   public static AutoBuff autoBuff = new AutoBuff();
   public static AutoAuth autoAuth = new AutoAuth();
   public static AutoDungeon autoDungeon = new AutoDungeon();
   public static AutoFarm autoFarm = new AutoFarm();
   public static MoveFix moveFix = new MoveFix();
   public static AutoEat autoEat = new AutoEat();
   public static Spammer spammer = new Spammer();
   public static CombatLeave combatLeave = new CombatLeave();
   public static FreeLook freeLook = new FreeLook();
   public static FreeCam freeCam = new FreeCam();
   public static NoDelay nodelay = new NoDelay();
   public static BowSpam bowSpam = new BowSpam();
   public static AntiAFK antiAFK = new AntiAFK();
   public static ItemESP itemESP = new ItemESP();
   public static GuiMove guiMove = new GuiMove();
   public static AutoWeb autoWeb = new AutoWeb();
   public static AntiWeb antiWeb = new AntiWeb();
   public static AntiBot antiBot = new AntiBot();
   public static AntiAim antiAim = new AntiAim();
   public static Arrows Arrows = new Arrows();
   public static NoSlow noSlow = new NoSlow();
   public static NoFall noFall = new NoFall();
   public static HitBox hitBox = new HitBox();
   public static Flight flight = new Flight();
   public static AimBot aimBot = new AimBot();
   public static NoPush noPush = new NoPush();
   public static UnHook unHook = new UnHook();
   public static FixHP fixHP = new FixHP();
   public static Speed speed = new Speed();
   public static Reach reach = new Reach();
   public static Nuker nuker = new Nuker();
   public static Blink blink = new Blink();
   public static Phase phase = new Phase();
   public static XRay xray = new XRay();
   public static AutoCart autoCart = new AutoCart();
   public static AutoCrystal autoCrystal = new AutoCrystal();
   public static CrystalOptimizer crystalOptimizer = new CrystalOptimizer();
   public static Aura aura = new Aura();
   public static ESP esp = new ESP();
   public static Hat hat = new Hat();
   public static RPC rpc = new RPC();

   public ModuleManager() {
      for (Field field : this.getClass().getDeclaredFields()) {
         if (Module.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);

            try {
               this.modules.add((Module)field.get(this));
            } catch (IllegalAccessException e) {
               Vcore.LOGGER.error("Error initializing modules", e);
            }
         }
      }

      hudEditor.enableSilently();
   }

   public Module get(String name) {
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(name)) {
            return module;
         }
      }

      return null;
   }

   public ArrayList<Module> getEnabledModules() {
      ArrayList<Module> enabledModules = new ArrayList<>();

      for (Module module : this.modules) {
         if (module.isEnabled()) {
            enabledModules.add(module);
         }
      }

      return enabledModules;
   }

   public ArrayList<Module> getModulesByCategory(Module.Category category) {
      ArrayList<Module> modulesCategory = new ArrayList<>();
      this.modules.forEach(module -> {
         if (module.getCategory() == category) {
            modulesCategory.add(module);
         }
      });
      return modulesCategory;
   }

   public List<Module.Category> getCategories() {
      return new ArrayList<>(Module.Category.values());
   }

   public void onLoad(String category) {
      try {
         Vcore.EVENT_BUS.unsubscribe(unHook);
      } catch (Exception var3) {
      }

      unHook.setEnabled(false);
      this.modules.sort(Comparator.comparing(Module::getName));
      this.modules.forEach(m -> {
         boolean shouldEnable = m.isEnabled() && (m.getCategory().getName().equalsIgnoreCase(category) || category.equals("none"));
         if (shouldEnable) {
            m.enableSilently();
         }
      });
      if (ConfigManager.firstLaunch) {
         notifications.enable();
         rpc.enable();
         ClientSound.enable();
      }
   }

   public void onUpdate() {
      if (!Module.fullNullCheck()) {
         this.modules.stream().filter(Module::isEnabled).forEach(Module::onUpdate);
      }
   }

   public void onRender2D(DrawContext context) {
      if (!mc.getDebugHud().shouldShowDebugHud() && !mc.options.hudHidden) {
         HudElement.anyHovered = false;
         this.modules.stream().filter(Module::isEnabled).forEach(module -> module.onRender2D(context));
         if (!HudElement.anyHovered && !ClickGUI.anyHovered && GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221185));
         }

         Vcore.core.onRender2D(context);
      }
   }

   public void onRender3D(MatrixStack stack) {
      this.modules.stream().filter(Module::isEnabled).forEach(module -> module.onRender3D(stack));
   }

   public void onLogout() {
      this.modules.forEach(Module::onLogout);
   }

   public void onLogin() {
      this.modules.forEach(Module::onLogin);
   }

   public void onUnload(String category) {
      this.modules.forEach(module -> {
         if (module.isEnabled() && (module.getCategory().getName().equalsIgnoreCase(category) || category.equals("none"))) {
            Vcore.EVENT_BUS.unsubscribe(module);
            module.setEnabled(false);
         }
      });
      this.modules.forEach(Module::onUnload);
   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != -1 && eventKey != 0 && !(mc.currentScreen instanceof ClickGUI)) {
         this.modules.forEach(module -> {
            if (module.getBind().getKey() == eventKey) {
               module.toggle();
            }
         });
      }
   }

   public void onKeyReleased(int eventKey) {
      if (eventKey != -1 && eventKey != 0 && !(mc.currentScreen instanceof ClickGUI)) {
         this.modules.forEach(module -> {
            if (module.getBind().getKey() == eventKey && module.getBind().isHold()) {
               module.disable();
            }
         });
      }
   }

   public void onMoseKeyPressed(int eventKey) {
      if (eventKey != -1 && !(mc.currentScreen instanceof ClickGUI)) {
         this.modules.forEach(module -> {
            if (Objects.equals(module.getBind().getBind(), "M" + eventKey)) {
               module.toggle();
            }
         });
      }
   }

   public void onMoseKeyReleased(int eventKey) {
      if (eventKey != -1 && !(mc.currentScreen instanceof ClickGUI)) {
         this.activeMouseKeys.add(eventKey);
         this.modules.forEach(module -> {
            if (Objects.equals(module.getBind().getBind(), "M" + eventKey) && module.getBind().isHold()) {
               module.disable();
            }
         });
      }
   }

   public ArrayList<Module> getModulesSearch(String string) {
      ArrayList<Module> modulesCategory = new ArrayList<>();
      this.modules.forEach(module -> {
         if (module.getName().toLowerCase().contains(string.toLowerCase())) {
            modulesCategory.add(module);
         }
      });
      return modulesCategory;
   }

   public void registerModule(Module module) {
      if (module != null) {
         this.modules.add(module);
         if (module.isEnabled()) {
            Vcore.EVENT_BUS.subscribe(module);
         }
      }
   }
}
