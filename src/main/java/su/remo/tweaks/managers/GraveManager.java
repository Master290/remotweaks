package su.remo.tweaks.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import su.remo.tweaks.RemoTweaks;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GraveManager {

    private final RemoTweaks plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<Location, GraveData> activeGraves = new HashMap<>();
    private final Map<UUID, Location> lastDeathLocations = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    public static class GraveData {
        private final UUID graveId;
        private final UUID ownerUUID;
        private final String ownerName;
        private final Location location;
        private final List<ItemStack> items;
        private final int exp;
        private final String time;
        private UUID hologramUUID;

        public GraveData(UUID graveId, UUID ownerUUID, String ownerName, Location location,
                         List<ItemStack> items, int exp, String time, UUID hologramUUID) {
            this.graveId = graveId;
            this.ownerUUID = ownerUUID;
            this.ownerName = ownerName;
            this.location = location;
            this.items = items;
            this.exp = exp;
            this.time = time;
            this.hologramUUID = hologramUUID;
        }

        public UUID getGraveId() { return graveId; }
        public UUID getOwnerUUID() { return ownerUUID; }
        public String getOwnerName() { return ownerName; }
        public Location getLocation() { return location; }
        public List<ItemStack> getItems() { return items; }
        public int getExp() { return exp; }
        public String getTime() { return time; }
        public UUID getHologramUUID() { return hologramUUID; }
        public void setHologramUUID(UUID hologramUUID) { this.hologramUUID = hologramUUID; }
    }

    public GraveManager(RemoTweaks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "graves.yml");
        load();
    }

    public synchronized void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать graves.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        activeGraves.clear();

        if (config.isConfigurationSection("graves")) {
            for (String key : config.getConfigurationSection("graves").getKeys(false)) {
                try {
                    String path = "graves." + key;
                    UUID graveId = UUID.fromString(key);
                    UUID ownerUUID = UUID.fromString(config.getString(path + ".owner-uuid"));
                    String ownerName = config.getString(path + ".owner-name");
                    World world = Bukkit.getWorld(config.getString(path + ".world"));
                    if (world == null) continue;

                    int x = config.getInt(path + ".x");
                    int y = config.getInt(path + ".y");
                    int z = config.getInt(path + ".z");
                    Location loc = new Location(world, x, y, z);

                    int exp = config.getInt(path + ".exp", 0);
                    String time = config.getString(path + ".time", "");
                    String holoStr = config.getString(path + ".hologram-uuid");
                    UUID holoUUID = holoStr != null ? UUID.fromString(holoStr) : null;

                    List<?> rawItems = config.getList(path + ".items");
                    List<ItemStack> items = new ArrayList<>();
                    if (rawItems != null) {
                        for (Object o : rawItems) {
                            if (o instanceof ItemStack is) {
                                items.add(is);
                            }
                        }
                    }

                    GraveData grave = new GraveData(graveId, ownerUUID, ownerName, loc, items, exp, time, holoUUID);
                    activeGraves.put(loc, grave);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка загрузки могилы " + key + ": " + e.getMessage());
                }
            }
        }
    }

    public synchronized void save() {
        config.set("graves", null);
        for (GraveData grave : activeGraves.values()) {
            String path = "graves." + grave.getGraveId().toString();
            config.set(path + ".owner-uuid", grave.getOwnerUUID().toString());
            config.set(path + ".owner-name", grave.getOwnerName());
            config.set(path + ".world", grave.getLocation().getWorld().getName());
            config.set(path + ".x", grave.getLocation().getBlockX());
            config.set(path + ".y", grave.getLocation().getBlockY());
            config.set(path + ".z", grave.getLocation().getBlockZ());
            config.set(path + ".exp", grave.getExp());
            config.set(path + ".time", grave.getTime());
            config.set(path + ".items", grave.getItems());
            if (grave.getHologramUUID() != null) {
                config.set(path + ".hologram-uuid", grave.getHologramUUID().toString());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить graves.yml: " + e.getMessage());
        }
    }

    public Location findSafeLocation(Location deathLoc) {
        World world = deathLoc.getWorld();
        if (world == null) return deathLoc;

        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();

        // Если упал в пустоту ниже мира
        if (deathLoc.getBlockY() < minHeight) {
            Location spawn = world.getSpawnLocation();
            return world.getHighestBlockAt(spawn).getLocation().add(0, 1, 0);
        }

        Block block = deathLoc.getBlock();
        // Если в лаве или огне — ищем воздух над лавой
        int y = Math.min(deathLoc.getBlockY(), maxHeight - 2);
        while (y < maxHeight - 2) {
            Block b = world.getBlockAt(deathLoc.getBlockX(), y, deathLoc.getBlockZ());
            if (b.getType() != Material.LAVA && b.getType() != Material.FIRE && b.getType() != Material.SOUL_FIRE) {
                return b.getLocation();
            }
            y++;
        }

        return deathLoc.getBlock().getLocation();
    }

    public GraveData createGrave(Player player, List<ItemStack> drops, int droppedExp) {
        if (drops == null || drops.isEmpty()) return null;

        Location safeLoc = findSafeLocation(player.getLocation());
        Block block = safeLoc.getBlock();

        // Запоминаем точку смерти
        setLastDeathLocation(player.getUniqueId(), safeLoc);

        // Устанавливаем голову игрока
        block.setType(Material.PLAYER_HEAD);
        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update(true, false);
        }

        // Спавним голограмму над могилой
        Location holoLoc = safeLoc.clone().add(0.5, 0.4, 0.5);
        ArmorStand holo = (ArmorStand) safeLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        holo.setVisible(false);
        holo.setGravity(false);
        holo.setInvulnerable(true);
        holo.setMarker(true);
        holo.setSmall(true);
        holo.setCustomName(plugin.color("&c🪦 Могила: &e" + player.getName() + " &7(" + drops.size() + " предм.)"));
        holo.setCustomNameVisible(true);

        UUID graveId = UUID.randomUUID();
        GraveData grave = new GraveData(
                graveId,
                player.getUniqueId(),
                player.getName(),
                safeLoc,
                new ArrayList<>(drops),
                droppedExp,
                LocalDateTime.now().format(formatter),
                holo.getUniqueId()
        );

        activeGraves.put(safeLoc, grave);
        save();
        return grave;
    }

    public boolean isGrave(Location loc) {
        return activeGraves.containsKey(loc.getBlock().getLocation());
    }

    public GraveData getGrave(Location loc) {
        return activeGraves.get(loc.getBlock().getLocation());
    }

    public boolean collectGrave(Player player, GraveData grave) {
        Location loc = grave.getLocation();
        Block block = loc.getBlock();

        // Выдаем предметы игроку
        for (ItemStack item : grave.getItems()) {
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack remaining : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                }
            }
        }

        // Выдаем опыт
        if (grave.getExp() > 0) {
            player.giveExp(grave.getExp());
        }

        // Удаляем голограмму
        if (grave.getHologramUUID() != null) {
            Entity holo = Bukkit.getEntity(grave.getHologramUUID());
            if (holo != null) {
                holo.remove();
            }
        }

        // Удаляем блок могилы
        if (block.getType() == Material.PLAYER_HEAD) {
            block.setType(Material.AIR);
        }

        activeGraves.remove(loc);
        save();

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        player.sendMessage(plugin.color("&a✔ Вы успешно забрали свои вещи из могилы! &7(Предметов: " + grave.getItems().size() + ")"));
        return true;
    }

    public List<GraveData> getPlayerGraves(UUID playerUUID) {
        List<GraveData> list = new ArrayList<>();
        for (GraveData g : activeGraves.values()) {
            if (g.getOwnerUUID().equals(playerUUID)) {
                list.add(g);
            }
        }
        return list;
    }

    public Location getLastDeathLocation(UUID uuid) {
        return lastDeathLocations.get(uuid);
    }

    public void setLastDeathLocation(UUID uuid, Location loc) {
        lastDeathLocations.put(uuid, loc);
    }

    public void cleanup() {
        save();
    }
}
