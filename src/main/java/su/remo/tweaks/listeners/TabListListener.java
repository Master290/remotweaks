package su.remo.tweaks.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.remo.tweaks.RemoTweaks;

public class TabListListener implements Listener {

    private final RemoTweaks plugin;

    public TabListListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getTabListManager() != null) {
            plugin.getTabListManager().onPlayerJoin(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getTabListManager() != null) {
            plugin.getTabListManager().onPlayerQuit(event.getPlayer());
        }
    }
}
