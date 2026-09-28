package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import su.remo.tweaks.RemoTweaks;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PrefixManager {

    private final RemoTweaks plugin;
    private final File file;
    private YamlConfiguration config;

    private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();

    private static final String TEAM_PREFIX = "rp_";

    public PrefixManager(RemoTweaks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "prefixes.yml");
        load();
    }

    public synchronized void load() {
        prefixes.clear();
        playerNames.clear();
        nameToUuid.clear();

        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create prefixes.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = config.getConfigurationSection("prefixes");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = sec.getString(key + ".name", "");
                    String prefix = sec.getString(key + ".prefix", "");

                    if (!prefix.isEmpty()) {
                        prefixes.put(uuid, prefix);
                        if (!name.isEmpty()) {
                            playerNames.put(uuid, name);
                            nameToUuid.put(name.toLowerCase(), uuid);
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public synchronized void save() {
        try {
            config.set("prefixes", null);
            for (Map.Entry<UUID, String> entry : prefixes.entrySet()) {
                String uuidStr = entry.getKey().toString();
                String name = playerNames.getOrDefault(entry.getKey(), "");
                config.set("prefixes." + uuidStr + ".name", name);
                config.set("prefixes." + uuidStr + ".prefix", entry.getValue());
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save prefixes.yml", e);
        }
    }

    public void setPrefix(UUID uuid, String name, String prefix) {
        prefixes.put(uuid, prefix);
        if (name != null && !name.isEmpty()) {
            playerNames.put(uuid, name);
            nameToUuid.put(name.toLowerCase(), uuid);
        }
        save();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            updatePlayer(player);
        }
    }

    public boolean clearPrefix(UUID uuid) {
        String removed = prefixes.remove(uuid);
        if (removed != null) {
            save();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updatePlayer(player);
            }
            return true;
        }
        return false;
    }

    public String getRawPrefix(UUID uuid) {
        return prefixes.get(uuid);
    }

    public Component getPrefixComponent(UUID uuid) {
        String raw = prefixes.get(uuid);
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return parseComponent(raw);
    }

    public Component parseComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Поддержка стандартных цветовых кодов &a, &c, &6 и т.д.
        if (text.contains("&") || text.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }

        // Поддержка современных MiniMessage тегов: <red>, <gradient:...>, etc.
        try {
            return MiniMessage.miniMessage().deserialize(text);
        } catch (Exception e) {
            return Component.text(text);
        }
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        // 1. Обновляем префикс над головой (Scoreboard Team)
        updateScoreboardTeam(player);

        // 2. Обновляем отображение в Tab-листе
        if (plugin.getTabListManager() != null) {
            plugin.getTabListManager().updatePlayerName(player);
        }
    }

    public void updateScoreboardTeam(Player player) {
        Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
        applyTeam(mainSb, player);

        if (player.getScoreboard() != mainSb) {
            applyTeam(player.getScoreboard(), player);
        }
    }

    private void applyTeam(Scoreboard sb, Player player) {
        String name = player.getName();
        String teamName = TEAM_PREFIX + name.toLowerCase();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = sb.getTeam(teamName);
        if (team == null) {
            team = sb.registerNewTeam(teamName);
        }

        Component prefixComp = getPrefixComponent(player.getUniqueId());
        team.prefix(prefixComp);

        if (!team.hasEntry(name)) {
            team.addEntry(name);
        }
    }

    public void removePlayerTeam(Player player) {
        String name = player.getName();
        String teamName = TEAM_PREFIX + name.toLowerCase();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainSb.getTeam(teamName);
        if (team != null) {
            team.removeEntry(name);
        }

        if (player.getScoreboard() != mainSb) {
            Team playerTeam = player.getScoreboard().getTeam(teamName);
            if (playerTeam != null) {
                playerTeam.removeEntry(name);
            }
        }
    }

    public UUID findUuidByName(String name) {
        UUID uuid = nameToUuid.get(name.toLowerCase());
        if (uuid != null) return uuid;

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        return null;
    }

    public String getStoredPlayerName(UUID uuid) {
        return playerNames.getOrDefault(uuid, uuid.toString());
    }

    public Map<UUID, String> getAllPrefixes() {
        return Collections.unmodifiableMap(prefixes);
    }

    public void cleanup() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : sb.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                try {
                    team.unregister();
                } catch (Exception ignored) {}
            }
        }
    }
}
