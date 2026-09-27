package ru.daniar.remotweaks.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.daniar.remotweaks.RemoTweaks;

public class SleepListener implements Listener {

    private final RemoTweaks plugin;

    public SleepListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getSleepManager().checkWorldSleep(player.getWorld());
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getSleepManager().checkWorldSleep(player.getWorld());
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSleepManager().checkWorldSleep(player.getWorld());
        }, 1L);
    }
}
