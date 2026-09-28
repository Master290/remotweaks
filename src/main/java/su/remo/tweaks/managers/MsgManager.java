package su.remo.tweaks.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.remo.tweaks.RemoTweaks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MsgManager {

    private final RemoTweaks plugin;
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();
    private final Set<UUID> socialSpyUsers = ConcurrentHashMap.newKeySet();

    public MsgManager(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    public void sendPrivateMessage(CommandSender sender, Player target, String message) {
        String senderName = sender.getName();
        String targetName = target.getName();

        Component senderComp = Component.text("[Я -> " + targetName + "] ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.WHITE));

        Component targetComp = Component.text("[" + senderName + " -> Мне] ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.WHITE));

        sender.sendMessage(senderComp);
        target.sendMessage(targetComp);

        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);

        UUID senderUuid = (sender instanceof Player p) ? p.getUniqueId() : null;
        UUID targetUuid = target.getUniqueId();

        if (senderUuid != null) {
            lastMessaged.put(senderUuid, targetUuid);
            lastMessaged.put(targetUuid, senderUuid);
        }

        // SocialSpy уведомление
        Component spyComp = Component.text("[Spy] " + senderName + " -> " + targetName + ": " + message, NamedTextColor.DARK_GRAY);
        for (UUID spyUuid : socialSpyUsers) {
            if (spyUuid.equals(senderUuid) || spyUuid.equals(targetUuid)) continue;
            Player spyPlayer = Bukkit.getPlayer(spyUuid);
            if (spyPlayer != null && spyPlayer.isOnline()) {
                spyPlayer.sendMessage(spyComp);
            }
        }
    }

    public UUID getLastMessaged(UUID uuid) {
        return lastMessaged.get(uuid);
    }

    public boolean toggleSocialSpy(UUID uuid) {
        if (socialSpyUsers.contains(uuid)) {
            socialSpyUsers.remove(uuid);
            return false;
        } else {
            socialSpyUsers.add(uuid);
            return true;
        }
    }

    public boolean isSocialSpy(UUID uuid) {
        return socialSpyUsers.contains(uuid);
    }

    public void removePlayer(UUID uuid) {
        lastMessaged.remove(uuid);
        socialSpyUsers.remove(uuid);
    }
}
