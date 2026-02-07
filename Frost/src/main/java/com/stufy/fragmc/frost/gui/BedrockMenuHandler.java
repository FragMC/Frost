package com.stufy.fragmc.frost.gui;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.handlers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BedrockMenuHandler {

    private final Frost plugin;

    public BedrockMenuHandler(Frost plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("Frost Shop")
                .content("Select a shop category:");

        builder.button("Wind Charge Skins", FormImage.Type.PATH, "textures/items/wind_charge.png");
        builder.button("Spear Skins", FormImage.Type.PATH, "textures/items/netherite_sword.png");
        builder.button("Mace Skins", FormImage.Type.PATH, "textures/items/mace.png");
        builder.button("Powerup Shop", FormImage.Type.PATH, "textures/items/emerald.png");

        builder.validResultHandler(response -> {
            switch (response.clickedButtonId()) {
                case 0 -> openCategory(player, "wind_charge");
                case 1 -> openCategory(player, "spear");
                case 2 -> openCategory(player, "mace");
                case 3 -> openPowerupShop(player);
            }
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    public void openCategory(Player player, String category) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(formatCategoryName(category))
                .content("Select a skin to purchase or equip:");

        Map<String, ShopManager.SkinData> skins = plugin.getShopManager().getSkinsForType(category);
        List<ShopManager.SkinData> skinList = new ArrayList<>(skins.values());

        for (ShopManager.SkinData skin : skinList) {
            String buttonText = skin.name + "\n";
            if (plugin.getShopManager().hasPlayerPurchasedSkin(player, category, skin.id)) {
                buttonText += ChatColor.GREEN + "Owned";
            } else {
                buttonText += ChatColor.GOLD + plugin.formatCurrency(skin.price);
            }
            builder.button(buttonText, FormImage.Type.PATH, getTexturePath(skin.icon));
        }

        builder.button("Back");

        builder.validResultHandler(response -> {
            if (response.clickedButtonId() >= skinList.size()) {
                openMainMenu(player);
                return;
            }

            ShopManager.SkinData selectedSkin = skinList.get(response.clickedButtonId());
            plugin.getShopManager().buySkin(player, category, selectedSkin.id);
            // Re-open to update status
            openCategory(player, category);
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    public void openPowerupShop(Player player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("Powerup Shop")
                .content("Select a powerup to purchase:");

        Map<String, ShopManager.PowerupData> powerups = plugin.getShopManager().getPowerups();
        List<ShopManager.PowerupData> powerupList = new ArrayList<>(powerups.values());

        for (ShopManager.PowerupData powerup : powerupList) {
            builder.button(powerup.name + "\n" + ChatColor.GOLD + plugin.formatCurrency(powerup.price),
                    FormImage.Type.PATH, getTexturePath(powerup.icon));
        }

        builder.button("Back");

        builder.validResultHandler(response -> {
            if (response.clickedButtonId() >= powerupList.size()) {
                openMainMenu(player);
                return;
            }

            ShopManager.PowerupData selectedPowerup = powerupList.get(response.clickedButtonId());
            plugin.getShopManager().buyPowerup(player, selectedPowerup.id);
        });

        FloodgateApi.getInstance().getPlayer(player.getUniqueId()).sendForm(builder.build());
    }

    private String getTexturePath(Material material) {
        if (material == null)
            return "textures/items/paper.png";

        // Handle special cases
        return switch (material) {
            case WIND_CHARGE -> "textures/items/wind_charge.png";
            case MACE -> "textures/items/mace.png";
            // Bedrock uses specific names for some items
            default -> "textures/items/" + material.name().toLowerCase() + ".png";
        };
    }

    private String formatCategoryName(String category) {
        return switch (category) {
            case "wind_charge" -> "Wind Charge Skins";
            case "spear" -> "Spear Skins";
            case "mace" -> "Mace Skins";
            default -> "Skins";
        };
    }
}
