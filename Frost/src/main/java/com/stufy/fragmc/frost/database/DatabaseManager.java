package com.stufy.fragmc.frost.database;

import com.stufy.fragmc.frost.Frost;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {
    private final Frost plugin;
    private Connection connection;

    public DatabaseManager(Frost plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "database.db");
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            initDatabase();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
        }
    }

    private void initDatabase() {
        // Table to store player data: unlocked modifiers and selected modifiers for
        // each item type
        String sql = "CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "hotbar_locked BOOLEAN DEFAULT 1, " +
                "unlocked_modifiers TEXT, " + // JSON string or comma separated list
                "selected_modifiers TEXT" + // JSON string: {"hotbar_slot_index": "modifier_id"}
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database tables", e);
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
