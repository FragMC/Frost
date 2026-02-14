package com.stufy.fragmc.frost.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

public class Cosmetic {
    private final String id;
    private final String categoryId;
    private final String name;
    private final List<String> description;
    private final double price;
    private final Material icon;
    private final String appliesType;
    private final String appliesProfile;
    private final Integer appliesSlot;
    private final String appliesArmorSlot;
    private final ConfigurationSection modifications;

    public Cosmetic(String id, String categoryId, ConfigurationSection section) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = section.getString("name", id);
        this.description = section.getStringList("description");
        this.price = section.getDouble("price", 0.0);

        String iconMaterial = section.getString("icon", "PAPER");
        Material parsedIcon = Material.matchMaterial(iconMaterial);
        this.icon = parsedIcon != null ? parsedIcon : Material.PAPER;

        ConfigurationSection appliesTo = section.getConfigurationSection("applies-to");
        if (appliesTo != null) {
            this.appliesType = appliesTo.getString("type", "weapon");
            this.appliesProfile = appliesTo.getString("profile");
            this.appliesSlot = appliesTo.contains("slot") ? appliesTo.getInt("slot") : null;
            this.appliesArmorSlot = appliesTo.getString("slot");
        } else {
            this.appliesType = "weapon";
            this.appliesProfile = null;
            this.appliesSlot = null;
            this.appliesArmorSlot = null;
        }

        this.modifications = section.getConfigurationSection("modifications");
    }

    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public List<String> getDescription() { return description; }
    public double getPrice() { return price; }
    public Material getIcon() { return icon; }
    public String getAppliesType() { return appliesType; }
    public String getAppliesProfile() { return appliesProfile; }
    public Integer getAppliesSlot() { return appliesSlot; }
    public String getAppliesArmorSlot() { return appliesArmorSlot; }
    public ConfigurationSection getModifications() { return modifications; }
}
