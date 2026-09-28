package su.remo.tweaks.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.Collections;
import java.util.List;

public class SocialSpyCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public SocialSpyCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        if (!player.hasPermission("remotweaks.socialspy") && !player.isOp()) {
            player.sendMessage(Component.text("У вас нет прав для использования SocialSpy!", NamedTextColor.RED));
            return true;
        }

        boolean enabled = plugin.getMsgManager().toggleSocialSpy(player.getUniqueId());
        if (enabled) {
            player.sendMessage(Component.text("✔ Режим SocialSpy включен (вы видите все личные сообщения).", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✔ Режим SocialSpy отключен.", NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
