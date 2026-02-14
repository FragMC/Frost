package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.models.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ParticleManager {
    private final Frost plugin;
    private final Map<String, ParticleEffect> particleEffects = new HashMap<>();
    private final Map<UUID, Set<BukkitTask>> activeTasks = new HashMap<>();
    private final String serverVersion;

    public ParticleManager(Frost plugin) {
        this.plugin = plugin;
        this.serverVersion = getServerVersion();
        loadParticleEffects();
        startContinuousEffects();
    }

    private String getServerVersion() {
        String version = Bukkit.getBukkitVersion();
        // Extract version like "1.21.1-R0.1-SNAPSHOT" -> "1.21.1"
        if (version.contains("-")) {
            version = version.split("-")[0];
        }
        return version;
    }

    public void loadParticleEffects() {
        particleEffects.clear();
        var config = plugin.getConfig();
        var cosmeticsSection = config.getConfigurationSection("cosmetics");

        if (cosmeticsSection == null) return;

        var particleSection = cosmeticsSection.getConfigurationSection("particle-effects");
        if (particleSection == null) return;

        var itemsSection = particleSection.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String effectId : itemsSection.getKeys(false)) {
            var effectSection = itemsSection.getConfigurationSection(effectId);
            if (effectSection != null) {
                ParticleEffect effect = new ParticleEffect(effectId, "particle-effects", effectSection);

                // Only load if available for this version
                if (effect.isAvailableForVersion(serverVersion)) {
                    particleEffects.put(effectId, effect);
                } else {
                    plugin.getLogger().info("Skipped particle effect '" + effectId +
                            "' (requires newer Minecraft version)");
                }
            }
        }

        plugin.getLogger().info("Loaded " + particleEffects.size() + " particle effects (server: " + serverVersion + ")");
    }

    public Map<String, ParticleEffect> getParticleEffects() {
        return particleEffects;
    }

    public ParticleEffect getParticleEffect(String id) {
        return particleEffects.get(id);
    }

    private void startContinuousEffects() {
        // Task for ALWAYS effects (trails, surrounds)
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                var data = plugin.getPlayerDataManager().getPlayerData(player);
                if (data == null) continue;

                String particleKey = data.equippedCosmetics.get("particle-effects:");
                if (particleKey != null) {
                    ParticleEffect effect = particleEffects.get(particleKey);
                    if (effect != null && effect.getTriggerEvent() == ParticleEffect.TriggerEvent.ALWAYS) {
                        spawnParticles(player, effect);
                    }
                }
            }
        }, 0L, 2L); // Every 2 ticks for smooth effects
    }

    /**
     * Trigger a particle effect for a player based on an event
     */
    public void triggerEffect(Player player, ParticleEffect.TriggerEvent event) {
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        String particleKey = data.equippedCosmetics.get("particle-effects:");
        if (particleKey == null) return;

        ParticleEffect effect = particleEffects.get(particleKey);
        if (effect == null || effect.getTriggerEvent() != event) return;

        // For burst effects, spawn immediately
        if (effect.getEffectType() == ParticleEffect.EffectType.BURST) {
            spawnParticles(player, effect);
        } else {
            // For trail/surround effects on events, create temporary effect
            startTemporaryEffect(player, effect);
        }
    }

    private void startTemporaryEffect(Player player, ParticleEffect effect) {
        // Cancel any existing temporary tasks for this player
        stopTemporaryEffects(player);

        Set<BukkitTask> tasks = activeTasks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

        // Create temporary effect task
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopTemporaryEffects(player);
                return;
            }
            spawnParticles(player, effect);
        }, 0L, 2L);

        tasks.add(task);

        // Cancel after duration
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            tasks.remove(task);
        }, effect.getDuration());
    }

    public void stopTemporaryEffects(Player player) {
        Set<BukkitTask> tasks = activeTasks.remove(player.getUniqueId());
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }
    }

    private void spawnParticles(Player player, ParticleEffect effect) {
        Location loc = player.getLocation();

        switch (effect.getEffectType()) {
            case TRAIL:
                spawnTrailParticles(loc, effect);
                break;
            case SURROUND:
                spawnSurroundParticles(loc, effect);
                break;
            case BURST:
                spawnBurstParticles(loc, effect);
                break;
        }
    }

    private void spawnTrailParticles(Location loc, ParticleEffect effect) {
        // Spawn particles at player's feet
        loc.add(0, 0.1, 0);
        loc.getWorld().spawnParticle(
                effect.getParticle(),
                loc,
                effect.getCount(),
                effect.getOffsetX(),
                effect.getOffsetY(),
                effect.getOffsetZ(),
                effect.getSpeed()
        );
    }

    private void spawnSurroundParticles(Location loc, ParticleEffect effect) {
        // Create a circle of particles around the player
        double radius = effect.getRadius();
        int points = effect.getCount();

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location particleLoc = loc.clone().add(x, 1.0, z);
            particleLoc.getWorld().spawnParticle(
                    effect.getParticle(),
                    particleLoc,
                    1,
                    0, 0, 0,
                    effect.getSpeed()
            );
        }
    }

    private void spawnBurstParticles(Location loc, ParticleEffect effect) {
        // Spawn a burst of particles
        loc.add(0, 1, 0);
        loc.getWorld().spawnParticle(
                effect.getParticle(),
                loc,
                effect.getCount(),
                effect.getOffsetX(),
                effect.getOffsetY(),
                effect.getOffsetZ(),
                effect.getSpeed()
        );
    }

    public void cleanup() {
        // Stop all active tasks
        for (Set<BukkitTask> tasks : activeTasks.values()) {
            tasks.forEach(BukkitTask::cancel);
        }
        activeTasks.clear();
    }
}
