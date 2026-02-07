package com.stufy.fragmc.frost.listeners;

import com.stufy.fragmc.frost.Frost;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class HotbarListener implements Listener {

    private final Frost plugin;

    public HotbarListener(Frost plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getHotbarManager().isEnabled()) return;
        if (plugin.getHotbarManager().isLockedItem(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getHotbarManager().isEnabled()) return;
        
        ItemStack item = event.getItem();
        if (plugin.getHotbarManager().isLockedItem(item)) {
            Player player = event.getPlayer();
            // Refill the item after consumption
            int slot = player.getInventory().getHeldItemSlot();
            if (plugin.getHotbarManager().isLockedSlot(slot)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getHotbarManager().refillItem(player, slot);
                    }
                }.runTask(plugin);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (player.hasPermission("fragmc.bypass.hotbar"))
            return;
        if (!plugin.getHotbarManager().isEnabled())
            return;

        // Check if clicking in player's inventory
        if (event.getClickedInventory() == null)
            return;

        int slot = event.getSlot();

        // Block swapping into locked slots using number keys
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton();
            if (plugin.getHotbarManager().isLockedSlot(hotbarButton)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
                return;
            }
        }

        // If clicking a hotbar slot
        if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
            if (plugin.getHotbarManager().isHotbarSlot(slot) &&
                    plugin.getHotbarManager().isLockedSlot(slot)) {

                // Block all interactions except using items
                if (event.getClick().toString().contains("SHIFT") ||
                        event.getAction().toString().contains("MOVE") ||
                        event.getAction().toString().contains("SWAP") ||
                        event.getAction().toString().contains("PLACE") ||
                        event.getAction().toString().contains("PICKUP")) {

                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
                    return;
                }
            }
        }

        // Block shift-clicking into locked slots
        if (event.getClick().toString().contains("SHIFT")) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && plugin.getHotbarManager().isLockedItem(clicked)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
            }
        }

        // Block dragging onto locked slots
        if (event.getCursor() != null) {
            if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
                if (plugin.getHotbarManager().isHotbarSlot(slot) &&
                        plugin.getHotbarManager().isLockedSlot(slot)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (player.hasPermission("fragmc.bypass.hotbar"))
            return;
        if (!plugin.getHotbarManager().isEnabled())
            return;

        // Check if any of the dragged slots are locked hotbar slots
        for (int slot : event.getRawSlots()) {
            if (slot < 9) { // Hotbar slots are 0-8 in raw slot numbers (bottom inventory)
                if (plugin.getHotbarManager().isLockedSlot(slot)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("fragmc.bypass.hotbar"))
            return;
        if (!plugin.getHotbarManager().isEnabled())
            return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (plugin.getHotbarManager().isLockedItem(item)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("fragmc.bypass.hotbar"))
            return;
        if (!plugin.getHotbarManager().isEnabled())
            return;

        ItemStack offHand = event.getOffHandItem();
        ItemStack mainHand = event.getMainHandItem();

        if ((offHand != null && plugin.getHotbarManager().isLockedItem(offHand)) ||
                (mainHand != null && plugin.getHotbarManager().isLockedItem(mainHand))) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + plugin.getMessage("hotbar-locked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (!plugin.getHotbarManager().isEnabled())
            return;
        if (plugin.getHotbarManager().isLockedItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!plugin.getHotbarManager().isEnabled())
            return;
        if (!(event.getEntity().getShooter() instanceof Player player))
            return;
        if (player.hasPermission("fragmc.bypass.hotbar"))
            return;

        int slot = player.getInventory().getHeldItemSlot();
        if (plugin.getHotbarManager().isLockedSlot(slot)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getHotbarManager().refillItem(player, slot);
                }
            }.runTask(plugin);
        }
    }
}