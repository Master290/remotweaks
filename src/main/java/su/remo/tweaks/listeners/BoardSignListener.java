package su.remo.tweaks.listeners;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import su.remo.tweaks.RemoTweaks;

public class BoardSignListener implements Listener {

    private final RemoTweaks plugin;

    public BoardSignListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getState() instanceof Sign sign) {
            boolean isBoardSign = checkSignLines(sign, Side.FRONT) || checkSignLines(sign, Side.BACK);
            if (isBoardSign) {
                Player player = event.getPlayer();
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
                player.performCommand("board list 1");
                event.setCancelled(true);
            }
        }
    }

    private boolean checkSignLines(Sign sign, Side side) {
        var signSide = sign.getSide(side);
        for (String line : signSide.getLines()) {
            if (line == null) continue;
            String lower = line.toLowerCase().trim();
            if (lower.contains("[доска]") || lower.contains("[объявления]") || lower.contains("[board]") || lower.contains("[notice]")) {
                return true;
            }
        }
        return false;
    }
}
