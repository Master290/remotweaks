package su.remo.tweaks.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EmoteCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public EmoteCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Использование: /" + label + " <игрок>", NamedTextColor.RED));
            return true;
        }

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 3000L) {
            player.sendMessage(Component.text("Подождите пару секунд перед следующим взаимодействием!", NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Игрок " + targetName + " не найден в сети.", NamedTextColor.RED));
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(Component.text("Нельзя взаимодействовать с самим собой.", NamedTextColor.YELLOW));
            return true;
        }

        if (!player.getWorld().equals(target.getWorld()) || player.getLocation().distanceSquared(target.getLocation()) > 100.0) {
            player.sendMessage(Component.text("Игрок " + target.getName() + " находится слишком далеко от вас (максимум 10 блоков).", NamedTextColor.RED));
            return true;
        }

        cooldowns.put(player.getUniqueId(), now);
        String action = label.toLowerCase();

        switch (action) {
            case "hug" -> {
                broadcastNearby(player, target, "&d❤ &e" + player.getName() + " &dкрепко обнял &e" + target.getName() + "&d!");
                spawnParticles(player.getLocation(), Particle.HEART, 5);
                spawnParticles(target.getLocation(), Particle.HEART, 5);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }
            case "kiss" -> {
                broadcastNearby(player, target, "&d💋 &e" + player.getName() + " &dнежно поцеловал &e" + target.getName() + "&d!");
                spawnParticles(target.getLocation().add(0, 1.5, 0), Particle.HEART, 8);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            }
            case "highfive", "five" -> {
                broadcastNearby(player, target, "&a✋ &e" + player.getName() + " &aдал пять &e" + target.getName() + "&a!");
                Location mid = player.getLocation().add(target.getLocation()).multiply(0.5).add(0, 1.2, 0);
                mid.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, mid, 10, 0.3, 0.3, 0.3, 0.1);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            }
        }

        return true;
    }

    private void broadcastNearby(Player p1, Player p2, String msg) {
        String formatted = plugin.color(msg);
        for (Player online : p1.getWorld().getPlayers()) {
            if (online.getLocation().distanceSquared(p1.getLocation()) <= 900.0) { // 30 блоков
                online.sendMessage(formatted);
            }
        }
    }

    private void spawnParticles(Location loc, Particle particle, int count) {
        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(particle, loc.clone().add(0, 1.2, 0), count, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
