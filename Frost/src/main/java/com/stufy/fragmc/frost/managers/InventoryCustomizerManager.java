package com.stufy.fragmc.frost.managers;

import com.stufy.fragmc.frost.Frost;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Double-chest (54) inventory customizer.
 * - Slots 0,1,2 are LOCKED: Spear (slot1), Mace (slot2), Wind Charge (slot3) - gray glass + item display.
 * - Slots 3-8 are CUSTOMIZABLE hotbar slots (player can place any item).
 * - Slots 9-17, 45-53 are GRAY_STAINED_GLASS_PANE filler for "slots that cant be used".
 * - Slots 18-44 show player's inventory preview (optional) but we fill with filler to keep simple.
 * - Controls: 45 Save, 49 Clear, 53 Close.
 *
 * Fixed hotbar enforcement: HotbarLockListener reads PlayerData.customHotbar for slots 3-8, otherwise falls back to profile.
 */
public class InventoryCustomizerManager implements Listener {

    private final Frost plugin;
    private final NamespacedKey customizerKey;
    private final ItemStack filler;
    private final ItemStack lockedFiller;

    // Fixed locked items
    public static final int SPEAR_SLOT = 0;
    public static final int MACE_SLOT = 1;
    public static final int WIND_SLOT = 2;

    private final boolean isFloodgatePresent;

