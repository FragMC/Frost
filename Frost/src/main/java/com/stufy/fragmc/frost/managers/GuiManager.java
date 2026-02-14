package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.managers.ConfigManager.Modifier;
import com.stufy.fragmc.frost.managers.PlayerDataManager.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final NamespacedKey modifierKey;
    private final NamespacedKey slotKey;

    public GuiManager(Frost plugin) {
        this.plugin = plugin;
        this.isFloodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        this.modifierKey = new NamespacedKey(plugin, "modifier_id");
        this.slotKey = new NamespacedKey(plugin, "target_slot");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // --- SHOP LOGIC ---
    public void openShop(Player player) {
        if (isFloodgatePresent && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openBedrockShopItemSelection(player);
        } else {
            openJavaShopItemSelection(player);
        }
    }

    private void openJavaShopItemSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Shop: Select Item"));
        
        plugin.getConfigManager().getHotbarItems().forEach((slot, item) -> {
            ItemStack icon = item.clone();
            ItemMeta meta = icon.getItemMeta();
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(Component.text("Click to view modifiers", NamedTextColor.YELLOW));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
        });
        
        player.openInventory(inv);
    }

    private void openBedrockShopItemSelection(Player player) {
        SimpleForm.Builder form = SimpleForm.builder()
            .title("Shop: Select Item")
            .content("Choose an item to browse modifiers for");

        List<Integer> slots = new ArrayList<>(plugin.getConfigManager().getHotbarItems().keySet());
        
        for (Integer slot : slots) {
            ItemStack item = plugin.getConfigManager().getHotbarItems().get(slot);
            String name = item.getItemMeta().hasDisplayName() 
                ? ((net.kyori.adventure.text.TextComponent) item.getItemMeta().displayName()).content() 
                : item.getType().name();
            form.button("Slot " + slot + ": " + name);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < slots.size()) {
                openBedrockShopModifiers(player, slots.get(index));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openJavaShopModifiers(Player player, int slot) {
        Inventory shop = Bukkit.createInventory(null, 54, Component.text("Shop: Modifiers for Slot " + slot));
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        
        List<Modifier> modifiers = plugin.getConfigManager().getItemModifiers().get(slot);
        if (modifiers == null) modifiers = new ArrayList<>();

        for (Modifier modifier : modifiers) {
            ItemStack icon = new ItemStack(Material.PAPER); 
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(modifier.getName(), NamedTextColor.AQUA));
            meta.getPersistentDataContainer().set(modifierKey, PersistentDataType.STRING, modifier.getId());
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Price: " + plugin.getEconomy().format(modifier.getPrice()), NamedTextColor.YELLOW));
            
            if (data.unlockedModifiers.contains(modifier.getId())) {
                lore.add(Component.text("ALREADY OWNED", NamedTextColor.GREEN));
            } else {
                lore.add(Component.text("Click to Buy", NamedTextColor.GOLD));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
            shop.addItem(icon);
        }
        player.openInventory(shop);
    }

    private void openBedrockShopModifiers(Player player, int slot) {
        SimpleForm.Builder form = SimpleForm.builder()
            .title("Modifiers for Slot " + slot)
            .content("Choose a modifier to buy");

        List<Modifier> modifiers = plugin.getConfigManager().getItemModifiers().get(slot);
        if (modifiers == null) modifiers = new ArrayList<>();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        for (Modifier modifier : modifiers) {
             String status = data.unlockedModifiers.contains(modifier.getId()) ? " (Owned)" : " - " + modifier.getPrice();
             form.button(modifier.getName() + status);
        }

        List<Modifier> finalModifiers = modifiers;
        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < finalModifiers.size()) {
                Modifier selected = finalModifiers.get(index);
                buyModifier(player, selected);
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }


    // --- EQUIP LOGIC ---
    public void openEquipMenu(Player player) {
        if (isFloodgatePresent && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            openBedrockEquipSlots(player);
        } else {
            openJavaEquipSlots(player);
        }
    }

    private void openJavaEquipSlots(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Equip: Select Item"));
        
        plugin.getConfigManager().getHotbarItems().forEach((slot, item) -> {
            ItemStack icon = item.clone();
            ItemMeta meta = icon.getItemMeta();
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(Component.text("Click to manage modifiers", NamedTextColor.YELLOW));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
        });
        
        player.openInventory(inv);
    }

    private void openBedrockEquipSlots(Player player) {
        SimpleForm.Builder form = SimpleForm.builder()
            .title("Equip: Select Item")
            .content("Choose an item to modify");

        List<Integer> slots = new ArrayList<>(plugin.getConfigManager().getHotbarItems().keySet());
        
        for (Integer slot : slots) {
            ItemStack item = plugin.getConfigManager().getHotbarItems().get(slot);
            String name = item.getItemMeta().hasDisplayName() 
                ? ((net.kyori.adventure.text.TextComponent) item.getItemMeta().displayName()).content() 
                : item.getType().name();
            form.button("Slot " + slot + ": " + name);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < slots.size()) {
                openBedrockEquipModifiers(player, slots.get(index));
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void openJavaEquipModifiers(Player player, int slot) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Equip: Modifiers for Slot " + slot));
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        
        // Unequip option
        ItemStack unequip = new ItemStack(Material.BARRIER);
        ItemMeta unequipMeta = unequip.getItemMeta();
        unequipMeta.displayName(Component.text("Unequip Current Modifier", NamedTextColor.RED));
        unequipMeta.getPersistentDataContainer().set(modifierKey, PersistentDataType.STRING, "UNEQUIP");
        unequipMeta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
        unequip.setItemMeta(unequipMeta);
        inv.setItem(0, unequip);

        List<Modifier> modifiers = plugin.getConfigManager().getItemModifiers().get(slot);
        if (modifiers == null) modifiers = new ArrayList<>();

        for (Modifier modifier : modifiers) {
            if (!data.unlockedModifiers.contains(modifier.getId())) continue;

            ItemStack icon = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(modifier.getName(), NamedTextColor.AQUA));
            meta.getPersistentDataContainer().set(modifierKey, PersistentDataType.STRING, modifier.getId());
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
            
            List<Component> lore = new ArrayList<>();
            if (data.selectedModifiers.getOrDefault(slot, "").equals(modifier.getId())) {
                lore.add(Component.text("CURRENTLY EQUIPPED", NamedTextColor.GREEN));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(Component.text("Click to Equip", NamedTextColor.YELLOW));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
            inv.addItem(icon);
        }
        
        player.openInventory(inv);
    }

    private void openBedrockEquipModifiers(Player player, int slot) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        
        List<Modifier> modifiers = plugin.getConfigManager().getItemModifiers().get(slot);
        if (modifiers == null) modifiers = new ArrayList<>();
        
        // Filter only unlocked
        List<Modifier> unlocked = new ArrayList<>();
        for (Modifier m : modifiers) {
            if (data.unlockedModifiers.contains(m.getId())) {
                unlocked.add(m);
            }
        }
        
        SimpleForm.Builder form = SimpleForm.builder()
            .title("Modifiers for Slot " + slot)
            .content("Choose a modifier to equip");

        form.button("Unequip Current");

        for (Modifier modifier : unlocked) {
            String status = data.selectedModifiers.getOrDefault(slot, "").equals(modifier.getId()) ? " (Equipped)" : "";
            form.button(modifier.getName() + status);
        }

        form.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index == 0) {
                equipModifier(player, slot, null); // Unequip
            } else if (index > 0 && index <= unlocked.size()) {
                equipModifier(player, slot, unlocked.get(index - 1).getId());
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }


    // --- SHARED LOGIC ---
    private void buyModifier(Player player, Modifier modifier) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data.unlockedModifiers.contains(modifier.getId())) {
            player.sendMessage(Component.text("You already own this!", NamedTextColor.RED));
            return;
        }

        if (plugin.getEconomy().getBalance(player) >= modifier.getPrice()) {
            plugin.getEconomy().withdrawPlayer(player, modifier.getPrice());
            data.unlockedModifiers.add(modifier.getId());
            plugin.getPlayerDataManager().savePlayerData(player);
            player.sendMessage(Component.text("Purchased " + modifier.getName(), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Not enough money!", NamedTextColor.RED));
        }
    }

    private void equipModifier(Player player, int slot, String modifierId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (modifierId == null) {
            data.selectedModifiers.remove(slot);
            player.sendMessage(Component.text("Unequipped modifier from slot " + slot, NamedTextColor.YELLOW));
        } else {
            data.selectedModifiers.put(slot, modifierId);
            player.sendMessage(Component.text("Equipped modifier to slot " + slot, NamedTextColor.GREEN));
        }
        plugin.getPlayerDataManager().savePlayerData(player);
        
        if (plugin.getHotbarLockListener() != null) {
            plugin.getHotbarLockListener().giveHotbarItems(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Component titleComp = event.getView().title();
        String title = null;
        if (titleComp instanceof net.kyori.adventure.text.TextComponent) {
            title = ((net.kyori.adventure.text.TextComponent) titleComp).content();
        } else {
            // Fallback for complex components or just ignore
            // For now, let's rely on simple string check if possible, or assume our titles are simple
            // We can also assume the title we set in createInventory is preserved.
            // But Adventure components are tricky. 
            // Let's use getPersistentDataContainer on items to route logic safely.
        }
        
        // Simple check on top inventory title if text content is available
        // Or just checking the items they clicked.
        
        // Route based on what the inventory IS.
        // We can't easily identify inventory by object reference since it's recreated.
        // Let's rely on Title Strings for now, assuming standard client.
        
        // Re-serializing component to plain text for check:
        // Actually, we can just use the plain string if we used Component.text("...")
        
        // Let's try to match logic by item NBT
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        
        boolean isShopItemSelection = event.getView().title().equals(Component.text("Shop: Select Item"));
        boolean isShopModifierSelection = event.getView().title().toString().contains("Shop: Modifiers for Slot"); // Rough check
        boolean isEquipItemSelection = event.getView().title().equals(Component.text("Equip: Select Item"));
        boolean isEquipModifierSelection = event.getView().title().toString().contains("Equip: Modifiers for Slot");

        // Better approach: Check if we are in one of our GUIs
        // Since we can't easily check title string from Component without serializer,
        // let's rely on our custom PDC keys to identify actions.
        
        if (item.getItemMeta().getPersistentDataContainer().has(slotKey, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            int slot = item.getItemMeta().getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
            
            // If it also has modifierKey, it's a modifier or unequip button
            if (item.getItemMeta().getPersistentDataContainer().has(modifierKey, PersistentDataType.STRING)) {
                String modId = item.getItemMeta().getPersistentDataContainer().get(modifierKey, PersistentDataType.STRING);
                
                // Need to distinguish between SHOP buy and EQUIP select.
                // We can't do that solely by item. We need context.
                // Since Title matching is annoying with Components, let's just try to match exact component
                if (event.getView().title().equals(Component.text("Shop: Modifiers for Slot " + slot))) {
                     Modifier mod = plugin.getConfigManager().getModifierRegistry().get(modId);
                     if (mod != null) buyModifier(player, mod);
                } else if (event.getView().title().equals(Component.text("Equip: Modifiers for Slot " + slot))) {
                     if ("UNEQUIP".equals(modId)) {
                         equipModifier(player, slot, null);
                     } else {
                         equipModifier(player, slot, modId);
                     }
                }
            } else {
                // It's a slot selection button
                // Distinguish Shop vs Equip via Title
                 if (event.getView().title().equals(Component.text("Shop: Select Item"))) {
                     openJavaShopModifiers(player, slot);
                 } else if (event.getView().title().equals(Component.text("Equip: Select Item"))) {
                     openJavaEquipModifiers(player, slot);
                 }
            }
        }
    }
}
