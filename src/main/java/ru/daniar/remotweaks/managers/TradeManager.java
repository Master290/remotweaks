package ru.daniar.remotweaks.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.daniar.remotweaks.RemoTweaks;

import java.util.*;

public class TradeManager {

    private final RemoTweaks plugin;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    public static final Set<Integer> P1_SLOTS = Set.of(
            0, 1, 2, 3,
            9, 10, 11, 12,
            18, 19, 20, 21,
            27, 28, 29, 30
    );

    public static final Set<Integer> P2_SLOTS = Set.of(
            5, 6, 7, 8,
            14, 15, 16, 17,
            23, 24, 25, 26,
            32, 33, 34, 35
    );

    public TradeManager(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    public void handleTradeCommand(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(plugin.color("&cВы не можете торговать с самим собой!"));
            return;
        }

        if (isInTrade(sender)) {
            sender.sendMessage(plugin.color("&cВы уже находитесь в процессе обмена!"));
            return;
        }

        if (isInTrade(target)) {
            sender.sendMessage(plugin.color("&cЭтот игрок сейчас занят другим обменом!"));
            return;
        }

        double maxDist = plugin.getConfig().getDouble("trade.max-distance", 15.0);
        if (maxDist > 0) {
            if (!sender.getWorld().equals(target.getWorld()) || sender.getLocation().distance(target.getLocation()) > maxDist) {
                sender.sendMessage(plugin.color("&cИгрок слишком далеко! (Максимальная дистанция: " + (int)maxDist + " блоков)"));
                return;
            }
        }

        // Если уже есть встречный запрос - запускаем трейд
        if (pendingRequests.containsKey(target.getUniqueId()) && pendingRequests.get(target.getUniqueId()).equals(sender.getUniqueId())) {
            pendingRequests.remove(target.getUniqueId());
            startTrade(sender, target);
            return;
        }

        pendingRequests.put(sender.getUniqueId(), target.getUniqueId());
        sender.sendMessage(plugin.color("&aЗапрос на обмен отправлен игроку &e" + target.getName() + "&a!"));
        target.sendMessage(plugin.color("&6[RemoTweaks] &e" + sender.getName() + " &fпредлагает вам обмен! Введите &a/trade " + sender.getName() + " &fдля согласия."));

        // Автоматическая отмена через 60 секунд
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.remove(sender.getUniqueId(), target.getUniqueId())) {
                sender.sendMessage(plugin.color("&7Срок действия запроса на обмен к &e" + target.getName() + " &7истёк."));
            }
        }, 1200L);
    }

    public void startTrade(Player p1, Player p2) {
        TradeSession session = new TradeSession(plugin, p1, p2);
        activeTrades.put(p1.getUniqueId(), session);
        activeTrades.put(p2.getUniqueId(), session);
        session.open();
    }

    public boolean isInTrade(Player player) {
        return activeTrades.containsKey(player.getUniqueId());
    }

    public TradeSession getSession(Player player) {
        return activeTrades.get(player.getUniqueId());
    }

    public void endTrade(TradeSession session) {
        activeTrades.remove(session.getP1().getUniqueId());
        activeTrades.remove(session.getP2().getUniqueId());
    }

    public static class TradeSession {
        private final RemoTweaks plugin;
        private final Player p1;
        private final Player p2;
        private final Inventory inv;

        private boolean p1Ready = false;
        private boolean p2Ready = false;
        private BukkitTask countdownTask = null;
        private boolean completed = false;

        public TradeSession(RemoTweaks plugin, Player p1, Player p2) {
            this.plugin = plugin;
            this.p1 = p1;
            this.p2 = p2;
            this.inv = Bukkit.createInventory(null, 54, plugin.color("&8Обмен: " + p1.getName() + " <-> " + p2.getName()));

            setupGUI();
        }

        private void setupGUI() {
            ItemStack separator = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
            for (int r = 0; r < 6; r++) {
                inv.setItem(r * 9 + 4, separator);
            }
            for (int c = 36; c <= 44; c++) {
                if (c != 40) inv.setItem(c, separator);
            }
            for (int c = 46; c <= 52; c++) {
                if (c != 49) inv.setItem(c, separator);
            }

            inv.setItem(40, createItem(Material.PAPER, "&eБезопасный обмен",
                    List.of("&7Положите свои предметы слева/справа", "&7Нажмите кнопку готовности внизу")));

            updateReadyButtons();
        }

        public void open() {
            p1.openInventory(inv);
            p2.openInventory(inv);
            p1.playSound(p1.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
            p2.playSound(p2.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
        }

        public void updateReadyButtons() {
            inv.setItem(45, createItem(p1Ready ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                    p1Ready ? "&a" + p1.getName() + " готов!" : "&c" + p1.getName() + " не готов",
                    List.of("&7Клик для изменения статуса")));

            inv.setItem(53, createItem(p2Ready ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                    p2Ready ? "&a" + p2.getName() + " готов!" : "&c" + p2.getName() + " не готов",
                    List.of("&7Клик для изменения статуса")));

            if (p1Ready && p2Ready) {
                startCountdown();
            } else {
                cancelCountdown();
                inv.setItem(49, createItem(Material.CLOCK, "&eОжидание игроков...",
                        List.of("&7Оба игрока должны подтвердить готовность")));
            }
        }

        public void toggleReady(Player clicker) {
            if (clicker.equals(p1)) {
                p1Ready = !p1Ready;
            } else if (clicker.equals(p2)) {
                p2Ready = !p2Ready;
            }
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            updateReadyButtons();
        }

        public void onItemsChanged() {
            if (p1Ready || p2Ready) {
                p1Ready = false;
                p2Ready = false;
                updateReadyButtons();
                p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
                p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            }
        }

        private void startCountdown() {
            if (countdownTask != null) return;

            countdownTask = new BukkitRunnable() {
                int seconds = 3;

                @Override
                public void run() {
                    if (!p1Ready || !p2Ready) {
                        cancel();
                        countdownTask = null;
                        return;
                    }

                    if (seconds <= 0) {
                        completeTrade();
                        cancel();
                        countdownTask = null;
                        return;
                    }

                    inv.setItem(49, createItem(Material.GOLD_BLOCK, "&aЗавершение обмена: &e" + seconds + "с.", null));
                    p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.0f + (3 - seconds) * 0.2f);
                    p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.0f + (3 - seconds) * 0.2f);
                    seconds--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }

        private void cancelCountdown() {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }

        public void completeTrade() {
            completed = true;

            // Сбор предметов P1
            List<ItemStack> p1Items = new ArrayList<>();
            for (int slot : P1_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    p1Items.add(item.clone());
                    inv.setItem(slot, null);
                }
            }

            // Сбор предметов P2
            List<ItemStack> p2Items = new ArrayList<>();
            for (int slot : P2_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    p2Items.add(item.clone());
                    inv.setItem(slot, null);
                }
            }

            // Передача P1 предметов в инвентарь P2
            for (ItemStack item : p1Items) {
                HashMap<Integer, ItemStack> leftover = p2.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) p2.getWorld().dropItemNaturally(p2.getLocation(), drop);
            }

            // Передача P2 предметов в инвентарь P1
            for (ItemStack item : p2Items) {
                HashMap<Integer, ItemStack> leftover = p1.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) p1.getWorld().dropItemNaturally(p1.getLocation(), drop);
            }

            p1.sendMessage(plugin.color("&aОбмен успешно завершен!"));
            p2.sendMessage(plugin.color("&aОбмен успешно завершен!"));
            p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);

            p1.closeInventory();
            p2.closeInventory();
            plugin.getTradeManager().endTrade(this);
        }

        public void cancelTrade() {
            if (completed) return;
            cancelCountdown();

            // Возврат предметов
            for (int slot : P1_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> leftover = p1.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) p1.getWorld().dropItemNaturally(p1.getLocation(), drop);
                    inv.setItem(slot, null);
                }
            }

            for (int slot : P2_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> leftover = p2.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) p2.getWorld().dropItemNaturally(p2.getLocation(), drop);
                    inv.setItem(slot, null);
                }
            }

            p1.sendMessage(plugin.color("&cОбмен был отменён. Все предметы возвращены!"));
            p2.sendMessage(plugin.color("&cОбмен был отменён. Все предметы возвращены!"));
            plugin.getTradeManager().endTrade(this);
        }

        private ItemStack createItem(Material material, String name, List<String> lore) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.color(name));
                if (lore != null) {
                    List<String> colored = new ArrayList<>();
                    for (String l : lore) colored.add(plugin.color(l));
                    meta.setLore(colored);
                }
                item.setItemMeta(meta);
            }
            return item;
        }

        public Player getP1() { return p1; }
        public Player getP2() { return p2; }
        public Inventory getInventory() { return inv; }
    }
}
