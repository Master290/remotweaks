package su.remo.tweaks.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import su.remo.tweaks.RemoTweaks;
import su.remo.tweaks.managers.LockManager;

import java.util.regex.Pattern;

public class LockListener implements Listener {

    private final RemoTweaks plugin;
    private final LockManager lockManager;
    private static final Pattern LOCK_PATTERN = Pattern.compile("(?i)^\\[?(приват|private|замок)\\]?$");

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

        String line0 = PlainTextComponentSerializer.plainText().serialize(line0Comp).trim();
        line0 = ChatColor.stripColor(line0);

        if (!LOCK_PATTERN.matcher(line0).matches()) {
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
            return;
        }

        // Автоматически форматируем табличку
        event.line(0, Component.text("[Приват]", NamedTextColor.AQUA));
        event.line(1, Component.text(player.getName(), NamedTextColor.GRAY));

        for (int i = 2; i <= 3; i++) {
            Component c = event.line(i);
            if (c != null) {
                String name = PlainTextComponentSerializer.plainText().serialize(c).trim();
                name = ChatColor.stripColor(name);
                if (!name.isEmpty()) {
                    event.line(i, Component.text(name, NamedTextColor.GRAY));
                }
            }
        }

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

        if (lockManager.isLockable(clicked)) {
            if (!lockManager.canAccess(player, clicked)) {
                event.setCancelled(true);
                String owner = lockManager.getOwner(clicked);
                player.sendActionBar(plugin.color("&cЭтот контейнер заблокирован игроком &e" + owner + "&c!"));
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
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
                player.sendMessage(plugin.color("&cВы не можете сломать чужой заблокированный контейнер!"));
                return;
            }
        }

        // 2. Попытка сломать саму табличку привата
        if (block.getState() instanceof Sign sign && lockManager.isLockSign(sign)) {
            Block attached = getAttachedBlock(block);
            if (attached != null && !lockManager.isOwner(player, attached)) {
                event.setCancelled(true);
                player.sendMessage(plugin.color("&cВы не можете снять чужую табличку привата!"));
                return;
            }
        }

        // 3. Попытка сломать блок, на котором висит табличка привата
        for (BlockFace face : HORIZONTAL_FACES) {
            Block side = block.getRelative(face);
            if (side.getBlockData() instanceof WallSign ws && ws.getFacing() == face) {
                if (side.getState() instanceof Sign s && lockManager.isLockSign(s)) {
                    if (!lockManager.isOwner(player, block)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.color("&cВы не можете сломать блок, удерживающий табличку привата!"));
                        return;
                    }
                }
            } else if (side.getBlockData() instanceof WallHangingSign whs && whs.getFacing() == face) {
                if (side.getState() instanceof Sign s && lockManager.isLockSign(s)) {
                    if (!lockManager.isOwner(player, block)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.color("&cВы не можете сломать блок, удерживающий табличку привата!"));
                        return;
                    }
                }
            }
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

        // Если источник или цель - заблокированный контейнер
        if (event.getSource().getHolder() instanceof org.bukkit.block.BlockState state) {
            if (lockManager.isLocked(state.getBlock())) {
                // Если перемещение вызвано воронкой, защищаем контейнер
                if (event.getDestination().getHolder() instanceof org.bukkit.block.Hopper) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private Block getAttachedBlock(Block signBlock) {
        if (signBlock.getBlockData() instanceof WallSign ws) {
            return signBlock.getRelative(ws.getFacing().getOppositeFace());
        }
        if (signBlock.getBlockData() instanceof WallHangingSign whs) {
            return signBlock.getRelative(whs.getFacing().getOppositeFace());
        }
        return signBlock.getRelative(BlockFace.DOWN);
    }
}
