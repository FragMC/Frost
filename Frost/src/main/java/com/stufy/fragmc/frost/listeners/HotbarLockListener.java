package com.stufy.fragmc.frost.listeners;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.managers.ConfigManager;
import com.stufy.fragmc.frost.managers.PlayerDataManager;
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

        // Periodic check to ensure items are present
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (isLocked(player)) {
                    giveHotbarItems(player);
                }
            }
        }, 20L, 40L); // Check every 2 seconds
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer());
        // Delay slightly to ensure data is loaded and inventory is ready
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            giveHotbarItems(event.getPlayer());
        }, 5L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
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
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        if (!isLocked(player))
            return;

        // Prevent clicking on locked slots in player inventory
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            int slot = event.getSlot();
            if (plugin.getConfigManager().getHotbarItems().containsKey(slot)) {
                event.setCancelled(true);
            }
        }

        // Prevent hotbar swapping into locked slots
        if (event.getHotbarButton() != -1) {
            if (plugin.getConfigManager().getHotbarItems().containsKey(event.getHotbarButton())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!isLocked(event.getPlayer()))
            return;

        // Prevent swapping if main hand is locked slot
        int slot = event.getPlayer().getInventory().getHeldItemSlot();
        if (plugin.getConfigManager().getHotbarItems().containsKey(slot)) {
            event.setCancelled(true);
        }
    }

    public void giveHotbarItems(Player player) {
        PlayerInventory inv = player.getInventory();
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        plugin.getConfigManager().getHotbarItems().forEach((slot, item) -> {
            ItemStack toGive = item.clone();

            // Mark as locked
            ItemMeta meta = toGive.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1);

                // Re-apply modifier if selected (to ensure it stays updated)
                if (data.selectedModifiers.containsKey(slot)) {
                    String modifierId = data.selectedModifiers.get(slot);
                    ConfigManager.Modifier modifier = plugin.getConfigManager().getModifierRegistry().get(modifierId);
                    if (modifier != null) {
                        toGive = plugin.getModifierManager().applyModifier(toGive, modifier);
                        meta = toGive.getItemMeta(); // Refresh meta
                        meta.getPersistentDataContainer().set(lockedKey, PersistentDataType.BYTE, (byte) 1); // Re-tag
                    }
                }

                toGive.setItemMeta(meta);
            }

            // Only set if different to avoid visual glitching?
            // For now, force set to ensure it's there.
            ItemStack current = inv.getItem(slot);
            if (current == null || !current.isSimilar(toGive)) {
                inv.setItem(slot, toGive);
            }
        });
    }

    private boolean isLockedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(lockedKey, PersistentDataType.BYTE);
    }

    private boolean isLocked(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return data != null && data.hotbarLocked;
    }
}
