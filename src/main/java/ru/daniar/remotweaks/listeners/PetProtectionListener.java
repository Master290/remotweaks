package ru.daniar.remotweaks.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.daniar.remotweaks.RemoTweaks;

public class PetProtectionListener implements Listener {

    private final RemoTweaks plugin;

    public PetProtectionListener(RemoTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("pet-protection.enabled", true)) return;

        if (!(event.getEntity() instanceof Tameable pet)) return;
        if (!pet.isTamed()) return;

        Player damager = null;
        if (event.getDamager() instanceof Player player) {
            damager = player;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damager = shooter;
        }

        if (damager == null) return;

        AnimalTamer owner = pet.getOwner();
        if (owner != null && owner.getUniqueId().equals(damager.getUniqueId())) {
            event.setCancelled(true);
            damager.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(plugin.color("&aВы защитили своего питомца от случайного урона!")));
        }
    }
}
