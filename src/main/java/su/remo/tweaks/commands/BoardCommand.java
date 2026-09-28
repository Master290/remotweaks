package su.remo.tweaks.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.BoardManager;
import su.remo.tweaks.managers.BoardManager.BoardAd;

import java.util.ArrayList;
import java.util.List;

public class BoardCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;
    private final BoardManager boardManager;

    public BoardCommand(RemoTweaks plugin, BoardManager boardManager) {
        this.plugin = plugin;
        this.boardManager = boardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("remotweaks.board")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0 || (args.length == 1 && (args[0].equalsIgnoreCase("list") || isInteger(args[0])))) {
            int page = 1;
            if (args.length == 1 && isInteger(args[0])) {
                page = Integer.parseInt(args[0]);
            }
            showBoard(sender, page);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> {
                int page = 1;
                if (args.length >= 2 && isInteger(args[1])) {
                    page = Integer.parseInt(args[1]);
                }
                showBoard(sender, page);
                return true;
            }
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.color("&cТолько игроки могут вешать объявления!"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.color("&cИспользование: &e/board add <текст объявления>"));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                String text = sb.toString().trim();
                int maxLength = plugin.getConfig().getInt("notice-board.max-length", 150);
                if (text.length() > maxLength) {
                    player.sendMessage(plugin.color("&cСлишком длинное объявление! Максимум " + maxLength + " символов (сейчас " + text.length() + ")."));
                    return true;
                }
                if (text.length() < 3) {
                    player.sendMessage(plugin.color("&cСлишком короткое объявление! Минимум 3 символа."));
                    return true;
                }
                if (boardManager.addAd(player, text)) {
                    player.sendMessage(plugin.color("&aВаше объявление успешно размещено на городской доске!"));
                }
                return true;
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2 || !isInteger(args[1])) {
                    sender.sendMessage(plugin.color("&cИспользование: &e/board remove <ID>"));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.color("&cТолько игроки могут использовать эту команду!"));
                    return true;
                }
                int id = Integer.parseInt(args[1]);
                if (boardManager.removeAd(id, player)) {
                    player.sendMessage(plugin.color("&aОбъявление #" + id + " успешно удалено!"));
                } else {
                    player.sendMessage(plugin.color("&cОбъявление #" + id + " не найдено или у вас нет прав на его удаление."));
                }
                return true;
            }
            case "clear" -> {
                if (!sender.hasPermission("remotweaks.board.admin")) {
                    sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.color("&cИспользование: &e/board clear <игрок>"));
                    return true;
                }
                int cleared = boardManager.clearPlayerAds(args[1]);
                sender.sendMessage(plugin.color("&aУдалено &e" + cleared + " &aобъявлений игрока " + args[1] + "."));
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void showBoard(CommandSender sender, int page) {
        List<BoardAd> ads = boardManager.getAds();
        int totalAds = ads.size();
        int pageSize = 5;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalAds / pageSize));
        int safePage = Math.max(1, Math.min(page, totalPages));

        sender.sendMessage(Component.text("§8§m----------------§r §6📜 Доска объявлений §7(Стр. " + safePage + "/" + totalPages + ") §8§m----------------"));

        if (ads.isEmpty()) {
            sender.sendMessage(Component.text("§7На доске пока нет объявлений. Повесьте своё: §e/board add <текст>"));
        } else {
            int start = (safePage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalAds);

            boolean isPlayer = sender instanceof Player;
            Player player = isPlayer ? (Player) sender : null;

            for (int i = start; i < end; i++) {
                BoardAd ad = ads.get(i);
                boolean canDelete = isPlayer && (ad.authorUuid().equals(player.getUniqueId()) || player.hasPermission("remotweaks.board.admin"));

                Component adLine = Component.text("§e#" + ad.id() + " §7[" + ad.timeStr() + "] ")
                        .append(Component.text("§6" + ad.authorName(), NamedTextColor.GOLD)
                                .hoverEvent(HoverEvent.showText(Component.text("§7Нажмите, чтобы отправить сообщение §e" + ad.authorName())))
                                .clickEvent(ClickEvent.suggestCommand("/msg " + ad.authorName() + " ")))
                        .append(Component.text(" §8» §f" + ad.text()));

                if (canDelete) {
                    adLine = adLine.append(Component.text("  §c[✖]", NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(Component.text("§cУдалить это объявление")))
                            .clickEvent(ClickEvent.runCommand("/board remove " + ad.id())));
                }
                sender.sendMessage(adLine);
            }
        }

        // Кнопки перелистывания
        Component footer = Component.empty();
        if (safePage > 1) {
            footer = footer.append(Component.text("§a[◄ Пред.] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("§aПредыдущая страница")))
                    .clickEvent(ClickEvent.runCommand("/board list " + (safePage - 1))));
        } else {
            footer = footer.append(Component.text("§8[◄ Пред.] "));
        }

        footer = footer.append(Component.text(" §7Стр. " + safePage + " из " + totalPages + " §r "));

        if (safePage < totalPages) {
            footer = footer.append(Component.text("§a[След. ►]", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("§aСледующая страница")))
                    .clickEvent(ClickEvent.runCommand("/board list " + (safePage + 1))));
        } else {
            footer = footer.append(Component.text("§8[След. ►]"));
        }

        sender.sendMessage(footer);
        sender.sendMessage(Component.text("§7Разместить объявление: §e/board add <текст>"));
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("&6📜 Доска объявлений:"));
        sender.sendMessage(plugin.color("  &e/board [страница] &7- открыть доску объявлений"));
        sender.sendMessage(plugin.color("  &e/board add <текст> &7- повесить новое объявление"));
        sender.sendMessage(plugin.color("  &e/board remove <ID> &7- удалить своё объявление"));
        if (sender.hasPermission("remotweaks.board.admin")) {
            sender.sendMessage(plugin.color("  &e/board clear <игрок> &7- удалить все объявления игрока"));
        }
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("list", "add", "remove")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    list.add(sub);
                }
            }
            if (sender.hasPermission("remotweaks.board.admin") && "clear".startsWith(args[0].toLowerCase())) {
                list.add("clear");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove") && sender instanceof Player player) {
            for (BoardAd ad : boardManager.getAds()) {
                if (ad.authorUuid().equals(player.getUniqueId()) || player.hasPermission("remotweaks.board.admin")) {
                    String idStr = String.valueOf(ad.id());
                    if (idStr.startsWith(args[1])) {
                        list.add(idStr);
                    }
                }
            }
        }
        return list;
    }
}
