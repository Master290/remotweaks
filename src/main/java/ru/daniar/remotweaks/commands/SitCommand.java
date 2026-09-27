package ru.daniar.remotweaks.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.daniar.remotweaks.RemoTweaks;

public class SitCommand implements CommandExecutor {

    private final RemoTweaks plugin;

    public SitCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.color("&cЭту команду может выполнять только игрок!"));
            return true;
        }

        if (!player.hasPermission("remotweaks.sit")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав!")));
            return true;
        }

        plugin.getSitManager().sitAnywhere(player);
        return true;
    }
}
