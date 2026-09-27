package su.remo.tweaks;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import su.remo.tweaks.commands.SitCommand;
import su.remo.tweaks.commands.TradeCommand;
import su.remo.tweaks.commands.RemoTweaksCommand;
import su.remo.tweaks.listeners.*;
import su.remo.tweaks.managers.AFKManager;
import su.remo.tweaks.managers.SitManager;
import su.remo.tweaks.managers.SleepManager;
import su.remo.tweaks.managers.TradeManager;

public class RemoTweaks extends JavaPlugin {

    private static RemoTweaks instance;
    private SitManager sitManager;
    private SleepManager sleepManager;
    private TradeManager tradeManager;
    private AFKManager afkManager;
    private ShulkerQuickOpenListener shulkerQuickOpenListener;
    private FastLeafDecayListener fastLeafDecayListener;

    @Override
    public void onEnable() {
        instance = this;

        // Сохраняем конфиг по умолчанию, если не существует
        saveDefaultConfig();

        // Инициализация менеджеров
        this.sitManager = new SitManager(this);
        this.afkManager = new AFKManager(this);
        this.sleepManager = new SleepManager(this);
        this.tradeManager = new TradeManager(this);

        // Регистрация слушателей событий
        var pm = getServer().getPluginManager();
        pm.registerEvents(new SitListener(this), this);
        pm.registerEvents(new ItemFrameListener(this), this);
        pm.registerEvents(new SleepListener(this), this);
        pm.registerEvents(new PlayerRideListener(this), this);
        pm.registerEvents(new DoubleDoorListener(this), this);
        pm.registerEvents(new HarvestListener(this), this);
        pm.registerEvents(new DeathListener(this), this);
        pm.registerEvents(new PetProtectionListener(this), this);
        pm.registerEvents(new ArmorStandEditorListener(this), this);
        pm.registerEvents(new TradeListener(this), this);
        pm.registerEvents(new ChopTreeListener(this), this);
        pm.registerEvents(new MobPickupListener(this), this);
        pm.registerEvents(new ChatShowItemListener(this), this);
        pm.registerEvents(new DropProtectionListener(this), this);
        pm.registerEvents(new AFKListener(this), this);
        pm.registerEvents(new BeehiveInspectorListener(this), this);
        pm.registerEvents(new ColorFormatListener(this), this);
        pm.registerEvents(new ChestSortListener(this), this);
        pm.registerEvents(new PotionStackListener(this), this);
        this.shulkerQuickOpenListener = new ShulkerQuickOpenListener(this);
        pm.registerEvents(shulkerQuickOpenListener, this);
        pm.registerEvents(new ExpBottlingListener(this), this);
        pm.registerEvents(new ChatMentionListener(this), this);
        this.fastLeafDecayListener = new FastLeafDecayListener(this);
        pm.registerEvents(fastLeafDecayListener, this);

        // Регистрация команд
        if (getCommand("sit") != null) {
            getCommand("sit").setExecutor(new SitCommand(this));
        }
        if (getCommand("remotweaks") != null) {
            var vsmpCmd = new RemoTweaksCommand(this);
            getCommand("remotweaks").setExecutor(vsmpCmd);
            getCommand("remotweaks").setTabCompleter(vsmpCmd);
        }
        if (getCommand("trade") != null) {
            var tradeCmd = new TradeCommand(this);
            getCommand("trade").setExecutor(tradeCmd);
            getCommand("trade").setTabCompleter(tradeCmd);
        }
        if (getCommand("sort") != null) {
            var sortCmd = new su.remo.tweaks.commands.SortCommand(this);
            getCommand("sort").setExecutor(sortCmd);
            getCommand("sort").setTabCompleter(sortCmd);
        }
        if (getCommand("bottle") != null) {
            var bottleCmd = new su.remo.tweaks.commands.BottleCommand(this);
            getCommand("bottle").setExecutor(bottleCmd);
            getCommand("bottle").setTabCompleter(bottleCmd);
        }
        if (getCommand("stats") != null) {
            var statsCmd = new su.remo.tweaks.commands.StatsCommand(this);
            getCommand("stats").setExecutor(statsCmd);
            getCommand("stats").setTabCompleter(statsCmd);
        }

        getLogger().info("RemoTweaks успешно запущен! Все Vanilla+ функции активированы.");
    }

    @Override
    public void onDisable() {
        if (shulkerQuickOpenListener != null) {
            shulkerQuickOpenListener.cleanupAll();
        }
        if (fastLeafDecayListener != null) {
            fastLeafDecayListener.cleanup();
        }
        if (sitManager != null) {
            sitManager.cleanupAll();
        }
        if (sleepManager != null) {
            sleepManager.cleanupAll();
        }
        if (afkManager != null) {
            afkManager.cleanup();
        }
        getLogger().info("RemoTweaks выключен. Все активные сессии очищены.");
    }

    public static RemoTweaks getInstance() {
        return instance;
    }

    public FastLeafDecayListener getFastLeafDecayListener() {
        return fastLeafDecayListener;
    }

    public ShulkerQuickOpenListener getShulkerQuickOpenListener() {
        return shulkerQuickOpenListener;
    }

    public SitManager getSitManager() {
        return sitManager;
    }

    public SleepManager getSleepManager() {
        return sleepManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public AFKManager getAfkManager() {
        return afkManager;
    }

    public String color(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
