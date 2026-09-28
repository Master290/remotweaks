package su.remo.tweaks.listeners;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.GraveManager;

public class GraveListener implements Listener {

    private final RemoTweaks plugin;
    private final GraveManager graveManager;

    public GraveListener(RemoTweaks plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Location loc = clicked.getLocation();
        if (!graveManager.isGrave(loc)) return;

        GraveManager.GraveData grave = graveManager.getGrave(loc);
        if (grave == null) return;

        Player player = event.getPlayer();
        boolean isOwner = grave.getOwnerUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("remotweaks.admin") || player.isOp();

        if (isOwner || (isAdmin && player.isSneaking())) {
            event.setCancelled(true);
            graveManager.collectGrave(player, grave);
        } else {
            event.setCancelled(true);
            player.sendActionBar(plugin.color("&cЭто могила игрока &e" + grave.getOwnerName() + "&c!"));
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (!graveManager.isGrave(loc)) return;

        GraveManager.GraveData grave = graveManager.getGrave(loc);
        if (grave == null) return;

        Player player = event.getPlayer();
        boolean isOwner = grave.getOwnerUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("remotweaks.admin") || player.isOp();

        if (isOwner || (isAdmin && player.isSneaking())) {
            event.setCancelled(true);
            graveManager.collectGrave(player, grave);
        } else {
            event.setCancelled(true);
            player.sendMessage(plugin.color("&cВы не можете сломать чужую могилу игрока &e" + grave.getOwnerName() + "&c!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> graveManager.isGrave(b.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> graveManager.isGrave(b.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (graveManager.isGrave(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (graveManager.isGrave(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
