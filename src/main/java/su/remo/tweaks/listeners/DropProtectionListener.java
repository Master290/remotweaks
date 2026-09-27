package su.remo.tweaks.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import su.remo.tweaks.RemoTweaks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DropProtectionListener implements Listener {

    private final RemoTweaks plugin;
    private final Map<UUID, Long> confirmDrops = new ConcurrentHashMap<>();

    public DropProtectionListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("drop-protection.enabled", true)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (!isValuable(item)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long last = confirmDrops.get(uuid);
        if (last != null && (now - last) <= 2000L) {
            // Подтверждено повторным нажатием
            confirmDrops.remove(uuid);
        } else {
            event.setCancelled(true);
            confirmDrops.put(uuid, now);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);

            String message = plugin.getConfig().getString("drop-protection.message",
                    "&c[!] Нажмите Q ещё раз в течение 2 сек для подтверждения!");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(plugin.color(message)));
        }
    }

    private boolean isValuable(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        String name = type.name();

        if (name.contains("NETHERITE") || name.contains("DIAMOND")) return true;
        if (type == Material.ELYTRA || type == Material.ENCHANTED_BOOK || type == Material.TRIDENT) return true;
        if (name.equals("MACE") || name.equals("HEAVY_CORE")) return true;

        return item.hasItemMeta() && item.getItemMeta().hasEnchants();
    }
}
