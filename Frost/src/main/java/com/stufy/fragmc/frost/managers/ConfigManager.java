package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {
    private final Frost plugin;
    private final Map<Integer, ItemStack> hotbarItems = new HashMap<>();
    // Modifiers are now mapped by Slot -> List<Modifier>
    private final Map<Integer, List<Modifier>> itemModifiers = new HashMap<>();
    // Global registry for quick lookup by ID (e.g., for loading player data)
    private final Map<String, Modifier> modifierRegistry = new HashMap<>();

    public ConfigManager(Frost plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        hotbarItems.clear();
        itemModifiers.clear();
        modifierRegistry.clear();

        // Load Hotbar Items
        ConfigurationSection itemsSection = config.getConfigurationSection("hotbar-items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        ItemStack item = createItemFromConfig(itemSection);
                        hotbarItems.put(slot, item);

                        // Load Modifiers for this item
                        List<Modifier> modList = new ArrayList<>();
                        if (itemSection.contains("modifiers")) {
                            ConfigurationSection modsSection = itemSection.getConfigurationSection("modifiers");
                            if (modsSection != null) {
                                for (String modId : modsSection.getKeys(false)) {
                                    ConfigurationSection modConfig = modsSection.getConfigurationSection(modId);
                                    if (modConfig != null) {
                                        // Prefix modifier ID with slot to ensure uniqueness in registry if needed,
                                        // OR just use raw ID if we assume they are unique enough.
                                        // Let's use raw ID for cleaner player data, but warn if duplicates?
                                        // Actually, let's strictly scope it internally if we want, but for simplicity
                                        // let's assume raw ID is what we store.

                                        Modifier modifier = new Modifier(modId, slot, modConfig);
                                        modList.add(modifier);
                                        modifierRegistry.put(modId, modifier);
                                    }
                                }
                            }
                        }
                        itemModifiers.put(slot, modList);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid hotbar slot: " + key);
                }
            }
        }
    }

    public ItemStack createItemFromConfig(ConfigurationSection section) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null)
            material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (section.contains("custom_name")) {
                meta.displayName(MiniMessage.miniMessage().deserialize(section.getString("custom_name")));
            }

            if (section.contains("lore")) {
                List<Component> lore = section.getStringList("lore").stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
                        .collect(Collectors.toList());
                meta.lore(lore);
            }

            if (section.contains("custom_model_data")) {
                meta.setCustomModelData(section.getInt("custom_model_data"));
            }

            if (section.contains("enchantments")) {
                ConfigurationSection enchants = section.getConfigurationSection("enchantments");
                if (enchants != null) {
                    for (String enchantName : enchants.getKeys(false)) {
                        Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                        if (enchant != null) {
                            meta.addEnchant(enchant, enchants.getInt(enchantName), true);
                        }
                    }
                }
            }

            if (section.contains("flags")) {
                List<String> flags = section.getStringList("flags");
                for (String flagName : flags) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    public Map<Integer, ItemStack> getHotbarItems() {
        return hotbarItems;
    }

    public Map<Integer, List<Modifier>> getItemModifiers() {
        return itemModifiers;
    }

    public Map<String, Modifier> getModifierRegistry() {
        return modifierRegistry;
    }

    public static class Modifier {
        private final String id;
        private final int targetSlot;
        private final double price;
        private final String name;
        private final ConfigurationSection properties;

        public Modifier(String id, int targetSlot, ConfigurationSection section) {
            this.id = id;
            this.targetSlot = targetSlot;
            this.price = section.getDouble("price", 0.0);
            this.name = section.getString("name", id);
            this.properties = section.getConfigurationSection("properties");
        }

        public String getId() {
            return id;
        }

        public int getTargetSlot() {
            return targetSlot;
        }

        public double getPrice() {
            return price;
        }

        public String getName() {
            return name;
        }

        public ConfigurationSection getProperties() {
            return properties;
        }
    }
}
