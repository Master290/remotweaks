package ru.daniar.remotweaks.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.daniar.remotweaks.RemoTweaks;

import java.util.*;

public class ChestSortListener implements Listener {

    private final RemoTweaks plugin;

    public ChestSortListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getConfig().getBoolean("chest-sort.enabled", true)) return;

        Inventory top = event.getView().getTopInventory();
        if (!isSortableContainer(top)) return;

        // Игнорируем кастомные меню плагина
        String title = event.getView().getTitle();
        if (title.contains("Редактор стойки") || title.contains("Обмен:")) return;

        boolean trigger = false;

        // 1. Клик колесиком мыши (средней кнопкой)
        if (event.getClick() == ClickType.MIDDLE) {
            trigger = true;
        }
        // 2. Клик за пределами окна инвентаря
        else if (event.getSlotType() == InventoryType.SlotType.OUTSIDE || event.getRawSlot() == -999) {
            trigger = true;
        }
        // 3. Shift + клик или двойной клик по пустому слоту контейнера
        else if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()) {
            ItemStack current = event.getCurrentItem();
            boolean isEmptySlot = (current == null || current.getType() == Material.AIR);
            if (isEmptySlot && (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK)) {
                trigger = true;
            }
        }

        if (trigger) {
            if (!player.hasPermission("remotweaks.sort")) return;

            // Не сортируем, если на курсоре висит предмет, чтобы избежать коллизий
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                return;
            }

            event.setCancelled(true);
            sortInventory(top);

            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7f, 1.3f);
            player.sendActionBar(plugin.color(plugin.getConfig().getString("chest-sort.messages.sorted-chest", "&a✔ Контейнер отсортирован!")));
        }
    }

    public static boolean isSortableContainer(Inventory inv) {
        if (inv == null) return false;
        InventoryType type = inv.getType();
        return type == InventoryType.CHEST ||
               type == InventoryType.BARREL ||
               type == InventoryType.SHULKER_BOX ||
               type == InventoryType.ENDER_CHEST;
    }

    public static void sortInventory(Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                items.add(item.clone());
            }
        }

        List<ItemStack> consolidated = consolidateItems(items);
        sortItemList(consolidated);

        inv.clear();
        for (int i = 0; i < consolidated.size() && i < inv.getSize(); i++) {
            inv.setItem(i, consolidated.get(i));
        }
    }

    public static void sortPlayerInventory(Player player, boolean includeHotbar) {
        Inventory inv = player.getInventory();
        int startSlot = includeHotbar ? 0 : 9;
        int endSlot = 36; // Слоты основного инвентаря 0-35

        List<ItemStack> items = new ArrayList<>();
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                items.add(item.clone());
            }
        }

        List<ItemStack> consolidated = consolidateItems(items);
        sortItemList(consolidated);

        for (int i = startSlot; i < endSlot; i++) {
            inv.setItem(i, null);
        }

        for (int i = 0; i < consolidated.size() && (startSlot + i) < endSlot; i++) {
            inv.setItem(startSlot + i, consolidated.get(i));
        }
    }

    private static List<ItemStack> consolidateItems(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : items) {
            for (ItemStack target : result) {
                if (target.isSimilar(item) && target.getAmount() < target.getMaxStackSize()) {
                    int space = target.getMaxStackSize() - target.getAmount();
                    int toAdd = Math.min(space, item.getAmount());
                    target.setAmount(target.getAmount() + toAdd);
                    item.setAmount(item.getAmount() - toAdd);
                    if (item.getAmount() <= 0) break;
                }
            }
            if (item.getAmount() > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private static void sortItemList(List<ItemStack> items) {
        items.sort((a, b) -> {
            int catA = getItemCategory(a.getType());
            int catB = getItemCategory(b.getType());
            if (catA != catB) {
                return Integer.compare(catA, catB);
            }

            int typeCompare = a.getType().name().compareTo(b.getType().name());
            if (typeCompare != 0) {
                return typeCompare;
            }

            String nameA = (a.hasItemMeta() && a.getItemMeta().hasDisplayName()) ? a.getItemMeta().getDisplayName() : "";
            String nameB = (b.hasItemMeta() && b.getItemMeta().hasDisplayName()) ? b.getItemMeta().getDisplayName() : "";
            int nameCompare = nameA.compareTo(nameB);
            if (nameCompare != 0) {
                return nameCompare;
            }

            return Integer.compare(b.getAmount(), a.getAmount());
        });
    }

    private static int getItemCategory(Material m) {
        String name = m.name();

        // 1. Оружие, броня и инструменты
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE") ||
            name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_HELMET") ||
            name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
            m == Material.BOW || m == Material.CROSSBOW || m == Material.TRIDENT || m == Material.MACE ||
            m == Material.SHIELD || m == Material.ELYTRA || m == Material.SHEARS || m == Material.FISHING_ROD ||
            m == Material.FLINT_AND_STEEL || m == Material.SPYGLASS) {
            return 1;
        }

        // 2. Минералы, руды, драгоценности
        if (name.contains("ORE") || name.endsWith("_INGOT") || name.endsWith("_RAW") || name.startsWith("RAW_") ||
            m == Material.NETHERITE_INGOT || m == Material.NETHERITE_SCRAP ||
            m == Material.DIAMOND || m == Material.EMERALD || m == Material.GOLD_INGOT ||
            m == Material.IRON_INGOT || m == Material.COPPER_INGOT || m == Material.COAL ||
            m == Material.CHARCOAL || m == Material.LAPIS_LAZULI || m == Material.REDSTONE ||
            m == Material.QUARTZ || m == Material.AMETHYST_SHARD) {
            return 2;
        }

        // 3. Строительные блоки и природные материалы
        if (m.isBlock()) {
            return 3;
        }

        // 4. Еда и растения
        if (m.isEdible() || name.endsWith("_SEEDS") || name.endsWith("_SAPLING") ||
            m == Material.WHEAT || m == Material.SUGAR_CANE || m == Material.BAMBOO) {
            return 4;
        }

        // 5. Зелья и алхимия
        if (name.contains("POTION") || m == Material.GLASS_BOTTLE || m == Material.BREWING_STAND ||
            m == Material.BLAZE_POWDER || m == Material.BLAZE_ROD || m == Material.GHAST_TEAR ||
            m == Material.MAGMA_CREAM || m == Material.FERMENTED_SPIDER_EYE || m == Material.NETHER_WART) {
            return 5;
        }

        // 6. Прочее
        return 6;
    }
}
