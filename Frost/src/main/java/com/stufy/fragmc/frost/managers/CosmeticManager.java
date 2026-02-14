package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.models.Cosmetic;
import com.stufy.fragmc.frost.models.CosmeticCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CosmeticManager {
    private final Frost plugin;
    private final Map<String, CosmeticCategory> categories = new HashMap<>();
    private final Map<String, Cosmetic> cosmeticsById = new HashMap<>();

    public CosmeticManager(Frost plugin) {
        this.plugin = plugin;
        loadCosmetics();
    }

    public void loadCosmetics() {
        categories.clear();
        cosmeticsById.clear();

        var config = plugin.getConfig();
        var cosmeticsSection = config.getConfigurationSection("cosmetics");

        if (cosmeticsSection == null) {
            plugin.getLogger().warning("No cosmetics found in config!");
            return;
        }

        for (String categoryId : cosmeticsSection.getKeys(false)) {
            var categorySection = cosmeticsSection.getConfigurationSection(categoryId);
            if (categorySection == null) continue;

            String categoryName = categorySection.getString("category-name", categoryId);
            String categoryDesc = categorySection.getString("category-description", "");
            String iconMaterial = categorySection.getString("category-icon", "PAPER");
            Material icon = Material.matchMaterial(iconMaterial);
            if (icon == null) icon = Material.PAPER;

            CosmeticCategory category = new CosmeticCategory(categoryId, categoryName, icon, categoryDesc);

            var itemsSection = categorySection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String cosmeticId : itemsSection.getKeys(false)) {
                    var cosmeticSection = itemsSection.getConfigurationSection(cosmeticId);
                    if (cosmeticSection != null) {
                        Cosmetic cosmetic = new Cosmetic(cosmeticId, categoryId, cosmeticSection);
                        category.addCosmetic(cosmetic);
                        cosmeticsById.put(cosmeticId, cosmetic);
                    }
                }
            }

            categories.put(categoryId, category);
            plugin.getLogger().info("Loaded cosmetic category: " + categoryId +
                    " with " + category.getCosmetics().size() + " items");
        }
    }

    public Map<String, CosmeticCategory> getCategories() {
        return categories;
    }

    public Cosmetic getCosmetic(String id) {
        return cosmeticsById.get(id);
    }

    public ItemStack applyCosmetic(ItemStack item, Cosmetic cosmetic) {
        if (item == null || cosmetic == null) return item;

        ItemStack modified = item.clone();
        ItemMeta meta = modified.getItemMeta();
        ConfigurationSection mods = cosmetic.getModifications();

        if (meta != null && mods != null) {
            if (mods.contains("custom-name")) {
                meta.displayName(MiniMessage.miniMessage().deserialize(mods.getString("custom-name")));
            }

            if (mods.contains("lore")) {
                List<Component> lore = mods.getStringList("lore").stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
                        .collect(Collectors.toList());
                meta.lore(lore);
            }

            if (mods.contains("custom-model-data")) {
                meta.setCustomModelData(mods.getInt("custom-model-data"));
            }

            if (mods.contains("enchantments")) {
                ConfigurationSection enchants = mods.getConfigurationSection("enchantments");
                if (enchants != null) {
                    for (String enchantName : enchants.getKeys(false)) {
                        String key = enchantName.toLowerCase().replace(" ", "_").replace("-", "_");
                        Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(key));
                        if (enchant != null) {
                            meta.addEnchant(enchant, enchants.getInt(enchantName), true);
                        }
                    }
                }
            }

            if (mods.contains("flags")) {
                List<String> flags = mods.getStringList("flags");
                for (String flagName : flags) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        modified.setItemMeta(meta);
        return modified;
    }

    public void applyArmorCosmetic(Player player, Cosmetic cosmetic) {
        if (!cosmetic.getAppliesType().equals("armor-set") &&
                !cosmetic.getAppliesType().equals("armor-piece")) {
            return;
        }

        ConfigurationSection mods = cosmetic.getModifications();
        if (mods == null) return;

        if (cosmetic.getAppliesType().equals("armor-set")) {
            applyArmorPiece(player, "helmet", mods.getConfigurationSection("helmet"));
            applyArmorPiece(player, "chestplate", mods.getConfigurationSection("chestplate"));
            applyArmorPiece(player, "leggings", mods.getConfigurationSection("leggings"));
            applyArmorPiece(player, "boots", mods.getConfigurationSection("boots"));
        } else {
            String slot = cosmetic.getAppliesArmorSlot();
            if (slot != null) {
                applyArmorPiece(player, slot, mods);
            }
        }
    }

    public void removeArmorCosmetic(Player player, Cosmetic cosmetic) {
        if (!cosmetic.getAppliesType().equals("armor-set") &&
                !cosmetic.getAppliesType().equals("armor-piece")) {
            return;
        }
        if (cosmetic.getAppliesType().equals("armor-set")) {
            clearArmorPiece(player, "helmet");
            clearArmorPiece(player, "chestplate");
            clearArmorPiece(player, "leggings");
            clearArmorPiece(player, "boots");
        } else {
            String slot = cosmetic.getAppliesArmorSlot();
            if (slot != null) {
                clearArmorPiece(player, slot);
            }
        }
    }

    private void applyArmorPiece(Player player, String slot, ConfigurationSection modsPiece) {
        if (modsPiece == null) return;
        ItemStack piece = switch (slot.toLowerCase()) {
            case "helmet" -> player.getInventory().getHelmet();
            case "chestplate" -> player.getInventory().getChestplate();
            case "leggings" -> player.getInventory().getLeggings();
            case "boots" -> player.getInventory().getBoots();
            default -> null;
        };
        if (piece == null || piece.getType() == Material.AIR) {
            piece = defaultArmorForSlot(slot);
        }
        if (piece == null || piece.getType() == Material.AIR) return;

        ItemMeta meta = piece.getItemMeta();
        if (meta == null) return;

        if (modsPiece.contains("custom-model-data")) {
            meta.setCustomModelData(modsPiece.getInt("custom-model-data"));
        }
        if (modsPiece.contains("lore")) {
            List<Component> lore = modsPiece.getStringList("lore").stream()
                    .map(line -> MiniMessage.miniMessage().deserialize(line))
                    .collect(Collectors.toList());
            meta.lore(lore);
        }
        piece.setItemMeta(meta);

        switch (slot.toLowerCase()) {
            case "helmet" -> player.getInventory().setHelmet(piece);
            case "chestplate" -> player.getInventory().setChestplate(piece);
            case "leggings" -> player.getInventory().setLeggings(piece);
            case "boots" -> player.getInventory().setBoots(piece);
        }
    }

    private void clearArmorPiece(Player player, String slot) {
        ItemStack piece = switch (slot.toLowerCase()) {
            case "helmet" -> player.getInventory().getHelmet();
            case "chestplate" -> player.getInventory().getChestplate();
            case "leggings" -> player.getInventory().getLeggings();
            case "boots" -> player.getInventory().getBoots();
            default -> null;
        };
        if (piece == null || piece.getType() == Material.AIR) return;
        ItemMeta meta = piece.getItemMeta();
        if (meta == null) return;
        meta.setCustomModelData(null);
        meta.lore(null);
        piece.setItemMeta(meta);
        switch (slot.toLowerCase()) {
            case "helmet" -> player.getInventory().setHelmet(piece);
            case "chestplate" -> player.getInventory().setChestplate(piece);
            case "leggings" -> player.getInventory().setLeggings(piece);
            case "boots" -> player.getInventory().setBoots(piece);
        }
    }

    private ItemStack defaultArmorForSlot(String slot) {
        return switch (slot.toLowerCase()) {
            case "helmet" -> new ItemStack(Material.LEATHER_HELMET);
            case "chestplate" -> new ItemStack(Material.LEATHER_CHESTPLATE);
            case "leggings" -> new ItemStack(Material.LEATHER_LEGGINGS);
            case "boots" -> new ItemStack(Material.LEATHER_BOOTS);
            default -> new ItemStack(Material.AIR);
        };
    }
}
