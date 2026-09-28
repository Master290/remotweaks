package su.remo.tweaks.commands;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import su.remo.tweaks.RemoTweaks;

import java.util.*;

public class ChunkBorderCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    private static final Particle.DustOptions CORNER_DUST = new Particle.DustOptions(Color.fromRGB(0, 255, 128), 1.2f);
    private static final Particle.DustOptions EDGE_DUST = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 0.8f);

    public ChunkBorderCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (activeTasks.containsKey(uuid)) {
            activeTasks.remove(uuid).cancel();
            player.sendMessage(plugin.color("&e📐 Отображение границ чанка отключено."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return true;
        }

        player.sendMessage(plugin.color("&a📐 Отображение границ чанка включено на 25 секунд! &7(Повторите /" + label + " для отключения)"));
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);

        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticksElapsed >= 500) { // 25 секунд
                    activeTasks.remove(uuid);
                    if (player.isOnline()) {
                        player.sendMessage(plugin.color("&7[📐 Отображение границ чанка автоматически завершено]"));
                    }
                    cancel();
                    return;
                }
                ticksElapsed += 4;

                Location loc = player.getLocation();
                int chunkX = loc.getBlockX() >> 4;
                int chunkZ = loc.getBlockZ() >> 4;

                int minX = chunkX << 4;
                int maxX = minX + 16;
                int minZ = chunkZ << 4;
                int maxZ = minZ + 16;

                int playerY = loc.getBlockY();
                int minY = Math.max(player.getWorld().getMinHeight(), playerY - 4);
                int maxY = Math.min(player.getWorld().getMaxHeight(), playerY + 8);

                // 1. Углы чанка (вертикальные столбы частиц)
                for (int y = minY; y <= maxY; y++) {
                    spawnDust(player, minX, y, minZ, CORNER_DUST);
                    spawnDust(player, maxX, y, minZ, CORNER_DUST);
                    spawnDust(player, minX, y, maxZ, CORNER_DUST);
                    spawnDust(player, maxX, y, maxZ, CORNER_DUST);
                }

                // 2. Горизонтальные ребра на уровне ног и головы
                int[] yLevels = {playerY, playerY + 2};
                for (int y : yLevels) {
                    for (int x = minX; x <= maxX; x += 2) {
                        spawnDust(player, x, y, minZ, EDGE_DUST);
                        spawnDust(player, x, y, maxZ, EDGE_DUST);
                    }
                    for (int z = minZ; z <= maxZ; z += 2) {
                        spawnDust(player, minX, y, z, EDGE_DUST);
                        spawnDust(player, maxX, y, z, EDGE_DUST);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);

        activeTasks.put(uuid, task);
        return true;
    }

    private void spawnDust(Player player, double x, double y, double z, Particle.DustOptions dust) {
        player.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
    }

    public void cleanup() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
