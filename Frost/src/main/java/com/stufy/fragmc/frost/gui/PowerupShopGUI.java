package com.stufy.fragmc.frost.gui;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.handlers.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PowerupShopGUI implements Listener {

    private final Frost plugin;
    private final Player player;
    private Inventory inventory;
    private String title;

    public PowerupShopGUI(Frost plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        this.title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("powerup-shop.title", "&d&lPowerup Shop"));
        inventory = Bukkit.createInventory(null, 54, title);

        // Navigation
        inventory.setItem(8, createItem(Material.BARRIER, "&cClose", null));
        inventory.setItem(49, createItem(Material.PAINTING, "&bSkin Shop", List.of("&7Click to open the skin shop")));

        // Load powerups
        loadPowerups();

        player.openInventory(inventory);
    }

    private void loadPowerups() {
        Map<String, ShopManager.PowerupData> powerups = plugin.getShopManager().getPowerups();
        int slot = 9;

        for (ShopManager.PowerupData powerup : powerups.values()) {
            if (slot >= 45)
                break;

            ItemStack item = new ItemStack(powerup.icon);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(powerup.name);

                List<String> lore = new ArrayList<>(powerup.lore);
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to purchase!");

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot++, item);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        if (!event.getView().getTitle().equals(title))
            return;
        if (event.getClickedInventory() != inventory)
            return;

        event.setCancelled(true);

        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player))
            return;

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Close button
        if (slot == 8) {
            player.closeInventory();
            return;
        }

        // Skin shop button
        if (slot == 49) {
            player.closeInventory();
            new SkinShopGUI(plugin, player).open();
            return;
        }

        // Powerup purchase
        if (slot >= 9 && slot < 45) {
            Map<String, ShopManager.PowerupData> powerups = plugin.getShopManager().getPowerups();
            int index = slot - 9;
            int count = 0;

            for (ShopManager.PowerupData powerup : powerups.values()) {
                if (count == index) {
                    plugin.getShopManager().buyPowerup(player, powerup.id);
                    return;
                }
                count++;
            }
        }
    }
}