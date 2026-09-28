package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
    public static final Pattern LOCK_PATTERN = Pattern.compile(
            "^\\s*\\[?\\s*(приват|private|замок)\\s*\\]?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

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
                || mat == Material.DROPPER || mat == Material.BREWING_STAND || mat == Material.JUKEBOX
                || mat == Material.CHISELED_BOOKSHELF || mat == Material.CRAFTER) {
            return true;
        }

        return false;
    }

    public boolean isLocked(Block block) {
        return getLockSign(block) != null;
    }

    public Sign getLockSign(Block block) {
        if (block == null) return null;

        // Если сам переданный блок является табличкой
        if (block.getState() instanceof Sign sign) {
            if (isLockSign(sign)) {
                return sign;
            }
        }

        List<Block> connected = getConnectedBlocks(block);
        for (Block b : connected) {
            // 1. Проверяем 4 горизонтальных направления
            for (BlockFace face : HORIZONTAL_FACES) {
                Block side = b.getRelative(face);
                if (side.getBlockData() instanceof WallSign wallSign) {
                    if (side.getRelative(wallSign.getFacing().getOppositeFace()).equals(b)) {
                        if (side.getState() instanceof Sign s && isLockSign(s)) {
                            return s;
                        }
                    }
                } else if (side.getBlockData() instanceof WallHangingSign whs) {
                    if (side.getRelative(whs.getFacing().getOppositeFace()).equals(b)) {
                        if (side.getState() instanceof Sign s && isLockSign(s)) {
                            return s;
                        }
                    }
                } else if (side.getState() instanceof Sign s && !(side.getBlockData() instanceof WallSign)) {
                    // Стоячая табличка рядом
                    if (isLockSign(s)) {
                        return s;
                    }
                }
            }

            // 2. Проверяем табличку сверху блока
            Block top = b.getRelative(BlockFace.UP);
            if (top.getState() instanceof Sign s && isLockSign(s)) {
                return s;
            }

            // 3. Проверяем висячую табличку снизу блока
            Block bottom = b.getRelative(BlockFace.DOWN);
            if (bottom.getState() instanceof Sign s && isLockSign(s)) {
                return s;
            }
        }

        return null;
    }

    public boolean isLockSign(Sign sign) {
        if (sign == null) return false;
        for (Side side : Side.values()) {
            Component line0Comp = sign.getSide(side).line(0);
            if (line0Comp == null) continue;
            String line0 = plainSerializer.serialize(line0Comp).trim();
            line0 = ChatColor.stripColor(line0);
            if (LOCK_PATTERN.matcher(line0).matches()) {
                return true;
            }
        }
        return false;
    }

    public Side getLockSide(Sign sign) {
        if (sign == null) return Side.FRONT;
        for (Side side : Side.values()) {
            Component line0Comp = sign.getSide(side).line(0);
            if (line0Comp == null) continue;
            String line0 = ChatColor.stripColor(plainSerializer.serialize(line0Comp).trim());
            if (LOCK_PATTERN.matcher(line0).matches()) {
                return side;
            }
        }
        return Side.FRONT;
    }

    public String getOwner(Block block) {
        Sign sign = getLockSign(block);
        if (sign == null) return null;
        Side side = getLockSide(sign);
        Component line1 = sign.getSide(side).line(1);
        if (line1 == null) return null;
        String owner = plainSerializer.serialize(line1).trim();
        return ChatColor.stripColor(owner);
    }

    public List<String> getAllowedUsers(Block block) {
        List<String> list = new ArrayList<>();
        Sign sign = getLockSign(block);
        if (sign == null) return list;
        Side side = getLockSide(sign);

        for (int i = 1; i <= 3; i++) {
            Component c = sign.getSide(side).line(i);
            if (c == null) continue;
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

        List<String> allowed = getAllowedUsers(block);
        for (String user : allowed) {
            if (user.equalsIgnoreCase(player.getName())) {
                return true;
            }
        }

        // Обход для администратора: только в креативе или при приседании (Shift)
        if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.isSneaking()) {
                return true;
            }
        }

        return false;
    }

    public boolean isOwner(Player player, Block block) {
        String owner = getOwner(block);
        if (owner != null && owner.equalsIgnoreCase(player.getName())) {
            return true;
        }

        // Обход для администратора: только в креативе или при приседании (Shift)
        if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.isSneaking()) {
                return true;
            }
        }

        return false;
    }

    public List<Block> getConnectedBlocks(Block block) {
        List<Block> list = new ArrayList<>();
        list.add(block);

        // Двойной сундук
        if (block.getState() instanceof Chest chest) {
            try {
                InventoryHolder holder = chest.getInventory().getHolder();
                if (holder instanceof DoubleChest doubleChest) {
                    if (chest.getInventory() instanceof DoubleChestInventory) {
                        Chest left = (Chest) doubleChest.getLeftSide();
                        Chest right = (Chest) doubleChest.getRightSide();
                        if (left != null && !left.getBlock().equals(block)) list.add(left.getBlock());
                        if (right != null && !right.getBlock().equals(block)) list.add(right.getBlock());
                    }
                }
            } catch (Exception ignored) {}
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
        if (sign == null) return false;
        Side side = getLockSide(sign);
        for (int i = 2; i <= 3; i++) {
            Component c = sign.getSide(side).line(i);
            String text = c != null ? plainSerializer.serialize(c).trim() : "";
            if (text.isEmpty()) {
                sign.getSide(side).line(i, Component.text(friend, NamedTextColor.GRAY));
                sign.update(true, false);
                return true;
            }
        }
        return false;
    }

    public boolean removeFriend(Sign sign, String friend) {
        if (sign == null) return false;
        Side side = getLockSide(sign);
        for (int i = 2; i <= 3; i++) {
            Component c = sign.getSide(side).line(i);
            String text = c != null ? ChatColor.stripColor(plainSerializer.serialize(c).trim()) : "";
            if (text.equalsIgnoreCase(friend)) {
                sign.getSide(side).line(i, Component.empty());
                sign.update(true, false);
                return true;
            }
        }
        return false;
    }
}
