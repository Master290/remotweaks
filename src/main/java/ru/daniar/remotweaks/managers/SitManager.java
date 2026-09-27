package ru.daniar.remotweaks.managers;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import ru.daniar.remotweaks.RemoTweaks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SitManager {

    private final RemoTweaks plugin;
    private final Map<UUID, ArmorStand> seats = new ConcurrentHashMap<>();
    private final Map<UUID, Location> returnLocations = new ConcurrentHashMap<>();

    public SitManager(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    public boolean isSitting(Player player) {
        return seats.containsKey(player.getUniqueId());
    }

    public boolean sitOnBlock(Player player, Block block) {
        if (!plugin.getConfig().getBoolean("sitting.enabled", true)) {
            return false;
        }

        if (isSitting(player)) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("sitting.messages.already-sitting")));
            return false;
        }

        Block above = block.getRelative(BlockFace.UP);
        if (!above.isPassable()) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("sitting.messages.cannot-sit-here")));
            return false;
        }

        for (ArmorStand stand : seats.values()) {
            if (stand.isValid() && stand.getLocation().getBlock().equals(above.getLocation().getBlock())) {
                player.sendMessage(plugin.color(plugin.getConfig().getString("sitting.messages.seat-occupied")));
                return false;
            }
        }

        double offset = plugin.getConfig().getDouble("sitting.offset", 0.025);
        double stairXzOffset = plugin.getConfig().getDouble("sitting.stairs-xz-offset", 0.12);
        Location sitLoc = block.getLocation().add(0.5, 0.0, 0.5);

        if (block.getBlockData() instanceof Stairs stairs) {
            boolean isBottom = stairs.getHalf() == Bisected.Half.BOTTOM;
            sitLoc.setY(block.getY() + (isBottom ? 0.5 : 1.0) + offset);

            BlockFace facing = stairs.getFacing();
            switch (facing) {
                case NORTH -> {
                    sitLoc.add(0, 0, -stairXzOffset);
                    sitLoc.setYaw(0f);
                }
                case SOUTH -> {
                    sitLoc.add(0, 0, stairXzOffset);
                    sitLoc.setYaw(180f);
                }
                case WEST -> {
                    sitLoc.add(-stairXzOffset, 0, 0);
                    sitLoc.setYaw(-90f);
                }
                case EAST -> {
                    sitLoc.add(stairXzOffset, 0, 0);
                    sitLoc.setYaw(90f);
                }
                default -> sitLoc.setYaw(player.getLocation().getYaw());
            }
        } else if (block.getBlockData() instanceof Slab slab) {
            boolean isBottom = slab.getType() == Slab.Type.BOTTOM;
            sitLoc.setY(block.getY() + (isBottom ? 0.5 : 1.0) + offset);
            sitLoc.setYaw(player.getLocation().getYaw());
        } else {
            sitLoc.setY(block.getY() + 1.0 + offset);
            sitLoc.setYaw(player.getLocation().getYaw());
        }

        createSeatAndSit(player, sitLoc);
        return true;
    }

    public boolean sitAnywhere(Player player) {
        if (!plugin.getConfig().getBoolean("sitting.enabled", true)) {
            return false;
        }

        if (isSitting(player)) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("sitting.messages.already-sitting")));
            return false;
        }

        Location loc = player.getLocation();
        Block blockBelow = loc.getBlock().getRelative(BlockFace.DOWN);

        if (loc.getBlock().isPassable() && blockBelow.isPassable()) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("sitting.messages.cannot-sit-air")));
            return false;
        }

        double offset = plugin.getConfig().getDouble("sitting.offset", 0.025);
        Location sitLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + offset, loc.getZ(), loc.getYaw(), 0f);

        createSeatAndSit(player, sitLoc);
        return true;
    }

    private void createSeatAndSit(Player player, Location sitLoc) {
        returnLocations.put(player.getUniqueId(), player.getLocation());

        ArmorStand seat = sitLoc.getWorld().spawn(sitLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(false);
            stand.addScoreboardTag("remotweaks_seat");

            // Убираем шкалу сердец маунта в хотбаре клиента:
            // Клиент рендерит сердца транспорта только при maxHealth > 1.4f.
            // При значении 1.0 количество отображаемых сердец равно 0.
            try {
                AttributeInstance maxHealth = stand.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(1.0);
                }
                stand.setHealth(1.0);
            } catch (Throwable ignored) {
            }
        });

        seat.addPassenger(player);
        seats.put(player.getUniqueId(), seat);
    }

    public void removeSeat(Player player) {
        ArmorStand seat = seats.remove(player.getUniqueId());
        Location returnLoc = returnLocations.remove(player.getUniqueId());

        if (seat != null && seat.isValid()) {
            seat.eject();
            seat.remove();
        }

        if (returnLoc != null && player.isValid()) {
            player.teleport(returnLoc);
        }
    }

    public void cleanup(UUID playerId) {
        returnLocations.remove(playerId);
        ArmorStand seat = seats.remove(playerId);
        if (seat != null && seat.isValid()) {
            seat.eject();
            seat.remove();
        }
    }

    public void cleanupAll() {
        for (ArmorStand seat : seats.values()) {
            if (seat != null && seat.isValid()) {
                seat.eject();
                seat.remove();
            }
        }
        seats.clear();
        returnLocations.clear();
    }
}
