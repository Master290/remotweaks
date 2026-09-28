package su.remo.tweaks.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.GraveManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DeathListener implements Listener {

    private final RemoTweaks plugin;
    private final GraveManager graveManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final Map<UUID, Boolean> compassModes = new HashMap<>(); // true = death, false = spawn

    public DeathListener(RemoTweaks plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Location loc = victim.getLocation();

        // Запоминаем точку смерти для компаса
        graveManager.setLastDeathLocation(victim.getUniqueId(), loc);

        // 1. Создание надгробия (могилы)
        if (plugin.getConfig().getBoolean("graves.enabled", true)) {
            List<ItemStack> drops = new ArrayList<>(event.getDrops());
            if (!drops.isEmpty() && !event.getKeepInventory()) {
                GraveManager.GraveData grave = graveManager.createGrave(victim, drops, event.getDroppedExp());
                if (grave != null) {
                    event.getDrops().clear();
                    event.setDroppedExp(0);
                }
            }
        }

        // 2. Кликабельное сообщение о смерти в чат
        if (plugin.getConfig().getBoolean("death-coordinates.enabled", true)) {
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            Component deathMsg = Component.text("☠ Место вашей смерти: ", NamedTextColor.RED)
                    .append(Component.text(worldName + " ", NamedTextColor.GOLD))
                    .append(Component.text("[X: " + x + ", Y: " + y + ", Z: " + z + "]", NamedTextColor.YELLOW, TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы просмотреть информацию о могиле", NamedTextColor.GRAY)))
                            .clickEvent(ClickEvent.runCommand("/grave")));

            victim.sendMessage(deathMsg);
        }

        // 3. Выпадение головы в PvP
        if (plugin.getConfig().getBoolean("pvp-head-drops.enabled", true)) {
            Player killer = victim.getKiller();
            if (killer != null && !killer.equals(victim)) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(victim);
                    meta.setDisplayName(plugin.color("&6Голова игрока &e" + victim.getName()));

                    List<String> lore = new ArrayList<>();
                    lore.add(plugin.color("&7Жертва: &f" + victim.getName()));
                    lore.add(plugin.color("&7Победитель: &a" + killer.getName()));

                    ItemStack weapon = killer.getInventory().getItemInMainHand();
                    String weaponName = (weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName())
                            ? weapon.getItemMeta().getDisplayName()
                            : weapon.getType().name();
                    lore.add(plugin.color("&7Оружие: &e" + weaponName));
                    lore.add(plugin.color("&7Дата: &8" + LocalDateTime.now().format(formatter)));

                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }

                loc.getWorld().dropItemNaturally(loc, head);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = graveManager.getLastDeathLocation(player.getUniqueId());

        if (deathLoc != null) {
            // Настраиваем ванильный компас игрока на место смерти
            player.setCompassTarget(deathLoc);
            compassModes.put(player.getUniqueId(), true);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(plugin.color("&7🧭 Ваш компас автоматически настроен на координаты гибели!"));
                }
            }, 30L);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCompassInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.COMPASS && item.getType() != Material.RECOVERY_COMPASS) return;

        Location deathLoc = graveManager.getLastDeathLocation(player.getUniqueId());
        Location spawnLoc = player.getWorld().getSpawnLocation();

        boolean toDeath = !compassModes.getOrDefault(player.getUniqueId(), false);
        compassModes.put(player.getUniqueId(), toDeath);

        if (toDeath && deathLoc != null) {
            player.setCompassTarget(deathLoc);
            int dist = (int) (player.getWorld().equals(deathLoc.getWorld()) ? player.getLocation().distance(deathLoc) : -1);
            String distStr = dist >= 0 ? " &7(дистанция: &e" + dist + "м&7)" : "";
            player.sendActionBar(plugin.color("&7🧭 Компас: &cМесто гибели" + distStr));
            player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.2f);
        } else {
            player.setCompassTarget(spawnLoc);
            int dist = (int) player.getLocation().distance(spawnLoc);
            player.sendActionBar(plugin.color("&7🧭 Компас: &aСпавн мира &7(дистанция: &e" + dist + "м&7)"));
            player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 0.8f);
        }
    }
}
