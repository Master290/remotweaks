package su.remo.tweaks.managers;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailManager {

    private final RemoTweaks plugin;
    private final File file;
    private FileConfiguration config;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    public record MailItem(int id, String sender, String time, String message, boolean read) {}

    public MailManager(RemoTweaks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mail.yml");
        load();
    }

    public synchronized void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать mail.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить mail.yml: " + e.getMessage());
        }
    }

    public synchronized boolean sendMail(String senderName, String targetPlayerName, String message) {
        String key = "mails." + targetPlayerName.toLowerCase();
        List<Map<?, ?>> list = config.getMapList(key);
        List<Map<String, Object>> updated = new ArrayList<>();
        int nextId = 1;

        for (Map<?, ?> m : list) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            if (map.containsKey("id")) {
                int id = ((Number) map.get("id")).intValue();
                if (id >= nextId) nextId = id + 1;
            }
            updated.add(map);
        }

        Map<String, Object> newMail = new HashMap<>();
        newMail.put("id", nextId);
        newMail.put("sender", senderName);
        newMail.put("time", LocalDateTime.now().format(formatter));
        newMail.put("message", message);
        newMail.put("read", false);
        updated.add(newMail);

        config.set(key, updated);
        save();

        // Если получатель онлайн, отправляем мгновенное уведомление со звуком
        Player target = Bukkit.getPlayerExact(targetPlayerName);
        if (target != null && target.isOnline()) {
            target.sendMessage(plugin.color("&e📬 Вам пришло новое письмо от &6" + senderName + "&e! Напишите &a/mail read &eдля прочтения."));
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }

        return true;
    }

    public synchronized List<MailItem> getMails(String playerName) {
        String key = "mails." + playerName.toLowerCase();
        List<Map<?, ?>> list = config.getMapList(key);
        List<MailItem> result = new ArrayList<>();

        for (Map<?, ?> m : list) {
            int id = m.containsKey("id") && m.get("id") instanceof Number num ? num.intValue() : 1;
            String sender = m.get("sender") != null ? String.valueOf(m.get("sender")) : "Неизвестный";
            String time = m.get("time") != null ? String.valueOf(m.get("time")) : "";
            String message = m.get("message") != null ? String.valueOf(m.get("message")) : "";
            boolean read = Boolean.TRUE.equals(m.get("read"));
            result.add(new MailItem(id, sender, time, message, read));
        }

        return result;
    }

    public synchronized int getUnreadCount(String playerName) {
        List<MailItem> mails = getMails(playerName);
        int unread = 0;
        for (MailItem m : mails) {
            if (!m.read()) unread++;
        }
        return unread;
    }

    public synchronized void markAllRead(String playerName) {
        String key = "mails." + playerName.toLowerCase();
        List<Map<?, ?>> list = config.getMapList(key);
        List<Map<String, Object>> updated = new ArrayList<>();

        for (Map<?, ?> m : list) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            map.put("read", true);
            updated.add(map);
        }

        config.set(key, updated);
        save();
    }

    public synchronized void clearMails(String playerName) {
        String key = "mails." + playerName.toLowerCase();
        config.set(key, null);
        save();
    }
}
