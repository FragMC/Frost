package com.stufy.fragmc.frost.handlers;

import com.stufy.fragmc.frost.Frost;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyRewardManager {

    private final Frost plugin;
    private boolean enabled;
    private double baseReward;
    private boolean streakEnabled;
    private Map<Integer, Double> streakMultipliers;
    private int maxStreak;
    private long resetTime;

    public DailyRewardManager(Frost plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("daily-rewards.enabled", true);
        baseReward = plugin.getConfig().getDouble("daily-rewards.base-reward", 100);
        streakEnabled = plugin.getConfig().getBoolean("daily-rewards.streak-enabled", true);
        resetTime = plugin.getConfig().getLong("daily-rewards.reset-time", 86400) * 1000; // Convert to ms
        maxStreak = plugin.getConfig().getInt("daily-rewards.max-streak", 30);

        streakMultipliers = new HashMap<>();
        if (plugin.getConfig().contains("daily-rewards.streak-multipliers")) {
            for (String key : plugin.getConfig().getConfigurationSection("daily-rewards.streak-multipliers").getKeys(false)) {
                try {
                    int day = Integer.parseInt(key);
                    double multiplier = plugin.getConfig().getDouble("daily-rewards.streak-multipliers." + key);
                    streakMultipliers.put(day, multiplier);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid streak day: " + key);
                }
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("daily-rewards.enabled", enabled);
        plugin.saveConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void claimReward(Player player) {
        if (!enabled) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Daily rewards are currently disabled.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processClaim(player);
                } catch (SQLException e) {
                    e.printStackTrace();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "An error occurred while claiming your reward.");
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void processClaim(Player player) throws SQLException {
        UUID uuid = player.getUniqueId();
        Connection conn = plugin.getDatabaseManager().getConnection();
        
        long lastClaim = 0;
        int streak = 0;

        // Get current data
        try (PreparedStatement ps = conn.prepareStatement("SELECT last_claim, streak FROM daily_rewards WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lastClaim = rs.getLong("last_claim");
                    streak = rs.getInt("streak");
                }
            }
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastClaim = currentTime - lastClaim;

        // Check if already claimed
        if (timeSinceLastClaim < resetTime) {
            long timeLeft = resetTime - timeSinceLastClaim;
            String timeLeftStr = formatTime(timeLeft);
            String msg = plugin.getMessage("daily-already-claimed").replace("%time%", timeLeftStr);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage(plugin.getPrefix() + msg);
                }
            }.runTask(plugin);
            return;
        }

        // Calculate streak
        if (streakEnabled) {
            if (timeSinceLastClaim > resetTime * 2 && lastClaim != 0) {
                // Streak broken
                streak = 1;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(plugin.getPrefix() + plugin.getMessage("daily-streak-broken"));
                    }
                }.runTask(plugin);
            } else {
                streak++;
                if (streak > maxStreak) {
                    streak = maxStreak;
                }
            }
        } else {
            streak = 1;
        }

        // Calculate reward
        double reward = calculateReward(streak);
        int finalStreak = streak;

        // Update database
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO daily_rewards (uuid, last_claim, streak) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET last_claim = ?, streak = ?")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, currentTime);
            ps.setInt(3, finalStreak);
            ps.setLong(4, currentTime);
            ps.setInt(5, finalStreak);
            ps.executeUpdate();
        }

        // Give reward on main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getEconomy().depositPlayer(player, reward);
                String msg = plugin.getMessage("daily-claimed")
                        .replace("%amount%", plugin.formatCurrency(reward))
                        .replace("%streak%", String.valueOf(finalStreak));
                player.sendMessage(plugin.getPrefix() + msg);
            }
        }.runTask(plugin);
    }

    private double calculateReward(int streak) {
        double multiplier = 1.0;

        if (streakEnabled && !streakMultipliers.isEmpty()) {
            for (Map.Entry<Integer, Double> entry : streakMultipliers.entrySet()) {
                if (streak >= entry.getKey()) {
                    multiplier = Math.max(multiplier, entry.getValue());
                }
            }
        }

        return baseReward * multiplier;
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
