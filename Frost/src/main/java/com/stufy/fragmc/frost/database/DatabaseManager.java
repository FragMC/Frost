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
    private final String databasePath;

    public DatabaseManager(Frost plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.databasePath = new File(dataFolder, "database.db").getAbsolutePath();
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
            // Use connection properties to improve reliability
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "?journal_mode=WAL");
            connection.setAutoCommit(true);
            initDatabase();
            plugin.getLogger().info("Database connected successfully!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
        }
    }

    private void initDatabase() {
        // Updated table structure with profiles and cosmetics
        String sql = "CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "hotbar_locked BOOLEAN DEFAULT 1, " +
                "current_profile VARCHAR(50) DEFAULT 'warrior', " +
                "owned_cosmetics TEXT, " + // JSON: ["cosmetic1", "cosmetic2"]
                "equipped_cosmetics TEXT" + // JSON: {"weapon-skins:0": "golden-spear"}
                ");";
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database tables", e);
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database disconnected successfully!");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }

    /**
     * Get a connection, reconnecting if necessary
     */
    public synchronized Connection getConnection() {
        try {
            // Check if connection is closed or null, reconnect if needed
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("Database connection was closed, reconnecting...");
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "?journal_mode=WAL");
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get/reconnect database connection", e);
        }
        return connection;
    }

    /**
     * Test if connection is alive
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }
}