package com.stufy.fragmc.frost.listeners;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.managers.PlayerDataManager;
import com.stufy.fragmc.frost.models.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HotbarLockListener implements Listener {
    private final Frost plugin;
    private final NamespacedKey lockedKey;
    private final boolean isFloodgatePresent;

    public HotbarLockListener(Frost plugin) {
        this.plugin = plugin;
        this.lockedKey = new NamespacedKey(plugin, "frost_locked");
        this.isFloodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        // Load player data first
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer());

        // INSTANT - removed delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            giveHotbarItems(event.getPlayer());
        }, 5L); // Small delay to ensure everything is loaded
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Save player data when they leave
        plugin.getPlayerDataManager().unloadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // INSTANT - removed delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            giveHotbarItems(event.getPlayer());
        }, 5L);
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

        // Always lock fixed Spear/Mace/Wind slots 0-2
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            int slot = event.getSlot();
            if (isLockedSlot(slot, data)) {
                event.setCancelled(true);
            }
        }

        // Prevent hotbar swapping into locked slots
        if (event.getHotbarButton() != -1) {
            if (isLockedSlot(event.getHotbarButton(), data)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!isLocked(event.getPlayer())) return;

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;

        int slot = event.getPlayer().getInventory().getHeldItemSlot();
        if (isLockedSlot(slot, data)) {
            event.setCancelled(true);
        }
    }

    private boolean isLockedSlot(int slot, PlayerDataManager.PlayerData data) {
        // Fixed core slots 0,1,2 always locked (Spear, Mace, Wind Charge)
        if (slot >= 0 && slot <= 2) return true;
        // Slots 3-8 locked if either customHotbar has entry or profile defines it (customizable but locked from manual move - must use /inventory GUI)
        if (slot >= 3 && slot <= 8) {
            if (data.customHotbar != null && data.customHotbar.containsKey(slot)) return true;
            Profile profile = plugin.getProfileManager().getProfile(data.currentProfile);
            if (profile != null && profile.getHotbarItems().containsKey(slot)) return true;
            // Even empty customizable slots are considered locked areas (prevent shift-click nonsense) - we still block manual inventory manipulation
            // But allow empty slots to be managed via customizer only
            return true;
        }
        return false;
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

        // Fixed locked slots 0,1,2 - Spear, Mace, Wind Charge (Bedrock crossplay: Geyser translates MACE/TRIDENT/WIND_CHARGE, fallback if needed)
        giveFixedSlot(player, inv, data, 0, getFixedSpear(profile, player));
        giveFixedSlot(player, inv, data, 1, getFixedMace(player));
        giveFixedSlot(player, inv, data, 2, getFixedWindCharge(player));

        // Customizable slots 3-8 - respect customHotbar first, then profile fallback
        for (int slot = 3; slot <= 8; slot++) {
            ItemStack base = null;
            if (data.customHotbar != null && data.customHotbar.containsKey(slot)) {
                base = data.customHotbar.get(slot);
            } else if (profile.getHotbarItems().containsKey(slot)) {
                base = profile.getHotbarItems().get(slot);
            }
            if (base == null) {
                // Empty slot - clear if not already empty and locked handling will prevent manual fill
                ItemStack current = inv.getItem(slot);
                if (current != null && isLockedItem(current)) {
                    // If we have a locked empty slot previously, keep it empty (don't clear custom empty)
                    // Do nothing - leave as is or clear if profile says no item and no custom
                    if (data.customHotbar == null || !data.customHotbar.containsKey(slot)) {
                        inv.setItem(slot, null);
                    }
                }
                continue;
            }
            ItemStack toGive = base.clone();
            ItemMeta meta = toGive.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);
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
                // Apply FMM model if present on base item
                if (meta != null && plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
                    String fmm = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING);
                    if (fmm != null) {
                        toGive = plugin.getFmmHook().applyFmmModel(toGive, fmm);
                        meta = toGive.getItemMeta();
                        if (meta != null) meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);
                    }
                }
                toGive.setItemMeta(meta);
            }
            ItemStack current = inv.getItem(slot);
            if (current == null || !current.isSimilar(toGive)) {
                inv.setItem(slot, toGive);
            }
        }
    }

    private void giveFixedSlot(Player player, PlayerInventory inv, PlayerDataManager.PlayerData data, int slot, ItemStack base) {
        ItemStack toGive = base.clone();
        ItemMeta meta = toGive.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);
            String cosmeticKey = "weapon-skins:" + slot;
            if (data.equippedCosmetics.containsKey(cosmeticKey)) {
                String cosmeticId = data.equippedCosmetics.get(cosmeticKey);
                var cosmetic = plugin.getCosmeticManager().getCosmetic(cosmeticId);
                if (cosmetic != null) {
                    toGive = plugin.getCosmeticManager().applyCosmetic(toGive, cosmetic);
                    meta = toGive.getItemMeta();
                    if (meta != null) meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);
                }
            }
            // FMM tagging
            if (meta != null && plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
                String fmm = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING);
                if (fmm != null) {
                    toGive = plugin.getFmmHook().applyFmmModel(toGive, fmm);
                    meta = toGive.getItemMeta();
                    if (meta != null) meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);
                }
            }
            toGive.setItemMeta(meta);
        }
        ItemStack current = inv.getItem(slot);
        if (current == null || !current.isSimilar(toGive)) {
            inv.setItem(slot, toGive);
        }
    }

    private ItemStack getFixedSpear(Profile profile, Player player) {
        if (profile != null && profile.getHotbarItems().containsKey(0)) {
            ItemStack fromProfile = profile.getHotbarItems().get(0);
            return bedrockFallback(fromProfile.clone(), player);
        }
        ItemStack spear = new ItemStack(Material.TRIDENT);
        ItemMeta meta = spear.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<aqua><bold>Spear</bold> <gray>(Locked)"));
        meta.lore(java.util.List.of(Component.text("Always in slot 1", NamedTextColor.GRAY)));
        if (plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING, "frost_spear");
        } else {
            meta.setCustomModelData(1001);
        }
        spear.setItemMeta(meta);
        return bedrockFallback(spear, player);
    }

    private ItemStack getFixedMace(Player player) {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>Mace</bold> <gray>(Locked)"));
        meta.lore(java.util.List.of(Component.text("Always in slot 2", NamedTextColor.GRAY)));
        if (plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING, "frost_mace");
        } else {
            meta.setCustomModelData(1002);
        }
        mace.setItemMeta(meta);
        return bedrockFallback(mace, player);
    }

    private ItemStack getFixedWindCharge(Player player) {
        ItemStack wind = new ItemStack(Material.WIND_CHARGE);
        ItemMeta meta = wind.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<white><bold>Wind Charge</bold> <gray>(Locked)"));
        meta.lore(java.util.List.of(Component.text("Always in slot 3", NamedTextColor.GRAY)));
        if (plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING, "frost_wind_charge");
        } else {
            meta.setCustomModelData(1003);
        }
        wind.setItemMeta(meta);
        return bedrockFallback(wind, player);
    }

    private boolean isBedrock(Player player) {
        return isFloodgatePresent && org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    private ItemStack bedrockFallback(ItemStack item, Player player) {
        if (!isBedrock(player)) return item;
        // Geyser 2.2.0+ on 26.2 translates TRIDENT/MACE/WIND_CHARGE, but provide safe fallback for older Geyser or if translation fails
        // For Bedrock, ensure item is visually distinct and not AIR - Geyser handles MACE via custom item mapping, but we keep fallback logic
        // FreeMinecraftModels also has armor-stand fallback for Bedrock (display entities -> armor stands)
        // No change needed for 26.2 Geyser, but we keep method for future material compatibility
        return item;
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