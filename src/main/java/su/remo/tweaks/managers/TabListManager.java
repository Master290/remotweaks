package su.remo.tweaks.managers;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import su.remo.tweaks.RemoTweaks;

import java.util.*;

public class TabListManager {

    private final RemoTweaks plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private BukkitTask updateTask;

    private static final String OBJECTIVE_NAME = "remo_ping";

    public TabListManager(RemoTweaks plugin) {
        this.plugin = plugin;
        startTask();
    }

    public void startTask() {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) {
            return;
        }

        long interval = plugin.getConfig().getLong("tablist.update-interval", 20L);
        if (interval < 5L) interval = 5L;

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 10L, interval);
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) {
            return;
        }

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }

        int onlineCount = players.size();
        int maxPlayers = Bukkit.getMaxPlayers();
        double tps = Math.min(20.0, Math.round(Bukkit.getTPS()[0] * 10.0) / 10.0);
        String tpsStr = String.format(Locale.US, "%.1f", tps);

        boolean pingScoreEnabled = plugin.getConfig().getBoolean("tablist.ping-score.enabled", true);
        String scoreFormat = plugin.getConfig().getString("tablist.ping-score.format", "NUMBER_MS");

        // 1. Обновляем числовой пинг в ванильном слоте Tab (Scoreboard DisplaySlot.PLAYER_LIST)
        if (pingScoreEnabled) {
            Set<Scoreboard> scoreboards = new HashSet<>();
            scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());
            for (Player player : players) {
                scoreboards.add(player.getScoreboard());
            }

            for (Scoreboard sb : scoreboards) {
                Objective obj = getOrCreatePingObjective(sb);
                for (Player target : players) {
                    int ping = target.getPing();
                    Score score = obj.getScore(target.getName());
                    score.setScore(ping);

                    NamedTextColor color = getPingNamedColor(ping);
                    if ("NUMBER_MS".equalsIgnoreCase(scoreFormat)) {
                        score.numberFormat(NumberFormat.fixed(Component.text(ping + "ms", color)));
                    } else {
                        score.numberFormat(NumberFormat.styled(color));
                    }
                }
            }
        }

        // 2. Обновляем Header, Footer и playerListName для каждого игрока
        List<String> headerLines = plugin.getConfig().getStringList("tablist.header");
        List<String> footerLines = plugin.getConfig().getStringList("tablist.footer");

        for (Player player : players) {
            int playerPing = player.getPing();
            String pingColorTag = getPingColorTag(playerPing);

            Component header = buildComponent(headerLines, onlineCount, maxPlayers, playerPing, pingColorTag, tpsStr);
            Component footer = buildComponent(footerLines, onlineCount, maxPlayers, playerPing, pingColorTag, tpsStr);

            player.sendPlayerListHeaderAndFooter(header, footer);

            updatePlayerName(player);
        }
    }

    public void updatePlayerName(Player player) {
        boolean showPingInName = plugin.getConfig().getBoolean("tablist.player-format.show-ping-in-name", false);
        boolean isAfk = plugin.getAfkManager() != null && plugin.getAfkManager().isAfk(player);

        Component comp;
        if (isAfk) {
            comp = Component.text("[AFK] ", NamedTextColor.GRAY).append(Component.text(player.getName(), NamedTextColor.WHITE));
        } else {
            comp = Component.text(player.getName(), NamedTextColor.WHITE);
        }

        if (showPingInName) {
            int ping = player.getPing();
            NamedTextColor color = getPingNamedColor(ping);
            comp = comp.append(Component.text(" [", NamedTextColor.GRAY))
                    .append(Component.text(ping + "ms", color))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        player.playerListName(comp);
    }

    public void onPlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateAll();
            }
        }, 5L);
    }

    public void onPlayerQuit(Player player) {
        Set<Scoreboard> scoreboards = new HashSet<>();
        scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player p : Bukkit.getOnlinePlayers()) {
            scoreboards.add(p.getScoreboard());
        }

        for (Scoreboard sb : scoreboards) {
            try {
                sb.resetScores(player.getName());
            } catch (Exception ignored) {}
        }
    }

    private Objective getOrCreatePingObjective(Scoreboard scoreboard) {
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.text("ms"));
        }
        if (obj.getDisplaySlot() != DisplaySlot.PLAYER_LIST) {
            obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
        return obj;
    }

    private Component buildComponent(List<String> lines, int online, int max, int ping, String pingColorTag, String tps) {
        if (lines == null || lines.isEmpty()) {
            return Component.empty();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i)
                    .replace("{online}", String.valueOf(online))
                    .replace("{max}", String.valueOf(max))
                    .replace("{ping}", String.valueOf(ping))
                    .replace("{ping_color}", pingColorTag)
                    .replace("{tps}", tps);

            sb.append(line);
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }

        return miniMessage.deserialize(sb.toString());
    }

    public NamedTextColor getPingNamedColor(int ping) {
        if (ping < 50) return NamedTextColor.GREEN;
        if (ping < 120) return NamedTextColor.YELLOW;
        if (ping < 200) return NamedTextColor.GOLD;
        return NamedTextColor.RED;
    }

    public String getPingColorTag(int ping) {
        if (ping < 50) return "<green>";
        if (ping < 120) return "<yellow>";
        if (ping < 200) return "<gold>";
        return "<red>";
    }

    public void reload() {
        cleanup();
        startTask();
        updateAll();
    }

    public void cleanup() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Очищаем objective со скорбордов
        Set<Scoreboard> scoreboards = new HashSet<>();
        scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player p : Bukkit.getOnlinePlayers()) {
            scoreboards.add(p.getScoreboard());
        }

        for (Scoreboard sb : scoreboards) {
            Objective obj = sb.getObjective(OBJECTIVE_NAME);
            if (obj != null) {
                try {
                    obj.unregister();
                } catch (Exception ignored) {}
            }
        }
    }
}
