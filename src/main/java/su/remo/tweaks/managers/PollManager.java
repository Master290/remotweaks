package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import su.remo.tweaks.RemoTweaks;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PollManager {

    private final RemoTweaks plugin;
    private final File file;
    private FileConfiguration config;
    private final List<Poll> polls = new CopyOnWriteArrayList<>();
    private BukkitTask expiryTask;

    public static class Poll {
        private final int id;
        private final UUID creatorUuid;
        private final String creatorName;
        private final String question;
        private final List<String> options;
        private final Map<UUID, Integer> votes; // player UUID -> option index (0-based)
        private final long createdAt;
        private final long durationMillis; // 0 for unlimited / until manual end
        private boolean active;

        public Poll(int id, UUID creatorUuid, String creatorName, String question, List<String> options,
                    Map<UUID, Integer> votes, long createdAt, long durationMillis, boolean active) {
            this.id = id;
            this.creatorUuid = creatorUuid;
            this.creatorName = creatorName;
            this.question = question;
            this.options = new ArrayList<>(options);
            this.votes = new ConcurrentHashMap<>(votes);
            this.createdAt = createdAt;
            this.durationMillis = durationMillis;
            this.active = active;
        }

        public int getId() { return id; }
        public UUID getCreatorUuid() { return creatorUuid; }
        public String getCreatorName() { return creatorName; }
        public String getQuestion() { return question; }
        public List<String> getOptions() { return options; }
        public Map<UUID, Integer> getVotes() { return votes; }
        public long getCreatedAt() { return createdAt; }
        public long getDurationMillis() { return durationMillis; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public int getVoteCount(int optionIndex) {
            int count = 0;
            for (int opt : votes.values()) {
                if (opt == optionIndex) count++;
            }
            return count;
        }

        public int getTotalVotes() {
            return votes.size();
        }

        public boolean hasVoted(UUID uuid, int optionIndex) {
            Integer v = votes.get(uuid);
            return v != null && v == optionIndex;
        }

        public Integer getPlayerVote(UUID uuid) {
            return votes.get(uuid);
        }

        public boolean isExpired() {
            if (!active || durationMillis <= 0) return false;
            return System.currentTimeMillis() - createdAt >= durationMillis;
        }
    }

    public PollManager(RemoTweaks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "polls.yml");
        load();
        startExpiryCheck();
    }

    public void startExpiryCheck() {
        if (expiryTask != null) expiryTask.cancel();
        // Проверяем истечение голосований каждую минуту (1200 тиков)
        this.expiryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkExpirations, 1200L, 1200L);
    }

    public void cleanup() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }

    private void checkExpirations() {
        for (Poll poll : polls) {
            if (poll.isActive() && poll.isExpired()) {
                endPoll(poll.getId(), null);
            }
        }
    }

    public synchronized void load() {
        polls.clear();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать polls.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        List<Map<?, ?>> list = config.getMapList("polls");
        for (Map<?, ?> m : list) {
            try {
                int id = ((Number) m.get("id")).intValue();
                UUID creatorUuid = UUID.fromString((String) m.get("creatorUuid"));
                String creatorName = (String) m.get("creatorName");
                String question = (String) m.get("question");
                @SuppressWarnings("unchecked")
                List<String> options = (List<String>) m.get("options");
                long createdAt = ((Number) m.get("createdAt")).longValue();
                long durationMillis = ((Number) m.get("durationMillis")).longValue();
                boolean active = (Boolean) m.get("active");

                Map<UUID, Integer> votes = new ConcurrentHashMap<>();
                if (m.containsKey("votes") && m.get("votes") instanceof Map<?, ?> vMap) {
                    for (Map.Entry<?, ?> entry : vMap.entrySet()) {
                        votes.put(UUID.fromString(entry.getKey().toString()), ((Number) entry.getValue()).intValue());
                    }
                }

                polls.add(new Poll(id, creatorUuid, creatorName, question, options, votes, createdAt, durationMillis, active));
            } catch (Exception ignored) {}
        }
    }

    public synchronized void save() {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Poll poll : polls) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", poll.getId());
            map.put("creatorUuid", poll.getCreatorUuid().toString());
            map.put("creatorName", poll.getCreatorName());
            map.put("question", poll.getQuestion());
            map.put("options", poll.getOptions());
            map.put("createdAt", poll.getCreatedAt());
            map.put("durationMillis", poll.getDurationMillis());
            map.put("active", poll.isActive());

            Map<String, Integer> votesMap = new LinkedHashMap<>();
            for (Map.Entry<UUID, Integer> entry : poll.getVotes().entrySet()) {
                votesMap.put(entry.getKey().toString(), entry.getValue());
            }
            map.put("votes", votesMap);

            serialized.add(map);
        }
        config.set("polls", serialized);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить polls.yml: " + e.getMessage());
        }
    }

    public synchronized Poll createPoll(Player creator, String question, List<String> options, long durationMinutes) {
        int maxId = polls.stream().mapToInt(Poll::getId).max().orElse(0);
        int newId = maxId + 1;
        long now = System.currentTimeMillis();
        long durationMillis = durationMinutes > 0 ? durationMinutes * 60L * 1000L : 0L;

        Poll poll = new Poll(newId, creator.getUniqueId(), creator.getName(), question.trim(), options, new ConcurrentHashMap<>(), now, durationMillis, true);
        polls.add(0, poll);
        save();

        broadcastPollCreation(poll);
        return poll;
    }

    public synchronized boolean vote(Player player, int pollId, int optionIndex) {
        Poll poll = getPoll(pollId);
        if (poll == null || !poll.isActive()) {
            player.sendMessage(plugin.color("&cГолосование #" + pollId + " не существует или уже завершено!"));
            return false;
        }

        if (optionIndex < 0 || optionIndex >= poll.getOptions().size()) {
            player.sendMessage(plugin.color("&cНедопустимый номер варианта! Доступно от 1 до " + poll.getOptions().size()));
            return false;
        }

        UUID uuid = player.getUniqueId();
        Integer prev = poll.getVotes().get(uuid);
        String optName = poll.getOptions().get(optionIndex);

        if (prev != null && prev == optionIndex) {
            player.sendMessage(plugin.color("&eВы уже отдали свой голос за вариант «" + optName + "»!"));
            return false;
        }

        poll.getVotes().put(uuid, optionIndex);
        save();

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        if (prev != null) {
            player.sendMessage(plugin.color("&aВы изменили свой голос на «&e" + optName + "&a»!"));
        } else {
            player.sendMessage(plugin.color("&aВаш голос за «&e" + optName + "&a» учтён! Спасибо за участие."));
        }

        // Показываем обновленный опрос
        showPoll(player, poll);
        return true;
    }

    public synchronized boolean endPoll(int pollId, Player requester) {
        Poll poll = getPoll(pollId);
        if (poll == null || !poll.isActive()) {
            if (requester != null) requester.sendMessage(plugin.color("&cГолосование #" + pollId + " не найдено или уже завершено!"));
            return false;
        }

        if (requester != null && !requester.hasPermission("remotweaks.poll.admin") && !poll.getCreatorUuid().equals(requester.getUniqueId())) {
            requester.sendMessage(plugin.color("&cВы можете завершать только созданные вами голосования!"));
            return false;
        }

        poll.setActive(false);
        save();

        broadcastPollResults(poll);
        return true;
    }

    public Poll getPoll(int id) {
        for (Poll p : polls) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public List<Poll> getPolls() {
        return Collections.unmodifiableList(polls);
    }

    public List<Poll> getActivePolls() {
        return polls.stream().filter(Poll::isActive).toList();
    }

    public void showPoll(Player player, Poll poll) {
        Component separator = Component.text("§8§m--------------------------------------------------");
        player.sendMessage(separator);

        String statusStr = poll.isActive() ? "§a[АКТИВНО]" : "§c[ЗАВЕРШЕНО]";
        player.sendMessage(Component.text("🗳️ Голосование #" + poll.getId() + " " + statusStr + " §7(автор: §e" + poll.getCreatorName() + "§7)", NamedTextColor.GOLD));
        player.sendMessage(Component.text("«" + poll.getQuestion() + "»", NamedTextColor.WHITE, TextDecoration.BOLD));
        player.sendMessage(Component.empty());

        if (poll.isActive()) {
            player.sendMessage(Component.text("§7Нажмите на вариант для голосования:"));
        } else {
            player.sendMessage(Component.text("§7Итоговые результаты:"));
        }

        int total = Math.max(1, poll.getTotalVotes());
        for (int i = 0; i < poll.getOptions().size(); i++) {
            String opt = poll.getOptions().get(i);
            int count = poll.getVoteCount(i);
            int pct = (int) Math.round((double) count * 100.0 / (double) total);
            boolean isChosen = poll.hasVoted(player.getUniqueId(), i);

            NamedTextColor color = isChosen ? NamedTextColor.GREEN : NamedTextColor.AQUA;
            String prefix = isChosen ? "✔ " : "";

            Component btn = Component.text("[" + prefix + opt + " (" + count + " • " + pct + "%)]", color);
            if (poll.isActive()) {
                btn = btn.hoverEvent(HoverEvent.showText(Component.text("§eНажмите, чтобы проголосовать за «" + opt + "»")))
                        .clickEvent(ClickEvent.runCommand("/poll vote " + poll.getId() + " " + (i + 1)));
            }

            player.sendMessage(Component.text("  ").append(btn));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§8Всего голосов: " + poll.getTotalVotes() + "  •  /poll list"));
        player.sendMessage(separator);
    }

    public void broadcastPollCreation(Poll poll) {
        Component separator = Component.text("§8§m--------------------------------------------------");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
            p.sendMessage(separator);
            p.sendMessage(Component.text("🗳️ Новое голосование #" + poll.getId() + " от §e" + poll.getCreatorName() + "§r:", NamedTextColor.GOLD));
            p.sendMessage(Component.text("«" + poll.getQuestion() + "»", NamedTextColor.WHITE, TextDecoration.BOLD));
            p.sendMessage(Component.empty());
            p.sendMessage(Component.text("§7Нажмите на нужный вариант для ответа:"));

            Component buttons = Component.text("  ");
            for (int i = 0; i < poll.getOptions().size(); i++) {
                String opt = poll.getOptions().get(i);
                Component btn = Component.text("[" + opt + " (0)]", NamedTextColor.AQUA)
                        .hoverEvent(HoverEvent.showText(Component.text("§eНажмите, чтобы проголосовать за «" + opt + "»")))
                        .clickEvent(ClickEvent.runCommand("/poll vote " + poll.getId() + " " + (i + 1)));
                buttons = buttons.append(btn).append(Component.text("   "));
            }
            p.sendMessage(buttons);
            p.sendMessage(Component.empty());
            p.sendMessage(Component.text("§8Голосование активно  •  /poll view " + poll.getId()));
            p.sendMessage(separator);
        }
    }

    public void broadcastPollResults(Poll poll) {
        Component separator = Component.text("§8§m--------------------------------------------------");

        int maxVotes = -1;
        String winner = "Ничья";
        boolean tie = false;
        int totalVotes = poll.getTotalVotes();

        for (int i = 0; i < poll.getOptions().size(); i++) {
            int cnt = poll.getVoteCount(i);
            if (cnt > maxVotes) {
                maxVotes = cnt;
                winner = poll.getOptions().get(i);
                tie = false;
            } else if (cnt == maxVotes && cnt > 0) {
                tie = true;
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.1f);
            p.sendMessage(separator);
            p.sendMessage(Component.text("🗳️ Голосование #" + poll.getId() + " завершено!", NamedTextColor.GOLD, TextDecoration.BOLD));
            p.sendMessage(Component.text("«" + poll.getQuestion() + "»", NamedTextColor.WHITE));
            p.sendMessage(Component.empty());

            if (totalVotes == 0) {
                p.sendMessage(Component.text("§7В голосовании никто не принял участия."));
            } else if (tie) {
                p.sendMessage(Component.text("§e⚖ Ничья между лидирующими вариантами! (" + maxVotes + " голосов)", NamedTextColor.YELLOW));
            } else {
                p.sendMessage(Component.text("§a🏆 Победил вариант: §e«" + winner + "» §7(" + maxVotes + " голосов)"));
            }

            p.sendMessage(Component.text("§7Все результаты:"));
            int safeTotal = Math.max(1, totalVotes);
            for (int i = 0; i < poll.getOptions().size(); i++) {
                String opt = poll.getOptions().get(i);
                int cnt = poll.getVoteCount(i);
                int pct = (int) Math.round((double) cnt * 100.0 / (double) safeTotal);
                p.sendMessage(Component.text("  • §f" + opt + ": §e" + cnt + " §7(" + pct + "%)"));
            }

            p.sendMessage(Component.empty());
            p.sendMessage(Component.text("§8Всего участников: " + totalVotes));
            p.sendMessage(separator);
        }
    }
}
