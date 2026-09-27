package ru.daniar.remotweaks.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import ru.daniar.remotweaks.RemoTweaks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    private final RemoTweaks plugin;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public DeathListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Location loc = victim.getLocation();

        // 1. Координаты смерти
        if (plugin.getConfig().getBoolean("death-coordinates.enabled", true)) {
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
            String msg = plugin.getConfig().getString("death-coordinates.message",
                            "&c☠ Место вашей смерти: &6{world} &7[&eX: {x}, Y: {y}, Z: {z}&7]")
                    .replace("{world}", worldName)
                    .replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{y}", String.valueOf(loc.getBlockY()))
                    .replace("{z}", String.valueOf(loc.getBlockZ()));
            victim.sendMessage(plugin.color(msg));
        }

        // 2. Выпадение головы в PvP
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
}
