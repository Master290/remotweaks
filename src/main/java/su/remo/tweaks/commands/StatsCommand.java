package su.remo.tweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public StatsCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                target = Bukkit.getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(plugin.color("&cИгрок &e" + args[0] + " &cне найден на сервере!"));
                    return true;
                }
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.color("&cУкажите ник игрока: /stats <ник>"));
                return true;
            }
            target = player;
        }

        // Время в игре
        int ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long totalSeconds = ticks / 20;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        // Добыча
        int diamonds = target.getStatistic(Statistic.MINE_BLOCK, Material.DIAMOND_ORE) +
                       target.getStatistic(Statistic.MINE_BLOCK, Material.DEEPSLATE_DIAMOND_ORE);
        int debris = target.getStatistic(Statistic.MINE_BLOCK, Material.ANCIENT_DEBRIS);

        // Бой
        int mobKills = target.getStatistic(Statistic.MOB_KILLS);
        int playerKills = target.getStatistic(Statistic.PLAYER_KILLS);
        int deaths = target.getStatistic(Statistic.DEATHS);
        String kd = (deaths == 0) ? String.valueOf(playerKills) : String.format("%.2f", (double) playerKills / deaths);

        // Дистанция
        long cmWalk = target.getStatistic(Statistic.WALK_ONE_CM);
        long cmFly = target.getStatistic(Statistic.FLY_ONE_CM);
        long cmBoat = target.getStatistic(Statistic.BOAT_ONE_CM);
        long cmTotal = cmWalk + cmFly + cmBoat + target.getStatistic(Statistic.MINECART_ONE_CM);

        double kmTotal = cmTotal / 100000.0;
        double kmWalk = cmWalk / 100000.0;
        double kmFly = cmFly / 100000.0;

        sender.sendMessage(plugin.color("&8&m----------------&r &6&lСтатистика: &e" + target.getName() + " &8&m----------------&r"));
        sender.sendMessage(plugin.color("&7⏱ Время в игре: &f" + days + "д " + hours + "ч " + minutes + "м"));
        sender.sendMessage(plugin.color("&7💎 Алмазов добыто: &b" + diamonds + " шт. &7(Незерита: &8" + debris + " шт.&7)"));
        sender.sendMessage(plugin.color("&7⚔ Убийств: &a" + mobKills + " &7мобов &8| &c" + playerKills + " &7игроков"));
        sender.sendMessage(plugin.color("&7☠ Смертей: &c" + deaths + " &7(K/D: &e" + kd + "&7)"));
        sender.sendMessage(plugin.color("&7🏃 Пройдено пути: &f" + String.format("%.1f", kmTotal) + " км &7(Пешком: &f" + String.format("%.1f", kmWalk) + " км&7, Элитры: &f" + String.format("%.1f", kmFly) + " км&7)"));
        sender.sendMessage(plugin.color("&8&m--------------------------------------------------&r"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
