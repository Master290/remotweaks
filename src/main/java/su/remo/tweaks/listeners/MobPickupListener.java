package su.remo.tweaks.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import su.remo.tweaks.RemoTweaks;

public class MobPickupListener implements Listener {

    private final RemoTweaks plugin;

    public MobPickupListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!plugin.getConfig().getBoolean("mob-pickup.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.mobpickup")) return;

        // Только пустой рукой и сидя на корточках (Shift)
        if (!player.isSneaking()) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        Entity target = event.getRightClicked();

        // Подбираем жителей и мирных животных
        if (target instanceof Villager || target instanceof Animals) {
            if (player.getPassengers().isEmpty()) {
                event.setCancelled(true);
                player.addPassenger(target);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                sendActionbar(player, plugin.color("&aВы взяли " + target.getName() + " на руки! &7(Shift + ПКМ по земле, чтобы отпустить)"));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onGroundInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!plugin.getConfig().getBoolean("mob-pickup.enabled", true)) return;

        Player player = event.getPlayer();
        if (player.getPassengers().isEmpty()) return;

        // Опустить моба: Shift + ПКМ пустой рукой по блоку
        if (!player.isSneaking()) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        event.setCancelled(true);

        for (Entity passenger : player.getPassengers()) {
            player.removePassenger(passenger);
            passenger.teleport(clicked.getLocation().add(0.5, 1.0, 0.5));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_PLACE, 0.8f, 1.0f);
        sendActionbar(player, plugin.color("&eВы опустили существо на землю."));
    }

    private void sendActionbar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
