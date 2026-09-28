package su.remo.tweaks.commands;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.RulerManager;

import java.util.Collections;
import java.util.List;

public class RulerCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;
    private final RulerManager rulerManager;

    public RulerCommand(RemoTweaks plugin, RulerManager rulerManager) {
        this.plugin = plugin;
        this.rulerManager = rulerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        boolean active = rulerManager.toggleRuler(player);
        if (active) {
            player.sendMessage(plugin.color("&6=== 📏 Режим рулетки активирован ==="));
            player.sendMessage(plugin.color("&f1. Нажмите &aЛКМ &fс палкой или рукой по первому блоку &7(Точка A)"));
            player.sendMessage(plugin.color("&f2. Нажмите &bПКМ &fпо второму блоку &7(Точка B)"));
            player.sendMessage(plugin.color("&7Повторите &e/" + label + " &7для выключения режима."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        } else {
            player.sendMessage(plugin.color("&e📏 Режим рулетки отключен."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
