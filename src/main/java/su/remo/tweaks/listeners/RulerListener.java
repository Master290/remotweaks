package su.remo.tweaks.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.RulerManager;

public class RulerListener implements Listener {

    private final RemoTweaks plugin;
    private final RulerManager rulerManager;

    public RulerListener(RemoTweaks plugin, RulerManager rulerManager) {
        this.plugin = plugin;
        this.rulerManager = rulerManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        boolean rulerMode = rulerManager.isRulerActive(player);
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean stickSneak = player.isSneaking() && item.getType() == Material.STICK;

        if (!rulerMode && !stickSneak) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            rulerManager.setPointA(player, clicked);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            rulerManager.setPointB(player, clicked);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        rulerManager.cleanup(event.getPlayer().getUniqueId());
    }
}
