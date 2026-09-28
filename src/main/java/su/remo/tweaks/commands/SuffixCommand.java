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

import java.util.*;

public class SuffixCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public SuffixCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                String raw = plugin.getPrefixManager().getRawSuffix(player.getUniqueId());
                if (raw == null || raw.isEmpty()) {
                    sender.sendMessage(Component.text("У вас не установлен кастомный суффикс.", NamedTextColor.YELLOW));
                } else {
                    Component preview = plugin.getPrefixManager().getSuffixComponent(player.getUniqueId());
                    sender.sendMessage(Component.text("Ваш текущий суффикс: ", NamedTextColor.GREEN)
                            .append(Component.text(player.getName(), NamedTextColor.WHITE)).append(preview));
                }
            } else {
                sendHelp(sender, label);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> {
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(Component.text("У вас нет прав для изменения суффиксов!", NamedTextColor.RED));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(Component.text("Использование: /" + label + " set <игрок> <суффикс>", NamedTextColor.RED));
                    sender.sendMessage(Component.text("Пример: /" + label + " set Zoydi &7[&aШахтёр&7]", NamedTextColor.GRAY));
                    return true;
                }

                String targetName = args[1];
                Player onlineTarget = Bukkit.getPlayerExact(targetName);
                UUID uuid = onlineTarget != null ? onlineTarget.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();
                String exactName = onlineTarget != null ? onlineTarget.getName() : targetName;

                String suffix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (!suffix.startsWith(" ")) {
                    suffix = " " + suffix;
                }

                plugin.getPrefixManager().setSuffix(uuid, exactName, suffix);
                Component preview = plugin.getPrefixManager().parseComponent(suffix);

                sender.sendMessage(Component.text("✔ Суффикс для ", NamedTextColor.GREEN)
                        .append(Component.text(exactName, NamedTextColor.GOLD))
                        .append(Component.text(" успешно установлен: ", NamedTextColor.GREEN))
                        .append(Component.text(exactName, NamedTextColor.WHITE)).append(preview));

                if (onlineTarget != null && !onlineTarget.equals(sender)) {
                    onlineTarget.sendMessage(Component.text("Вам установлен новый суффикс: ", NamedTextColor.GREEN)
                            .append(Component.text(exactName, NamedTextColor.WHITE)).append(preview));
                }
                return true;
            }

            case "clear", "remove", "reset" -> {
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(Component.text("У вас нет прав для изменения суффиксов!", NamedTextColor.RED));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(Component.text("Использование: /" + label + " clear <игрок>", NamedTextColor.RED));
                    return true;
                }

                String targetName = args[1];
                UUID uuid = plugin.getPrefixManager().findUuidByName(targetName);
                if (uuid == null) {
                    Player p = Bukkit.getPlayerExact(targetName);
                    uuid = p != null ? p.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();
                }

                boolean removed = plugin.getPrefixManager().clearSuffix(uuid);
                if (removed) {
                    sender.sendMessage(Component.text("✔ Суффикс игрока ", NamedTextColor.GREEN)
                            .append(Component.text(targetName, NamedTextColor.GOLD))
                            .append(Component.text(" был успешно удален.", NamedTextColor.GREEN)));
                } else {
                    sender.sendMessage(Component.text("У игрока " + targetName + " не было установленного суффикса.", NamedTextColor.YELLOW));
                }
                return true;
            }

            case "list" -> {
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(Component.text("У вас нет прав для просмотра списка суффиксов!", NamedTextColor.RED));
                    return true;
                }

                Map<UUID, String> all = plugin.getPrefixManager().getAllSuffixes();
                if (all.isEmpty()) {
                    sender.sendMessage(Component.text("Список установленных суффиксов пуст.", NamedTextColor.YELLOW));
                    return true;
                }

                sender.sendMessage(Component.text("=== Список суффиксов (" + all.size() + ") ===", NamedTextColor.GOLD));
                for (Map.Entry<UUID, String> entry : all.entrySet()) {
                    UUID u = entry.getKey();
                    String name = plugin.getPrefixManager().getStoredPlayerName(u);
                    Component comp = plugin.getPrefixManager().getSuffixComponent(u);

                    sender.sendMessage(Component.text("• ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(name, NamedTextColor.YELLOW))
                            .append(Component.text(" -> ", NamedTextColor.GRAY))
                            .append(Component.text(name, NamedTextColor.WHITE)).append(comp));
                }
                return true;
            }

            case "check", "get" -> {
                String targetName;
                if (args.length >= 2) {
                    targetName = args[1];
                } else if (sender instanceof Player p) {
                    targetName = p.getName();
                } else {
                    sender.sendMessage(Component.text("Укажите ник игрока: /" + label + " check <игрок>", NamedTextColor.RED));
                    return true;
                }

                UUID uuid = plugin.getPrefixManager().findUuidByName(targetName);
                if (uuid == null) {
                    Player p = Bukkit.getPlayerExact(targetName);
                    uuid = p != null ? p.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();
                }

                String raw = plugin.getPrefixManager().getRawSuffix(uuid);
                if (raw == null || raw.isEmpty()) {
                    sender.sendMessage(Component.text("У игрока " + targetName + " нет суффикса.", NamedTextColor.YELLOW));
                } else {
                    Component preview = plugin.getPrefixManager().getSuffixComponent(uuid);
                    sender.sendMessage(Component.text("Суффикс игрока ", NamedTextColor.GREEN)
                            .append(Component.text(targetName, NamedTextColor.GOLD))
                            .append(Component.text(": ", NamedTextColor.GREEN))
                            .append(Component.text(targetName, NamedTextColor.WHITE)).append(preview));
                }
                return true;
            }

            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== Управление суффиксами ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " set <игрок> <суффикс> - Установить суффикс", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " clear <игрок> - Удалить суффикс игрока", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " check <игрок> - Проверить суффикс игрока", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " list - Список всех игроков с суффиксами", NamedTextColor.YELLOW));
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("remotweaks.prefix.admin") || sender.hasPermission("remotweaks.admin") || sender.isOp();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAdminPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String sub : List.of("set", "clear", "check", "list")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    list.add(sub);
                }
            }
            return list;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("check"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return List.of("&7[&aШахтёр&7]", "&7[&6Фермер&7]", "&7[&bМаг&7]", "&e★");
        }

        return Collections.emptyList();
    }
}
