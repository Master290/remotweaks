package su.remo.tweaks.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import su.remo.tweaks.RemoTweaks;

public class ChatShowItemListener implements Listener {

    private final RemoTweaks plugin;

    public ChatShowItemListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-show-item.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.showitem")) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        Component itemComp;
        if (item.getType() == Material.AIR) {
            itemComp = Component.text("[Пустая рука]", NamedTextColor.GRAY);
        } else {
            // item.displayName() уже содержит квадратные скобки и форматирование предмета
            itemComp = item.displayName().hoverEvent(item.asHoverEvent());
        }

        Component modified = event.message()
                .replaceText(builder -> builder.matchLiteral("[item]").replacement(itemComp))
                .replaceText(builder -> builder.matchLiteral("[hand]").replacement(itemComp));

        event.message(modified);
    }
}
