package su.remo.tweaks.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import su.remo.tweaks.RemoTweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoTweaksCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public RemoTweaksCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("remotweaks.admin")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав!")));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.reload-success", "&aКонфигурация RemoTweaks успешно перезагружена!")));
            return true;
        }

        sender.sendMessage(plugin.color("&6[RemoTweaks] &fИспользуйте: &e/" + label + " reload &fдля перезагрузки конфига."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("remotweaks.admin")) {
            List<String> list = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                list.add("reload");
            }
            return list;
        }
        return Collections.emptyList();
    }
}
