package com.stufy.fragmc.frost;

import com.stufy.fragmc.frost.api.FrostAPI;
import com.stufy.fragmc.frost.commands.EquipCommand;
import com.stufy.fragmc.frost.commands.FrostCommand;
import com.stufy.fragmc.frost.commands.ShopCommand;
import com.stufy.fragmc.frost.commands.ToggleLockCommand;
import com.stufy.fragmc.frost.database.DatabaseManager;
import com.stufy.fragmc.frost.listeners.HotbarLockListener;
import com.stufy.fragmc.frost.listeners.ParticleListener;
import com.stufy.fragmc.frost.managers.ConfigManager;
import com.stufy.fragmc.frost.managers.CosmeticManager;
import com.stufy.fragmc.frost.managers.GuiManager;
import com.stufy.fragmc.frost.managers.ParticleManager;
import com.stufy.fragmc.frost.managers.PlayerDataManager;
import com.stufy.fragmc.frost.managers.ProfileManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class Frost extends JavaPlugin {

    private static Frost instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private ProfileManager profileManager;
    private CosmeticManager cosmeticManager;
    private ParticleManager particleManager;
    private HotbarLockListener hotbarLockListener;
    private ParticleListener particleListener;
    private GuiManager guiManager;
    private FrostAPI api;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Setup database
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Vault or EssentialsX not found! Economy features will be disabled.");
        }

        // Initialize managers in correct order
        configManager = new ConfigManager(this);
        profileManager = new ProfileManager(this);
        cosmeticManager = new CosmeticManager(this);
        particleManager = new ParticleManager(this);
        playerDataManager = new PlayerDataManager(this);
        guiManager = new GuiManager(this);

        // Initialize API
        api = new FrostAPI(this);
        getServer().getServicesManager().register(FrostAPI.class, api, this, ServicePriority.Normal);

        // Register listeners
        hotbarLockListener = new HotbarLockListener(this);
        particleListener = new ParticleListener(this);
        getServer().getPluginManager().registerEvents(hotbarLockListener, this);
        getServer().getPluginManager().registerEvents(particleListener, this);

        // Register commands
        getCommand("frost").setExecutor(new FrostCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("equip").setExecutor(new EquipCommand(this));
        getCommand("togglelock").setExecutor(new ToggleLockCommand(this));

        getLogger().info("Frost Plugin has been enabled!");
        getLogger().info("Using SQLite database for player data storage");
        getLogger().info("API registered for external plugin integration");
    }

    @Override
    public void onDisable() {
        if (particleManager != null) {
            particleManager.cleanup();
        }

        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("Frost Plugin has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static Frost getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public HotbarLockListener getHotbarLockListener() {
        return hotbarLockListener;
    }

    public FrostAPI getAPI() {
        return api;
    }
}
