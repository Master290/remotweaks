package su.remo.tweaks.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import su.remo.tweaks.RemoTweaks;

import java.util.*;

public class ChopTreeListener implements Listener {

    private final RemoTweaks plugin;
    private final Set<Block> currentlyBreaking = new HashSet<>();

    public ChopTreeListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("chop-tree.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.choptree")) return;

        // Рубка всего дерева работает только с зажатым Shift
        if (!player.isSneaking()) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isAxe(tool.getType())) return;

        Block initialBlock = event.getBlock();
        if (!isLog(initialBlock.getType())) return;

        // Защита от рекурсии
        if (currentlyBreaking.contains(initialBlock)) return;

        int maxBlocks = plugin.getConfig().getInt("chop-tree.max-blocks", 150);

        // BFS поиск всех соединенных бревен
        List<Block> logsToBreak = new ArrayList<>();
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        boolean foundLeaves = false;

        queue.add(initialBlock);
        visited.add(initialBlock);

        Material logType = initialBlock.getType();

        while (!queue.isEmpty() && logsToBreak.size() < maxBlocks) {
            Block current = queue.poll();
            logsToBreak.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        Block relative = current.getRelative(dx, dy, dz);
                        if (!foundLeaves && isLeaves(relative.getType())) {
                            foundLeaves = true;
                        }

                        if (!visited.contains(relative) && relative.getType() == logType) {
                            visited.add(relative);
                            queue.add(relative);
                        }
                    }
                }
            }
        }

        // Если у скопления бревен нет листвы - считаем это постройкой и не рубим
        if (!foundLeaves) return;

        currentlyBreaking.addAll(logsToBreak);

        // Сортируем снизу вверх
        logsToBreak.sort(Comparator.comparingInt(Block::getY));

        for (Block log : logsToBreak) {
            if (log.equals(initialBlock)) continue;

            if (tool.getType() == Material.AIR) break;

            log.breakNaturally(tool);

            // Запускаем быстрое опадание листвы вокруг сломанного бревна
            if (plugin.getFastLeafDecayListener() != null) {
                plugin.getFastLeafDecayListener().onBlockRemove(log, 2L);
            }

            // Тратим прочность топора
            if (tool.getItemMeta() instanceof Damageable damageable) {
                int damage = damageable.getDamage() + 1;
                damageable.setDamage(damage);
                tool.setItemMeta(damageable);

                if (damage >= tool.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    break;
                }
            }
        }

        currentlyBreaking.removeAll(logsToBreak);
    }

    private boolean isAxe(Material material) {
        return material.name().endsWith("_AXE");
    }

    private boolean isLog(Material material) {
        return Tag.LOGS.isTagged(material) || material.name().endsWith("_LOG") || material.name().endsWith("_STEM");
    }

    private boolean isLeaves(Material material) {
        return Tag.LEAVES.isTagged(material) || material.name().endsWith("_LEAVES") || material.name().endsWith("_WART_BLOCK");
    }
}
