package su.remo.tweaks.commands;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DiceCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public DiceCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        String cmd = label.toLowerCase();
        if (cmd.equals("coin") || cmd.equals("flip") || cmd.equals("toss")) {
            handleCoin(player);
            return true;
        }

        // roll / dice
        int max = 100;
        if (args.length > 0) {
            try {
                max = Integer.parseInt(args[0]);
                if (max < 2 || max > 1_000_000) {
                    player.sendMessage(plugin.color("&cУкажите число от 2 до 1 000 000!"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.color("&cНекорректное число: &e" + args[0]));
                return true;
            }
        }

        handleRoll(player, max);
        return true;
    }

    private void handleRoll(Player player, int max) {
        int result = ThreadLocalRandom.current().nextInt(1, max + 1);
        String msg = plugin.color("&d🎲 &e" + player.getName() + " &fбросает кубик &7(1-" + max + ")&f... Выпало: &a&l" + result + "&f!");

        broadcastNearby(player, msg, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    private void handleCoin(Player player) {
        boolean heads = ThreadLocalRandom.current().nextBoolean();
        String resultText = heads ? "&a&lОрёл" : "&b&lРешка";
        String msg = plugin.color("&6🪙 &e" + player.getName() + " &fподбрасывает монетку... Выпало: " + resultText + "&f!");

        broadcastNearby(player, msg, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.5f);
    }

    private void broadcastNearby(Player source, String message, Sound sound, float vol, float pitch) {
        Location loc = source.getLocation();
        double radius = 35.0;
        double radiusSq = radius * radius;

        int count = 0;
        for (Player p : source.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= radiusSq) {
                p.sendMessage(message);
                p.playSound(loc, sound, vol, pitch);
                count++;
            }
        }

        if (count <= 1) {
            source.sendMessage(plugin.color("&7(Поблизости никого нет, кто мог бы это увидеть)"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && (alias.equalsIgnoreCase("roll") || alias.equalsIgnoreCase("dice"))) {
            return List.of("6", "20", "100");
        }
        return Collections.emptyList();
    }
}
