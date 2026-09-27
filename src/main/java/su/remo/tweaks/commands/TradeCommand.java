package su.remo.tweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradeCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public TradeCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.color("&cКоманду может выполнять только игрок!"));
            return true;
        }

        if (!player.hasPermission("remotweaks.trade")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав!")));
            return true;
        }

        if (!plugin.getConfig().getBoolean("trade.enabled", true)) {
            player.sendMessage(plugin.color("&cСистема обмена отключена!"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.color("&6[RemoTweaks] &fИспользование: &e/trade <игрок>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.color("&cИгрок не найден или не в сети!"));
            return true;
        }

        plugin.getTradeManager().handleTradeCommand(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName()) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
