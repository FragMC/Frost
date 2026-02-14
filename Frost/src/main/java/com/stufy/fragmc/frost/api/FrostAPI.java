package com.stufy.fragmc.frost.api;

import com.stufy.fragmc.frost.Frost;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Public API for external plugins to interact with Frost
 */
public class FrostAPI {
    private final Frost plugin;

    public FrostAPI(Frost plugin) {
        this.plugin = plugin;
    }

    /**
     * Set a player's profile
     * @param player The player
     * @param profileId The profile ID from config
     * @return true if successful
     */
    public boolean setPlayerProfile(Player player, String profileId) {
        return plugin.getProfileManager().setPlayerProfile(player, profileId);
    }

    /**
     * Get a player's current profile ID
     * @param player The player
     * @return The profile ID or null
     */
    public String getPlayerProfile(Player player) {
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        return data != null ? data.currentProfile : null;
    }

    /**
     * Check if a profile exists
     * @param profileId The profile ID
     * @return true if it exists
     */
    public boolean profileExists(String profileId) {
        return plugin.getProfileManager().getProfile(profileId) != null;
    }

    /**
     * Get all available profile IDs
     * @return Set of profile IDs
     */
    public Set<String> getAvailableProfiles() {
        return plugin.getProfileManager().getAllProfileIds();
    }

    /**
     * Check if player owns a cosmetic
     * @param player The player
     * @param cosmeticId The cosmetic ID
     * @return true if owned
     */
    public boolean playerOwnsCosmetic(Player player, String cosmeticId) {
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        return data != null && data.ownedCosmetics.contains(cosmeticId);
    }

    /**
     * Give a cosmetic to a player (bypasses economy)
     * @param player The player
     * @param cosmeticId The cosmetic ID
     * @return true if successful
     */
    public boolean giveCosmetic(Player player, String cosmeticId) {
        if (plugin.getCosmeticManager().getCosmetic(cosmeticId) == null) {
            return false;
        }
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return false;

        data.ownedCosmetics.add(cosmeticId);
        plugin.getPlayerDataManager().savePlayerData(player);
        return true;
    }

    /**
     * Remove a cosmetic from a player
     * @param player The player
     * @param cosmeticId The cosmetic ID
     * @return true if successful
     */
    public boolean removeCosmetic(Player player, String cosmeticId) {
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return false;

        data.ownedCosmetics.remove(cosmeticId);
        plugin.getPlayerDataManager().savePlayerData(player);
        return true;
    }

    /**
     * Get Frost plugin instance
     * @return Frost plugin
     */
    public Frost getPlugin() {
        return plugin;
    }
}
