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
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

public class GuiManager implements Listener {
    private final Frost plugin;
    private final boolean isFloodgatePresent;
    private final NamespacedKey categoryKey;
    private final NamespacedKey cosmeticKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey subgroupKey;

    public GuiManager(Frost plugin) {
        this.plugin = plugin;
        this.isFloodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        this.categoryKey = new NamespacedKey(plugin, "category_id");
        this.cosmeticKey = new NamespacedKey(plugin, "cosmetic_id");
        this.actionKey = new NamespacedKey(plugin, "action");
        this.subgroupKey = new NamespacedKey(plugin, "subgroup_id");
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
            if (slot == 17)
                slot = 19;
        }

        player.openInventory(inv);
    }

    private void openBedrockShopCategories(Player player) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§bCosmetics Shop")
                .content("§7Choose a category to browse");

        List<String> categoryIds = new ArrayList<>(plugin.getCosmeticManager().getCategories().keySet());
        for (String catId : categoryIds) {
            CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(catId);
            String cleanName = stripMiniMessage(category.getName());
            form.button("§r" + cleanName + "\n§7" + category.getCosmetics().size() + " items");
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < categoryIds.size()) {
                openBedrockSubgroups(player, categoryIds.get(index));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openJavaCategoryItems(Player player, String categoryId) {
        // Subgroup picker first
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Shop: ").append(MiniMessage.miniMessage().deserialize(category.getName()))
                        .append(Component.text(" • Choose Type", NamedTextColor.GRAY)));

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back to Categories", NamedTextColor.YELLOW));
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_to_categories");
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        java.util.Map<String, java.util.List<Cosmetic>> groups = subgroupMapForCategory(player, categoryId);

        int slot = 9;
        for (var entry : groups.entrySet()) {
            ItemStack button = new ItemStack(Material.PAPER);
            ItemMeta meta = button.getItemMeta();
            meta.displayName(Component.text(entry.getKey(), NamedTextColor.AQUA));
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
            meta.getPersistentDataContainer().set(subgroupKey, PersistentDataType.STRING, entry.getKey());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "browse_subgroup");
            button.setItemMeta(meta);
            inv.setItem(slot++, button);
        }

        player.openInventory(inv);
    }

    private void openJavaItemsForSubgroup(Player player, String categoryId, String subgroup) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Shop: ").append(MiniMessage.miniMessage().deserialize(category.getName()))
                        .append(Component.text(" • " + subgroup, NamedTextColor.GRAY)));

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back to Types", NamedTextColor.YELLOW));
        backMeta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_to_subgroups");
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        java.util.Map<String, java.util.List<Cosmetic>> groups = subgroupMapForCategory(player, categoryId);
        List<Cosmetic> items = groups.getOrDefault(subgroup, java.util.List.of());

        int slot = 9;
        for (Cosmetic cosmetic : items) {
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
                lore.add(Component.text("Price: " + plugin.getEconomy().format(cosmetic.getPrice()),
                        NamedTextColor.YELLOW));
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

        player.openInventory(inv);
    }

    private java.util.Map<String, java.util.List<Cosmetic>> subgroupMapForCategory(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        java.util.Map<String, java.util.List<Cosmetic>> groups = new java.util.LinkedHashMap<>();
        if (category == null)
            return groups;
        for (Cosmetic cosmetic : category.getCosmetics()) {
            if (cosmetic.isAdminOnly() && !player.hasPermission("frost.admin"))
                continue;
            String group = "Other";
            if (categoryId.equals("weapon-skins")) {
                String label = "Misc Item";
                String profileId = cosmetic.getAppliesProfile();
                Integer s = cosmetic.getAppliesSlot();
                if (profileId != null && s != null) {
                    var profile = plugin.getProfileManager().getProfile(profileId);
                    if (profile != null) {
                        var item = profile.getHotbarItems().get(s);
                        if (item != null) {
                            var meta = item.getItemMeta();
                            if (meta != null && meta.hasDisplayName()) {
                                label = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                                        .plainText().serialize(meta.displayName());
                            } else {
                                label = prettifyMaterial(item.getType().name());
                            }
                        } else {
                            label = "Profile " + profileId + " Slot " + s;
                        }
                    }
                }
                group = label;
            } else if (categoryId.equals("armor-cosmetics")) {
                String type = cosmetic.getAppliesType();
                if ("armor-set".equalsIgnoreCase(type)) {
                    group = "Sets";
                } else {
                    String slotName = cosmetic.getAppliesArmorSlot();
                    group = slotName != null ? capitalize(slotName) : "Pieces";
                }
            } else if (categoryId.equals("particle-effects")) {
                ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
                if (pe != null)
                    group = prettifyMaterial(pe.getTriggerEvent().name());
            }
            groups.computeIfAbsent(group, k -> new java.util.ArrayList<>()).add(cosmetic);
        }
        return groups;
    }

    private void openBedrockSubgroups(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        String cleanCategoryName = stripMiniMessage(category.getName());
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b" + cleanCategoryName)
                .content("§7Choose a type to browse");

        form.button("§e← Back to Categories");

        var groups = subgroupMapForCategory(player, categoryId);
        java.util.List<String> subgroupNames = new java.util.ArrayList<>(groups.keySet());
        for (String name : subgroupNames) {
            form.button("§r" + name + "\n§7" + groups.getOrDefault(name, java.util.List.of()).size() + " items");
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == 0) {
                openBedrockShopCategories(player);
            } else if (index > 0 && index <= subgroupNames.size()) {
                openBedrockItemsForSubgroup(player, categoryId, subgroupNames.get(index - 1));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openBedrockItemsForSubgroup(Player player, String categoryId, String subgroup) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        var groups = subgroupMapForCategory(player, categoryId);
        java.util.List<Cosmetic> cosmetics = new java.util.ArrayList<>(
                groups.getOrDefault(subgroup, java.util.List.of()));

        String cleanCategoryName = stripMiniMessage(category.getName());
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b" + cleanCategoryName + " §7• " + subgroup)
                .content("§7Tap an item for details");

        form.button("§e← Back to Types");

        for (Cosmetic cosmetic : cosmetics) {
            boolean owned = data.ownedCosmetics.contains(cosmetic.getId()) || player.hasPermission("frost.admin");
            String cleanName = stripMiniMessage(cosmetic.getName());
            String status = owned ? "§a✓ OWNED" : "§6" + plugin.getEconomy().format(cosmetic.getPrice());

            form.button("§r" + cleanName + "\n" + status);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == 0) {
                openBedrockSubgroups(player, categoryId);
            } else if (index > 0 && index <= cosmetics.size()) {
                showBedrockCosmeticDetails(player, cosmetics.get(index - 1), categoryId, subgroup, false);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void showBedrockCosmeticDetails(Player player, Cosmetic cosmetic, String categoryId, String subgroup, boolean isEquipMenu) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        String cleanName = stripMiniMessage(cosmetic.getName());
        boolean owned = data.ownedCosmetics.contains(cosmetic.getId()) || player.hasPermission("frost.admin");

        // Build description
        StringBuilder content = new StringBuilder();
        for (String line : cosmetic.getDescription()) {
            content.append("§7").append(stripMiniMessage(line)).append("\n");
        }
        content.append("\n");

        if (cosmetic.isAdminOnly() && !player.hasPermission("frost.admin")) {
            content.append("§c§lADMIN-ONLY\n");
            content.append("§7This cosmetic is restricted to admins.");
        } else if (!owned) {
            content.append("§6Price: ").append(plugin.getEconomy().format(cosmetic.getPrice())).append("\n");
            content.append("§7Balance: ").append(plugin.getEconomy().format(plugin.getEconomy().getBalance(player)));
        } else {
            content.append("§a✓ You own this cosmetic");
        }

        if (isEquipMenu) {
            // Equip menu - show equip/unequip button
            String equipKey = getEquipKey(cosmetic, categoryId);
            boolean isEquipped = cosmetic.getId().equals(data.equippedCosmetics.get(equipKey));

            ModalForm.Builder form = ModalForm.builder()
                    .title("§b" + cleanName)
                    .content(content.toString())
                    .button1(isEquipped ? "§cUnequip" : "§aEquip")
                    .button2("§7Cancel");

            form.validResultHandler(response -> {
                if (response.clickedButtonId() == 0) {
                    toggleEquipCosmetic(player, cosmetic);
                }
                openBedrockEquipItemsForSubgroup(player, categoryId, subgroup);
            });

            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
        } else {
            // Shop menu - show purchase button
            ModalForm.Builder form = ModalForm.builder()
                    .title("§b" + cleanName)
                    .content(content.toString());

            if (!owned && !cosmetic.isAdminOnly()) {
                form.button1("§aPurchase")
                        .button2("§7Cancel");
            } else {
                form.button1("§7Back")
                        .button2("");
            }

            form.validResultHandler(response -> {
                if (response.clickedButtonId() == 0 && !owned && !cosmetic.isAdminOnly()) {
                    buyCosmetic(player, cosmetic);
                }
                openBedrockItemsForSubgroup(player, categoryId, subgroup);
            });

            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
        }
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
            if (slot == 17)
                slot = 19;
        }

        player.openInventory(inv);
    }

    private void openBedrockEquipCategories(Player player) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§bEquip Cosmetics")
                .content("§7Choose a category");

        List<String> categoryIds = new ArrayList<>(plugin.getCosmeticManager().getCategories().keySet());
        for (String catId : categoryIds) {
            CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(catId);
            String cleanName = stripMiniMessage(category.getName());
            form.button("§r" + cleanName);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < categoryIds.size()) {
                openBedrockEquipSubgroups(player, categoryIds.get(index));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openJavaEquipSubgroups(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                Component.text("Equip: ")
                        .append(MiniMessage.miniMessage().deserialize(category.getName()))
                        .append(Component.text(" • Choose Type", NamedTextColor.GRAY)));

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back to Categories", NamedTextColor.YELLOW));
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_to_equip_categories");
        back.setItemMeta(backMeta);
        inv.setItem(0, back);

        var groups = subgroupMapForCategory(player, categoryId);
        // Only show subgroups with at least one owned cosmetic
        int slot = 9;
        for (var entry : groups.entrySet()) {
            boolean hasOwned = entry.getValue().stream()
                    .anyMatch(c -> data.ownedCosmetics.contains(c.getId()) || player.hasPermission("frost.admin"));
            if (!hasOwned)
                continue;
            ItemStack button = new ItemStack(Material.PAPER);
            ItemMeta meta = button.getItemMeta();
            meta.displayName(Component.text(entry.getKey(), NamedTextColor.AQUA));
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
            meta.getPersistentDataContainer().set(subgroupKey, PersistentDataType.STRING, entry.getKey());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "equip_browse_subgroup");
            button.setItemMeta(meta);
            inv.setItem(slot++, button);
        }

        player.openInventory(inv);
    }

    private void openJavaEquipCategoryItems(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                Component.text("Equip: ").append(MiniMessage.miniMessage().deserialize(category.getName())));

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

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
            if (!owned)
                continue;
            ItemStack icon = new ItemStack(cosmetic.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(cosmetic.getName()));

            List<Component> lore = new ArrayList<>();
            for (String line : cosmetic.getDescription()) {
                lore.add(MiniMessage.miniMessage().deserialize(line));
            }

            // Check if equipped
            String equipKey = getEquipKey(cosmetic, categoryId);
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

    private void openJavaEquipItemsForSubgroup(Player player, String categoryId, String subgroup) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("Equip: ").append(MiniMessage.miniMessage().deserialize(category.getName()))
                        .append(Component.text(" • " + subgroup, NamedTextColor.GRAY)));

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("← Back to Types", NamedTextColor.YELLOW));
        backMeta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
        backMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_to_equip_subgroups");
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

        var groups = subgroupMapForCategory(player, categoryId);
        var items = groups.getOrDefault(subgroup, java.util.List.of());

        int slot = 9;
        for (Cosmetic cosmetic : items) {
            boolean owned = data.ownedCosmetics.contains(cosmetic.getId()) || player.hasPermission("frost.admin");
            if (!owned)
                continue;

            ItemStack icon = new ItemStack(cosmetic.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(cosmetic.getName()));

            List<Component> lore = new ArrayList<>();
            for (String line : cosmetic.getDescription()) {
                lore.add(MiniMessage.miniMessage().deserialize(line));
            }

            String equipKey = getEquipKey(cosmetic, categoryId);
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

    private void openBedrockEquipSubgroups(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        String cleanCategoryName = stripMiniMessage(category.getName());
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b" + cleanCategoryName)
                .content("§7Choose a type");

        form.button("§e← Back to Categories");
        form.button("§cUnequip All");

        var allGroups = subgroupMapForCategory(player, categoryId);
        java.util.List<String> subgroups = new java.util.ArrayList<>();
        for (var e : allGroups.entrySet()) {
            boolean hasOwned = e.getValue().stream()
                    .anyMatch(c -> data.ownedCosmetics.contains(c.getId()) || player.hasPermission("frost.admin"));
            if (hasOwned) {
                subgroups.add(e.getKey());
                form.button("§r" + e.getKey());
            }
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == 0) {
                openBedrockEquipCategories(player);
            } else if (index == 1) {
                unequipCategory(player, categoryId);
                openBedrockEquipSubgroups(player, categoryId);
            } else if (index > 1 && index - 2 < subgroups.size()) {
                openBedrockEquipItemsForSubgroup(player, categoryId, subgroups.get(index - 2));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openBedrockEquipItemsForSubgroup(Player player, String categoryId, String subgroup) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        var groups = subgroupMapForCategory(player, categoryId);
        List<Cosmetic> items = groups.getOrDefault(subgroup, java.util.List.of()).stream()
                .filter(c -> data.ownedCosmetics.contains(c.getId()) || player.hasPermission("frost.admin"))
                .toList();

        String cleanCategoryName = stripMiniMessage(category.getName());
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b" + cleanCategoryName + " §7• " + subgroup)
                .content("§7Tap an item for details");

        form.button("§e← Back to Types");

        for (Cosmetic cosmetic : items) {
            String equipKey = getEquipKey(cosmetic, categoryId);
            boolean isEquipped = cosmetic.getId().equals(data.equippedCosmetics.get(equipKey));
            String cleanName = stripMiniMessage(cosmetic.getName());
            String status = isEquipped ? "§a✓ EQUIPPED" : "§7Not equipped";

            form.button("§r" + cleanName + "\n" + status);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == 0) {
                openBedrockEquipSubgroups(player, categoryId);
            } else if (index > 0 && index <= items.size()) {
                showBedrockCosmeticDetails(player, items.get(index - 1), categoryId, subgroup, true);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openBedrockEquipCategoryItems(Player player, String categoryId) {
        CosmeticCategory category = plugin.getCosmeticManager().getCategories().get(categoryId);
        if (category == null)
            return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        List<Cosmetic> ownedCosmetics = category.getCosmetics().stream()
                .filter(c -> data.ownedCosmetics.contains(c.getId()) || player.hasPermission("frost.admin"))
                .toList();

        String cleanCategoryName = stripMiniMessage(category.getName());
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§b" + cleanCategoryName)
                .content("§7Choose a cosmetic to equip");

        form.button("§e← Back to Categories");
        form.button("§cUnequip All");

        for (Cosmetic cosmetic : ownedCosmetics) {
            String equipKey = getEquipKey(cosmetic, categoryId);
            boolean isEquipped = cosmetic.getId().equals(data.equippedCosmetics.get(equipKey));
            String cleanName = stripMiniMessage(cosmetic.getName());
            String status = isEquipped ? "§a✓ EQUIPPED" : "§7Not equipped";

            form.button("§r" + cleanName + "\n" + status);
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
    private String getEquipKey(Cosmetic cosmetic, String categoryId) {
        if (categoryId.equals("particle-effects")) {
            ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
            String eventKey = pe != null ? pe.getTriggerEvent().name() : "";
            return categoryId + ":" + eventKey;
        } else if (categoryId.equals("armor-cosmetics")) {
            String type = cosmetic.getAppliesType();
            if ("armor-set".equalsIgnoreCase(type)) {
                return categoryId + ":SET";
            } else {
                String slotName = cosmetic.getAppliesArmorSlot();
                return categoryId + ":" + (slotName != null ? slotName.toUpperCase() : "PIECE");
            }
        } else {
            return categoryId + ":" + (cosmetic.getAppliesSlot() != null ? cosmetic.getAppliesSlot() : "");
        }
    }

    private void buyCosmetic(Player player, Cosmetic cosmetic) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        // Check if admin trying to "buy" admin cosmetic
        if (player.hasPermission("frost.admin") && cosmetic.isAdminOnly()) {
            player.sendMessage(Component.text("Admins automatically have access to all cosmetics.", NamedTextColor.YELLOW));
            return;
        }

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
        if (data == null)
            return;

        String equipKey = getEquipKey(cosmetic, cosmetic.getCategoryId());
        boolean currentlyEquipped = cosmetic.getId().equals(data.equippedCosmetics.get(equipKey));

        // Enforce particle rules
        if (cosmetic.getCategoryId().equals("particle-effects")) {
            ParticleEffect pe = plugin.getParticleManager().getParticleEffect(cosmetic.getId());
            if (pe == null)
                return;
            String eventName = pe.getTriggerEvent().name();

            if (!eventName.equals("ALWAYS") && data.equippedCosmetics.containsKey("particle-effects:ALWAYS")) {
                player.sendMessage(
                        Component.text("Disable your ALWAYS particle before equipping others.", NamedTextColor.RED));
                return;
            }
            if (eventName.equals("ALWAYS")) {
                boolean hasOthers = data.equippedCosmetics.keySet().stream()
                        .anyMatch(k -> k.startsWith("particle-effects:") && !k.equals("particle-effects:ALWAYS"));
                if (hasOthers) {
                    player.sendMessage(Component.text("Unequip other particles before enabling an ALWAYS effect.",
                            NamedTextColor.RED));
                    return;
                }
            }
        }

        if (currentlyEquipped) {
            data.equippedCosmetics.remove(equipKey);
            player.sendMessage(Component.text("Unequipped ", NamedTextColor.YELLOW)
                    .append(MiniMessage.miniMessage().deserialize(cosmetic.getName())));
            if (cosmetic.getCategoryId().equals("armor-cosmetics")) {
                plugin.getCosmeticManager().removeArmorCosmetic(player, cosmetic);
            }
        } else {
            data.equippedCosmetics.put(equipKey, cosmetic.getId());
            player.sendMessage(Component.text("Equipped ", NamedTextColor.GREEN)
                    .append(MiniMessage.miniMessage().deserialize(cosmetic.getName())));
            if (cosmetic.getCategoryId().equals("armor-cosmetics")) {
                plugin.getCosmeticManager().applyArmorCosmetic(player, cosmetic);
            }
        }

        plugin.getPlayerDataManager().savePlayerData(player);

        if (plugin.getHotbarLockListener() != null) {
            plugin.getHotbarLockListener().giveHotbarItems(player);
        }
    }

    private void unequipCategory(Player player, String categoryId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null)
            return;

        data.equippedCosmetics.keySet().removeIf(key -> key.startsWith(categoryId + ":"));
        plugin.getPlayerDataManager().savePlayerData(player);
        player.sendMessage(Component.text("Unequipped all cosmetics in this category", NamedTextColor.YELLOW));

        if (plugin.getHotbarLockListener() != null) {
            plugin.getHotbarLockListener().giveHotbarItems(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta())
            return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING))
            return;

        event.setCancelled(true);
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        switch (action) {
            case "browse_category":
                String categoryId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                openJavaCategoryItems(player, categoryId);
                break;
            case "browse_subgroup":
                String catId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                String sub = meta.getPersistentDataContainer().get(subgroupKey, PersistentDataType.STRING);
                openJavaItemsForSubgroup(player, catId, sub);
                break;
            case "back_to_categories":
                openJavaShopCategories(player);
                break;
            case "back_to_subgroups":
                String backCat = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                openJavaCategoryItems(player, backCat);
                break;
            case "buy_cosmetic":
                String cosmeticId = meta.getPersistentDataContainer().get(cosmeticKey, PersistentDataType.STRING);
                Cosmetic cosmetic = plugin.getCosmeticManager().getCosmetic(cosmeticId);
                if (cosmetic != null) {
                    buyCosmetic(player, cosmetic);
                    String categoryAfterBuy = meta.getPersistentDataContainer().get(categoryKey,
                            PersistentDataType.STRING);
                    openJavaCategoryItems(player, categoryAfterBuy);
                }
                break;
            case "equip_category":
                String equipCatId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                openJavaEquipSubgroups(player, equipCatId);
                break;
            case "back_to_equip_categories":
                openJavaEquipCategories(player);
                break;
            case "equip_browse_subgroup":
                String equipCat = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                String equipSub = meta.getPersistentDataContainer().get(subgroupKey, PersistentDataType.STRING);
                openJavaEquipItemsForSubgroup(player, equipCat, equipSub);
                break;
            case "back_to_equip_subgroups":
                String backEquipCat = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                openJavaEquipSubgroups(player, backEquipCat);
                break;
            case "toggle_equip":
                String toggleCosmeticId = meta.getPersistentDataContainer().get(cosmeticKey, PersistentDataType.STRING);
                Cosmetic toggleCosmetic = plugin.getCosmeticManager().getCosmetic(toggleCosmeticId);
                if (toggleCosmetic != null) {
                    toggleEquipCosmetic(player, toggleCosmetic);
                    String toggleCatId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                    openJavaEquipSubgroups(player, toggleCatId);
                }
                break;
            case "unequip_category":
                String unequipCatId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                unequipCategory(player, unequipCatId);
                openJavaEquipSubgroups(player, unequipCatId);
                break;
        }
    }

    // ============= UTILITY METHODS =============

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String prettifyMaterial(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty())
                continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
            if (i < parts.length - 1)
                sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Strip MiniMessage tags from text for Bedrock forms
     */
    private String stripMiniMessage(String text) {
        if (text == null) return "";
        // Remove all MiniMessage tags like <red>, </red>, <bold>, <gradient:...>, etc.
        return text.replaceAll("</?[^>]+>", "");
    }
}