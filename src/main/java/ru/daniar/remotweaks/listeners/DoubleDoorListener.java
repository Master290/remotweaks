package ru.daniar.remotweaks.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.daniar.remotweaks.RemoTweaks;

public class DoubleDoorListener implements Listener {

    private final RemoTweaks plugin;
    private static final BlockFace[] HORIZONTALS = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    public DoubleDoorListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDoorInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!plugin.getConfig().getBoolean("double-doors.enabled", true)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getBlockData() instanceof Door door)) return;

        // Исключаем железные двери
        if (block.getType() == Material.IRON_DOOR) return;

        Block bottomBlock = door.getHalf() == Bisected.Half.TOP ? block.getRelative(BlockFace.DOWN) : block;
        if (!(bottomBlock.getBlockData() instanceof Door bottomDoor)) return;

        boolean newOpenState = !bottomDoor.isOpen();

        for (BlockFace face : HORIZONTALS) {
            Block neighbor = bottomBlock.getRelative(face);
            if (neighbor.getType() == bottomBlock.getType() && neighbor.getBlockData() instanceof Door neighborDoor) {
                if (neighborDoor.getFacing() == bottomDoor.getFacing() && neighborDoor.getHinge() != bottomDoor.getHinge()) {
                    if (neighborDoor.isOpen() != newOpenState) {
                        neighborDoor.setOpen(newOpenState);
                        neighbor.setBlockData(neighborDoor);

                        // Также обновляем верхнюю половину соседней двери
                        Block topNeighbor = neighbor.getRelative(BlockFace.UP);
                        if (topNeighbor.getBlockData() instanceof Door topNeighborDoor) {
                            topNeighborDoor.setOpen(newOpenState);
                            topNeighbor.setBlockData(topNeighborDoor);
                        }

                        neighbor.getWorld().playSound(neighbor.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0f, newOpenState ? 1.0f : 0.8f);
                    }
                    break;
                }
            }
        }
    }
}
