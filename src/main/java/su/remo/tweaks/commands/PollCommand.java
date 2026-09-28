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
import su.remo.tweaks.managers.PollManager;
import su.remo.tweaks.managers.PollManager.Poll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PollCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;
    private final PollManager pollManager;

    public PollCommand(RemoTweaks plugin, PollManager pollManager) {
        this.plugin = plugin;
        this.pollManager = pollManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("remotweaks.poll")) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
            listPolls(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "view" -> {
                if (args.length < 2 || !isInteger(args[1])) {
                    sender.sendMessage(plugin.color("&cИспользование: &e/poll view <ID>"));
                    return true;
                }
                int id = Integer.parseInt(args[1]);
                Poll poll = pollManager.getPoll(id);
                if (poll == null) {
                    sender.sendMessage(plugin.color("&cГолосование #" + id + " не найдено."));
                    return true;
                }
                if (sender instanceof Player player) {
                    pollManager.showPoll(player, poll);
                } else {
                    sender.sendMessage("Голосование #" + id + ": " + poll.getQuestion() + " (Всего голосов: " + poll.getTotalVotes() + ")");
                }
                return true;
            }
            case "vote" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.color("&cТолько игроки могут голосовать!"));
                    return true;
                }
                if (args.length < 3 || !isInteger(args[1]) || !isInteger(args[2])) {
                    player.sendMessage(plugin.color("&cИспользование: &e/poll vote <ID> <номер варианта (1, 2, ...)>"));
                    return true;
                }
                int id = Integer.parseInt(args[1]);
                int optNum = Integer.parseInt(args[2]);
                pollManager.vote(player, id, optNum - 1);
                return true;
            }
            case "create", "new" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.color("&cТолько игроки могут создавать голосования!"));
                    return true;
                }
                if (!player.hasPermission("remotweaks.poll.create")) {
                    player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.color("&cИспользование: &e/poll create <вопрос> [| вариант 1 | вариант 2 ...]"));
                    player.sendMessage(plugin.color("&7Пример: &e/poll create Строим хаб в Аду? | На крыше | Внутри"));
                    player.sendMessage(plugin.color("&7Или: &e/poll create Нужен ли мост на спавне? &8(по умолчанию Да/Нет)"));
                    return true;
                }

                int maxActive = plugin.getConfig().getInt("polls.max-active-polls", 10);
                if (pollManager.getActivePolls().size() >= maxActive) {
                    player.sendMessage(plugin.color("&cДостигнут лимит активных голосований на сервере (" + maxActive + "). Дождитесь завершения текущих."));
                    return true;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                String input = sb.toString().trim();

                String question;
                List<String> options = new ArrayList<>();

                if (input.contains("|")) {
                    String[] parts = input.split("\\|");
                    question = parts[0].trim();
                    for (int i = 1; i < parts.length; i++) {
                        String opt = parts[i].trim();
                        if (!opt.isEmpty()) {
                            options.add(opt);
                        }
                    }
                } else {
                    question = input;
                    options.add("Да");
                    options.add("Нет");
                }

                int minLen = plugin.getConfig().getInt("polls.min-question-length", 5);
                if (question.length() < minLen) {
                    player.sendMessage(plugin.color("&cВопрос слишком короткий! Минимум " + minLen + " символов."));
                    return true;
                }
                if (options.size() < 2) {
                    player.sendMessage(plugin.color("&cУкажите как минимум 2 варианта ответа!"));
                    return true;
                }
                if (options.size() > 8) {
                    player.sendMessage(plugin.color("&cМаксимум 8 вариантов ответа в одном голосовании!"));
                    return true;
                }

                long durationMinutes = plugin.getConfig().getLong("polls.default-duration-minutes", 1440L); // 24 часа по умолчанию
                Poll poll = pollManager.createPoll(player, question, options, durationMinutes);
                player.sendMessage(plugin.color("&aГолосование #" + poll.getId() + " успешно создано и объявлено всему серверу!"));
                return true;
            }
            case "end", "close", "stop" -> {
                if (args.length < 2 || !isInteger(args[1])) {
                    sender.sendMessage(plugin.color("&cИспользование: &e/poll end <ID>"));
                    return true;
                }
                int id = Integer.parseInt(args[1]);
                Player player = sender instanceof Player ? (Player) sender : null;
                if (!pollManager.endPoll(id, player)) {
                    sender.sendMessage(plugin.color("&cНе удалось завершить голосование #" + id + "."));
                }
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void listPolls(CommandSender sender) {
        List<Poll> active = pollManager.getActivePolls();
        sender.sendMessage(Component.text("§8§m----------------§r §6🗳️ Голосования сервера §8§m----------------"));

        if (active.isEmpty()) {
            sender.sendMessage(Component.text("§7Сейчас нет активных голосований. Создайте своё: §e/poll create <вопрос>"));
        } else {
            sender.sendMessage(Component.text("§aАктивные голосования:"));
            for (Poll p : active) {
                Component line = Component.text("  • §e#" + p.getId() + " §f«" + p.getQuestion() + "» §7(" + p.getTotalVotes() + " голосов) ")
                        .append(Component.text("§a[Открыть/Голосовать]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .hoverEvent(HoverEvent.showText(Component.text("§eНажмите, чтобы открыть это голосование")))
                                .clickEvent(ClickEvent.runCommand("/poll view " + p.getId())));
                sender.sendMessage(line);
            }
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("§7Создать опрос: §e/poll create <вопрос> [| вар1 | вар2 ...]"));
        sender.sendMessage(Component.text("§7Завершить свой опрос: §e/poll end <ID>"));
        sender.sendMessage(Component.text("§8§m--------------------------------------------------"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("&6🗳️ Команды голосований (/poll):"));
        sender.sendMessage(plugin.color("  &e/poll list &7- список активных голосований"));
        sender.sendMessage(plugin.color("  &e/poll view <ID> &7- открыть голосование с кнопками"));
        sender.sendMessage(plugin.color("  &e/poll vote <ID> <номер> &7- проголосовать"));
        sender.sendMessage(plugin.color("  &e/poll create <вопрос> [| вар1 | вар2] &7- создать опрос"));
        sender.sendMessage(plugin.color("  &e/poll end <ID> &7- завершить опрос и подвести итоги"));
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
            for (String sub : List.of("list", "view", "vote", "create", "end")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    list.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("view") || sub.equals("vote") || sub.equals("end")) {
                for (Poll p : pollManager.getPolls()) {
                    if (sub.equals("view") || p.isActive()) {
                        String idStr = String.valueOf(p.getId());
                        if (idStr.startsWith(args[1])) {
                            list.add(idStr);
                        }
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("vote") && isInteger(args[1])) {
            int id = Integer.parseInt(args[1]);
            Poll p = pollManager.getPoll(id);
            if (p != null) {
                for (int i = 1; i <= p.getOptions().size(); i++) {
                    String optStr = String.valueOf(i);
                    if (optStr.startsWith(args[2])) {
                        list.add(optStr);
                    }
                }
            }
        }
        return list;
    }
}
