package su.remo.tweaks;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import su.remo.tweaks.commands.*;
import su.remo.tweaks.listeners.*;
import su.remo.tweaks.managers.AFKManager;
import su.remo.tweaks.managers.BoardManager;
import su.remo.tweaks.managers.CampfireManager;
import su.remo.tweaks.managers.GraveManager;
import su.remo.tweaks.managers.LockManager;
import su.remo.tweaks.managers.MailManager;
import su.remo.tweaks.managers.MsgManager;
import su.remo.tweaks.managers.PollManager;
import su.remo.tweaks.managers.PrefixManager;
import su.remo.tweaks.managers.RulerManager;
import su.remo.tweaks.managers.SitManager;
import su.remo.tweaks.managers.SleepManager;
import su.remo.tweaks.managers.TabListManager;
import su.remo.tweaks.managers.TradeManager;

public class RemoTweaks extends JavaPlugin {

    private static RemoTweaks instance;
    private SitManager sitManager;
    private SleepManager sleepManager;
    private TradeManager tradeManager;
    private AFKManager afkManager;
    private TabListManager tabListManager;
    private PrefixManager prefixManager;
    private MsgManager msgManager;
    private LockManager lockManager;
    private MailManager mailManager;
    private GraveManager graveManager;
    private RulerManager rulerManager;
    private BoardManager boardManager;
    private CampfireManager campfireManager;
    private PollManager pollManager;
    private ChunkBorderCommand chunkBorderCommand;
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
        this.prefixManager = new PrefixManager(this);
        this.tabListManager = new TabListManager(this);
        this.msgManager = new MsgManager(this);
        this.lockManager = new LockManager(this);
        this.mailManager = new MailManager(this);
        this.graveManager = new GraveManager(this);
        this.rulerManager = new RulerManager(this);
        this.boardManager = new BoardManager(this);
        this.campfireManager = new CampfireManager(this);
        this.pollManager = new PollManager(this);

