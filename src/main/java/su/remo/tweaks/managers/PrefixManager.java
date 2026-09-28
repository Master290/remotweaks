package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final Map<UUID, String> suffixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> glowColorNames = new ConcurrentHashMap<>();
    private final Set<UUID> glowingPlayers = ConcurrentHashMap.newKeySet();

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
        suffixes.clear();
        glowColorNames.clear();
        glowingPlayers.clear();
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
        ConfigurationSection sec = config.getConfigurationSection("data");
        if (sec == null) {
            sec = config.getConfigurationSection("prefixes"); // обратная совместимость
        }

        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = sec.getString(key + ".name", "");
                    String prefix = sec.getString(key + ".prefix", "");
                    String suffix = sec.getString(key + ".suffix", "");
                    String glowColor = sec.getString(key + ".glow_color", "");
                    boolean glowing = sec.getBoolean(key + ".glowing", false);

                    if (!name.isEmpty()) {
                        playerNames.put(uuid, name);
                        nameToUuid.put(name.toLowerCase(), uuid);
                    }
                    if (!prefix.isEmpty()) {
                        prefixes.put(uuid, prefix);
                    }
                    if (!suffix.isEmpty()) {
                        suffixes.put(uuid, suffix);
                    }
                    if (!glowColor.isEmpty()) {
                        glowColorNames.put(uuid, glowColor);
                    }
                    if (glowing) {
                        glowingPlayers.add(uuid);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public synchronized void save() {
        try {
            config.set("prefixes", null); // очищаем легаси секцию
            config.set("data", null);

            Set<UUID> allUuids = new HashSet<>();
            allUuids.addAll(playerNames.keySet());
            allUuids.addAll(prefixes.keySet());
            allUuids.addAll(suffixes.keySet());
            allUuids.addAll(glowingPlayers);

            for (UUID uuid : allUuids) {
                String uuidStr = uuid.toString();
                String name = playerNames.getOrDefault(uuid, "");
                String prefix = prefixes.getOrDefault(uuid, "");
                String suffix = suffixes.getOrDefault(uuid, "");
                String glowColor = glowColorNames.getOrDefault(uuid, "");
                boolean glowing = glowingPlayers.contains(uuid);

                config.set("data." + uuidStr + ".name", name);
                if (!prefix.isEmpty()) config.set("data." + uuidStr + ".prefix", prefix);
                if (!suffix.isEmpty()) config.set("data." + uuidStr + ".suffix", suffix);
                if (!glowColor.isEmpty()) config.set("data." + uuidStr + ".glow_color", glowColor);
                if (glowing) config.set("data." + uuidStr + ".glowing", true);
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save prefixes.yml", e);
        }
    }

    // --- Префиксы ---

    public void setPrefix(UUID uuid, String name, String prefix) {
        prefixes.put(uuid, prefix);
        recordPlayerName(uuid, name);
        save();
        updateOnlinePlayer(uuid);
    }

    public boolean clearPrefix(UUID uuid) {
        String removed = prefixes.remove(uuid);
        if (removed != null) {
            save();
            updateOnlinePlayer(uuid);
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

    // --- Суффиксы ---

    public void setSuffix(UUID uuid, String name, String suffix) {
        suffixes.put(uuid, suffix);
        recordPlayerName(uuid, name);
        save();
        updateOnlinePlayer(uuid);
    }

    public boolean clearSuffix(UUID uuid) {
        String removed = suffixes.remove(uuid);
        if (removed != null) {
            save();
            updateOnlinePlayer(uuid);
            return true;
        }
        return false;
    }

    public String getRawSuffix(UUID uuid) {
        return suffixes.get(uuid);
    }

    public Component getSuffixComponent(UUID uuid) {
        String raw = suffixes.get(uuid);
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return parseComponent(raw);
    }

    // --- Свечение (/glow) ---

    public void setGlow(UUID uuid, String name, boolean glowing, String colorName) {
        if (glowing) {
            glowingPlayers.add(uuid);
            if (colorName != null && !colorName.isEmpty()) {
                glowColorNames.put(uuid, colorName.toLowerCase());
            }
        } else {
            glowingPlayers.remove(uuid);
        }
        recordPlayerName(uuid, name);
        save();
        updateOnlinePlayer(uuid);
    }

    public boolean isGlowing(UUID uuid) {
        return glowingPlayers.contains(uuid);
    }

    public NamedTextColor getGlowNamedColor(UUID uuid) {
        String colStr = glowColorNames.get(uuid);
        if (colStr == null || colStr.isEmpty()) {
            return NamedTextColor.GOLD; // цвет по умолчанию
        }
        return parseNamedColor(colStr);
    }

    public String getGlowColorName(UUID uuid) {
        return glowColorNames.getOrDefault(uuid, "gold");
    }

    public static NamedTextColor parseNamedColor(String name) {
        if (name == null) return NamedTextColor.GOLD;
        return switch (name.toLowerCase()) {
            case "black" -> NamedTextColor.BLACK;
            case "dark_blue", "darkblue" -> NamedTextColor.DARK_BLUE;
            case "dark_green", "darkgreen" -> NamedTextColor.DARK_GREEN;
            case "dark_aqua", "darkaqua", "cyan" -> NamedTextColor.DARK_AQUA;
            case "dark_red", "darkred" -> NamedTextColor.DARK_RED;
            case "dark_purple", "darkpurple", "purple" -> NamedTextColor.DARK_PURPLE;
            case "gold", "orange" -> NamedTextColor.GOLD;
            case "gray", "grey" -> NamedTextColor.GRAY;
            case "dark_gray", "darkgrey" -> NamedTextColor.DARK_GRAY;
            case "blue" -> NamedTextColor.BLUE;
            case "green", "lime" -> NamedTextColor.GREEN;
            case "aqua" -> NamedTextColor.AQUA;
            case "red" -> NamedTextColor.RED;
            case "light_purple", "pink", "magenta" -> NamedTextColor.LIGHT_PURPLE;
            case "yellow" -> NamedTextColor.YELLOW;
            case "white" -> NamedTextColor.WHITE;
            default -> NamedTextColor.GOLD;
        };
    }

    // --- Вспомогательные методы ---

    private void recordPlayerName(UUID uuid, String name) {
        if (name != null && !name.isEmpty()) {
            playerNames.put(uuid, name);
            nameToUuid.put(name.toLowerCase(), uuid);
        }
    }

    private void updateOnlinePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            updatePlayer(player);
        }
    }

    public Component parseComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (text.contains("&") || text.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }

        try {
            return MiniMessage.miniMessage().deserialize(text);
        } catch (Exception e) {
            return Component.text(text);
        }
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        // 1. Обновляем префикс, суффикс и цвет свечения над головой (Scoreboard Team)
        updateScoreboardTeam(player);

        // 2. Обновляем эффект свечения игрока
        player.setGlowing(isGlowing(player.getUniqueId()));

        // 3. Обновляем отображение в Tab-листе
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

        UUID uuid = player.getUniqueId();
        Component prefixComp = getPrefixComponent(uuid);
        Component suffixComp = getSuffixComponent(uuid);

        team.prefix(prefixComp);
        team.suffix(suffixComp);

        NamedTextColor glowColor = getGlowNamedColor(uuid);
        team.color(glowColor);

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

    public Map<UUID, String> getAllSuffixes() {
        return Collections.unmodifiableMap(suffixes);
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
