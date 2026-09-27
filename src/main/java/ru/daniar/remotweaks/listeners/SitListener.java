package ru.daniar.remotweaks.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.daniar.remotweaks.RemoTweaks;

public class SitListener implements Listener {

    private final RemoTweaks plugin;

    public SitListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) return;
        if (!player.hasPermission("remotweaks.sit")) return;
        if (!plugin.getConfig().getBoolean("sitting.block-click", true)) return;

        // Только пустой рукой
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        if (clicked.getBlockData() instanceof Stairs || clicked.getBlockData() instanceof Slab) {
            if (plugin.getSitManager().sitOnBlock(player, clicked)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.getSitManager().isSitting(player)) {
                plugin.getSitManager().removeSeat(player);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getSitManager().cleanup(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getSitManager().cleanup(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.getSitManager().isSitting(event.getPlayer())) {
            plugin.getSitManager().cleanup(event.getPlayer().getUniqueId());
        }
    }
}
