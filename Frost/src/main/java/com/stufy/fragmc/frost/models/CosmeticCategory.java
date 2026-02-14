package com.stufy.fragmc.frost.models;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class CosmeticCategory {
    private final String id;
    private final String name;
    private final Material icon;
    private final String description;
    private final List<Cosmetic> cosmetics;

    public CosmeticCategory(String id, String name, Material icon, String description) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.cosmetics = new ArrayList<>();
    }

    public void addCosmetic(Cosmetic cosmetic) {
        cosmetics.add(cosmetic);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
    public List<Cosmetic> getCosmetics() { return cosmetics; }
}
