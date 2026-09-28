package su.remo.tweaks.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import su.remo.tweaks.RemoTweaks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Передача предметов из рук в руки (Shift + ПКМ с предметом в руке по другому игроку).
 * Защищает от потери предметов в лаве/дырах и случайного подбора третьими лицами.
 */
public class ItemGiftListener implements Listener {

    private final RemoTweaks plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public ItemGiftListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerGift(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        if (!plugin.getConfig().getBoolean("item-gifting.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.itemgift")) return;
        if (!player.isSneaking()) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR || handItem.getAmount() <= 0) return;

        // Защита от спама и двойного вызова события (400 мс)
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 400L) {
            event.setCancelled(true);
            return;
        }
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);

        ItemStack toGive = handItem.clone();
        HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(toGive);

        if (leftover.isEmpty()) {
            player.getInventory().setItemInMainHand(null);
        } else {
            ItemStack remaining = leftover.get(0);
            int givenAmount = toGive.getAmount() - remaining.getAmount();
            if (givenAmount <= 0) {
                player.sendActionBar(plugin.color("&cИнвентарь " + target.getName() + " переполнен!"));
                return;
            }
            handItem.setAmount(remaining.getAmount());
            player.getInventory().setItemInMainHand(handItem);
        }

        int count = toGive.getAmount() - (leftover.isEmpty() ? 0 : leftover.get(0).getAmount());

        Component itemName = handItem.getItemMeta() != null && handItem.getItemMeta().hasDisplayName()
                ? handItem.getItemMeta().displayName()
                : Component.translatable(toGive.translationKey());

        Component countComp = count > 1 ? Component.text(" x" + count, NamedTextColor.GOLD) : Component.empty();

        player.sendActionBar(Component.text("🎁 Вы передали ", NamedTextColor.GREEN)
                .append(itemName)
                .append(countComp)
                .append(Component.text(" игроку " + target.getName(), NamedTextColor.GREEN)));

        target.sendActionBar(Component.text("🎁 " + player.getName() + " передал вам ", NamedTextColor.GOLD)
                .append(itemName)
                .append(countComp));

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.3f);
        target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
        target.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation().add(0, 0.3, 0), 2, 0.15, 0.15, 0.15, 0.02);
    }
}
