package ru.daniar.remotweaks.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.daniar.remotweaks.RemoTweaks;

public class ItemFrameListener implements Listener {

    private final RemoTweaks plugin;

    public ItemFrameListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFrameInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;

        if (!plugin.getConfig().getBoolean("invisible-frames.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.frames")) return;

        boolean requireSneak = plugin.getConfig().getBoolean("invisible-frames.require-sneak", true);
        if (requireSneak && !player.isSneaking()) return;

        String toolName = plugin.getConfig().getString("invisible-frames.tool", "SHEARS");
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (!heldItem.getType().name().equalsIgnoreCase(toolName)) return;

        // Отменяем стандартное вращение предмета в рамке
        event.setCancelled(true);

        boolean newVisible = !frame.isVisible();
        frame.setVisible(newVisible);

        if (plugin.getConfig().getBoolean("invisible-frames.play-sound", true)) {
            player.playSound(frame.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0f, newVisible ? 0.8f : 1.2f);
        }

        String msgKey = newVisible ? "invisible-frames.messages.frame-visible" : "invisible-frames.messages.frame-invisible";
        String message = plugin.getConfig().getString(msgKey);
        if (message != null && !message.isEmpty()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(plugin.color(message)));
        }
    }
}
