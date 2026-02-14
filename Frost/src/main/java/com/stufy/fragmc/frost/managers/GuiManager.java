package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.models.Cosmetic;
import com.stufy.fragmc.frost.models.CosmeticCategory;
import com.stufy.fragmc.frost.models.ParticleEffect;
import com.stufy.fragmc.frost.managers.PlayerDataManager.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

public class GuiManager implements Listener {
    private final Frost plugin;
    private final boolean isFloodgatePresent;
    private final NamespacedKey categoryKey;
    private final NamespacedKey cosmeticKey;
    private final NamespacedKey actionKey;

    public GuiManager(Frost plugin) {
        this.plugin = plugin;
        this.isFloodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        this.categoryKey = new NamespacedKey(plugin, "category_id");
        this.cosmeticKey = new NamespacedKey(plugin, "cosmetic_id");
        this.actionKey = new NamespacedKey(plugin, "action");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ============= SHOP LOGIC =============
    public void openShop(Player player) {
        if (isFloodgatePresent && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openBedrockShopCategories(player);
        } else {
            openJavaShopCategories(player);
        }
    }

    private void openJavaShopCategories(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Cosmetics Shop"));

        int slot = 10;
        for (CosmeticCategory category : plugin.getCosmeticManager().getCategories().values()) {
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(category.getName()));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(category.getDescription(), NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text(category.getCosmetics().size() + " items available", NamedTextColor.YELLOW));
            lore.add(Component.text("Click to browse", NamedTextColor.GREEN));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category.getId());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "browse_category");
            icon.setItemMeta(meta);

