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

public class SkinShopGUI implements Listener {

    private final Frost plugin;
    private final Player player;
    private Inventory inventory;
    private String title;
    private String currentCategory = "wind_charge";

    public SkinShopGUI(Frost plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        this.title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("skin-shop.title", "&6&lSkin Shop"));
        inventory = Bukkit.createInventory(null, 54, title);

        // Category selectors
        inventory.setItem(0, createCategoryItem(Material.WIND_CHARGE, "&bWind Charge Skins", "wind_charge"));
        inventory.setItem(1, createCategoryItem(Material.NETHERITE_SWORD, "&bSpear Skins", "spear"));
        inventory.setItem(2, createCategoryItem(Material.MACE, "&5Mace Skins", "mace"));

        // Navigation
        inventory.setItem(8, createItem(Material.BARRIER, "&cClose", null));
        inventory.setItem(49,
                createItem(Material.EMERALD, "&aPowerup Shop", List.of("&7Click to open the powerup shop")));

        // Load skins for current category
        loadSkins();

        player.openInventory(inventory);
    }

    private void loadSkins() {
        // Clear skin area
        for (int i = 9; i < 45; i++) {
            inventory.setItem(i, null);
        }

        Map<String, ShopManager.SkinData> skins = plugin.getShopManager().getSkinsForType(currentCategory);
        int slot = 9;

        for (ShopManager.SkinData skin : skins.values()) {
            if (slot >= 45)
                break;

            ItemStack item = new ItemStack(skin.icon);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(skin.name);

                List<String> lore = new ArrayList<>(skin.lore);
                lore.add("");

                if (plugin.getShopManager().hasPlayerPurchasedSkin(player, currentCategory, skin.id)) {
                    lore.add(ChatColor.GREEN + "✓ Owned");
                } else {
                    lore.add(ChatColor.YELLOW + "Click to purchase!");
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot++, item);
        }
    }

    private ItemStack createCategoryItem(Material material, String name, String category) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> lore = new ArrayList<>();
            if (category.equals(currentCategory)) {
                lore.add(ChatColor.GREEN + "✓ Selected");
            } else {
                lore.add(ChatColor.GRAY + "Click to view");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
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

        // Category selection
        if (slot == 0) {
            currentCategory = "wind_charge";
            loadSkins();
            return;
        } else if (slot == 1) {
            currentCategory = "spear";
            loadSkins();
            return;
        } else if (slot == 2) {
            currentCategory = "mace";
            loadSkins();
            return;
        }

        // Close button
        if (slot == 8) {
            player.closeInventory();
            return;
        }

        // Powerup shop button
        if (slot == 49) {
            player.closeInventory();
            new PowerupShopGUI(plugin, player).open();
            return;
        }

        // Skin purchase
        if (slot >= 9 && slot < 45) {
            Map<String, ShopManager.SkinData> skins = plugin.getShopManager().getSkinsForType(currentCategory);
            int index = slot - 9;
            int count = 0;

            for (ShopManager.SkinData skin : skins.values()) {
                if (count == index) {
                    if (plugin.getShopManager().buySkin(player, currentCategory, skin.id)) {
                        loadSkins(); // Refresh display
                    }
                    return;
                }
                count++;
            }
        }
    }
}