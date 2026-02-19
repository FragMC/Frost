package com.stufy.fragmc.frost.listeners;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.models.ParticleEffect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class ParticleListener implements Listener {
    private final Frost plugin;

    public ParticleListener(Frost plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check for jumping (Y velocity increase)
        if (event.getTo().getY() > event.getFrom().getY() && player.getVelocity().getY() > 0.4) {
            plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.JUMP);
        }

        // Check for sprinting
        if (player.isSprinting()) {
            plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.SPRINT);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            plugin.getParticleManager().triggerEffect(event.getPlayer(), ParticleEffect.TriggerEvent.SNEAK);
        }
    }

    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player && event.isGliding()) {
            plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.GLIDE);
        }
    }

    @EventHandler
    public void onPlayerSwim(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isSwimming()) {
            plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.SWIM);
        }
    }

    @EventHandler
    public void onRiptide(PlayerRiptideEvent event) {
        plugin.getParticleManager().triggerEffect(event.getPlayer(), ParticleEffect.TriggerEvent.RIPTIDE);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.DAMAGE_TAKEN);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.DAMAGE_DEALT);

            try {
                if (player.getVelocity().getY() < -0.5 && player.getFallDistance() > 3) {
                    if (player.getInventory().getItemInMainHand().getType().toString().equals("MACE")) {
                        plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.MACE_SMASH);
                    }
                }
            } catch (Exception e) {
            }

            if (player.getInventory().getItemInMainHand().getType().toString().equals("SPEAR")) {
                plugin.getParticleManager().triggerEffect(player, ParticleEffect.TriggerEvent.SPEAR_CHANGE);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            plugin.getParticleManager().triggerEffect(killer, ParticleEffect.TriggerEvent.KILL);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getParticleManager().triggerEffect(event.getPlayer(), ParticleEffect.TriggerEvent.BLOCK_BREAK);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        plugin.getParticleManager().triggerEffect(event.getPlayer(), ParticleEffect.TriggerEvent.BLOCK_PLACE);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up any active particle tasks
        plugin.getParticleManager().stopTemporaryEffects(event.getPlayer());
    }
}
