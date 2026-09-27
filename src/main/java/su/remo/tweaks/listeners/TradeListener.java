package su.remo.tweaks.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.TradeManager;

public class TradeListener implements Listener {

    private final RemoTweaks plugin;

    public TradeListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeManager.TradeSession session = plugin.getTradeManager().getSession(player);
        if (session == null) return;

        if (event.getView().getTopInventory().equals(session.getInventory())) {
            int slot = event.getRawSlot();

            // Клик в нижнем инвентаре (инвентарь игрока)
            if (slot >= 54) {
                // Если игрок с шифтом пытается закинуть вещь
                if (event.isShiftClick()) {
                    session.onItemsChanged();
                }
                return;
            }

            boolean isP1 = player.equals(session.getP1());

            if (isP1) {
                if (slot == 45) {
                    event.setCancelled(true);
                    session.toggleReady(player);
                } else if (TradeManager.P1_SLOTS.contains(slot)) {
                    session.onItemsChanged();
                } else {
                    event.setCancelled(true);
                }
            } else {
                if (slot == 53) {
                    event.setCancelled(true);
                    session.toggleReady(player);
                } else if (TradeManager.P2_SLOTS.contains(slot)) {
                    session.onItemsChanged();
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onTradeClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        TradeManager.TradeSession session = plugin.getTradeManager().getSession(player);
        if (session != null && event.getInventory().equals(session.getInventory())) {
            session.cancelTrade();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TradeManager.TradeSession session = plugin.getTradeManager().getSession(event.getPlayer());
        if (session != null) {
            session.cancelTrade();
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        TradeManager.TradeSession session = plugin.getTradeManager().getSession(event.getEntity());
        if (session != null) {
            session.cancelTrade();
        }
    }
}
