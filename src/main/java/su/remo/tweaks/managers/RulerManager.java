package su.remo.tweaks.managers;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import su.remo.tweaks.RemoTweaks;

import java.util.*;

public class RulerManager {

    private final RemoTweaks plugin;
    private final Set<UUID> enabledPlayers = new HashSet<>();
    private final Map<UUID, Location> pointOne = new HashMap<>();

    private static final Particle.DustOptions RED_DUST = new Particle.DustOptions(Color.fromRGB(255, 60, 60), 1.2f);
    private static final Particle.DustOptions BLUE_DUST = new Particle.DustOptions(Color.fromRGB(60, 160, 255), 1.2f);
    private static final Particle.DustOptions LINE_DUST = new Particle.DustOptions(Color.fromRGB(255, 230, 50), 0.8f);

    public RulerManager(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    public boolean toggleRuler(Player player) {
        UUID uuid = player.getUniqueId();
        if (enabledPlayers.contains(uuid)) {
            enabledPlayers.remove(uuid);
            pointOne.remove(uuid);
            return false;
        } else {
            enabledPlayers.add(uuid);
            return true;
        }
    }

    public boolean isRulerActive(Player player) {
        return enabledPlayers.contains(player.getUniqueId());
    }

    public void setPointA(Player player, Block block) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        pointOne.put(player.getUniqueId(), loc);

        player.sendActionBar(plugin.color("&a[Рулетка] Точка A: &e" + block.getX() + ", " + block.getY() + ", " + block.getZ()));
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
        player.spawnParticle(Particle.DUST, loc, 15, 0.3, 0.3, 0.3, 0, RED_DUST);
    }

    public void setPointB(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        Location locA = pointOne.get(uuid);
        Location locB = block.getLocation().add(0.5, 0.5, 0.5);

        if (locA == null || !Objects.equals(locA.getWorld(), locB.getWorld())) {
            player.sendMessage(plugin.color("&cСначала выберите Точку A (нажмите ЛКМ с палкой или в режиме рулетки)!"));
            return;
        }

        int x1 = locA.getBlockX();
        int y1 = locA.getBlockY();
        int z1 = locA.getBlockZ();

        int x2 = block.getX();
        int y2 = block.getY();
        int z2 = block.getZ();

        int dx = Math.abs(x2 - x1) + 1;
        int dy = Math.abs(y2 - y1) + 1;
        int dz = Math.abs(z2 - z1) + 1;
        double dist = locA.distance(locB);
        long volume = (long) dx * dy * dz;

        player.playSound(locB, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        player.spawnParticle(Particle.DUST, locB, 15, 0.3, 0.3, 0.3, 0, BLUE_DUST);

        player.sendMessage(plugin.color("&6=== 📏 Замер расстояния ==="));
        player.sendMessage(plugin.color("&fПрямая дистанция: &a" + String.format(Locale.US, "%.2f", dist) + " &fблоков"));
        player.sendMessage(plugin.color("&fГабариты (X × Y × Z): &e" + dx + " &7× &e" + dy + " &7× &e" + dz + " &7блоков"));
        player.sendMessage(plugin.color("&fОбщий объем: &b" + volume + " &fблоков"));

        player.sendActionBar(plugin.color("&6📏 Дистанция: &a" + String.format(Locale.US, "%.1f", dist) + " бл. &7| &e" + dx + "×" + dy + "×" + dz));

        // Рисуем линию частиц между двумя точками
        drawParticleLine(player, locA, locB);
    }

    private void drawParticleLine(Player player, Location p1, Location p2) {
        new BukkitRunnable() {
            int repeats = 0;

            @Override
            public void run() {
                if (!player.isOnline() || repeats >= 10) { // 2.5 секунды
                    cancel();
                    return;
                }
                repeats++;

                double distance = p1.distance(p2);
                int points = (int) Math.max(5, distance * 2);
                for (int i = 0; i <= points; i++) {
                    double ratio = (double) i / points;
                    double x = p1.getX() + (p2.getX() - p1.getX()) * ratio;
                    double y = p1.getY() + (p2.getY() - p1.getY()) * ratio;
                    double z = p1.getZ() + (p2.getZ() - p1.getZ()) * ratio;
                    player.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, LINE_DUST);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public void cleanup(UUID uuid) {
        enabledPlayers.remove(uuid);
        pointOne.remove(uuid);
    }
}
