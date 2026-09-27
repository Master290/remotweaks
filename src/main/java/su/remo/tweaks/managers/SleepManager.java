package su.remo.tweaks.managers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import su.remo.tweaks.RemoTweaks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SleepManager {

    private final RemoTweaks plugin;
    private final Map<UUID, BukkitTask> timelapseTasks = new ConcurrentHashMap<>();

    public SleepManager(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    public void checkWorldSleep(World world) {
        if (!plugin.getConfig().getBoolean("sleep.enabled", true)) {
            return;
        }

        // Подсчитываем активных игроков в выживании (исключая AFK игроков!)
        List<Player> validPlayers = world.getPlayers().stream()
                .filter(p -> (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                        && (plugin.getAfkManager() == null || !plugin.getAfkManager().isAfk(p)))
                .toList();

        if (validPlayers.isEmpty()) {
            stopTimelapse(world);
            return;
        }

        long sleepingCount = validPlayers.stream().filter(Player::isSleeping).count();
        int total = validPlayers.size();

        int percentage = plugin.getConfig().getInt("sleep.percentage", 20);
        int minPlayers = plugin.getConfig().getInt("sleep.min-players", 1);
        int needed = Math.max(minPlayers, (int) Math.ceil((percentage / 100.0) * total));
        if (needed > total) needed = total;

        boolean isNightOrStorm = (world.getTime() >= 12540 && world.getTime() <= 23990) || world.hasStorm();

        if (!isNightOrStorm || sleepingCount == 0) {
            stopTimelapse(world);
            return;
        }

        if (sleepingCount >= needed) {
            startTimelapse(world, (int) sleepingCount, needed, total);
        } else {
            stopTimelapse(world);
            if (plugin.getConfig().getBoolean("sleep.actionbar.enabled", true)) {
                int currentPercent = (int) Math.round(((double) sleepingCount / total) * 100);
                String msg = plugin.getConfig().getString("sleep.actionbar.sleeping-message", "&eСпят: &6{sleeping}&e/&6{needed} &7(&a{percent}%&7) 🌙")
                        .replace("{sleeping}", String.valueOf(sleepingCount))
                        .replace("{needed}", String.valueOf(needed))
                        .replace("{total}", String.valueOf(total))
                        .replace("{percent}", String.valueOf(currentPercent));

                broadcastActionbar(world, plugin.color(msg));
            }
        }
    }

    private void startTimelapse(World world, int sleeping, int needed, int total) {
        if (timelapseTasks.containsKey(world.getUID())) {
            return; // Уже запущен
        }

        int speed = plugin.getConfig().getInt("sleep.speed", 80);
        boolean clearWeather = plugin.getConfig().getBoolean("sleep.clear-weather", true);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                long currentSleeping = world.getPlayers().stream().filter(Player::isSleeping).count();
                if (currentSleeping == 0) {
                    stopTimelapse(world);
                    return;
                }

                long currentTime = world.getTime();
                long newTime = currentTime + speed;

                if (newTime >= 24000 || (newTime < 12540 && currentTime >= 23000)) {
                    // Наступило утро
                    world.setTime(0);
                    if (clearWeather) {
                        world.setStorm(false);
                        world.setThundering(false);
                    }

                    if (plugin.getConfig().getBoolean("sleep.actionbar.enabled", true)) {
                        String morningMsg = plugin.getConfig().getString("sleep.actionbar.morning-message", "&6Доброе утро! ☀️");
                        broadcastActionbar(world, plugin.color(morningMsg));
                    }

                    for (Player player : world.getPlayers()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                    }

                    stopTimelapse(world);
                    return;
                }

                world.setTime(newTime);

                if (plugin.getConfig().getBoolean("sleep.actionbar.enabled", true)) {
                    String skipMsg = plugin.getConfig().getString("sleep.actionbar.skipping-message", "&aНочь проходит... &7({sleeping} спят) 🌅")
                            .replace("{sleeping}", String.valueOf(currentSleeping))
                            .replace("{needed}", String.valueOf(needed));
                    broadcastActionbar(world, plugin.color(skipMsg));
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        timelapseTasks.put(world.getUID(), task);
    }

    public void stopTimelapse(World world) {
        BukkitTask task = timelapseTasks.remove(world.getUID());
        if (task != null) {
            task.cancel();
        }
    }

    public void cleanupAll() {
        for (BukkitTask task : timelapseTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        timelapseTasks.clear();
    }

    private void broadcastActionbar(World world, String message) {
        for (Player player : world.getPlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }
}
