package su.remo.tweaks.commands;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.listeners.ChestSortListener;

import java.util.Collections;
import java.util.List;

public class SortCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public SortCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.color("&cЭту команду могут использовать только игроки!"));
            return true;
        }

        if (!player.hasPermission("remotweaks.sort")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав!")));
            return true;
        }

        Inventory top = player.getOpenInventory().getTopInventory();
        if (ChestSortListener.isSortableContainer(top)) {
            ChestSortListener.sortInventory(top);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7f, 1.3f);
            player.sendActionBar(plugin.color(plugin.getConfig().getString("chest-sort.messages.sorted-chest", "&a✔ Контейнер отсортирован!")));
            return true;
        }

        boolean all = (args.length > 0 && args[0].equalsIgnoreCase("all"));
        ChestSortListener.sortPlayerInventory(player, all);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7f, 1.3f);
        player.sendActionBar(plugin.color(plugin.getConfig().getString("chest-sort.messages.sorted-inventory", "&a✔ Инвентарь отсортирован!")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("all");
        }
        return Collections.emptyList();
    }
}
