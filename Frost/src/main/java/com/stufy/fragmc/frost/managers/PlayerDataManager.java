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
        // Default data
        PlayerData data = new PlayerData();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    data.hotbarLocked = rs.getBoolean("hotbar_locked");
                    String unlockedJson = rs.getString("unlocked_modifiers");
                    String selectedJson = rs.getString("selected_modifiers");

                    if (unlockedJson != null) {
                        data.unlockedModifiers = gson.fromJson(unlockedJson, new TypeToken<Set<String>>() {
                        }.getType());
                    }
                    if (selectedJson != null) {
                        data.selectedModifiers = gson.fromJson(selectedJson, new TypeToken<Map<Integer, String>>() {
                        }.getType());
                    }
                } else {
                    // Create entry if not exists
                    savePlayerData(player, data);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + player.getName(), e);
        }

        cache.put(uuid, data);
    }

    public void savePlayerData(Player player) {
        savePlayerData(player, cache.get(player.getUniqueId()));
    }

    public void savePlayerData(Player player, PlayerData data) {
        if (data == null)
            return;
        UUID uuid = player.getUniqueId();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                String sql = "INSERT OR REPLACE INTO player_data (uuid, hotbar_locked, unlocked_modifiers, selected_modifiers) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, uuid.toString());
                stmt.setBoolean(2, data.hotbarLocked);
                stmt.setString(3, gson.toJson(data.unlockedModifiers));
                stmt.setString(4, gson.toJson(data.selectedModifiers));
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.getName(), e);
        }
    }

    public void saveAllPlayerData() {
        for (UUID uuid : cache.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                savePlayerData(player);
            }
        }
    }

    public void unloadPlayerData(Player player) {
        savePlayerData(player);
        cache.remove(player.getUniqueId());
    }

    public PlayerData getPlayerData(Player player) {
        return cache.get(player.getUniqueId());
    }

    public static class PlayerData {
        public boolean hotbarLocked = true;
        public Set<String> unlockedModifiers = new HashSet<>();
        public Map<Integer, String> selectedModifiers = new HashMap<>(); // Slot -> ModifierID
    }
}
