package ru.daniar.remotweaks.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.daniar.remotweaks.RemoTweaks;

import java.util.Collection;

public class HarvestListener implements Listener {

    private final RemoTweaks plugin;

    public HarvestListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCropHarvest(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!plugin.getConfig().getBoolean("harvest.enabled", true)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getBlockData() instanceof Ageable ageable)) return;

        // Только полностью созревший урожай
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.harvest")) return;

        event.setCancelled(true);

        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
        Material seedMaterial = getSeedForCrop(block.getType());

        // Отнимаем 1 семя на повторную посадку
        boolean seedDeducted = false;
        for (ItemStack drop : drops) {
            if (drop.getType() == seedMaterial && !seedDeducted) {
                drop.setAmount(drop.getAmount() - 1);
                seedDeducted = true;
            }
            if (drop.getAmount() > 0) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), drop);
            }
        }

        // Пересаживаем культуру
        ageable.setAge(0);
        block.setBlockData(ageable);

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
        player.swingMainHand();
    }

    private Material getSeedForCrop(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> crop;
        };
    }
}
