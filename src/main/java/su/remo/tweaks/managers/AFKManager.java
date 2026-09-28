package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import su.remo.tweaks.RemoTweaks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKManager {

    private final RemoTweaks plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask checkTask;

    public AFKManager(RemoTweaks plugin) {
        this.plugin = plugin;
        startCheckTask();
    }

    private void startCheckTask() {
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("afk.enabled", true)) return;

            long timeout = plugin.getConfig().getLong("afk.timeout-seconds", 300) * 1000L;
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                long last = lastActivity.computeIfAbsent(uuid, k -> now);

                if (now - last >= timeout && !afkPlayers.contains(uuid)) {
                    setAfk(player, true);
                }
            }
        }, 100L, 100L); // каждые 5 секунд
    }

    public void updateActivity(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        if (afkPlayers.contains(uuid)) {
            setAfk(player, false);
        }
    }

    public void setAfk(Player player, boolean afk) {
        UUID uuid = player.getUniqueId();
        if (afk) {
            afkPlayers.add(uuid);
            if (plugin.getTabListManager() != null) {
                plugin.getTabListManager().updatePlayerName(player);
            } else {
                player.playerListName(Component.text("[AFK] ", NamedTextColor.GRAY)
                        .append(Component.text(player.getName(), NamedTextColor.WHITE)));
            }

            Bukkit.broadcastMessage(plugin.color("&7[RemoTweaks] &e" + player.getName() + " &7отошел от компьютера (AFK)."));
        } else {
            afkPlayers.remove(uuid);
            if (plugin.getTabListManager() != null) {
                plugin.getTabListManager().updatePlayerName(player);
            } else {
                player.playerListName(null);
            }

            Bukkit.broadcastMessage(plugin.color("&7[RemoTweaks] &e" + player.getName() + " &aвернулся в игру!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.2f);
        }

        // Проверяем ночной сон, так как статус активных игроков изменился
        plugin.getSleepManager().checkWorldSleep(player.getWorld());
    }

    public boolean isAfk(Player player) {
        return afkPlayers.contains(player.getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        lastActivity.remove(uuid);
        afkPlayers.remove(uuid);
    }

    public void cleanup() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        afkPlayers.clear();
        lastActivity.clear();
    }
}
