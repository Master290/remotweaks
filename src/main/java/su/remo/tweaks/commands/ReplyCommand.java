package su.remo.tweaks.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public ReplyCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Использование: /" + label + " <сообщение>", NamedTextColor.RED));
            return true;
        }

        UUID targetUuid = plugin.getMsgManager().getLastMessaged(player.getUniqueId());
        if (targetUuid == null) {
            player.sendMessage(Component.text("Вам некому ответить.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Собеседник больше не в сети.", NamedTextColor.RED));
            return true;
        }

        String message = String.join(" ", args);
        plugin.getMsgManager().sendPrivateMessage(player, target, message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
