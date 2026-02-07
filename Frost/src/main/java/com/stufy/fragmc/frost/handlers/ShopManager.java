package com.stufy.fragmc.frost.handlers;

import com.stufy.fragmc.frost.Frost;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ShopManager {

    private final Frost plugin;
    private final Map<String, Map<String, SkinData>> skins;
    private final Map<String, PowerupData> powerups;
    private final Map<UUID, Set<String>> purchasedSkins;
    private final NamespacedKey skinKey;

    public ShopManager(Frost plugin) {
        this.plugin = plugin;
        this.skins = new HashMap<>();
        this.powerups = new HashMap<>();
        this.purchasedSkins = new HashMap<>();
        this.skinKey = new NamespacedKey(plugin, "active_skin");
        reload();
    }

    public void reload() {
        skins.clear();
        powerups.clear();
        loadSkins();
        loadPowerups();
    }

    private void loadSkins() {
        ConfigurationSection skinsSection = plugin.getConfig().getConfigurationSection("skin-shop.skins");
        if (skinsSection == null) return;

        for (String itemType : skinsSection.getKeys(false)) {
            Map<String, SkinData> itemSkins = new HashMap<>();
            ConfigurationSection typeSection = skinsSection.getConfigurationSection(itemType);

            if (typeSection != null) {
                for (String skinId : typeSection.getKeys(false)) {
                    ConfigurationSection skinSection = typeSection.getConfigurationSection(skinId);
                    if (skinSection != null) {
                        SkinData skin = new SkinData(
                                skinId,
                                itemType,
                                skinSection.getString("name", skinId),
                                skinSection.getStringList("lore"),
                                skinSection.getInt("custom-model-data", 0),
                                skinSection.getDouble("price", 0),
                                Material.valueOf(skinSection.getString("icon", "BARRIER"))
                        );
                        itemSkins.put(skinId, skin);
                    }
                }
            }
            skins.put(itemType, itemSkins);
        }
    }

    private void loadPowerups() {
        ConfigurationSection powerupsSection = plugin.getConfig().getConfigurationSection("powerup-shop.powerups");
        if (powerupsSection == null) return;

        for (String powerupId : powerupsSection.getKeys(false)) {
            ConfigurationSection powerupSection = powerupsSection.getConfigurationSection(powerupId);
            if (powerupSection != null) {
                List<PotionEffect> effects = new ArrayList<>();
                List<Map<?, ?>> effectsList = powerupSection.getMapList("effects");
                if (effectsList != null && !effectsList.isEmpty()) {
                    for (Map<?, ?> effectMap : effectsList) {
                        String typeName = (String) effectMap.get("type");
                        if (typeName != null) {
                            PotionEffectType type = PotionEffectType.getByName(typeName);
                            
                            Object amplifierObj = effectMap.get("amplifier");
                            int amplifier = amplifierObj instanceof Number ? ((Number) amplifierObj).intValue() : 0;
                            
                            Object durationObj = effectMap.get("duration");
                            int duration = (durationObj instanceof Number ? ((Number) durationObj).intValue() : 60) * 20;

                            if (type != null) {
                                effects.add(new PotionEffect(type, duration, amplifier, false, true));
                            }
                        }
                    }
                } else {
                    // Fallback for old config format or if someone used sections instead of list
                    ConfigurationSection effectsSection = powerupSection.getConfigurationSection("effects");
                    if (effectsSection != null) {
                        for (String key : effectsSection.getKeys(false)) {
                            ConfigurationSection effectSection = effectsSection.getConfigurationSection(key);
                            if (effectSection != null) {
                                PotionEffectType type = PotionEffectType.getByName(effectSection.getString("type", "SPEED"));
                                int amplifier = effectSection.getInt("amplifier", 0);
                                int duration = effectSection.getInt("duration", 60) * 20;

                                if (type != null) {
                                    effects.add(new PotionEffect(type, duration, amplifier, false, true));
                                }
                            }
                        }
                    }
                }

                PowerupData powerup = new PowerupData(
                        powerupId,
                        powerupSection.getString("name", powerupId),
                        powerupSection.getStringList("lore"),
                        powerupSection.getDouble("price", 0),
                        Material.valueOf(powerupSection.getString("icon", "PAPER")),
                        effects
                );
                powerups.put(powerupId, powerup);
            }
        }
    }

    public boolean buySkin(Player player, String itemType, String skinId) {
        SkinData skin = getSkin(itemType, skinId);
        if (skin == null) return false;

        if (hasPlayerPurchasedSkin(player, itemType, skinId)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "You already own this skin!");
            return false;
        }

        if (!plugin.getEconomy().has(player, skin.price)) {
            String msg = plugin.getMessage("not-enough-money").replace("%price%", plugin.formatCurrency(skin.price));
            player.sendMessage(plugin.getPrefix() + msg);
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, skin.price);
        addPurchasedSkin(player, itemType, skinId);

        String msg = plugin.getMessage("purchase-success").replace("%item%", skin.name);
        player.sendMessage(plugin.getPrefix() + msg);

        // Apply skin immediately
        applySkinToHotbar(player, itemType, skinId);

        return true;
    }

    public boolean buyPowerup(Player player, String powerupId) {
        PowerupData powerup = powerups.get(powerupId);
        if (powerup == null) return false;

        if (!plugin.getEconomy().has(player, powerup.price)) {
            String msg = plugin.getMessage("not-enough-money").replace("%price%", plugin.formatCurrency(powerup.price));
            player.sendMessage(plugin.getPrefix() + msg);
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, powerup.price);

        // Apply effects
        for (PotionEffect effect : powerup.effects) {
            player.addPotionEffect(effect);
        }

        String msg = plugin.getMessage("purchase-success").replace("%item%", powerup.name);
        player.sendMessage(plugin.getPrefix() + msg);

        return true;
    }

    private void applySkinToHotbar(Player player, String itemType, String skinId) {
        int slot = getSlotForItemType(itemType);
        if (slot == -1) return;

        ItemStack item = player.getInventory().getItem(slot);
        if (item != null) {
            item = applySkinToItem(player, item, itemType);
            player.getInventory().setItem(slot, item);

            String msg = plugin.getMessage("skin-applied")
                    .replace("%skin%", getSkin(itemType, skinId).name)
                    .replace("%item%", itemType);
            player.sendMessage(plugin.getPrefix() + msg);
        }
    }

    public ItemStack applySkinToItem(Player player, ItemStack item, String itemType) {
        if (item == null || itemType == null) return item;

        String activeSkin = getActiveSkin(player, itemType);
        if (activeSkin == null) return item;

        SkinData skin = getSkin(itemType, activeSkin);
        if (skin == null) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(skin.customModelData);
            item.setItemMeta(meta);
        }

        return item;
    }

    private int getSlotForItemType(String itemType) {
        return switch (itemType) {
            case "wind_charge" -> 0;
            case "spear" -> 1;
            case "mace" -> 2;
            default -> -1;
        };
    }

    public void addPurchasedSkin(Player player, String itemType, String skinId) {
        String key = itemType + ":" + skinId;
        purchasedSkins.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(key);
    }

    public boolean hasPlayerPurchasedSkin(Player player, String itemType, String skinId) {
        String key = itemType + ":" + skinId;
        Set<String> playerSkins = purchasedSkins.get(player.getUniqueId());
        return playerSkins != null && playerSkins.contains(key);
    }

    public String getActiveSkin(Player player, String itemType) {
        Set<String> playerSkins = purchasedSkins.get(player.getUniqueId());
        if (playerSkins == null) return null;

        for (String skin : playerSkins) {
            if (skin.startsWith(itemType + ":")) {
                return skin.substring(itemType.length() + 1);
            }
        }
        return null;
    }

    public SkinData getSkin(String itemType, String skinId) {
        Map<String, SkinData> itemSkins = skins.get(itemType);
        return itemSkins != null ? itemSkins.get(skinId) : null;
    }

    public Map<String, SkinData> getSkinsForType(String itemType) {
        return skins.getOrDefault(itemType, new HashMap<>());
    }

    public Map<String, PowerupData> getPowerups() {
        return powerups;
    }

    // Data classes
    public static class SkinData {
        public final String id;
        public final String itemType;
        public final String name;
        public final List<String> lore;
        public final int customModelData;
        public final double price;
        public final Material icon;

        public SkinData(String id, String itemType, String name, List<String> lore,
                        int customModelData, double price, Material icon) {
            this.id = id;
            this.itemType = itemType;
            this.name = ChatColor.translateAlternateColorCodes('&', name);
            this.lore = new ArrayList<>();
            for (String line : lore) {
                this.lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            this.customModelData = customModelData;
            this.price = price;
            this.icon = icon;
        }
    }

    public static class PowerupData {
        public final String id;
        public final String name;
        public final List<String> lore;
        public final double price;
        public final Material icon;
        public final List<PotionEffect> effects;

        public PowerupData(String id, String name, List<String> lore, double price,
                           Material icon, List<PotionEffect> effects) {
            this.id = id;
            this.name = ChatColor.translateAlternateColorCodes('&', name);
            this.lore = new ArrayList<>();
            for (String line : lore) {
                this.lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            this.price = price;
            this.icon = icon;
            this.effects = effects;
        }
    }
}