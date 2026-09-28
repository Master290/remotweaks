package su.remo.tweaks.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.remo.tweaks.RemoTweaks;

public class PrefixChatListener implements Listener {

    private final RemoTweaks plugin;

    public PrefixChatListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("prefixes.chat-format", true)) {
            return;
        }

        Player player = event.getPlayer();
        Component prefix = plugin.getPrefixManager().getPrefixComponent(player.getUniqueId());

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component result = Component.empty();
            if (prefix != null && !prefix.equals(Component.empty())) {
                result = result.append(prefix);
            }
            return result.append(sourceDisplayName)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(message);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getPrefixManager().updatePlayer(player);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPrefixManager().removePlayerTeam(event.getPlayer());
    }
}
