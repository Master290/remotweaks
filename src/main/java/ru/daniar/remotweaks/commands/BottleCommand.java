package ru.daniar.remotweaks.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.daniar.remotweaks.RemoTweaks;
import ru.daniar.remotweaks.utils.ExpUtil;

import java.util.Collections;
import java.util.List;

public class BottleCommand implements CommandExecutor, TabCompleter {

    private final RemoTweaks plugin;

    public BottleCommand(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.color("&cЭту команду могут использовать только игроки!"));
            return true;
        }

        if (!player.hasPermission("remotweaks.bottle")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав!")));
            return true;
        }

        int availableBottles = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GLASS_BOTTLE) {
                availableBottles += item.getAmount();
            }
        }

        if (availableBottles <= 0) {
            player.sendMessage(plugin.color("&cУ вас нет пустых стеклянных бутылочек в инвентаре!"));
            return true;
        }

        int cost = plugin.getConfig().getInt("exp-bottling.cost-per-bottle", 10);
        int playerExp = ExpUtil.getTotalExperience(player);

        if (playerExp < cost) {
            player.sendMessage(plugin.color("&cУ вас недостаточно опыта! Нужно минимум " + cost + " XP на 1 пузырёк."));
            return true;
        }

        int maxAffordable = playerExp / cost;
        int targetAmount;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("all")) {
                targetAmount = Math.min(availableBottles, maxAffordable);
            } else {
                try {
                    int parsed = Integer.parseInt(args[0]);
                    if (parsed <= 0) {
                        player.sendMessage(plugin.color("&cКоличество должно быть положительным числом!"));
                        return true;
                    }
                    targetAmount = Math.min(parsed, Math.min(availableBottles, maxAffordable));
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.color("&cИспользование: /bottle [число | all]"));
                    return true;
                }
            }
        } else {
            // По умолчанию - сколько держим в руке, либо 1 штука
            ItemStack hand = player.getInventory().getItemInMainHand();
            int defaultAmount = (hand.getType() == Material.GLASS_BOTTLE) ? hand.getAmount() : 1;
            targetAmount = Math.min(defaultAmount, Math.min(availableBottles, maxAffordable));
        }

        if (targetAmount <= 0) {
            player.sendMessage(plugin.color("&cНе удалось создать пузырьки опыта!"));
            return true;
        }

        int totalCost = targetAmount * cost;
        ExpUtil.setTotalExperience(player, playerExp - totalCost);

        // Изымаем пустые бутылочки
        removeGlassBottles(player, targetAmount);

        // Выдаем пузырьки опыта
        var leftovers = player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, targetAmount));
        for (ItemStack drop : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        player.sendMessage(plugin.color("&a✔ Успешно упаковано пузырьков опыта: &e" + targetAmount + " шт. &7(-" + totalCost + " XP)"));
        return true;
    }

    private void removeGlassBottles(Player player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.GLASS_BOTTLE) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("1", "16", "32", "64", "all");
        }
        return Collections.emptyList();
    }
}
