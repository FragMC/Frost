package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    private final Frost plugin;

    public ConfigManager(Frost plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        // Profile and cosmetic loading handled by respective managers
    }

    /**
     * Create an ItemStack from a config section
     * Public so ProfileManager and CosmeticManager can use it
     */
    public ItemStack createItemFromConfig(ConfigurationSection section) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (section.contains("custom-name")) {
                meta.displayName(MiniMessage.miniMessage().deserialize(section.getString("custom-name")));
            }

            if (section.contains("lore")) {
                List<Component> lore = section.getStringList("lore").stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
                        .collect(Collectors.toList());
                meta.lore(lore);
            }

            if (section.contains("custom-model-data")) {
                meta.setCustomModelData(section.getInt("custom-model-data"));
            }

            if (section.contains("enchantments")) {
                ConfigurationSection enchants = section.getConfigurationSection("enchantments");
                if (enchants != null) {
                    for (String enchantName : enchants.getKeys(false)) {
                        Enchantment enchant = Enchantment.getByKey(
                                NamespacedKey.minecraft(enchantName.toLowerCase().replace("_", "-"))
                        );
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
}
