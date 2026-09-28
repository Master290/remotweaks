package su.remo.tweaks.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.remo.tweaks.RemoTweaks;

import java.util.UUID;

public class PrefixChatListener implements Listener {

    private final RemoTweaks plugin;

    public PrefixChatListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean localGlobalEnabled = plugin.getConfig().getBoolean("chat.local-global.enabled", true);
        Component chatChannelTag = Component.empty();

        if (localGlobalEnabled) {
            String plain = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            if (plain.startsWith("!")) {
                // Удаляем ведущий знак !
                event.message(event.message().replaceText(b -> b.match("^!\\s*").replacement("")));
                chatChannelTag = Component.text("[G] ", NamedTextColor.GOLD);
            } else {
                boolean campfireChat = plugin.getCampfireManager() != null && plugin.getCampfireManager().isNearLitCampfire(player);
                if (campfireChat) {
                    chatChannelTag = Component.text("[У костра] ", NamedTextColor.GOLD);

                    double radius = plugin.getConfig().getDouble("campfire-rest.chat-radius", 20.0);
                    double radiusSq = radius * radius;

                    int recipientCount = 0;
                    var it = event.viewers().iterator();
                    while (it.hasNext()) {
                        var viewer = it.next();
                        if (viewer instanceof Player p) {
                            if (!p.getWorld().equals(player.getWorld()) || p.getLocation().distanceSquared(player.getLocation()) > radiusSq) {
                                it.remove();
                            } else {
                                recipientCount++;
                            }
                        }
                    }

                    if (recipientCount <= 1) {
                        player.sendActionBar(plugin.color("&7[У костра] Вас слышат только те, кто греется у огня... (используйте !текст для глобального)"));
                    }
                } else {
                    chatChannelTag = Component.text("[L] ", NamedTextColor.GRAY);

                    double radius = plugin.getConfig().getDouble("chat.local-global.radius", 100.0);
                    double radiusSq = radius * radius;

                    int recipientCount = 0;
                    var it = event.viewers().iterator();
                    while (it.hasNext()) {
                        var viewer = it.next();
                        if (viewer instanceof Player p) {
                            if (!p.getWorld().equals(player.getWorld()) || p.getLocation().distanceSquared(player.getLocation()) > radiusSq) {
                                it.remove();
                            } else {
                                recipientCount++;
                            }
                        }
                    }

                    if (recipientCount <= 1) {
                        player.sendActionBar(plugin.color("&7[L] Вас никто не услышал... (используйте !текст для глобального чата)"));
                    }
                }
            }
        }

        final Component channelPrefix = chatChannelTag;
        Component prefix = plugin.getPrefixManager().getPrefixComponent(uuid);
        Component suffix = plugin.getPrefixManager().getSuffixComponent(uuid);

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component result = Component.empty();
            if (!channelPrefix.equals(Component.empty())) {
                result = result.append(channelPrefix);
            }
            if (prefix != null && !prefix.equals(Component.empty())) {
                result = result.append(prefix);
            }
            result = result.append(sourceDisplayName);
            if (suffix != null && !suffix.equals(Component.empty())) {
                result = result.append(suffix);
            }
            return result.append(Component.text(": ", NamedTextColor.GRAY))
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
