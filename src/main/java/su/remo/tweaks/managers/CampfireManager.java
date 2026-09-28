package su.remo.tweaks.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import su.remo.tweaks.RemoTweaks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CampfireManager {

    private final RemoTweaks plugin;
    private BukkitTask task;
    private final Map<UUID, Long> lastActionBar = new ConcurrentHashMap<>();
    private int tickCounter = 0;

    public CampfireManager(RemoTweaks plugin) {
        this.plugin = plugin;
        start();
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        // Запускаем проверку каждые 60 тиков (3 секунды)
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 60L, 60L);
    }

    public void cleanup() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastActionBar.clear();
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("campfire-rest.enabled", true)) {
            return;
        }

        tickCounter++;
        boolean feedCycle = (tickCounter % 2 == 0); // каждые 6 секунд восстанавливаем сытость
        double radius = plugin.getConfig().getDouble("campfire-rest.radius", 5.0);
        long now = System.currentTimeMillis();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isValid() || player.isDead()) continue;

            // Проверяем, сидит ли игрок или отдыхает
            boolean sitting = plugin.getSitManager().isSitting(player);
            boolean resting = sitting || (player.isSneaking() && player.isOnGround());

            if (!resting) continue;

            Block campfireBlock = getNearbyLitCampfire(player.getLocation(), radius);
            if (campfireBlock == null) continue;

            // 1. Эффект регенерации здоровья (Regeneration I на 5 секунд)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, false, true));

            // 2. Медленное восстановление сытости
            if (feedCycle && player.getFoodLevel() < 20) {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
                player.setSaturation(Math.min(player.getFoodLevel(), player.getSaturation() + 1.0f));
            }

            // 3. Теплые визуальные эффекты
            Location cfLoc = campfireBlock.getLocation().add(0.5, 0.7, 0.5);
            campfireBlock.getWorld().spawnParticle(Particle.WAX_ON, cfLoc, 2, 0.25, 0.1, 0.25, 0.02);
            campfireBlock.getWorld().spawnParticle(Particle.FLAME, cfLoc.add(0, -0.2, 0), 1, 0.1, 0.05, 0.1, 0.01);
            player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation().add(0, 0.35, 0), 1, 0.15, 0.15, 0.15, 0.01);

            // 4. Ненавязчивая подсказка в Actionbar раз в 25 секунд
            long lastMsg = lastActionBar.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastMsg > 25000L) {
                player.sendActionBar(plugin.color("&6🔥 Уют у костра... Здоровье и сытость восстанавливаются"));
                player.playSound(player.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1.0f);
                lastActionBar.put(player.getUniqueId(), now);
            }
        }
    }

    public boolean isNearLitCampfire(Player player) {
        if (!plugin.getConfig().getBoolean("campfire-rest.enabled", true)) {
            return false;
        }
        double radius = plugin.getConfig().getDouble("campfire-rest.radius", 5.0);
        return getNearbyLitCampfire(player.getLocation(), radius) != null;
    }

    public Block getNearbyLitCampfire(Location loc, double radius) {
        World world = loc.getWorld();
        if (world == null) return null;

        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        int r = (int) Math.ceil(radius);
        double rSq = radius * radius;

        for (int x = cx - r; x <= cx + r; x++) {
            for (int y = cy - 2; y <= cy + 2; y++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() == Material.CAMPFIRE || b.getType() == Material.SOUL_CAMPFIRE) {
                        if (b.getBlockData() instanceof Campfire cf && cf.isLit()) {
                            if (b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(loc) <= rSq) {
                                return b;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
