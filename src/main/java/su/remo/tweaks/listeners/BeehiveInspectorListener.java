package su.remo.tweaks.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import su.remo.tweaks.RemoTweaks;

public class BeehiveInspectorListener implements Listener {

    private final RemoTweaks plugin;

    public BeehiveInspectorListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBeehiveInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.BEEHIVE && block.getType() != Material.BEE_NEST) return;

        if (!plugin.getConfig().getBoolean("beehive-inspector.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("remotweaks.beehive")) return;

        // Shift + ПКМ пустой рукой
        if (!player.isSneaking()) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        if (block.getBlockData() instanceof Beehive data &&
            block.getState() instanceof org.bukkit.block.Beehive state) {

            event.setCancelled(true);

            int honey = data.getHoneyLevel();
            int maxHoney = data.getMaximumHoneyLevel();
            int bees = state.getEntityCount();
            int maxBees = state.getMaxEntities();

            String readySuffix = (honey >= maxHoney)
                    ? plugin.getConfig().getString("beehive-inspector.ready-suffix", " &a(Готов к сбору!)")
                    : "";

            String template = plugin.getConfig().getString("beehive-inspector.actionbar-message",
                    "&6🍯 Мёд: &e{honey}/{max_honey}{ready} &7| &6🐝 Пчёл внутри: &e{bees}/{max_bees}");

            String message = template
                    .replace("{honey}", String.valueOf(honey))
                    .replace("{max_honey}", String.valueOf(maxHoney))
                    .replace("{ready}", readySuffix)
                    .replace("{bees}", String.valueOf(bees))
                    .replace("{max_bees}", String.valueOf(maxBees));

            player.sendActionBar(plugin.color(message));
            player.playSound(block.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 0.8f, 1.2f);
        }
    }
}
