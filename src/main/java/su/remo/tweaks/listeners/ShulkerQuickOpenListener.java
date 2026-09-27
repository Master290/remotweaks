package su.remo.tweaks.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import su.remo.tweaks.RemoTweaks;

import java.util.*;

public class ShulkerQuickOpenListener implements Listener {

    private final RemoTweaks plugin;
    private final Map<UUID, ShulkerSession> activeSessions = new HashMap<>();

    public static class ShulkerSession {
        private final UUID playerUuid;
        private final EquipmentSlot hand; // null if opened from inventory slot
        private final int slotIndex;     // player inventory slot index (0-35, 40 for offhand)
        private final Inventory inventory;

        public ShulkerSession(UUID playerUuid, EquipmentSlot hand, int slotIndex, Inventory inventory) {
            this.playerUuid = playerUuid;
            this.hand = hand;
            this.slotIndex = slotIndex;
            this.inventory = inventory;
        }

        public boolean isShulkerSlot(int slot) {
            return this.slotIndex == slot;
        }

        public ItemStack getShulkerItem(Player player) {
            if (hand == EquipmentSlot.HAND) {
                return player.getInventory().getItemInMainHand();
            } else if (hand == EquipmentSlot.OFF_HAND) {
                return player.getInventory().getItemInOffHand();
            } else if (slotIndex >= 0 && slotIndex < player.getInventory().getSize()) {
                return player.getInventory().getItem(slotIndex);
            }
            return null;
        }
    }

    public ShulkerQuickOpenListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    public static boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return item.getType().name().endsWith("SHULKER_BOX");
    }

    public static boolean isShulkerBox(Material m) {
        if (m == null) return false;
        return m.name().endsWith("SHULKER_BOX");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("shulker-quick-open.enabled", true)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.shulkeropen")) return;

        ItemStack item = (event.getHand() == EquipmentSlot.OFF_HAND)
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        if (!isShulkerBox(item)) return;

        // Если клик по блоку, требуем приседание (чтобы не блокировать обычную установку блока)
        if (action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()) {
            return;
        }

        event.setCancelled(true);
        int slot = (event.getHand() == EquipmentSlot.OFF_HAND) ? 40 : player.getInventory().getHeldItemSlot();
        openShulker(player, item, event.getHand(), slot);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 1. Быстрое открытие шалкера из инвентаря по Shift + ПКМ
        if (!activeSessions.containsKey(player.getUniqueId())) {
            if (plugin.getConfig().getBoolean("shulker-quick-open.enabled", true) &&
                event.isShiftClick() && event.isRightClick()) {
                ItemStack current = event.getCurrentItem();
                if (isShulkerBox(current) && player.hasPermission("remotweaks.shulkeropen")) {
                    event.setCancelled(true);
                    int slot = event.getSlot();
                    openShulker(player, current, null, slot);
                    return;
                }
            }
            return;
        }

        ShulkerSession session = activeSessions.get(player.getUniqueId());

        // 2. Если открыт шалкер:
        // Запрещаем помещать другие шалкеры внутрь открытого шалкера
        if (event.getRawSlot() < 27) {
            ItemStack cursor = event.getCursor();
            if (isShulkerBox(cursor)) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                return;
            }
            if (event.isShiftClick() && isShulkerBox(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }
        } else {
            // Клик в нижнем инвентаре
            if (event.isShiftClick() && isShulkerBox(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }
            // Запрещаем перемещать сам открытый шалкер
            if (session.isShulkerSlot(event.getSlot())) {
                event.setCancelled(true);
                return;
            }
            // Запрещаем хотбар-свап цифрами (1-9) на слот открытого шалкера
            if (event.getClick() == ClickType.NUMBER_KEY) {
                int hotbarSlot = event.getHotbarButton();
                if (session.isShulkerSlot(hotbarSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        if (isShulkerBox(event.getOldCursor())) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < 27) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        saveAndCloseSession(player, event.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (activeSessions.containsKey(player.getUniqueId())) {
            ShulkerSession session = activeSessions.get(player.getUniqueId());
            if (session.isShulkerSlot(player.getInventory().getHeldItemSlot())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeSessions.containsKey(player.getUniqueId())) {
            saveAndCloseSession(player, activeSessions.get(player.getUniqueId()).inventory);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (activeSessions.containsKey(player.getUniqueId())) {
            saveAndCloseSession(player, activeSessions.get(player.getUniqueId()).inventory);
        }
    }

    private void openShulker(Player player, ItemStack item, EquipmentSlot hand, int slot) {
        if (!(item.getItemMeta() instanceof BlockStateMeta bsm)) return;
        if (!(bsm.getBlockState() instanceof ShulkerBox shulker)) return;

        Component titleComp;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            titleComp = item.getItemMeta().displayName();
        } else {
            titleComp = Component.text(plugin.color("&8Шалкеровый ящик"));
        }

        Inventory inv = Bukkit.createInventory(player, 27, titleComp);
        inv.setContents(shulker.getInventory().getContents());

        activeSessions.put(player.getUniqueId(), new ShulkerSession(player.getUniqueId(), hand, slot, inv));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 0.8f, 1.0f);
    }

    private void saveAndCloseSession(Player player, Inventory openInv) {
        ShulkerSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        ItemStack shulkerItem = session.getShulkerItem(player);
        if (shulkerItem != null && isShulkerBox(shulkerItem)) {
            if (shulkerItem.getItemMeta() instanceof BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof ShulkerBox shulker) {
                    shulker.getInventory().setContents(openInv.getContents());
                    bsm.setBlockState(shulker);
                    shulkerItem.setItemMeta(bsm);
                }
            }
        }
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.8f, 1.0f);
    }

    public void cleanupAll() {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                saveAndCloseSession(player, activeSessions.get(uuid).inventory);
                player.closeInventory();
            }
        }
        activeSessions.clear();
    }
}