        // Регистрация слушателей событий
        var pm = getServer().getPluginManager();
        pm.registerEvents(new LockListener(this), this);
        pm.registerEvents(new PrefixChatListener(this), this);
        pm.registerEvents(new TabListListener(this), this);
        pm.registerEvents(new SitListener(this), this);
        pm.registerEvents(new ItemFrameListener(this), this);
        pm.registerEvents(new SleepListener(this), this);
        pm.registerEvents(new PlayerRideListener(this), this);
        pm.registerEvents(new DoubleDoorListener(this), this);
        pm.registerEvents(new HarvestListener(this), this);
        pm.registerEvents(new DeathListener(this, graveManager), this);
        pm.registerEvents(new GraveListener(this, graveManager), this);
        pm.registerEvents(new MailListener(this, mailManager), this);
        pm.registerEvents(new RulerListener(this, rulerManager), this);
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
        pm.registerEvents(new ItemGiftListener(this), this);
        pm.registerEvents(new BoardSignListener(this), this);

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
            var sortCmd = new SortCommand(this);
            getCommand("sort").setExecutor(sortCmd);
            getCommand("sort").setTabCompleter(sortCmd);
        }
        if (getCommand("bottle") != null) {
            var bottleCmd = new BottleCommand(this);
            getCommand("bottle").setExecutor(bottleCmd);
            getCommand("bottle").setTabCompleter(bottleCmd);
        }
        if (getCommand("stats") != null) {
            var statsCmd = new StatsCommand(this);
            getCommand("stats").setExecutor(statsCmd);
            getCommand("stats").setTabCompleter(statsCmd);
        }
        if (getCommand("prefix") != null) {
            var prefixCmd = new PrefixCommand(this);
            getCommand("prefix").setExecutor(prefixCmd);
            getCommand("prefix").setTabCompleter(prefixCmd);
        }
        if (getCommand("suffix") != null) {
            var suffixCmd = new SuffixCommand(this);
            getCommand("suffix").setExecutor(suffixCmd);
            getCommand("suffix").setTabCompleter(suffixCmd);
        }
        if (getCommand("glow") != null) {
            var glowCmd = new GlowCommand(this);
            getCommand("glow").setExecutor(glowCmd);
            getCommand("glow").setTabCompleter(glowCmd);
        }
        if (getCommand("msg") != null) {
            var msgCmd = new MsgCommand(this);
            getCommand("msg").setExecutor(msgCmd);
            getCommand("msg").setTabCompleter(msgCmd);
        }
        if (getCommand("r") != null) {
            var replyCmd = new ReplyCommand(this);
            getCommand("r").setExecutor(replyCmd);
            getCommand("r").setTabCompleter(replyCmd);
        }
        if (getCommand("socialspy") != null) {
            var spyCmd = new SocialSpyCommand(this);
            getCommand("socialspy").setExecutor(spyCmd);
            getCommand("socialspy").setTabCompleter(spyCmd);
        }
        EmoteCommand emoteCmd = new EmoteCommand(this);
        for (String em : java.util.List.of("hug", "kiss", "highfive")) {
            if (getCommand(em) != null) {
                getCommand(em).setExecutor(emoteCmd);
                getCommand(em).setTabCompleter(emoteCmd);
            }
        }
        if (getCommand("lock") != null) {
            var lockCmd = new LockCommand(this);
            getCommand("lock").setExecutor(lockCmd);
            getCommand("lock").setTabCompleter(lockCmd);
        }
        DiceCommand diceCmd = new DiceCommand(this);
        for (String dCmd : java.util.List.of("roll", "coin")) {
            if (getCommand(dCmd) != null) {
                getCommand(dCmd).setExecutor(diceCmd);
                getCommand(dCmd).setTabCompleter(diceCmd);
            }
        }
        if (getCommand("mail") != null) {
            var mailCmd = new MailCommand(this, mailManager);
            getCommand("mail").setExecutor(mailCmd);
            getCommand("mail").setTabCompleter(mailCmd);
        }
        this.chunkBorderCommand = new ChunkBorderCommand(this);
        if (getCommand("chunk") != null) {
            getCommand("chunk").setExecutor(chunkBorderCommand);
            getCommand("chunk").setTabCompleter(chunkBorderCommand);
        }
        if (getCommand("ruler") != null) {
            var rulerCmd = new RulerCommand(this, rulerManager);
            getCommand("ruler").setExecutor(rulerCmd);
            getCommand("ruler").setTabCompleter(rulerCmd);
        }
        if (getCommand("grave") != null) {
            var graveCmd = new GraveCommand(this, graveManager);
            getCommand("grave").setExecutor(graveCmd);
            getCommand("grave").setTabCompleter(graveCmd);
        }
        if (getCommand("board") != null) {
            var boardCmd = new BoardCommand(this, boardManager);
            getCommand("board").setExecutor(boardCmd);
            getCommand("board").setTabCompleter(boardCmd);
        }
        if (getCommand("poll") != null) {
            var pollCmd = new PollCommand(this, pollManager);
            getCommand("poll").setExecutor(pollCmd);
            getCommand("poll").setTabCompleter(pollCmd);
        }

        getLogger().info("RemoTweaks успешно запущен! Все Vanilla+ функции активированы.");
    }

    @Override
    public void onDisable() {
        if (campfireManager != null) {
            campfireManager.cleanup();
        }
        if (pollManager != null) {
            pollManager.cleanup();
        }
        if (chunkBorderCommand != null) {
            chunkBorderCommand.cleanup();
        }
        if (graveManager != null) {
            graveManager.cleanup();
        }
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
        if (prefixManager != null) {
            prefixManager.cleanup();
        }
        if (tabListManager != null) {
            tabListManager.cleanup();
        }
        getLogger().info("RemoTweaks выключен. Все активные сессии очищены.");
    }

    public static RemoTweaks getInstance() {
        return instance;
    }

    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
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

    public MsgManager getMsgManager() {
        return msgManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    public MailManager getMailManager() {
        return mailManager;
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }

    public RulerManager getRulerManager() {
        return rulerManager;
    }

    public BoardManager getBoardManager() {
        return boardManager;
    }

    public CampfireManager getCampfireManager() {
        return campfireManager;
    }

    public PollManager getPollManager() {
        return pollManager;
    }

    public String color(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
