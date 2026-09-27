package ru.daniar.remotweaks.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.daniar.remotweaks.RemoTweaks;

public class PotionStackListener implements Listener {

    private final RemoTweaks plugin;
    private static final int TARGET_MAX_STACK = 16;

    public PotionStackListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("potion-stacking.enabled", true);
    }

    public static void applyPotionStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Material m = item.getType();
        if (m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION) {
            Integer currentMax = item.getData(DataComponentTypes.MAX_STACK_SIZE);
            if (currentMax == null || currentMax != TARGET_MAX_STACK) {
                item.setData(DataComponentTypes.MAX_STACK_SIZE, TARGET_MAX_STACK);
            }
        }
    }

    public static void applyInventoryPotions(Inventory inv) {
        if (inv == null) return;
        for (ItemStack item : inv.getContents()) {
            applyPotionStack(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!isEnabled()) return;
        applyPotionStack(event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!isEnabled()) return;
        applyPotionStack(event.getEntity().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isEnabled()) return;
        applyInventoryPotions(event.getInventory());
        applyInventoryPotions(event.getPlayer().getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isEnabled()) return;
        applyPotionStack(event.getCurrentItem());
        applyPotionStack(event.getCursor());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!isEnabled()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            applyInventoryPotions(event.getContents());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;
        applyInventoryPotions(event.getPlayer().getInventory());
        applyInventoryPotions(event.getPlayer().getEnderChest());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isEnabled()) return;

        ItemStack item = event.getItem();
        if (item.getType() == Material.POTION && item.getAmount() > 1) {
            ItemStack remaining = item.clone();
            remaining.setAmount(remaining.getAmount() - 1);
            event.setReplacement(remaining);

            Player player = event.getPlayer();
            Bukkit.getScheduler().runTask(plugin, () -> {
                var leftover = player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            });
        }
    }
}
