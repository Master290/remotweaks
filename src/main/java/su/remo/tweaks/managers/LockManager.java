package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.InventoryHolder;
import su.remo.tweaks.RemoTweaks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LockManager {

    private final RemoTweaks plugin;
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    private static final Pattern LOCK_PATTERN = Pattern.compile("(?i)^\\[?(приват|private|замок)\\]?$");

    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public LockManager(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    public boolean isLockable(Block block) {
        if (block == null) return false;
        Material mat = block.getType();
        String name = mat.name();

        if (name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR") || name.endsWith("_FENCE_GATE")) {
            return true;
        }

        if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.BARREL
                || name.endsWith("SHULKER_BOX") || mat == Material.FURNACE || mat == Material.BLAST_FURNACE
                || mat == Material.SMOKER || mat == Material.HOPPER || mat == Material.DISPENSER
                || mat == Material.DROPPER || mat == Material.BREWING_STAND) {
            return true;
        }

        return false;
    }

    public boolean isLocked(Block block) {
        return getLockSign(block) != null;
    }

    public Sign getLockSign(Block block) {
        if (block == null) return null;

        // Если сам блок является табличкой
        if (block.getState() instanceof Sign sign) {
            if (isLockSign(sign)) {
                return sign;
            }
        }

        List<Block> connected = getConnectedBlocks(block);
        for (Block b : connected) {
            // 1. Проверяем настенные таблички на 4 горизонтальных сторонах
            for (BlockFace face : HORIZONTAL_FACES) {
                Block side = b.getRelative(face);
                if (side.getBlockData() instanceof WallSign wallSign) {
                    if (wallSign.getFacing() == face) {
                        if (side.getState() instanceof Sign s && isLockSign(s)) {
                            return s;
                        }
                    }
                } else if (side.getBlockData() instanceof WallHangingSign whs) {
                    if (whs.getFacing() == face) {
                        if (side.getState() instanceof Sign s && isLockSign(s)) {
                            return s;
                        }
                    }
                }
            }

            // 2. Проверяем табличку сверху блока
            Block top = b.getRelative(BlockFace.UP);
            if (top.getState() instanceof Sign s && !(top.getBlockData() instanceof WallSign)) {
                if (isLockSign(s)) {
                    return s;
                }
            }
        }

        return null;
    }

    public boolean isLockSign(Sign sign) {
        if (sign == null) return false;
        Component line0Comp = sign.getSide(Side.FRONT).line(0);
        String line0 = plainSerializer.serialize(line0Comp).trim();
        line0 = ChatColor.stripColor(line0);
        return LOCK_PATTERN.matcher(line0).matches();
    }

    public String getOwner(Block block) {
        Sign sign = getLockSign(block);
        if (sign == null) return null;
        Component line1 = sign.getSide(Side.FRONT).line(1);
        String owner = plainSerializer.serialize(line1).trim();
        return ChatColor.stripColor(owner);
    }

    public List<String> getAllowedUsers(Block block) {
        List<String> list = new ArrayList<>();
        Sign sign = getLockSign(block);
        if (sign == null) return list;

        for (int i = 1; i <= 3; i++) {
            Component c = sign.getSide(Side.FRONT).line(i);
            String name = plainSerializer.serialize(c).trim();
            name = ChatColor.stripColor(name);
            if (!name.isEmpty()) {
                list.add(name);
            }
        }
        return list;
    }

    public boolean canAccess(Player player, Block block) {
        if (!isLocked(block)) return true;
        if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) return true;

        List<String> allowed = getAllowedUsers(block);
        for (String user : allowed) {
            if (user.equalsIgnoreCase(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isOwner(Player player, Block block) {
        if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) return true;
        String owner = getOwner(block);
        return owner != null && owner.equalsIgnoreCase(player.getName());
    }

    public List<Block> getConnectedBlocks(Block block) {
        List<Block> list = new ArrayList<>();
        list.add(block);

        // Двойной сундук
        if (block.getState() instanceof Chest chest) {
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest doubleChest) {
                if (chest.getInventory() instanceof DoubleChestInventory) {
                    Chest left = (Chest) doubleChest.getLeftSide();
                    Chest right = (Chest) doubleChest.getRightSide();
                    if (left != null && !left.getBlock().equals(block)) list.add(left.getBlock());
                    if (right != null && !right.getBlock().equals(block)) list.add(right.getBlock());
                }
            }
        }

        // Дверь (верхняя и нижняя половины)
        if (block.getBlockData() instanceof Door door) {
            if (door.getHalf() == Bisected.Half.BOTTOM) {
                list.add(block.getRelative(BlockFace.UP));
            } else {
                list.add(block.getRelative(BlockFace.DOWN));
            }
        }

        return list;
    }

    public boolean addFriend(Sign sign, String friend) {
        for (int i = 2; i <= 3; i++) {
            String text = plainSerializer.serialize(sign.getSide(Side.FRONT).line(i)).trim();
            if (text.isEmpty()) {
                sign.getSide(Side.FRONT).line(i, Component.text(friend, NamedTextColor.GRAY));
                sign.update();
                return true;
            }
        }
        return false;
    }

    public boolean removeFriend(Sign sign, String friend) {
        for (int i = 2; i <= 3; i++) {
            String text = ChatColor.stripColor(plainSerializer.serialize(sign.getSide(Side.FRONT).line(i)).trim());
            if (text.equalsIgnoreCase(friend)) {
                sign.getSide(Side.FRONT).line(i, Component.empty());
                sign.update();
                return true;
            }
        }
        return false;
    }
}
