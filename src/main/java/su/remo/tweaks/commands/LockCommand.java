package su.remo.tweaks.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LockCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public LockCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        Block target = player.getTargetBlockExact(5);

        switch (sub) {
            case "add" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /" + label + " add <друг>", NamedTextColor.RED));
                    return true;
                }

                if (target == null || (!plugin.getLockManager().isLockable(target) && !(target.getState() instanceof Sign))) {
                    player.sendMessage(Component.text("Посмотрите на ваш заблокированный сундук или табличку привата (в пределах 5 блоков).", NamedTextColor.RED));
                    return true;
                }

                Sign sign = plugin.getLockManager().getLockSign(target);
                if (sign == null) {
                    player.sendMessage(Component.text("Этот контейнер не заблокирован.", NamedTextColor.RED));
                    return true;
                }

                if (!plugin.getLockManager().isOwner(player, target)) {
                    player.sendMessage(Component.text("Вы не являетесь владельцем этого замка!", NamedTextColor.RED));
                    return true;
                }

                String friend = args[1];
                List<String> allowed = plugin.getLockManager().getAllowedUsers(target);
                for (String u : allowed) {
                    if (u.equalsIgnoreCase(friend)) {
                        player.sendMessage(Component.text("Игрок " + friend + " уже имеет доступ к этому замку!", NamedTextColor.YELLOW));
                        return true;
                    }
                }

                boolean added = plugin.getLockManager().addFriend(sign, friend);
                if (added) {
                    player.sendMessage(Component.text("✔ Игрок " + friend + " успешно добавлен в замок!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("На табличке нет свободных строк для добавления друзей (максимум 2 друга).", NamedTextColor.YELLOW));
                }
                return true;
            }

            case "remove", "rem" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /" + label + " remove <друг>", NamedTextColor.RED));
                    return true;
                }

                if (target == null || (!plugin.getLockManager().isLockable(target) && !(target.getState() instanceof Sign))) {
                    player.sendMessage(Component.text("Посмотрите на ваш заблокированный сундук или табличку привата (в пределах 5 блоков).", NamedTextColor.RED));
                    return true;
                }

                Sign sign = plugin.getLockManager().getLockSign(target);
                if (sign == null) {
                    player.sendMessage(Component.text("Этот контейнер не заблокирован.", NamedTextColor.RED));
                    return true;
                }

                if (!plugin.getLockManager().isOwner(player, target)) {
                    player.sendMessage(Component.text("Вы не являетесь владельцем этого замка!", NamedTextColor.RED));
                    return true;
                }

                String friend = args[1];
                boolean removed = plugin.getLockManager().removeFriend(sign, friend);
                if (removed) {
                    player.sendMessage(Component.text("✔ Игрок " + friend + " удален из списка доступа замка.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Игрок " + friend + " не найден в списке доступа этого замка.", NamedTextColor.YELLOW));
                }
                return true;
            }

            case "info" -> {
                if (target == null || (!plugin.getLockManager().isLockable(target) && !(target.getState() instanceof Sign))) {
                    player.sendMessage(Component.text("Посмотрите на контейнер или табличку привата.", NamedTextColor.RED));
                    return true;
                }

                Sign sign = plugin.getLockManager().getLockSign(target);
                if (sign == null) {
                    player.sendMessage(Component.text("Этот контейнер не заблокирован (открыт для всех).", NamedTextColor.YELLOW));
                    return true;
                }

                String owner = plugin.getLockManager().getOwner(target);
                List<String> allowed = plugin.getLockManager().getAllowedUsers(target);

                List<String> friends = new ArrayList<>();
                if (allowed.size() > 1) {
                    for (int i = 1; i < allowed.size(); i++) {
                        friends.add(allowed.get(i));
                    }
                }
                String friendsStr = friends.isEmpty() ? "только владелец" : String.join(", ", friends);

                player.sendMessage(Component.text("=== Информация о замке ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text("Владелец: ", NamedTextColor.YELLOW).append(Component.text(owner != null ? owner : "Неизвестно", NamedTextColor.WHITE)));
                player.sendMessage(Component.text("Друзья с доступом: ", NamedTextColor.YELLOW).append(Component.text(friendsStr, NamedTextColor.GREEN)));
                return true;
            }

            default -> {
                sendHelp(player, label);
                return true;
            }
        }
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(Component.text("=== Защита сундуков и дверей ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Повесьте табличку со строкой [приват] на сундук или дверь.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/" + label + " add <друг> - Добавить друга в замок", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " remove <друг> - Удалить друга из замка", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " info - Проверить информацию о замке", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String sub : List.of("add", "remove", "info")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    list.add(sub);
                }
            }
            return list;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }

        return Collections.emptyList();
    }
}
