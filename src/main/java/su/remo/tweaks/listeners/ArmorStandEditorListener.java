package su.remo.tweaks.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.EulerAngle;
import su.remo.tweaks.RemoTweaks;

import java.util.*;

public class ArmorStandEditorListener implements Listener {

    private final RemoTweaks plugin;
    private final Map<UUID, ArmorStand> openEditors = new HashMap<>();

    public ArmorStandEditorListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;

        // Игнорируем стулья плагина
        if (stand.getScoreboardTags().contains("remotweaks_seat")) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();

        // 1. Быстрая смена брони по Shift + ПКМ с предметом брони в руке
        boolean swapEnabled = plugin.getConfig().getBoolean("armor-stand-swap.enabled", true);
        if (swapEnabled && isArmor(handItem)) {
            if (player.hasPermission("remotweaks.armorswap")) {
                event.setCancelled(true);
                swapArmor(player, stand);
            }
            return;
        }

        // 2. Взаимодействие с пустой рукой
        if (handItem.getType() == Material.AIR) {
            boolean editorEnabled = plugin.getConfig().getBoolean("armor-stand-editor.enabled", true);
            if (editorEnabled && player.hasPermission("remotweaks.armorstand")) {
                event.setCancelled(true);
                openMenu(player, stand);
            } else if (swapEnabled && player.hasPermission("remotweaks.armorswap")) {
                // Если редактор выключен, пустая рука тоже производит быструю смену брони
                event.setCancelled(true);
                swapArmor(player, stand);
            }
        }
    }

    public void swapArmor(Player player, ArmorStand stand) {
        EntityEquipment standEquip = stand.getEquipment();
        PlayerInventory playerInv = player.getInventory();

        ItemStack pHead = playerInv.getHelmet();
        ItemStack pChest = playerInv.getChestplate();
        ItemStack pLegs = playerInv.getLeggings();
        ItemStack pBoots = playerInv.getBoots();

        ItemStack sHead = standEquip.getHelmet();
        ItemStack sChest = standEquip.getChestplate();
        ItemStack sLegs = standEquip.getLeggings();
        ItemStack sBoots = standEquip.getBoots();

        boolean pHas = isNotEmpty(pHead) || isNotEmpty(pChest) || isNotEmpty(pLegs) || isNotEmpty(pBoots);
        boolean sHas = isNotEmpty(sHead) || isNotEmpty(sChest) || isNotEmpty(sLegs) || isNotEmpty(sBoots);

        if (!pHas && !sHas) {
            player.sendActionBar(plugin.color("&cНа вас и на стойке нет брони для обмена!"));
            return;
        }

        playerInv.setHelmet(sHead);
        playerInv.setChestplate(sChest);
        playerInv.setLeggings(sLegs);
        playerInv.setBoots(sBoots);

        standEquip.setHelmet(pHead);
        standEquip.setChestplate(pChest);
        standEquip.setLeggings(pLegs);
        standEquip.setBoots(pBoots);

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
        player.sendActionBar(plugin.color("&a✔ Сет брони успешно заменён!"));
    }

    public static boolean isNotEmpty(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    public static boolean isArmor(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String n = item.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") ||
               n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") ||
               item.getType() == Material.ELYTRA || item.getType() == Material.TURTLE_HELMET;
    }

    public void openMenu(Player player, ArmorStand stand) {
        openEditors.put(player.getUniqueId(), stand);
        Inventory inv = Bukkit.createInventory(null, 27, plugin.color("&8Редактор стойки для брони"));

        // Заполнитель фона
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        updateMenuButtons(inv, stand);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    private void updateMenuButtons(Inventory inv, ArmorStand stand) {
        inv.setItem(10, createItem(Material.STICK, "&6Руки",
                List.of(stand.hasArms() ? "&aВключены" : "&cВыключены", "&7Клик: переключить")));

        inv.setItem(11, createItem(Material.SMOOTH_STONE_SLAB, "&6Опорная плита",
                List.of(stand.hasBasePlate() ? "&aВключена" : "&cВыключена", "&7Клик: переключить")));

        inv.setItem(12, createItem(Material.LEATHER_BOOTS, "&6Размер",
                List.of(stand.isSmall() ? "&eМаленькая" : "&bОбычная", "&7Клик: изменить размер")));

        inv.setItem(13, createItem(Material.GLASS, "&6Видимость",
                List.of(stand.isVisible() ? "&aВидимая" : "&cНевидимая", "&7Клик: переключить")));

        inv.setItem(14, createItem(Material.FEATHER, "&6Гравитация",
                List.of(stand.hasGravity() ? "&aВключена" : "&cВыключена", "&7Клик: переключить")));

        inv.setItem(15, createItem(Material.NAME_TAG, "&6Имя над стойкой",
                List.of(stand.isCustomNameVisible() ? "&aОтображается" : "&cСкрыто", "&7Клик: переключить")));

        inv.setItem(16, createItem(Material.IRON_SWORD, "&6Смена позы",
                List.of("&eКлик: следующая поза", "&7На выбор: Стандарт, Воин, Приветствие, Охрана, Указатель")));

        // Кнопка быстрой смены брони прямо из интерфейса
        inv.setItem(22, createItem(Material.DIAMOND_CHESTPLATE, "&bСменить сет брони",
                List.of("&7Быстрый обмен всей экипированной", "&7бронёй между вами и стойкой.", "", "&e► Клик: поменяться местами")));
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openEditors.containsKey(player.getUniqueId())) return;

        if (event.getView().getTitle().equals(plugin.color("&8Редактор стойки для брони"))) {
            event.setCancelled(true);
            ArmorStand stand = openEditors.get(player.getUniqueId());
            if (stand == null || !stand.isValid()) {
                player.closeInventory();
                return;
            }

            int slot = event.getRawSlot();
            switch (slot) {
                case 10 -> {
                    stand.setArms(!stand.hasArms());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                case 11 -> {
                    stand.setBasePlate(!stand.hasBasePlate());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                case 12 -> {
                    stand.setSmall(!stand.isSmall());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                case 13 -> {
                    stand.setVisible(!stand.isVisible());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                case 14 -> {
                    stand.setGravity(!stand.hasGravity());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                case 15 -> {
                    stand.setCustomNameVisible(!stand.isCustomNameVisible());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                case 16 -> {
                    cyclePose(stand);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                }
                case 22 -> {
                    swapArmor(player, stand);
                }
                default -> { return; }
            }

            updateMenuButtons(event.getInventory(), stand);
        }
    }

    private void cyclePose(ArmorStand stand) {
        stand.setArms(true);
        int current = 0;
        for (String tag : stand.getScoreboardTags()) {
            if (tag.startsWith("pose_")) {
                try {
                    current = Integer.parseInt(tag.substring(5));
                    stand.removeScoreboardTag(tag);
                } catch (Exception ignored) {}
            }
        }

        int next = (current + 1) % 5;
        stand.addScoreboardTag("pose_" + next);

        switch (next) {
            case 0 -> { // Дефолт
                stand.setRightArmPose(EulerAngle.ZERO);
                stand.setLeftArmPose(EulerAngle.ZERO);
                stand.setHeadPose(EulerAngle.ZERO);
                stand.setBodyPose(EulerAngle.ZERO);
            }
            case 1 -> { // Воин (меч вперед)
                stand.setRightArmPose(new EulerAngle(Math.toRadians(300), Math.toRadians(340), 0));
                stand.setLeftArmPose(new EulerAngle(Math.toRadians(20), 0, Math.toRadians(350)));
                stand.setHeadPose(new EulerAngle(Math.toRadians(5), 0, 0));
            }
            case 2 -> { // Приветствие (рука вверх)
                stand.setRightArmPose(new EulerAngle(Math.toRadians(240), Math.toRadians(40), Math.toRadians(330)));
                stand.setLeftArmPose(new EulerAngle(Math.toRadians(10), 0, 0));
            }
            case 3 -> { // Охрана (руки на груди)
                stand.setRightArmPose(new EulerAngle(Math.toRadians(290), Math.toRadians(30), 0));
                stand.setLeftArmPose(new EulerAngle(Math.toRadians(290), Math.toRadians(330), 0));
            }
            case 4 -> { // Указатель
                stand.setRightArmPose(new EulerAngle(Math.toRadians(270), 0, 0));
                stand.setLeftArmPose(new EulerAngle(0, 0, 0));
                stand.setHeadPose(new EulerAngle(0, Math.toRadians(20), 0));
            }
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(name));
            if (lore != null) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) colored.add(plugin.color(l));
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
