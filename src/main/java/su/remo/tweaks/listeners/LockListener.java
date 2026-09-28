package su.remo.tweaks.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.LockManager;

import java.util.List;

public class LockListener implements Listener {

    private final RemoTweaks plugin;
    private final LockManager lockManager;
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public LockListener(RemoTweaks plugin) {
        this.plugin = plugin;
        this.lockManager = plugin.getLockManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;

        Player player = event.getPlayer();
        Component line0Comp = event.line(0);
        if (line0Comp == null) return;

        String line0 = plainSerializer.serialize(line0Comp).trim();
        line0 = ChatColor.stripColor(line0);

        if (!LockManager.LOCK_PATTERN.matcher(line0).matches()) {
            return;
        }

        Block signBlock = event.getBlock();
        Block attachedBlock = getAttachedBlock(signBlock);

        if (attachedBlock == null || !lockManager.isLockable(attachedBlock)) {
            player.sendMessage(plugin.color("&cТабличку привата можно вешать только на сундуки, двери и другие контейнеры!"));
            return;
        }

        if (lockManager.isLocked(attachedBlock) && !lockManager.isOwner(player, attachedBlock)) {
            player.sendMessage(plugin.color("&cЭтот контейнер уже заблокирован другим игроком (&e" + lockManager.getOwner(attachedBlock) + "&c)!"));
            event.setCancelled(true);
            signBlock.breakNaturally();
            return;
        }

        // Автоматически форматируем табличку
        event.line(0, Component.text("[Приват]", NamedTextColor.AQUA));
        event.line(1, Component.text(player.getName(), NamedTextColor.GRAY));

        for (int i = 2; i <= 3; i++) {
            Component c = event.line(i);
            if (c != null) {
                String name = plainSerializer.serialize(c).trim();
                name = ChatColor.stripColor(name);
                if (!name.isEmpty()) {
                    event.line(i, Component.text(name, NamedTextColor.GRAY));
                }
            }
        }

        // Защищаем табличку от редактирования (wax)
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (signBlock.getState() instanceof Sign s) {
                s.setWaxed(true);
                s.update(true, false);
            }
        });

        player.sendMessage(plugin.color("&a✔ Замок успешно установлен на контейнер!"));
        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Player player = event.getPlayer();

        // 1. Клик по самой табличке привата
        if (clicked.getState() instanceof Sign sign && lockManager.isLockSign(sign)) {
            Block attached = getAttachedBlock(clicked);
            if (attached != null && lockManager.isLockable(attached)) {
                if (!lockManager.canAccess(player, attached)) {
                    event.setCancelled(true);
                    String owner = lockManager.getOwner(attached);
                    if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) {
                        player.sendActionBar(plugin.color("&cЗаблокировано игроком &e" + owner + " &7[Shift+ПКМ: обход]"));
                    } else {
                        player.sendActionBar(plugin.color("&cЭтот контейнер заблокирован игроком &e" + owner + "&c!"));
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                    return;
                }

                // Если есть доступ, открываем инвентарь прикрепленного контейнера
                if (attached.getState() instanceof org.bukkit.inventory.InventoryHolder holder) {
                    event.setCancelled(true);
                    if (player.isSneaking() && (player.hasPermission("remotweaks.lock.admin") || player.isOp())
                            && !lockManager.getAllowedUsers(attached).contains(player.getName())) {
                        player.sendActionBar(plugin.color("&6[Админ-обход] &eВы открыли заблокированный контейнер игрока &6" + lockManager.getOwner(attached)));
                    }
                    player.openInventory(holder.getInventory());
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                    return;
                }
            }
        }

        // 2. Клик по контейнеру
        if (lockManager.isLockable(clicked)) {
            if (!lockManager.canAccess(player, clicked)) {
                event.setCancelled(true);
                String owner = lockManager.getOwner(clicked);
                if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) {
                    player.sendActionBar(plugin.color("&cЗаблокировано игроком &e" + owner + " &7[Shift+ПКМ: обход]"));
                } else {
                    player.sendActionBar(plugin.color("&cЭтот контейнер заблокирован игроком &e" + owner + "&c!"));
                }
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            } else if (player.isSneaking() && (player.hasPermission("remotweaks.lock.admin") || player.isOp()) && lockManager.isLocked(clicked)) {
                List<String> allowed = lockManager.getAllowedUsers(clicked);
                if (!allowed.contains(player.getName())) {
                    player.sendActionBar(plugin.color("&6[Админ-обход] &eВы открыли заблокированный контейнер игрока &6" + lockManager.getOwner(clicked)));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        // 1. Попытка сломать заблокированный контейнер
        if (lockManager.isLockable(block) && lockManager.isLocked(block)) {
            if (!lockManager.isOwner(player, block)) {
                event.setCancelled(true);
                if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) {
                    player.sendMessage(plugin.color("&cЭто чужой контейнер! Зажмите Shift для админ-сноса."));
                } else {
                    player.sendMessage(plugin.color("&cВы не можете сломать чужой заблокированный контейнер!"));
                }
                return;
            } else if (player.isSneaking() && (player.hasPermission("remotweaks.lock.admin") || player.isOp())) {
                String owner = lockManager.getOwner(block);
                if (owner != null && !owner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.color("&6[Админ-обход] &eВы сломали контейнер игрока &6" + owner));
                }
            }
        }

        // 2. Попытка сломать саму табличку привата
        if (block.getState() instanceof Sign sign && lockManager.isLockSign(sign)) {
            Block attached = getAttachedBlock(block);
            boolean canBreak;
            if (attached != null && lockManager.isLockable(attached)) {
                canBreak = lockManager.isOwner(player, attached);
            } else {
                canBreak = lockManager.isOwner(player, block);
            }

            if (!canBreak) {
                event.setCancelled(true);
                if (player.hasPermission("remotweaks.lock.admin") || player.isOp()) {
                    player.sendMessage(plugin.color("&cЭто чужая табличка привата! Зажмите Shift для админ-сноса."));
                } else {
                    player.sendMessage(plugin.color("&cВы не можете снять чужую табличку привата!"));
                }
                return;
            }
        }

        // 3. Попытка сломать блок, на котором держится настенная табличка привата
        for (BlockFace face : HORIZONTAL_FACES) {
            Block side = block.getRelative(face);
            if (side.getBlockData() instanceof WallSign ws && side.getRelative(ws.getFacing().getOppositeFace()).equals(block)) {
                if (side.getState() instanceof Sign s && lockManager.isLockSign(s)) {
                    if (!lockManager.isOwner(player, block)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.color("&cВы не можете сломать блок, удерживающий табличку привата!"));
                        return;
                    }
                }
            } else if (side.getBlockData() instanceof WallHangingSign whs && side.getRelative(whs.getFacing().getOppositeFace()).equals(block)) {
                if (side.getState() instanceof Sign s && lockManager.isLockSign(s)) {
                    if (!lockManager.isOwner(player, block)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.color("&cВы не можете сломать блок, удерживающий табличку привата!"));
                        return;
                    }
                }
            }
        }

        // 4. Попытка сломать блок под стоячей табличкой привата
        Block top = block.getRelative(BlockFace.UP);
        if (top.getState() instanceof Sign s && !(top.getBlockData() instanceof WallSign) && lockManager.isLockSign(s)) {
            if (!lockManager.isOwner(player, block)) {
                event.setCancelled(true);
                player.sendMessage(plugin.color("&cВы не можете сломать блок, удерживающий табличку привата!"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;

        Block placed = event.getBlockPlaced();
        Player player = event.getPlayer();

        // Запрещаем соединять одинарный сундук с чужим заблокированным сундуком
        if (placed.getType() == Material.CHEST || placed.getType() == Material.TRAPPED_CHEST) {
            for (BlockFace face : HORIZONTAL_FACES) {
                Block neighbor = placed.getRelative(face);
                if (neighbor.getType() == placed.getType() && lockManager.isLocked(neighbor)) {
                    if (!lockManager.canAccess(player, neighbor)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.color("&cВы не можете объединить сундук с чужим заблокированным сундуком!"));
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;
        for (Block b : event.getBlocks()) {
            if (lockManager.isLocked(b) || (b.getState() instanceof Sign s && lockManager.isLockSign(s))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;
        for (Block b : event.getBlocks()) {
            if (lockManager.isLocked(b) || (b.getState() instanceof Sign s && lockManager.isLockSign(s))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;
        Block block = event.getBlock();
        if (lockManager.isLockable(block) && lockManager.isLocked(block)) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;
        event.blockList().removeIf(b -> lockManager.isLocked(b) || (b.getState() instanceof Sign s && lockManager.isLockSign(s)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;
        event.blockList().removeIf(b -> lockManager.isLocked(b) || (b.getState() instanceof Sign s && lockManager.isLockSign(s)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!plugin.getConfig().getBoolean("locks.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("locks.protect-hoppers", true)) return;

        org.bukkit.inventory.InventoryHolder sourceHolder = event.getSource().getHolder();
        if (sourceHolder instanceof org.bukkit.block.DoubleChest doubleChest) {
            if (lockManager.isLocked(doubleChest.getLocation().getBlock())) {
                if (event.getDestination().getHolder() instanceof org.bukkit.block.Hopper) {
                    event.setCancelled(true);
                }
            }
        } else if (sourceHolder instanceof org.bukkit.block.BlockState state) {
            if (lockManager.isLocked(state.getBlock())) {
                if (event.getDestination().getHolder() instanceof org.bukkit.block.Hopper) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public Block getAttachedBlock(Block signBlock) {
        if (signBlock.getBlockData() instanceof WallSign ws) {
            return signBlock.getRelative(ws.getFacing().getOppositeFace());
        }
        if (signBlock.getBlockData() instanceof WallHangingSign whs) {
            return signBlock.getRelative(whs.getFacing().getOppositeFace());
        }

        // Стоячая табличка: проверяем блок снизу
        Block down = signBlock.getRelative(BlockFace.DOWN);
        if (lockManager.isLockable(down)) {
            return down;
        }

        // Также проверяем 4 соседние стороны, если табличка стоит рядом
        for (BlockFace face : HORIZONTAL_FACES) {
            Block side = signBlock.getRelative(face);
            if (lockManager.isLockable(side)) {
                return side;
            }
        }

        return down;
    }
}
