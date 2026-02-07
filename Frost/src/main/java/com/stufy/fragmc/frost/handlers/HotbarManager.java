package com.stufy.fragmc.frost.handlers;

import com.stufy.fragmc.frost.Frost;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HotbarManager {

    private final Frost plugin;
    private final Map<Integer, ItemStack> hotbarItems;
    private final NamespacedKey lockedItemKey;
    private final NamespacedKey toggleKey;
    private boolean enabled;
    private boolean autoRefill;
    private BukkitRunnable persistenceTask;

    public HotbarManager(Frost plugin) {
        this.plugin = plugin;
        this.hotbarItems = new HashMap<>();
        this.lockedItemKey = new NamespacedKey(plugin, "locked_hotbar_item");
        this.toggleKey = new NamespacedKey(plugin, "hotbar_toggled_off");
        reload();
        startPersistenceTask();
        registerProtectionListener();
        registerUsageListener();
    }

    private void startPersistenceTask() {
        if (persistenceTask != null) persistenceTask.cancel();

        persistenceTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) return;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.hasPermission("fragmc.bypass.hotbar")) continue;
                    if (isToggledOff(player)) continue; // Skip if toggled off

                    for (Map.Entry<Integer, ItemStack> entry : hotbarItems.entrySet()) {
                        int slot = entry.getKey();
                        ItemStack expectedItem = entry.getValue();
                        ItemStack currentItem = player.getInventory().getItem(slot);

                        if (currentItem == null || !isLockedItem(currentItem)) {
                            ItemStack newItem = expectedItem.clone();
                            newItem = plugin.getShopManager().applySkinToItem(player, newItem, getItemType(slot));
                            player.getInventory().setItem(slot, newItem);
                        }
                    }
                }
            }
        };

        persistenceTask.runTaskTimer(plugin, 100L, 100L); // every 5 seconds
    }

    private void registerProtectionListener() {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onItemDrop(PlayerDropItemEvent event) {
                Player player = event.getPlayer();
                if (isToggledOff(player)) return; // Allow dropping when toggled off
                ItemStack item = event.getItemDrop().getItemStack();
                if (isLockedItem(item)) event.setCancelled(true);
            }

            @EventHandler
            public void onInventoryClick(InventoryClickEvent event) {
                Player player = (Player) event.getWhoClicked();
                if (isToggledOff(player)) return; // Allow moving when toggled off
                ItemStack item = event.getCurrentItem();
                if (isLockedItem(item)) event.setCancelled(true);
            }

            @EventHandler
            public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
                Player player = event.getPlayer();
                if (isToggledOff(player)) return; // Allow swapping when toggled off
                ItemStack main = event.getMainHandItem();
                ItemStack off = event.getOffHandItem();
                if ((main != null && isLockedItem(main)) || (off != null && isLockedItem(off))) {
                    event.setCancelled(true);
                }
            }
        }, plugin);
    }

    private void registerUsageListener() {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onItemUse(PlayerInteractEvent event) {
                Player player = event.getPlayer();
                if (isToggledOff(player)) return; // No regeneration if toggled off

                ItemStack item = event.getItem();
                if (item == null) return;
                if (!isLockedItem(item)) return;

                int slot = player.getInventory().getHeldItemSlot();
                String type = getItemType(slot);
                if (type == null) return;

                // Regenerate immediately after use (1 tick)
                if (type.equals("wind_charge") || type.equals("spear") || type.equals("mace")) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> refillItem(player, slot), 1L);
                }
            }
        }, plugin);
    }

    public void reload() {
        hotbarItems.clear();
        enabled = plugin.getConfig().getBoolean("hotbar-items.enabled", true);
        autoRefill = plugin.getConfig().getBoolean("hotbar-items.auto-refill", true);
        if (!enabled) return;

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("hotbar-items.items");
        if (itemsSection == null) return;

        for (String slotStr : itemsSection.getKeys(false)) {
            try {
                int slot = Integer.parseInt(slotStr);
                if (slot < 0 || slot > 8) continue;

                ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotStr);
                ItemStack item = createItemFromConfig(itemSection);
                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(lockedItemKey, PersistentDataType.BYTE, (byte) 1);
                        item.setItemMeta(meta);
                    }
                    hotbarItems.put(slot, item);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid slot number: " + slotStr);
            }
        }
    }

    private ItemStack createItemFromConfig(ConfigurationSection section) {
        if (section == null) return null;

        String materialName = section.getString("material");
        if (materialName == null) return null;

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Invalid material: " + materialName);
            return null;
        }

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName = section.getString("display-name", "");
            if (!displayName.isEmpty()) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                meta.setLore(coloredLore);
            }

            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String enchantName : enchantSection.getKeys(false)) {
                    Enchantment enchant = getEnchantmentByName(enchantName);
                    if (enchant != null) {
                        int level = enchantSection.getInt(enchantName, 1);
                        meta.addEnchant(enchant, level, true);
                    }
                }
            }

            int customModelData = section.getInt("custom-model-data", 0);
            if (customModelData > 0) meta.setCustomModelData(customModelData);

            for (String flagName : section.getStringList("hide-flags")) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid item flag: " + flagName);
                }
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private Enchantment getEnchantmentByName(String name) {
        try {
            return Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception e) {
            return switch (name.toUpperCase()) {
                case "LOYALTY" -> Enchantment.LOYALTY;
                case "UNBREAKING" -> Enchantment.UNBREAKING;
                case "WIND_BURST" -> Enchantment.WIND_BURST;
                case "LUNGE" -> Enchantment.LOYALTY;
                default -> null;
            };
        }
    }

    public void giveHotbarItems(Player player) {
        if (!enabled || isToggledOff(player)) return;

        for (Map.Entry<Integer, ItemStack> entry : hotbarItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue().clone();
            item = plugin.getShopManager().applySkinToItem(player, item, getItemType(slot));
            player.getInventory().setItem(slot, item);
        }
    }

    public void refillItem(Player player, int slot) {
        if (!enabled || !autoRefill || isToggledOff(player)) return;
        ItemStack item = hotbarItems.get(slot);
        if (item != null) {
            ItemStack newItem = item.clone();
            newItem = plugin.getShopManager().applySkinToItem(player, newItem, getItemType(slot));
            player.getInventory().setItem(slot, newItem);
        }
    }

    public boolean isToggledOff(Player player) {
        return player.getPersistentDataContainer().has(toggleKey, PersistentDataType.BYTE);
    }

    public void setToggledOff(Player player, boolean toggledOff) {
        if (toggledOff) {
            player.getPersistentDataContainer().set(toggleKey, PersistentDataType.BYTE, (byte) 1);
            for (int slot : hotbarItems.keySet()) player.getInventory().setItem(slot, null);
        } else {
            player.getPersistentDataContainer().remove(toggleKey);
            giveHotbarItems(player);
        }
    }

    public boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot <= 8;
    }

    public boolean isLockedSlot(int slot) {
        return hotbarItems.containsKey(slot);
    }

    public boolean isLockedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(lockedItemKey, PersistentDataType.BYTE);
    }

    public String getItemType(int slot) {
        return switch (slot) {
            case 0 -> "wind_charge";
            case 1 -> "spear";
            case 2 -> "mace";
            default -> null;
        };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutoRefill() {
        return autoRefill;
    }
}
