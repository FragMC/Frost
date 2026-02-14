package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class ModifierManager {
    private final Frost plugin;

    public ModifierManager(Frost plugin) {
        this.plugin = plugin;
    }

    public ItemStack applyModifier(ItemStack item, ConfigManager.Modifier modifier) {
        if (item == null || modifier == null) return item;

        ItemStack modified = item.clone();
        ItemMeta meta = modified.getItemMeta();
        ConfigurationSection props = modifier.getProperties();

        if (meta != null && props != null) {
            if (props.contains("custom_name")) {
                meta.displayName(MiniMessage.miniMessage().deserialize(props.getString("custom_name")));
            }

            if (props.contains("lore")) {
                List<Component> lore = props.getStringList("lore").stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
                        .collect(Collectors.toList());
                meta.lore(lore);
            }

            if (props.contains("custom_model_data")) {
                meta.setCustomModelData(props.getInt("custom_model_data"));
            }

            if (props.contains("enchantments")) {
                ConfigurationSection enchants = props.getConfigurationSection("enchantments");
                if (enchants != null) {
                    for (String enchantName : enchants.getKeys(false)) {
                        Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                        if (enchant != null) {
                            meta.addEnchant(enchant, enchants.getInt(enchantName), true);
                        }
                    }
                }
            }

            modified.setItemMeta(meta);
        }
        return modified;
    }
}
