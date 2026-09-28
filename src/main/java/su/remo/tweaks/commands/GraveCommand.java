package su.remo.tweaks.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.GraveManager;

import java.util.Collections;
import java.util.List;

public class GraveCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;
    private final GraveManager graveManager;

    public GraveCommand(RemoTweaks plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        List<GraveManager.GraveData> graves = graveManager.getPlayerGraves(player.getUniqueId());
        if (graves.isEmpty()) {
            Location lastDeath = graveManager.getLastDeathLocation(player.getUniqueId());
            if (lastDeath != null && lastDeath.getWorld() != null) {
                int dist = (int) (player.getWorld().equals(lastDeath.getWorld()) ? player.getLocation().distance(lastDeath) : -1);
                String distStr = dist >= 0 ? " &7(дистанция: &e" + dist + "&7м)" : "";
                player.sendMessage(plugin.color("&7У вас нет активных могил, но последнее место смерти: &6" + lastDeath.getWorld().getName() + " &7[&eX: " + lastDeath.getBlockX() + ", Y: " + lastDeath.getBlockY() + ", Z: " + lastDeath.getBlockZ() + "&7]" + distStr));
            } else {
                player.sendMessage(plugin.color("&7У вас нет активных могил."));
            }
            return true;
        }

        player.sendMessage(plugin.color("&6=== 🪦 Ваши активные могилы (" + graves.size() + ") ==="));
        for (GraveManager.GraveData g : graves) {
            Location loc = g.getLocation();
            int dist = (int) (player.getWorld().equals(loc.getWorld()) ? player.getLocation().distance(loc) : -1);
            String distStr = dist >= 0 ? " &7(дистанция: &a" + dist + " блоков&7)" : " &7(в другом мире)";
            player.sendMessage(plugin.color("&e• &6" + loc.getWorld().getName() + " &7[&eX: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ() + "&7]" + distStr));
            player.sendMessage(plugin.color("  &7Время: &f" + g.getTime() + " &7| Предметов: &b" + g.getItems().size() + " &7| Опыт: &a" + g.getExp()));
        }
        player.sendMessage(plugin.color("&7(Подойдите и нажмите ПКМ по голове, чтобы забрать вещи)"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
