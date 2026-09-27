package ru.daniar.remotweaks.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.daniar.remotweaks.RemoTweaks;

public class ColorFormatListener implements Listener {

    private final RemoTweaks plugin;

    public ColorFormatListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("color-codes.anvil", true)) return;

        String renameText = event.getView().getRenameText();
        if (renameText == null || renameText.isBlank()) return;
        if (!renameText.contains("&")) return;

        if (event.getView().getPlayer() instanceof Player player) {
            if (!player.hasPermission("remotweaks.colors.anvil")) return;
        }

        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) {
            ItemStack first = event.getInventory().getFirstItem();
            if (first != null && first.getType() != Material.AIR) {
                result = first.clone();
            }
        }

        if (result != null && result.getType() != Material.AIR) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                Component colored = LegacyComponentSerializer.legacyAmpersand().deserialize(renameText);
                meta.displayName(colored);
                result.setItemMeta(meta);
                event.setResult(result);

                if (event.getView().getRepairCost() <= 0) {
                    event.getView().setRepairCost(1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!plugin.getConfig().getBoolean("color-codes.signs", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.colors.signs")) return;

        for (int i = 0; i < 4; i++) {
            Component line = event.line(i);
            if (line != null) {
                String plain = PlainTextComponentSerializer.plainText().serialize(line);
                if (plain.contains("&")) {
                    Component colored = LegacyComponentSerializer.legacyAmpersand().deserialize(plain);
                    event.line(i, colored);
                }
            }
        }
    }
}
