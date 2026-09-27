package su.remo.tweaks.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import su.remo.tweaks.RemoTweaks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatMentionListener implements Listener {

    private final RemoTweaks plugin;

    public ChatMentionListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-mention.enabled", true)) return;

        Player sender = event.getPlayer();
        Component message = event.message();
        String plain = PlainTextComponentSerializer.plainText().serialize(message);

        List<Player> mentionedPlayers = new ArrayList<>();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(sender)) continue;

            String name = target.getName();
            Pattern pattern = Pattern.compile("(?i)(?<=^|\\s)@?" + Pattern.quote(name) + "(?=$|\\s|[.,!?])");

            if (pattern.matcher(plain).find()) {
                mentionedPlayers.add(target);
                message = message.replaceText(builder -> {
                    builder.match(pattern).replacement(Component.text("@" + name, NamedTextColor.GOLD));
                });
            }
        }

        if (!mentionedPlayers.isEmpty()) {
            event.message(message);

            Bukkit.getScheduler().runTask(plugin, () -> {
                String template = plugin.getConfig().getString("chat-mention.actionbar", "&e🔔 Вас упомянул &6{player} &eв чате!");
                String actionbarMsg = plugin.color(template.replace("{player}", sender.getName()));

                for (Player target : mentionedPlayers) {
                    if (target.isOnline()) {
                        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
                        target.sendActionBar(plugin.color(actionbarMsg));
                    }
                }
            });
        }
    }
}
