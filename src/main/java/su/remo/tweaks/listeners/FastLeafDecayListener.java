package su.remo.tweaks.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import su.remo.tweaks.RemoTweaks;

import java.util.*;

public class FastLeafDecayListener implements Listener {

    private final RemoTweaks plugin;
    private final Set<Block> scheduledBlocks = new HashSet<>();
    private static final List<BlockFace> NEIGHBORS = List.of(
            BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN
    );

    public FastLeafDecayListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("fast-leaf-decay.enabled", true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled()) return;
        onBlockRemove(event.getBlock(), 4L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!isEnabled()) return;
        onBlockRemove(event.getBlock(), 2L);
    }

    public void onBlockRemove(Block oldBlock, long delay) {
        if (!Tag.LOGS.isTagged(oldBlock.getType()) && !Tag.LEAVES.isTagged(oldBlock.getType())) {
            return;
        }

        List<BlockFace> faces = new ArrayList<>(NEIGHBORS);
        Collections.shuffle(faces);

        for (BlockFace face : faces) {
            Block neighbor = oldBlock.getRelative(face);
            if (!Tag.LEAVES.isTagged(neighbor.getType())) continue;

            if (neighbor.getBlockData() instanceof Leaves leaves) {
                if (leaves.isPersistent()) continue;
                if (scheduledBlocks.contains(neighbor)) continue;

                scheduledBlocks.add(neighbor);
                Bukkit.getScheduler().runTaskLater(plugin, () -> decay(neighbor), delay);
            }
        }
    }

    private void decay(Block block) {
        scheduledBlocks.remove(block);
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return;
        if (!Tag.LEAVES.isTagged(block.getType())) return;

        if (block.getBlockData() instanceof Leaves leaves) {
            if (leaves.isPersistent()) return;
            if (leaves.getDistance() < 7) return;

            LeavesDecayEvent event = new LeavesDecayEvent(block);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            if (plugin.getConfig().getBoolean("fast-leaf-decay.spawn-particles", true)) {
                block.getWorld().spawnParticle(
                        Particle.BLOCK,
                        block.getLocation().add(0.5, 0.5, 0.5),
                        6, 0.2, 0.2, 0.2, 0,
                        block.getType().createBlockData()
                );
            }

            if (plugin.getConfig().getBoolean("fast-leaf-decay.play-sound", true)) {
                block.getWorld().playSound(
                        block.getLocation(),
                        Sound.BLOCK_GRASS_BREAK,
                        SoundCategory.BLOCKS, 0.4f, 1.2f
                );
            }

            block.breakNaturally();
            // Каскадный запуск на соседние листья
            onBlockRemove(block, 2L);
        }
    }

    public void cleanup() {
        scheduledBlocks.clear();
    }
}
