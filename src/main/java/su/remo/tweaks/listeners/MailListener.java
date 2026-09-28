package su.remo.tweaks.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.MailManager;

public class MailListener implements Listener {

    private final RemoTweaks plugin;
    private final MailManager mailManager;

    public MailListener(RemoTweaks plugin, MailManager mailManager) {
        this.plugin = plugin;
        this.mailManager = mailManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Небольшая задержка, чтобы сообщение не потерялось при входе
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            int unread = mailManager.getUnreadCount(player.getName());
            if (unread > 0) {
                player.sendMessage(plugin.color("&e📬 У вас есть &6" + unread + " &eнепрочитанных писем! Напишите &a/mail read &eдля прочтения."));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }
        }, 40L);
    }
}
