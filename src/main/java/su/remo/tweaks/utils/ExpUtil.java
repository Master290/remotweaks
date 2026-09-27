package su.remo.tweaks.utils;

import org.bukkit.entity.Player;

public class ExpUtil {

    public static int getExpAtLevel(int level) {
        if (level <= 16) {
            return (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
    }

    public static int getExpToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    public static int getTotalExperience(Player player) {
        int exp = getExpAtLevel(player.getLevel());
        exp += Math.round(getExpToNextLevel(player.getLevel()) * player.getExp());
        return exp;
    }

    public static void setTotalExperience(Player player, int totalExp) {
        if (totalExp < 0) totalExp = 0;
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        int level = 0;
        while (true) {
            int nextLevelExp = getExpAtLevel(level + 1);
            if (nextLevelExp > totalExp) {
                break;
            }
            level++;
        }

        int baseExp = getExpAtLevel(level);
        int expInCurrentLevel = totalExp - baseExp;
        int expToNext = getExpToNextLevel(level);

        float progress = (expToNext > 0) ? ((float) expInCurrentLevel / (float) expToNext) : 0f;
        player.setLevel(level);
        player.setExp(progress);
    }
}
