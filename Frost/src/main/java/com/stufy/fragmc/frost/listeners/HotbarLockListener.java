package com.stufy.fragmc.frost.listeners;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.managers.PlayerDataManager;
import com.stufy.fragmc.frost.models.Profile;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HotbarLockListener implements Listener {
    private final Frost plugin;
    private final NamespacedKey lockedKey;

    public HotbarLockListener(Frost plugin) {
        this.plugin = plugin;
        this.lockedKey = new NamespacedKey(plugin, "frost_locked");

        // INSTANT periodic check - no delays
        boolean instantReplace = plugin.getConfig().getBoolean("settings.instant-item-replace", true);
        long checkInterval = instantReplace ? 40L : 80L; // 2s or 4s

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (isLocked(player)) {
                    giveHotbarItems(player); // INSTANT - no delay
                }
            }
        }, 20L, checkInterval);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer());
        // INSTANT - removed delay
        giveHotbarItems(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // INSTANT - removed delay
        giveHotbarItems(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isLocked(event.getPlayer()) && isLockedItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!isLocked(player)) return;

        // Get current profile
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        Profile profile = plugin.getProfileManager().getProfile(data.currentProfile);
        if (profile == null) return;

        // Prevent clicking on locked slots
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            int slot = event.getSlot();
            if (profile.getHotbarItems().containsKey(slot)) {
                event.setCancelled(true);
            }
        }

        // Prevent hotbar swapping into locked slots
        if (event.getHotbarButton() != -1) {
            if (profile.getHotbarItems().containsKey(event.getHotbarButton())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!isLocked(event.getPlayer())) return;

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;

        Profile profile = plugin.getProfileManager().getProfile(data.currentProfile);
        if (profile == null) return;

        int slot = event.getPlayer().getInventory().getHeldItemSlot();
        if (profile.getHotbarItems().containsKey(slot)) {
            event.setCancelled(true);
        }
    }

    public void giveHotbarItems(Player player) {
        PlayerInventory inv = player.getInventory();
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        // Get profile
        Profile profile = plugin.getProfileManager().getProfile(data.currentProfile);
        if (profile == null) {
            plugin.getLogger().warning("Profile not found: " + data.currentProfile);
            return;
        }

        profile.getHotbarItems().forEach((slot, item) -> {
            ItemStack toGive = item.clone();

            // Mark as locked
            ItemMeta meta = toGive.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);

                // Apply equipped cosmetic if any
                String cosmeticKey = "weapon-skins:" + slot;
                if (data.equippedCosmetics.containsKey(cosmeticKey)) {
                    String cosmeticId = data.equippedCosmetics.get(cosmeticKey);
                    var cosmetic = plugin.getCosmeticManager().getCosmetic(cosmeticId);
                    if (cosmetic != null) {
                        toGive = plugin.getCosmeticManager().applyCosmetic(toGive, cosmetic);
                        meta = toGive.getItemMeta();
                        if (meta != null) {
                            meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);
                        }
                    }
                }

                toGive.setItemMeta(meta);
            }

            // Only set if different
            ItemStack current = inv.getItem(slot);
            if (current == null || !current.isSimilar(toGive)) {
                inv.setItem(slot, toGive);
            }
        });
    }

    private boolean isLockedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(lockedKey, PersistentDataType.BYTE);
    }

    private boolean isLocked(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return data != null && data.hotbarLocked;
    }
}
