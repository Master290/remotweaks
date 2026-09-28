package su.remo.tweaks.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlowCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    private static final List<String> COLORS = List.of(
            "gold", "yellow", "aqua", "green", "red", "blue",
            "light_purple", "dark_purple", "dark_aqua", "dark_green",
            "dark_red", "dark_blue", "white", "gray", "dark_gray", "black"
    );

    public GlowCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        if (!player.hasPermission("remotweaks.glow")) {
            player.sendMessage(Component.text("У вас нет прав для использования свечения!", NamedTextColor.RED));
            return true;
        }

        boolean currentGlowing = plugin.getPrefixManager().isGlowing(player.getUniqueId());

        if (args.length == 0) {
            // Переключаем текущее состояние
            boolean newState = !currentGlowing;
            String color = plugin.getPrefixManager().getGlowColorName(player.getUniqueId());
            plugin.getPrefixManager().setGlow(player.getUniqueId(), player.getName(), newState, color);

            if (newState) {
                player.sendMessage(Component.text("✔ Подсветка персонажа включена! (Цвет: " + color + ")", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("✔ Подсветка персонажа отключена.", NamedTextColor.YELLOW));
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("off") || sub.equals("disable") || sub.equals("stop")) {
            plugin.getPrefixManager().setGlow(player.getUniqueId(), player.getName(), false, null);
            player.sendMessage(Component.text("✔ Подсветка персонажа отключена.", NamedTextColor.YELLOW));
            return true;
        }

        if (COLORS.contains(sub)) {
            plugin.getPrefixManager().setGlow(player.getUniqueId(), player.getName(), true, sub);
            NamedTextColor col = plugin.getPrefixManager().getGlowNamedColor(player.getUniqueId());
            player.sendMessage(Component.text("✔ Цвет свечения установлен на ", NamedTextColor.GREEN)
                    .append(Component.text(sub, col))
                    .append(Component.text("!", NamedTextColor.GREEN)));
            return true;
        }

        player.sendMessage(Component.text("Неизвестный цвет или параметр. Доступные цвета: ", NamedTextColor.RED));
        player.sendMessage(Component.text(String.join(", ", COLORS), NamedTextColor.GRAY));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("off");
            list.addAll(COLORS);

            List<String> matches = new ArrayList<>();
            for (String c : list) {
                if (c.startsWith(args[0].toLowerCase())) {
                    matches.add(c);
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
