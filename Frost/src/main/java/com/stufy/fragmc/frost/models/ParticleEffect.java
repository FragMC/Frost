package com.stufy.fragmc.frost.models;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class ParticleEffect {
    private final String id;
    private final String categoryId;
    private final String name;
    private final List<String> description;
    private final double price;
    private final Particle particle;
    private final int count;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double speed;
    private final EffectType effectType;
    private final TriggerEvent triggerEvent;
    private final double radius; // For SURROUND type
    private final int duration; // How long effect lasts in ticks

    public enum EffectType {
        TRAIL,      // Particles follow player
        SURROUND,   // Particles circle around player
        BURST       // One-time particle burst
    }

    public enum TriggerEvent {
        ALWAYS,           // Continuous effect (1.8+)
        JUMP,             // When player jumps (1.8+)
        SNEAK,            // When player sneaks (1.8+)
        SPRINT,           // When player sprints (1.8+)
        DOUBLE_JUMP,      // When player double jumps (1.9+)
        GLIDE,            // When using elytra (1.9+)
        SWIM,             // When swimming (1.13+)
        RIPTIDE,          // When using riptide trident (1.13+)
        MACE_SMASH,       // When using mace smash attack (1.21+)
        DAMAGE_TAKEN,     // When taking damage (1.8+)
        DAMAGE_DEALT,     // When dealing damage (1.8+)
        KILL,             // When killing an entity (1.8+)
        BLOCK_BREAK,      // When breaking a block (1.8+)
        BLOCK_PLACE       // When placing a block (1.8+)
    }

    public ParticleEffect(String id, String categoryId, ConfigurationSection section) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = section.getString("name", id);
        this.description = section.getStringList("description");
        this.price = section.getDouble("price", 0.0);

        // Parse particle type
        String particleStr = section.getString("particle", "FLAME");
        Particle parsedParticle = null;
        try {
            parsedParticle = Particle.valueOf(particleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback to FLAME if invalid
            parsedParticle = Particle.FLAME;
        }
        this.particle = parsedParticle;

        this.count = section.getInt("count", 1);
        this.offsetX = section.getDouble("offset-x", 0.0);
        this.offsetY = section.getDouble("offset-y", 0.0);
        this.offsetZ = section.getDouble("offset-z", 0.0);
        this.speed = section.getDouble("speed", 0.0);
        this.radius = section.getDouble("radius", 1.0);
        this.duration = section.getInt("duration", 20);

        // Parse effect type
        // Parse effect type
        String effectTypeStr = section.getString("effect-type", "TRAIL");
        EffectType parsedEffectType;
        try {
            parsedEffectType = EffectType.valueOf(effectTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            parsedEffectType = EffectType.TRAIL;
        }
        this.effectType = parsedEffectType;

        // Parse trigger event
        String triggerStr = section.getString("trigger", "ALWAYS");
        TriggerEvent parsedTriggerEvent;
        try {
            parsedTriggerEvent = TriggerEvent.valueOf(triggerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            parsedTriggerEvent = TriggerEvent.ALWAYS;
        }
        this.triggerEvent = parsedTriggerEvent;

    }

    // Getters
    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public List<String> getDescription() { return description; }
    public double getPrice() { return price; }
    public Particle getParticle() { return particle; }
    public int getCount() { return count; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getOffsetZ() { return offsetZ; }
    public double getSpeed() { return speed; }
    public EffectType getEffectType() { return effectType; }
    public TriggerEvent getTriggerEvent() { return triggerEvent; }
    public double getRadius() { return radius; }
    public int getDuration() { return duration; }

    /**
     * Check if this particle effect is available for the current server version
     */
    public boolean isAvailableForVersion(String minecraftVersion) {
        // Parse version (e.g., "1.21.1" -> 1.21)
        double version = parseVersion(minecraftVersion);

        return switch (triggerEvent) {
            case DOUBLE_JUMP, GLIDE -> version >= 1.09;
            case SWIM, RIPTIDE -> version >= 1.13;
            case MACE_SMASH -> version >= 1.21;
            default -> true; // Available in all versions (1.8+)
        };
    }

    private double parseVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return major + (minor / 100.0);
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        return 1.08; // Default to 1.8
    }
}