    public InventoryCustomizerManager(Frost plugin) {
        this.plugin = plugin;
        this.customizerKey = new NamespacedKey(plugin, "customizer_action");
        this.filler = createFiller();
        this.lockedFiller = createLockedFiller();
        this.isFloodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isBedrock(Player player) {
        return isFloodgatePresent && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.GRAY));
        meta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "filler");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLockedFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Locked Slot", NamedTextColor.DARK_GRAY));
        meta.lore(java.util.List.of(Component.text("This slot cannot be used", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "locked");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack fixedSpear() {
        // Try to get from profile warrior slot 0, fallback to TRIDENT
        var profile = plugin.getProfileManager().getProfile("warrior");
        if (profile != null && profile.getHotbarItems().containsKey(0)) {
            return profile.getHotbarItems().get(0).clone();
        }
        ItemStack spear = new ItemStack(Material.TRIDENT);
        ItemMeta meta = spear.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<aqua><bold>Spear</bold> <gray>(Locked Slot 1)"));
        meta.lore(java.util.List.of(
                Component.text("Your spear - always in slot 1", NamedTextColor.GRAY),
                Component.text("Cannot be moved", NamedTextColor.DARK_GRAY)
        ));
        // FMM hook if configured
        if (plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
            // Example model id: frost_spear - server admin should place model file in FMM models folder
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING, "frost_spear");
        } else {
            meta.setCustomModelData(1001);
        }
        spear.setItemMeta(meta);
        return spear;
    }

    private ItemStack fixedMace() {
        // Bedrock fallback: MACE is Java 1.21+ and may not translate well on older Geyser; use STICK fallback for Bedrock
        // But Geyser 2.2.0+ does translate MACE for Bedrock on 26.2, so we keep MACE but provide fallback logic in giveHotbar
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>Mace</bold> <gray>(Locked Slot 2)"));
        meta.lore(java.util.List.of(
                Component.text("Your mace - always in slot 2", NamedTextColor.GRAY),
                Component.text("Cannot be moved", NamedTextColor.DARK_GRAY)
        ));
        if (plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING, "frost_mace");
        } else {
            meta.setCustomModelData(1002);
        }
        mace.setItemMeta(meta);
        return mace;
    }

    private ItemStack fixedWindCharge() {
        ItemStack wind = new ItemStack(Material.WIND_CHARGE);
        ItemMeta meta = wind.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<white><bold>Wind Charge</bold> <gray>(Locked Slot 3)"));
        meta.lore(java.util.List.of(
                Component.text("Your wind charge - always in slot 3", NamedTextColor.GRAY),
                Component.text("Cannot be moved", NamedTextColor.DARK_GRAY)
        ));
        if (plugin.getFmmHook() != null && plugin.getFmmHook().isFmmPresent()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fmm_model"), PersistentDataType.STRING, "frost_wind_charge");
        } else {
            meta.setCustomModelData(1003);
        }
        wind.setItemMeta(meta);
        return wind;
    }

    // Bedrock fallback materials (Geyser translates most Java items, but provide safe fallback)
    private ItemStack bedrockFallback(Material javaMaterial, String displayName) {
        // Geyser 2.2.0+ on 26.2 handles MACE/WIND_CHARGE, but if translation fails, fallback to familiar Bedrock items
        Material fallback = javaMaterial;
        if (javaMaterial == Material.MACE) fallback = Material.IRON_AXE; // Bedrock axe is familiar
        if (javaMaterial == Material.WIND_CHARGE) fallback = Material.FIRE_CHARGE;
        if (javaMaterial == Material.TRIDENT) fallback = Material.DIAMOND_SWORD;
        ItemStack item = new ItemStack(fallback);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(displayName));
        item.setItemMeta(meta);
        return item;
    }

    public void openCustomizer(Player player) {
        // Bedrock crossplay: use Cumulus forms for Bedrock players (Geyser/Floodgate) - Java chest GUI is translated but forms are native Bedrock UI
        if (isBedrock(player)) {
            openBedrockCustomizer(player);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Customize Inventory - Frost", NamedTextColor.AQUA));

        // Fill entire inventory with filler first (gray glass for "slots that cant be used")
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler.clone());
        }

        // Fixed locked slots 0,1,2
        inv.setItem(SPEAR_SLOT, fixedSpear());
        inv.setItem(MACE_SLOT, fixedMace());
        inv.setItem(WIND_SLOT, fixedWindCharge());

        // Load player's custom hotbar for slots 3-8
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        Map<Integer, ItemStack> custom = data != null ? data.customHotbar : null;
        for (int slot = 3; slot <= 8; slot++) {
            ItemStack existing = null;
            if (custom != null && custom.containsKey(slot)) {
                existing = custom.get(slot);
            } else {
                // fallback to profile defaults if any
                var profile = plugin.getProfileManager().getProfile(data != null ? data.currentProfile : "warrior");
                if (profile != null && profile.getHotbarItems().containsKey(slot)) {
                    existing = profile.getHotbarItems().get(slot);
                }
            }
            if (existing != null && !existing.getType().isAir()) {
                inv.setItem(slot, existing.clone());
            } else {
                inv.setItem(slot, null); // empty customizable slot - player can place item here
            }
        }

        // Separator row 9-17 already filler (visual separator)
        // Middle area 18-44 remains filler - these represent "blocked" inventory slots (cannot be used per spec)
        // But we could also show player's inventory contents as preview in 18-44 if desired - keep filler for now to satisfy spec "gray glass for slots that cant be used"
        // Add lore to filler to explain
        for (int i = 18; i < 45; i++) {
            ItemStack blocked = filler.clone();
            ItemMeta meta = blocked.getItemMeta();
            meta.displayName(Component.text("Blocked", NamedTextColor.DARK_GRAY));
            meta.lore(java.util.List.of(Component.text("Inventory customization only for hotbar 4-9", NamedTextColor.GRAY)));
            blocked.setItemMeta(meta);
            inv.setItem(i, blocked);
        }

        // Controls row 45-53
        ItemStack save = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.displayName(Component.text("Save Layout", NamedTextColor.GREEN));
        saveMeta.lore(java.util.List.of(Component.text("Click to save your hotbar", NamedTextColor.GRAY)));
        saveMeta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "save");
        save.setItemMeta(saveMeta);
        inv.setItem(45, save);

        ItemStack clear = new ItemStack(Material.RED_CONCRETE);
        ItemMeta clearMeta = clear.getItemMeta();
        clearMeta.displayName(Component.text("Clear Custom Slots", NamedTextColor.RED));
        clearMeta.lore(java.util.List.of(Component.text("Removes items from 4-9", NamedTextColor.GRAY)));
        clearMeta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "clear");
        clear.setItemMeta(clearMeta);
        inv.setItem(49, clear);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Close", NamedTextColor.RED));
        closeMeta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "close");
        close.setItemMeta(closeMeta);
        inv.setItem(53, close);

        // Fill remaining control row with filler
        for (int i : new int[]{46,47,48,50,51,52}) {
            inv.setItem(i, filler.clone());
        }

        player.openInventory(inv);
    }

    private void openBedrockCustomizer(Player player) {
        // Bedrock-native UI via Floodgate Cumulus - shows current hotbar 4-9 and allows clearing
        // Full drag-and-drop not possible on Bedrock, so we provide simple management + instructions
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        StringBuilder content = new StringBuilder("§7Slots 1-3 are locked: Spear, Mace, Wind Charge\n§7Slots 4-9 are customizable.\n\n");
        for (int slot = 3; slot <= 8; slot++) {
            ItemStack item = null;
            if (data != null && data.customHotbar != null && data.customHotbar.containsKey(slot)) {
                item = data.customHotbar.get(slot);
            } else {
                var profile = plugin.getProfileManager().getProfile(data != null ? data.currentProfile : "warrior");
                if (profile != null && profile.getHotbarItems().containsKey(slot)) item = profile.getHotbarItems().get(slot);
            }
            String name = "Empty";
            if (item != null && !item.getType().isAir()) {
                var meta = item.getItemMeta();
                name = meta != null && meta.hasDisplayName() ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName()) : item.getType().name();
            }
            content.append("§fSlot ").append(slot + 1).append(": §a").append(name).append("\n");
        }
        content.append("\n§7To customize, use Java Edition or drag items on Bedrock chest GUI (also available).");

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§bCustomize Hotbar - Frost")
                .content(content.toString())
                .button("§aOpen Chest GUI (Bedrock Translated)")
                .button("§cClear Slots 4-9")
                .button("§7Close");

        form.validResultHandler(response -> {
            int id = response.clickedButtonId();
            if (id == 0) {
                // Open Java GUI anyway - Geyser will translate chest to Bedrock container UI
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Temporarily mark as Java to bypass bedrock check
                    Inventory inv = Bukkit.createInventory(null, 54, Component.text("Customize Inventory - Frost", NamedTextColor.AQUA));
                    for (int i = 0; i < 54; i++) inv.setItem(i, filler.clone());
                    inv.setItem(SPEAR_SLOT, fixedSpear());
                    inv.setItem(MACE_SLOT, fixedMace());
                    inv.setItem(WIND_SLOT, fixedWindCharge());
                    var d = plugin.getPlayerDataManager().getPlayerData(player);
                    for (int slot = 3; slot <= 8; slot++) {
                        ItemStack existing = null;
                        if (d != null && d.customHotbar != null && d.customHotbar.containsKey(slot)) existing = d.customHotbar.get(slot);
                        else {
                            var profile = plugin.getProfileManager().getProfile(d != null ? d.currentProfile : "warrior");
                            if (profile != null && profile.getHotbarItems().containsKey(slot)) existing = profile.getHotbarItems().get(slot);
                        }
                        if (existing != null && !existing.getType().isAir()) inv.setItem(slot, existing.clone());
                        else inv.setItem(slot, null);
                    }
                    for (int i = 18; i < 45; i++) {
                        ItemStack blocked = filler.clone();
                        ItemMeta meta = blocked.getItemMeta();
                        meta.displayName(Component.text("Blocked", NamedTextColor.DARK_GRAY));
                        blocked.setItemMeta(meta);
                        inv.setItem(i, blocked);
                    }
                    ItemStack save = new ItemStack(Material.LIME_CONCRETE);
                    ItemMeta saveMeta = save.getItemMeta();
                    saveMeta.displayName(Component.text("Save Layout", NamedTextColor.GREEN));
                    saveMeta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "save");
                    save.setItemMeta(saveMeta);
                    inv.setItem(45, save);
                    ItemStack clear = new ItemStack(Material.RED_CONCRETE);
                    ItemMeta clearMeta = clear.getItemMeta();
                    clearMeta.displayName(Component.text("Clear Custom Slots", NamedTextColor.RED));
                    clearMeta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "clear");
                    clear.setItemMeta(clearMeta);
                    inv.setItem(49, clear);
                    ItemStack close = new ItemStack(Material.BARRIER);
                    ItemMeta closeMeta = close.getItemMeta();
                    closeMeta.displayName(Component.text("Close", NamedTextColor.RED));
                    closeMeta.getPersistentDataContainer().set(customizerKey, PersistentDataType.STRING, "close");
                    close.setItemMeta(closeMeta);
                    inv.setItem(53, close);
                    for (int i : new int[]{46,47,48,50,51,52}) inv.setItem(i, filler.clone());
                    player.openInventory(inv);
                });
            } else if (id == 1) {
                if (data != null) {
                    data.customHotbar.clear();
                    plugin.getPlayerDataManager().savePlayerData(player);
                    if (plugin.getHotbarLockListener() != null) plugin.getHotbarLockListener().giveHotbarItems(player);
                    player.sendMessage(Component.text("Cleared custom slots 4-9.", NamedTextColor.YELLOW));
                }
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("Customize Inventory")) return;

        // Cancel all by default, then selectively allow
        event.setCancelled(true);

        Inventory clicked = event.getClickedInventory();
        Inventory top = event.getView().getTopInventory();
        if (clicked == null) return;

        int slot = event.getSlot();
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Controls
        if (clicked.equals(top)) {
            if (current != null && current.hasItemMeta()) {
                String action = current.getItemMeta().getPersistentDataContainer().get(customizerKey, PersistentDataType.STRING);
                if ("save".equals(action)) {
                    saveLayout(player, top);
                    player.sendMessage(Component.text("Hotbar layout saved! Slots 1-3 are locked, 4-9 customized.", NamedTextColor.GREEN));
                    player.closeInventory();
                    // Apply immediately
                    if (plugin.getHotbarLockListener() != null) plugin.getHotbarLockListener().giveHotbarItems(player);
                    return;
                } else if ("clear".equals(action)) {
                    for (int i = 3; i <= 8; i++) top.setItem(i, null);
                    player.sendMessage(Component.text("Cleared custom slots 4-9.", NamedTextColor.YELLOW));
                    return;
                } else if ("close".equals(action)) {
                    player.closeInventory();
                    return;
                } else if ("filler".equals(action) || "locked".equals(action)) {
                    // blocked slots - do nothing
                    return;
                }
            }

            // Locked slots 0-2 cannot be interacted
            if (slot >= 0 && slot <= 2) {
                player.sendMessage(Component.text("Slots 1-3 are locked (Spear, Mace, Wind Charge).", NamedTextColor.RED));
                return;
            }

            // Blocked area 9-44 cannot be used
            if ((slot >= 9 && slot <= 17) || (slot >= 18 && slot <= 44) || (slot >= 45 && slot <= 53 && top.getItem(slot) != null && top.getItem(slot).getType() == Material.GRAY_STAINED_GLASS_PANE)) {
                // already cancelled
                return;
            }

            // Slots 3-8 are editable - allow placing/removing
            if (slot >= 3 && slot <= 8) {
                // Handle placement: if cursor has item and current is empty, place; if current has item and cursor empty, pick up; swap etc.
                // We manually handle because event cancelled: we need to implement logic
                if (event.isShiftClick()) {
                    // shift click from top to bottom not needed
                    return;
                }
                // For simplicity, allow direct set: if cursor not air, set slot to cursor and clear cursor; else clear slot and give to cursor
                if (cursor != null && !cursor.getType().isAir()) {
                    // place cursor item into slot (clone)
                    ItemStack toPlace = cursor.clone();
                    toPlace.setAmount(1); // only one for hotbar
                    top.setItem(slot, toPlace);
                    // if player was placing, clear cursor
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                } else {
                    // pick up current item to cursor
                    if (current != null && !current.getType().isAir()) {
                        player.setItemOnCursor(current.clone());
                        top.setItem(slot, null);
                    }
                }
            }
        } else {
            // Clicked bottom inventory (player inventory) while customizer open - allow shift-click to move to customizable slots?
            // For now, if shift-click, try to place into first empty customizable slot 3-8
            if (event.isShiftClick() && current != null && !current.getType().isAir()) {
                for (int i = 3; i <= 8; i++) {
                    if (top.getItem(i) == null || top.getItem(i).getType().isAir()) {
                        top.setItem(i, current.clone());
                        clicked.setItem(event.getSlot(), new ItemStack(Material.AIR));
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("Customize Inventory")) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("Customize Inventory")) return;
        // Optionally auto-save? We require explicit Save button, so discard on close without save is intentional.
    }

    private void saveLayout(Player player, Inventory top) {
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;
        Map<Integer, ItemStack> custom = new HashMap<>();
        for (int i = 3; i <= 8; i++) {
            ItemStack item = top.getItem(i);
            if (item != null && !item.getType().isAir() && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                // Don't save filler
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(customizerKey, PersistentDataType.STRING)) continue;
                custom.put(i, item.clone());
            }
        }
        data.customHotbar = custom;
        plugin.getPlayerDataManager().savePlayerData(player);
    }
}
