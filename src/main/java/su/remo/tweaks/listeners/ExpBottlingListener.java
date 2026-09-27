package su.remo.tweaks.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.utils.ExpUtil;

public class ExpBottlingListener implements Listener {

    private final RemoTweaks plugin;

    public ExpBottlingListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchantingTableInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;

        if (!plugin.getConfig().getBoolean("exp-bottling.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.bottle")) return;

        // Shift + ПКМ пустой стеклянной бутылочкой
        if (!player.isSneaking()) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() != Material.GLASS_BOTTLE) return;

        event.setCancelled(true);

        int cost = plugin.getConfig().getInt("exp-bottling.cost-per-bottle", 10);
        int playerExp = ExpUtil.getTotalExperience(player);

        if (playerExp < cost) {
            player.sendActionBar(plugin.color("&cУ вас недостаточно опыта! Нужно минимум " + cost + " XP"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        int maxBottles = playerExp / cost;
        int bottlesToConvert = Math.min(handItem.getAmount(), maxBottles);
        int totalCost = bottlesToConvert * cost;

        ExpUtil.setTotalExperience(player, playerExp - totalCost);

        handItem.setAmount(handItem.getAmount() - bottlesToConvert);

        ItemStack expBottles = new ItemStack(Material.EXPERIENCE_BOTTLE, bottlesToConvert);
        var leftovers = player.getInventory().addItem(expBottles);
        for (ItemStack drop : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        player.sendActionBar(plugin.color("&a✔ Создано пузырьков опыта: &e" + bottlesToConvert + " шт. &7(-" + totalCost + " XP)"));
    }
}
