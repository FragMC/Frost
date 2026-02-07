package com.stufy.fragmc.frost;

import com.stufy.fragmc.frost.commands.DailyRewardCommand;
import com.stufy.fragmc.frost.commands.FrostCommand;
import com.stufy.fragmc.frost.commands.ShopCommand;
import com.stufy.fragmc.frost.database.DatabaseManager;
import com.stufy.fragmc.frost.gui.BedrockMenuHandler;
import com.stufy.fragmc.frost.handlers.HotbarManager;
import com.stufy.fragmc.frost.handlers.ShopManager;
import com.stufy.fragmc.frost.handlers.DailyRewardManager;
import com.stufy.fragmc.frost.listeners.HotbarListener;
import com.stufy.fragmc.frost.listeners.PlayerListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.logging.Level;

public class Frost extends JavaPlugin {

    private static Frost instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private HotbarManager hotbarManager;
    private ShopManager shopManager;
    private DailyRewardManager dailyRewardManager;
    private BedrockMenuHandler bedrockMenuHandler;
    private boolean floodgateEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault or an economy plugin not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for Floodgate
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            floodgateEnabled = true;
            bedrockMenuHandler = new BedrockMenuHandler(this);
            getLogger().info("Floodgate detected! Bedrock support enabled.");
        }

        // Initialize managers
        databaseManager = new DatabaseManager(this);
        hotbarManager = new HotbarManager(this);
        shopManager = new ShopManager(this);
        dailyRewardManager = new DailyRewardManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new HotbarListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register commands
        getCommand("frost").setExecutor(new FrostCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("dailyreward").setExecutor(new DailyRewardCommand(this));

        getLogger().info("Frost Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save any data
        if (databaseManager != null) {
            databaseManager.close();
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

    public void reload() {
        reloadConfig();
        hotbarManager.reload();
        shopManager.reload();
        dailyRewardManager.reload();
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

    public HotbarManager getHotbarManager() {
        return hotbarManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public DailyRewardManager getDailyRewardManager() {
        return dailyRewardManager;
    }

    public BedrockMenuHandler getBedrockMenuHandler() {
        return bedrockMenuHandler;
    }

    public boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }

    public boolean isBedrockPlayer(java.util.UUID uuid) {
        if (!floodgateEnabled) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Exception e) {
            return false;
        }
    }

    public String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages." + path, "&cMessage not found: " + path));
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public String getCurrencySymbol() {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("currency.symbol", "♦"));
    }

    public String formatCurrency(double amount) {
        String symbol = getCurrencySymbol();
        String format = getConfig().getString("currency.format", "%symbol%%amount%");
        String amountStr = String.format("%.0f", amount);

        return ChatColor.translateAlternateColorCodes('&',
                format.replace("%symbol%", symbol).replace("%amount%", amountStr));
    }
}