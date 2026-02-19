package com.stufy.fragmc.frost.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stufy.fragmc.frost.Frost;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class PlayerDataManager {
    private final Frost plugin;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final Gson gson = new Gson();

    public PlayerDataManager(Frost plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = new PlayerData();
        data.currentProfile = plugin.getProfileManager().getDefaultProfile();

        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                plugin.getLogger().severe("Database connection is null when loading player data for " + player.getName());
                cache.put(uuid, data);
                return;
            }

            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                data.hotbarLocked = rs.getBoolean("hotbar_locked");
                data.currentProfile = rs.getString("current_profile");

                String ownedCosmeticsJson = rs.getString("owned_cosmetics");
                String equippedCosmeticsJson = rs.getString("equipped_cosmetics");

                if (ownedCosmeticsJson != null && !ownedCosmeticsJson.isEmpty()) {
                    try {
                        Set<String> owned = gson.fromJson(ownedCosmeticsJson, new TypeToken<Set<String>>() {}.getType());
                        if (owned != null) {
                            data.ownedCosmetics = owned;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to parse owned cosmetics for " + player.getName() + ": " + e.getMessage());
                    }
                }

                if (equippedCosmeticsJson != null && !equippedCosmeticsJson.isEmpty()) {
                    try {
                        Map<String, String> equipped = gson.fromJson(equippedCosmeticsJson, new TypeToken<Map<String, String>>() {}.getType());
                        if (equipped != null) {
                            data.equippedCosmetics = equipped;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to parse equipped cosmetics for " + player.getName() + ": " + e.getMessage());
                    }
                }
            } else {
                // New player, save default data
                savePlayerData(player, data);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + player.getName(), e);
        }

        cache.put(uuid, data);

        // Apply equipped cosmetics after loading
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            applyEquippedCosmetics(player);
        }, 10L); // Delay by 10 ticks to ensure player is fully loaded
    }

    /**
     * Apply all equipped cosmetics to a player
     */
    private void applyEquippedCosmetics(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) return;

        // Apply armor cosmetics
        for (Map.Entry<String, String> entry : data.equippedCosmetics.entrySet()) {
            String key = entry.getKey();
            String cosmeticId = entry.getValue();

            if (key.startsWith("armor-cosmetics:")) {
                var cosmetic = plugin.getCosmeticManager().getCosmetic(cosmeticId);
                if (cosmetic != null) {
                    plugin.getCosmeticManager().applyArmorCosmetic(player, cosmetic);
                }
            }
        }

        // Refresh hotbar items to apply weapon skins
        if (plugin.getHotbarLockListener() != null) {
            plugin.getHotbarLockListener().giveHotbarItems(player);
        }
    }

    public void savePlayerData(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data != null) {
            savePlayerData(player, data);
        }
    }

    public void savePlayerData(Player player, PlayerData data) {
        if (data == null) return;
        UUID uuid = player.getUniqueId();

        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) {
                plugin.getLogger().severe("Database connection is null when saving player data for " + player.getName());
                return;
            }

            String sql = "INSERT OR REPLACE INTO player_data (uuid, hotbar_locked, current_profile, owned_cosmetics, equipped_cosmetics) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, data.hotbarLocked);
            stmt.setString(3, data.currentProfile);
            stmt.setString(4, gson.toJson(data.ownedCosmetics));
            stmt.setString(5, gson.toJson(data.equippedCosmetics));
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.getName(), e);
        }
    }

    public void saveAllPlayerData() {
        plugin.getLogger().info("Saving all player data...");
        int saved = 0;
        for (UUID uuid : new HashSet<>(cache.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                savePlayerData(player);
                saved++;
            }
        }
        plugin.getLogger().info("Saved data for " + saved + " players");
    }

    public void unloadPlayerData(Player player) {
        savePlayerData(player);
        cache.remove(player.getUniqueId());
    }

    public PlayerData getPlayerData(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) {
            plugin.getLogger().warning("Player data not in cache for " + player.getName() + ", loading now...");
            loadPlayerData(player);
            data = cache.get(player.getUniqueId());
        }
        return data;
    }

    public static class PlayerData {
        public boolean hotbarLocked = true;
        public String currentProfile = "warrior";
        public Set<String> ownedCosmetics = new HashSet<>();
        public Map<String, String> equippedCosmetics = new HashMap<>();
    }
}