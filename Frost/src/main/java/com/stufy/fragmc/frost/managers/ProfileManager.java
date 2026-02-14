package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.models.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProfileManager {
    private final Frost plugin;
    private final Map<String, Profile> profiles = new HashMap<>();

    public ProfileManager(Frost plugin) {
        this.plugin = plugin;
        loadProfiles();
    }

    public void loadProfiles() {
        profiles.clear();
        var config = plugin.getConfig();
        var profilesSection = config.getConfigurationSection("profiles");

        if (profilesSection == null) {
            plugin.getLogger().warning("No profiles found in config!");
            return;
        }

        for (String profileId : profilesSection.getKeys(false)) {
            var profileSection = profilesSection.getConfigurationSection(profileId);
            if (profileSection == null) continue;

            String displayName = profileSection.getString("display-name", profileId);
            String description = profileSection.getString("description", "");

            Map<Integer, ItemStack> hotbarItems = new HashMap<>();
            var itemsSection = profileSection.getConfigurationSection("hotbar-items");
            if (itemsSection != null) {
                for (String slotStr : itemsSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        var itemSection = itemsSection.getConfigurationSection(slotStr);
                        if (itemSection != null) {
                            ItemStack item = plugin.getConfigManager().createItemFromConfig(itemSection);
                            hotbarItems.put(slot, item);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid slot in profile " + profileId + ": " + slotStr);
                    }
                }
            }

            Profile profile = new Profile(profileId, displayName, description, hotbarItems);
            profiles.put(profileId, profile);
            plugin.getLogger().info("Loaded profile: " + profileId + " with " + hotbarItems.size() + " items");
        }
    }

    public Profile getProfile(String profileId) {
        return profiles.get(profileId);
    }

    public Set<String> getAllProfileIds() {
        return profiles.keySet();
    }

    public String getDefaultProfile() {
        return plugin.getConfig().getString("settings.default-profile", "warrior");
    }

    public boolean setPlayerProfile(Player player, String profileId) {
        if (!profiles.containsKey(profileId)) {
            return false;
        }

        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return false;

        data.currentProfile = profileId;
        plugin.getPlayerDataManager().savePlayerData(player);

        // Refresh hotbar items
        if (plugin.getHotbarLockListener() != null) {
            plugin.getHotbarLockListener().giveHotbarItems(player);
        }

        Profile profile = profiles.get(profileId);
        player.sendMessage(Component.text("Profile changed to: ", NamedTextColor.GREEN)
                .append(MiniMessage.miniMessage().deserialize(profile.getDisplayName())));
        return true;
    }
}
