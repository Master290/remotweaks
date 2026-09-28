package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BoardManager {

    private final RemoTweaks plugin;
    private final File file;
    private FileConfiguration config;
    private final List<BoardAd> ads = new CopyOnWriteArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    public record BoardAd(int id, UUID authorUuid, String authorName, long timestamp, String timeStr, String text) {}

    public BoardManager(RemoTweaks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "board.yml");
        load();
    }

    public synchronized void load() {
        ads.clear();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать board.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        List<Map<?, ?>> list = config.getMapList("ads");
        long retentionDays = plugin.getConfig().getLong("notice-board.retention-days", 14);
        long maxAgeMillis = retentionDays > 0 ? retentionDays * 24L * 60L * 60L * 1000L : Long.MAX_VALUE;
        long now = System.currentTimeMillis();

        boolean needSave = false;

        for (Map<?, ?> m : list) {
            try {
                int id = ((Number) m.get("id")).intValue();
                UUID uuid = UUID.fromString((String) m.get("uuid"));
                String name = (String) m.get("name");
                long time = ((Number) m.get("time")).longValue();
                String timeStr = (String) m.get("timeStr");
                String text = (String) m.get("text");

                if (now - time > maxAgeMillis) {
                    needSave = true;
                    continue; // Устаревшее объявление
                }

                if (timeStr == null || timeStr.isBlank()) {
                    timeStr = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
                }

                ads.add(new BoardAd(id, uuid, name, time, timeStr, text));
            } catch (Exception ignored) {}
        }

        ads.sort(Comparator.comparingLong(BoardAd::timestamp).reversed());
        if (needSave) {
            save();
        }
    }

    public synchronized void save() {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (BoardAd ad : ads) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", ad.id());
            map.put("uuid", ad.authorUuid().toString());
            map.put("name", ad.authorName());
            map.put("time", ad.timestamp());
            map.put("timeStr", ad.timeStr());
            map.put("text", ad.text());
            serialized.add(map);
        }
        config.set("ads", serialized);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить board.yml: " + e.getMessage());
        }
    }

    public synchronized boolean addAd(Player player, String text) {
        int maxAds = plugin.getConfig().getInt("notice-board.max-ads-per-player", 3);
        long userAds = ads.stream().filter(a -> a.authorUuid().equals(player.getUniqueId())).count();
        if (userAds >= maxAds && !player.hasPermission("remotweaks.board.admin")) {
            player.sendMessage(plugin.color("&cУ вас уже есть " + userAds + " активных объявлений! Удалите старое через &e/board remove <id>"));
            return false;
        }

        int maxId = ads.stream().mapToInt(BoardAd::id).max().orElse(0);
        int newId = maxId + 1;
        long now = System.currentTimeMillis();
        String timeStr = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()));

        BoardAd ad = new BoardAd(newId, player.getUniqueId(), player.getName(), now, timeStr, text.trim());
        ads.add(0, ad);
        save();

        // Оповещение всего сервера
        Component broadcast = Component.text("📜 ", NamedTextColor.GOLD)
                .append(Component.text("[Доска] ", NamedTextColor.YELLOW))
                .append(Component.text(player.getName(), NamedTextColor.GOLD))
                .append(Component.text(" повесил новое объявление: ", NamedTextColor.GRAY))
                .append(Component.text("«" + (text.length() > 60 ? text.substring(0, 57) + "..." : text) + "» ", NamedTextColor.WHITE))
                .append(Component.text("(/board)", NamedTextColor.YELLOW));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(broadcast);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
        }

        return true;
    }

    public synchronized boolean removeAd(int id, Player requester) {
        BoardAd target = null;
        for (BoardAd ad : ads) {
            if (ad.id() == id) {
                target = ad;
                break;
            }
        }
        if (target == null) {
            return false;
        }

        if (!requester.hasPermission("remotweaks.board.admin") && !target.authorUuid().equals(requester.getUniqueId())) {
            requester.sendMessage(plugin.color("&cВы можете удалять только свои объявления!"));
            return false;
        }

        ads.remove(target);
        save();
        return true;
    }

    public synchronized int clearPlayerAds(String targetName) {
        int count = 0;
        Iterator<BoardAd> it = ads.iterator();
        while (it.hasNext()) {
            BoardAd ad = it.next();
            if (ad.authorName().equalsIgnoreCase(targetName)) {
                ads.remove(ad);
                count++;
            }
        }
        if (count > 0) {
            save();
        }
        return count;
    }

    public List<BoardAd> getAds() {
        return Collections.unmodifiableList(ads);
    }
}
