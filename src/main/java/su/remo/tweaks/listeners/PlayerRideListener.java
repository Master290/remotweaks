package su.remo.tweaks.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import su.remo.tweaks.RemoTweaks;

public class PlayerRideListener implements Listener {

    private final RemoTweaks plugin;

    public PlayerRideListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRide(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        if (!plugin.getConfig().getBoolean("player-riding.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.ride")) return;

        // Садиться на плечи только с зажатым Shift и пустой рукой
        if (!player.isSneaking()) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        if (target.getPassengers().isEmpty() && !target.equals(player)) {
            event.setCancelled(true);
            target.addPassenger(player);
        }
    }
}
