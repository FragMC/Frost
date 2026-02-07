package com.stufy.fragmc.frost.listeners;

import com.stufy.fragmc.frost.Frost;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener {

    private final Frost plugin;

    public PlayerListener(Frost plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Give hotbar items after a short delay
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getHotbarManager().giveHotbarItems(player);
            }
        }.runTaskLater(plugin, 5L);

        // Check if Bedrock player
        if (plugin.isBedrockPlayer(player.getUniqueId())) {
            plugin.getLogger().info("Bedrock player joined: " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Give hotbar items after respawn
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getHotbarManager().giveHotbarItems(player);
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Remove locked items from drops
        if (plugin.getHotbarManager().isEnabled()) {
            event.getDrops().removeIf(item -> plugin.getHotbarManager().isLockedItem(item));
        }
    }
}