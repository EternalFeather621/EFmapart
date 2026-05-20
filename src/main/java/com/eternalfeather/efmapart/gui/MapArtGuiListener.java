package com.eternalfeather.efmapart.gui;

import com.eternalfeather.efmapart.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MapArtGuiListener implements Listener {

    private final Main plugin;
    private final MapArtGui mapArtGui;

    public MapArtGuiListener(Main plugin, MapArtGui mapArtGui) {
        this.plugin = plugin;
        this.mapArtGui = mapArtGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) {
            return;
        }

        String title = event.getInventory().getTitle();

        if (!mapArtGui.isImageListTitle(title) && !mapArtGui.isSizeSelectTitle(title)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        if (mapArtGui.isImageListTitle(title)) {
            handleImageListClick(player, clickedItem);
            return;
        }

        if (mapArtGui.isSizeSelectTitle(title)) {
            handleSizeSelectClick(player, clickedItem);
        }
    }

    private void handleImageListClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta == null || meta.getDisplayName() == null) {
            return;
        }

        String displayName = ChatColor.stripColor(meta.getDisplayName());

        if (displayName.equals("上一页")) {
            mapArtGui.openPreviousPage(player);
            return;
        }

        if (displayName.equals("下一页")) {
            mapArtGui.openNextPage(player);
            return;
        }

        if (displayName.equals("刷新图片列表")) {
            mapArtGui.refreshImageList(player);
            return;
        }

        if (displayName.startsWith("第 ")) {
            return;
        }

        if (displayName.equals("使用说明")
                || displayName.equals("没有找到图片")
                || displayName.equals("没有找到可用图片")
                || displayName.equals("没有上一页")
                || displayName.equals("没有下一页")) {
            return;
        }

        String fileName = displayName;

        mapArtGui.openSizeSelect(player, fileName);
    }

    private void handleSizeSelectClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta == null || meta.getDisplayName() == null) {
            return;
        }

        String name = ChatColor.stripColor(meta.getDisplayName());

        if (name.equals("返回图片列表")) {
            mapArtGui.openImageList(player);
            return;
        }

        if (name.equals("自定义尺寸说明")) {
            String fileName = mapArtGui.getSelectedImage(player);

            if (fileName == null) {
                player.sendMessage(ChatColor.RED + "没有选择图片，请重新打开 GUI。");
                player.closeInventory();
                return;
            }

            player.closeInventory();

            player.sendMessage(ChatColor.GOLD + "========== EFMapArt 自定义尺寸 ==========");
            player.sendMessage(ChatColor.GRAY + "如果你不想使用推荐/常用尺寸，可以输入指令自行创建。");
            player.sendMessage(ChatColor.YELLOW + "普通玩家领取地图：");
            player.sendMessage(ChatColor.WHITE + "/efmapart get " + fileName + " <宽> <高>");
            player.sendMessage(ChatColor.GRAY + "例如：");
            player.sendMessage(ChatColor.WHITE + "/efmapart get " + fileName + " 3 4");

            if (canBypassLimit(player)) {
                player.sendMessage("");
                player.sendMessage(ChatColor.YELLOW + "管理员自动生成地图墙：");
                player.sendMessage(ChatColor.WHITE + "/efmapart wall " + fileName + " <宽> <高>");
                player.sendMessage(ChatColor.GRAY + "例如：");
                player.sendMessage(ChatColor.WHITE + "/efmapart wall " + fileName + " 6 4");
            }

            player.sendMessage(ChatColor.GOLD + "=======================================");
            return;
        }

        int[] size = mapArtGui.parseSizeFromItem(clickedItem);

        if (size == null) {
            return;
        }

        String fileName = mapArtGui.getSelectedImage(player);

        if (fileName == null) {
            player.sendMessage(ChatColor.RED + "没有选择图片，请重新打开 GUI。");
            player.closeInventory();
            return;
        }

        int columns = size[0];
        int rows = size[1];

        if (!canBypassLimit(player)) {
            int maxColumns = plugin.getConfig().getInt("settings.normal-max-columns", 4);
            int maxRows = plugin.getConfig().getInt("settings.normal-max-rows", 4);

            if (columns > maxColumns || rows > maxRows) {
                player.sendMessage(ChatColor.RED + "普通玩家最多只能领取 "
                        + maxColumns + "×" + maxRows + " 的地图画。");
                player.closeInventory();
                return;
            }
        }

        player.closeInventory();

        if (canBypassLimit(player)) {
            player.sendMessage(ChatColor.GREEN + "管理员模式：即将进入自动生成地图墙模式。");
            plugin.getWallPlacementManager().startPlacement(player, fileName, columns, rows);
        } else {
            player.sendMessage(ChatColor.GREEN + "普通玩家模式：只领取地图物品，不自动生成墙体。");
            plugin.getMapArtGenerator().giveMapArt(player, fileName, columns, rows, true);
        }

        mapArtGui.clearSelectedImage(player);
    }

    private boolean canBypassLimit(Player player) {
        return player.isOp()
                || player.hasPermission("efmapart.admin")
                || player.hasPermission("efmapart.bypass");
    }
}