            inv.setItem(slot++, icon);
            if (slot == 17) slot = 19;
        }

        player.openInventory(inv);
    }

    private void openBedrockShopCategories(Player player) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("Cosmetics Shop")
                .content("Choose a category to browse");

        List<String> categoryIds = new ArrayList<>(plugin.getCosmeticManager().getCategories().keySet());
        for (String catId : categoryIds) {
            CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(catId);
            form.button(category.getName() + " (" + category.getCosmetics().size() + " items)");
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < categoryIds.size()) {
                openBedrockCategoryItems(player, categoryIds.get(index));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openJavaCategoryItems(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null) return;

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Shop: ").append(MiniMessage.miniMessage().deserialize(category.getName())));

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back to Categories", NamedTextColor.YELLOW));
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_to_categories");
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        // Grouping
        java.util.Map<String, java.util.List<Cosmetic>> groups = new java.util.LinkedHashMap<>();
        for (Cosmetic cosmetic : category.getCosmetics()) {
            if (cosmetic.isAdminOnly() && !player.hasPermission("frost.admin")) continue;
            String group = "Other";
            if (categoryId.equals("weapon-skins")) {
                Integer s = cosmetic.getAppliesSlot();
                group = s != null ? "Slot " + s : "Misc";
            } else if (categoryId.equals("armor-cosmetics")) {
                String type = cosmetic.getAppliesType();
                if ("armor-set".equalsIgnoreCase(type)) group = "Sets";
                else {
                    String slotName = cosmetic.getAppliesArmorSlot();
                    group = slotName != null ? capitalize(slotName) : "Pieces";
                }
            } else if (categoryId.equals("particle-effects")) {
                ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
                if (pe != null) group = capitalize(pe.getEffectType().name().toLowerCase());
            }
            groups.computeIfAbsent(group, k -> new java.util.ArrayList<>()).add(cosmetic);
        }

        int slot = 9;
        for (var entry : groups.entrySet()) {
            // Header item
            ItemStack header = new ItemStack(Material.PAPER);
            ItemMeta hMeta = header.getItemMeta();
            hMeta.displayName(Component.text("— " + entry.getKey() + " —", NamedTextColor.AQUA));
            hMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
            header.setItemMeta(hMeta);
            inv.setItem(slot++, header);

            for (Cosmetic cosmetic : entry.getValue()) {
            ItemStack icon = new ItemStack(cosmetic.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(cosmetic.getName()));

            List<Component> lore = new ArrayList<>();
            for (String line : cosmetic.getDescription()) {
                lore.add(MiniMessage.miniMessage().deserialize(line));
            }
            lore.add(Component.text(""));
                if (cosmetic.isAdminOnly() && !player.hasPermission("frost.admin")) {
                    lore.add(Component.text("ADMIN-ONLY", NamedTextColor.RED));
                } else {
                    lore.add(Component.text("Price: " + plugin.getEconomy().format(cosmetic.getPrice()), NamedTextColor.YELLOW));
                }

                boolean owned = data.ownedCosmetics.contains(cosmetic.getId()) || player.hasPermission("frost.admin");
                if (owned) {
                lore.add(Component.text(""));
                lore.add(Component.text("✓ OWNED", NamedTextColor.GREEN));
            } else {
                lore.add(Component.text(""));
                lore.add(Component.text("Click to Purchase", NamedTextColor.GOLD));
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(cosmeticKey, PersistentDataType.STRING, cosmetic.getId());
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "buy_cosmetic");
            icon.setItemMeta(meta);

            inv.setItem(slot++, icon);
            }
        }

        player.openInventory(inv);
    }

    private void openBedrockCategoryItems(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        SimpleForm.Builder form = SimpleForm.builder()
                .title(category.getName())
                .content("Choose an item to purchase");

        form.button("← Back to Categories");

        // Build grouped list
        java.util.List<Object> entries = new java.util.ArrayList<>(); // String headers or Cosmetic items
        java.util.Map<String, java.util.List<Cosmetic>> groups = new java.util.LinkedHashMap<>();
        for (Cosmetic cosmetic : category.getCosmetics()) {
            if (cosmetic.isAdminOnly() && !player.hasPermission("frost.admin")) continue;
            String group = "Other";
            if (categoryId.equals("weapon-skins")) {
                Integer s = cosmetic.getAppliesSlot();
                group = s != null ? "Slot " + s : "Misc";
            } else if (categoryId.equals("armor-cosmetics")) {
                String type = cosmetic.getAppliesType();
                if ("armor-set".equalsIgnoreCase(type)) group = "Sets";
                else {
                    String slotName = cosmetic.getAppliesArmorSlot();
                    group = slotName != null ? capitalize(slotName) : "Pieces";
                }
            } else if (categoryId.equals("particle-effects")) {
                ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
                if (pe != null) group = capitalize(pe.getEffectType().name().toLowerCase());
            }
            groups.computeIfAbsent(group, k -> new java.util.ArrayList<>()).add(cosmetic);
        }
        for (var e : groups.entrySet()) {
            entries.add("== " + e.getKey() + " ==");
            entries.addAll(e.getValue());
        }

        for (Object entry : entries) {
            if (entry instanceof String headerText) {
                form.button(headerText);
                continue;
            }
            Cosmetic cosmetic = (Cosmetic) entry;
            boolean owned = data.ownedCosmetics.contains(cosmetic.getId()) || player.hasPermission("frost.admin");
            String status = owned
                    ? " ✓ OWNED"
                    : " - " + plugin.getEconomy().format(cosmetic.getPrice());
            form.button(cosmetic.getName() + status);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == 0) {
                openBedrockShopCategories(player);
            } else if (index > 0) {
                Object chosen = entries.get(index - 1);
                if (chosen instanceof String) {
                    openBedrockCategoryItems(player, categoryId);
                } else {
                    buyCosmetic(player, (Cosmetic) chosen);
                    openBedrockCategoryItems(player, categoryId); // Refresh
                }
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    // ============= EQUIP LOGIC =============
    public void openEquipMenu(Player player) {
        if (isFloodgatePresent && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openBedrockEquipCategories(player);
        } else {
            openJavaEquipCategories(player);
        }
    }

    private void openJavaEquipCategories(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Equip Cosmetics"));

        int slot = 10;
        for (CosmeticCategory category : plugin.getCosmeticManager().getCategories().values()) {
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(category.getName()));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(category.getDescription(), NamedTextColor.GRAY));
            lore.add(Component.text("Click to manage", NamedTextColor.YELLOW));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category.getId());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "equip_category");
            icon.setItemMeta(meta);

            inv.setItem(slot++, icon);
            if (slot == 17) slot = 19;
        }

        player.openInventory(inv);
    }

    private void openBedrockEquipCategories(Player player) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("Equip Cosmetics")
                .content("Choose a category");

        List<String> categoryIds = new ArrayList<>(plugin.getCosmeticManager().getCategories().keySet());
        for (String catId : categoryIds) {
            CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(catId);
            form.button(category.getName());
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < categoryIds.size()) {
                openBedrockEquipCategoryItems(player, categoryIds.get(index));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openJavaEquipCategoryItems(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null) return;

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Equip: ").append(MiniMessage.miniMessage().deserialize(category.getName())));

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back to Categories", NamedTextColor.YELLOW));
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_to_equip_categories");
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        // Unequip button
        ItemStack unequip = new ItemStack(Material.BARRIER);
        ItemMeta unequipMeta = unequip.getItemMeta();
        unequipMeta.displayName(Component.text("Unequip All", NamedTextColor.RED));
        unequipMeta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
        unequipMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "unequip_category");
        unequip.setItemMeta(unequipMeta);
        inv.setItem(8, unequip);

        int slot = 9;
        for (Cosmetic cosmetic : category.getCosmetics()) {
            boolean owned = data.ownedCosmetics.contains(cosmetic.getId()) || player.hasPermission("frost.admin");
            if (!owned) continue;

            ItemStack icon = new ItemStack(cosmetic.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(cosmetic.getName()));

            List<Component> lore = new ArrayList<>();
            for (String line : cosmetic.getDescription()) {
                lore.add(MiniMessage.miniMessage().deserialize(line));
            }

            // Check if equipped
            String equipKey;
            if (categoryId.equals("particle-effects")) {
                ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
                String eventKey = pe != null ? pe.getTriggerEvent().name() : "";
                equipKey = categoryId + ":" + eventKey;
            } else {
                equipKey = categoryId + ":" + (cosmetic.getAppliesSlot() != null ? cosmetic.getAppliesSlot() : "");
            }
            boolean isEquipped = cosmetic.getId().equals(data.equippedCosmetics.get(equipKey));

            lore.add(Component.text(""));
            if (isEquipped) {
                lore.add(Component.text("✓ EQUIPPED", NamedTextColor.GREEN));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(Component.text("Click to Equip", NamedTextColor.YELLOW));
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(cosmeticKey, PersistentDataType.STRING, cosmetic.getId());
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "toggle_equip");
            icon.setItemMeta(meta);

            inv.setItem(slot++, icon);
        }

        player.openInventory(inv);
    }

    private void openBedrockEquipCategoryItems(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        List<Cosmetic> ownedCosmetics = category.getCosmetics().stream()
                .filter(c -> data.ownedCosmetics.contains(c.getId()) || player.hasPermission("frost.admin"))
                .toList();

        SimpleForm.Builder form = SimpleForm.builder()
                .title(category.getName())
                .content("Choose a cosmetic to equip");

        form.button("← Back to Categories");
        form.button("Unequip All");

        for (Cosmetic cosmetic : ownedCosmetics) {
            String equipKey;
            if (categoryId.equals("particle-effects")) {
                ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
                String eventKey = pe != null ? pe.getTriggerEvent().name() : "";
                equipKey = categoryId + ":" + eventKey;
            } else {
                equipKey = categoryId + ":" + (cosmetic.getAppliesSlot() != null ? cosmetic.getAppliesSlot() : "");
            }
            boolean isEquipped = cosmetic.getId().equals(data.equippedCosmetics.get(equipKey));

            String status = isEquipped ? " ✓ EQUIPPED" : "";
            form.button(cosmetic.getName() + status);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == 0) {
                openBedrockEquipCategories(player);
            } else if (index == 1) {
                unequipCategory(player, categoryId);
                openBedrockEquipCategoryItems(player, categoryId);
            } else if (index > 1 && index - 2 < ownedCosmetics.size()) {
                toggleEquipCosmetic(player, ownedCosmetics.get(index - 2));
                openBedrockEquipCategoryItems(player, categoryId);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    // ============= SHARED LOGIC =============
    private void buyCosmetic(Player player, Cosmetic cosmetic) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        if (data.ownedCosmetics.contains(cosmetic.getId())) {
            player.sendMessage(Component.text("You already own this!", NamedTextColor.RED));
            return;
        }

        if (cosmetic.isAdminOnly() && !player.hasPermission("frost.admin")) {
            player.sendMessage(Component.text("This cosmetic is admin-only.", NamedTextColor.RED));
            return;
        }

        if (player.hasPermission("frost.admin")) {
            player.sendMessage(Component.text("Admins already have access to all cosmetics.", NamedTextColor.YELLOW));
            return;
        }

        if (plugin.getEconomy().getBalance(player) >= cosmetic.getPrice()) {
            plugin.getEconomy().withdrawPlayer(player, cosmetic.getPrice());
            data.ownedCosmetics.add(cosmetic.getId());
            plugin.getPlayerDataManager().savePlayerData(player);
            player.sendMessage(Component.text("Purchased ", NamedTextColor.GREEN)
                    .append(MiniMessage.miniMessage().deserialize(cosmetic.getName())));
        } else {
            player.sendMessage(Component.text("Not enough money!", NamedTextColor.RED));
        }
    }

    private void toggleEquipCosmetic(Player player, Cosmetic cosmetic) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        String equipKey;
        if (cosmetic.getCategoryId().equals("particle-effects")) {
            ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
            if (pe == null) return;
            String eventName = pe.getTriggerEvent().name();
            equipKey = cosmetic.getCategoryId() + ":" + eventName;

            // Enforce rules:
            // - If ALWAYS equipped, block others
            if (!eventName.equals("ALWAYS") && data.equippedCosmetics.containsKey("particle-effects:ALWAYS")) {
                player.sendMessage(Component.text("Disable your ALWAYS particle before equipping others.", NamedTextColor.RED));
                return;
            }
            // - If equipping ALWAYS, must have no other particle-effects equipped
            if (eventName.equals("ALWAYS")) {
                boolean hasOthers = data.equippedCosmetics.keySet().stream()
                        .anyMatch(k -> k.startsWith("particle-effects:") && !k.equals("particle-effects:ALWAYS"));
                if (hasOthers) {
                    player.sendMessage(Component.text("Unequip other particles before enabling an ALWAYS effect.", NamedTextColor.RED));
                    return;
                }
            }
        } else {
            equipKey = cosmetic.getCategoryId() + ":" +
                    (cosmetic.getAppliesSlot() != null ? cosmetic.getAppliesSlot() : "");
        }

        boolean currentlyEquipped = cosmetic.getId().equals(data.equippedCosmetics.get(equipKey));

        if (currentlyEquipped) {
            data.equippedCosmetics.remove(equipKey);
            player.sendMessage(Component.text("Unequipped ", NamedTextColor.YELLOW)
                    .append(MiniMessage.miniMessage().deserialize(cosmetic.getName())));
        } else {
            // For particle-effects: ensure only one per event
            if (cosmetic.getCategoryId().equals("particle-effects")) {
                data.equippedCosmetics.put(equipKey, cosmetic.getId());
            } else {
                data.equippedCosmetics.put(equipKey, cosmetic.getId());
            }
            player.sendMessage(Component.text("Equipped ", NamedTextColor.GREEN)
                    .append(MiniMessage.miniMessage().deserialize(cosmetic.getName())));
        }

        plugin.getPlayerDataManager().savePlayerData(player);

        if (plugin.getHotbarLockListener() != null) {
            plugin.getHotbarLockListener().giveHotbarItems(player);
        }
    }

    private void unequipCategory(Player player, String categoryId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        data.equippedCosmetics.keySet().removeIf(key -> key.startsWith(categoryId + ":"));
        plugin.getPlayerDataManager().savePlayerData(player);
        player.sendMessage(Component.text("Unequipped all cosmetics in this category", NamedTextColor.YELLOW));

        if (plugin.getHotbarLockListener() != null) {
            plugin.getHotbarLockListener().giveHotbarItems(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        switch (action) {
            case "browse_category":
                String categoryId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                openJavaCategoryItems(player, categoryId);
                break;
            case "back_to_categories":
                openJavaShopCategories(player);
                break;
            case "buy_cosmetic":
                String cosmeticId = meta.getPersistentDataContainer().get(cosmeticKey, PersistentDataType.STRING);
                Cosmetic cosmetic = plugin.getCosmeticManager().getCosmetic(cosmeticId);
                if (cosmetic != null) {
                    buyCosmetic(player, cosmetic);
                    String catId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                    openJavaCategoryItems(player, catId);
                }
                break;
            case "equip_category":
                String equipCatId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                openJavaEquipCategoryItems(player, equipCatId);
                break;
            case "back_to_equip_categories":
                openJavaEquipCategories(player);
                break;
            case "toggle_equip":
                String toggleCosmeticId = meta.getPersistentDataContainer().get(cosmeticKey, PersistentDataType.STRING);
                Cosmetic toggleCosmetic = plugin.getCosmeticManager().getCosmetic(toggleCosmeticId);
                if (toggleCosmetic != null) {
                    toggleEquipCosmetic(player, toggleCosmetic);
                    String toggleCatId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                    openJavaEquipCategoryItems(player, toggleCatId);
                }
                break;
            case "unequip_category":
                String unequipCatId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                unequipCategory(player, unequipCatId);
                openJavaEquipCategoryItems(player, unequipCatId);
                break;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
