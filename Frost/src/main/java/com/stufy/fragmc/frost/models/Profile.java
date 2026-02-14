package com.stufy.fragmc.frost.models;

import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class Profile {
    private final String id;
    private final String displayName;
    private final String description;
    private final Map<Integer, ItemStack> hotbarItems;

    public Profile(String id, String displayName, String description, Map<Integer, ItemStack> hotbarItems) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.hotbarItems = hotbarItems;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Map<Integer, ItemStack> getHotbarItems() {
        return hotbarItems;
    }
}
