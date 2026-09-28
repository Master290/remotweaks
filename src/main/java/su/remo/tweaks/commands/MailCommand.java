package su.remo.tweaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.MailManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MailCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;
    private final MailManager mailManager;

    public MailCommand(RemoTweaks plugin, MailManager mailManager) {
        this.plugin = plugin;
        this.mailManager = mailManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("read")) {
            handleRead(player, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "send" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.color("&cИспользование: /" + label + " send <игрок> <сообщение>"));
                    return true;
                }
                String targetName = args[1];
                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.color("&cВы не можете отправить письмо самому себе!"));
                    return true;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2) sb.append(" ");
                    sb.append(args[i]);
                }
                String message = sb.toString().trim();
                if (message.isEmpty()) {
                    player.sendMessage(plugin.color("&cСообщение не может быть пустым!"));
                    return true;
                }

                mailManager.sendMail(player.getName(), targetName, message);
                player.sendMessage(plugin.color("&a✔ Письмо успешно отправлено игроку &e" + targetName + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
                return true;
            }

            case "clear" -> {
                mailManager.clearMails(player.getName());
                player.sendMessage(plugin.color("&a✔ Ваш почтовый ящик полностью очищен."));
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.2f);
                return true;
            }

            default -> {
                sendHelp(player, label);
                return true;
            }
        }
    }

    private void handleRead(Player player, String label) {
        List<MailManager.MailItem> mails = mailManager.getMails(player.getName());
        if (mails.isEmpty()) {
            player.sendMessage(plugin.color("&7В вашем почтовом ящике нет писем."));
            return;
        }

        player.sendMessage(plugin.color("&6=== 📬 Ваши входящие письма (" + mails.size() + ") ==="));
        for (MailManager.MailItem m : mails) {
            String status = m.read() ? "&7[Прочитано]" : "&a[НОВОЕ]";
            player.sendMessage(plugin.color("&e#" + m.id() + " " + status + " &fОт: &6" + m.sender() + " &7(" + m.time() + "):"));
            player.sendMessage(plugin.color("   &f" + m.message()));
        }
        player.sendMessage(plugin.color("&7Очистить почту: &e/" + label + " clear"));

        mailManager.markAllRead(player.getName());
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(plugin.color("&6=== 📬 Система почты RemoTweaks ==="));
        player.sendMessage(plugin.color("&e/" + label + " read &7- Прочитать входящие письма"));
        player.sendMessage(plugin.color("&e/" + label + " send <игрок> <сообщение> &7- Отправить письмо"));
        player.sendMessage(plugin.color("&e/" + label + " clear &7- Очистить почтовый ящик"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String s : List.of("read", "send", "clear")) {
                if (s.startsWith(args[0].toLowerCase())) list.add(s);
            }
            return list;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("send")) {
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
