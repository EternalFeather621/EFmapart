package com.eternalfeather.efmapart.service;

import com.eternalfeather.efmapart.Main;
import com.eternalfeather.efmapart.renderer.ImageMapRenderer;
import com.eternalfeather.efmapart.util.ImageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapArtGenerator {

    private final Main plugin;

    public MapArtGenerator(Main plugin) {
        this.plugin = plugin;
    }

    public void giveMapArt(Player player, String fileName, int columns, int rows, boolean chargeCost) {
        File imageFile = new File(plugin.getImagesFolder(), fileName);

        if (!imageFile.exists()) {
            player.sendMessage(ChatColor.RED + "找不到图片文件：" + fileName);
            player.sendMessage(ChatColor.GRAY + "请确认图片在 plugins/EFMapArt/images/ 文件夹里。");
            return;
        }

        if (columns <= 0 || rows <= 0) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须大于 0。");
            return;
        }

        try {
            BufferedImage originalImage = ImageIO.read(imageFile);

            if (originalImage == null) {
                player.sendMessage(ChatColor.RED + "无法读取这张图片，请确认它是 png、jpg 或 jpeg 图片。");
                return;
            }

            int requiredSlots = columns * rows;

            if (!hasEnoughEmptySlots(player, requiredSlots)) {
                player.sendMessage(ChatColor.RED + "你的背包空间不足，无法领取这套地图画。");
                player.sendMessage(ChatColor.GRAY + "需要空格：" + requiredSlots + " 个");
                player.sendMessage(ChatColor.GRAY + "当前空格：" + getEmptySlotCount(player) + " 个");
                player.sendMessage(ChatColor.YELLOW + "请清理背包后再试。");
                return;
            }

            if (chargeCost) {
                if (!tryChargeGiveCost(player, columns, rows)) {
                    return;
                }
            }

            short[][] mapIds = getOrCreateMapIds(player, fileName, columns, rows, originalImage);

            int total = columns * rows;
            int created = 0;

            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    short mapId = mapIds[row][column];

                    ItemStack mapItem = createMapItem(mapId, fileName, columns, rows, column, row);

                    java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(mapItem);

                    if (!leftover.isEmpty()) {
                        for (ItemStack leftoverItem : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                        }
                    }

                    created++;
                }
            }

            player.sendMessage(ChatColor.GREEN + "已领取地图画：" + ChatColor.YELLOW + fileName);
            player.sendMessage(ChatColor.GREEN + "尺寸：" + ChatColor.YELLOW + columns + "×" + rows
                    + ChatColor.GREEN + "，共 " + ChatColor.YELLOW + total + ChatColor.GREEN + " 张地图。");
            player.sendMessage(ChatColor.GRAY + "请自己准备墙体和物品展示框。");
            player.sendMessage(ChatColor.GRAY + "摆放顺序：从左到右，从上到下。");
            player.sendMessage(ChatColor.GRAY + "每张地图的 lore 中写有第几行、第几列。");

            if (created != total) {
                player.sendMessage(ChatColor.RED + "注意：实际生成数量异常，请查看控制台。");
            }

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "生成地图画时发生错误，请查看控制台。");
            e.printStackTrace();
        }
    }

    public short[][] getOrCreateMapIds(Player player, String fileName, int columns, int rows, BufferedImage originalImage) throws Exception {
        short[][] cachedMapIds = plugin.getMapDataManager().getCachedMapIds(fileName, columns, rows);

        if (cachedMapIds != null) {
            player.sendMessage(ChatColor.GRAY + "检测到相同图片和尺寸，已复用已有地图缓存。");
            return cachedMapIds;
        }

        player.sendMessage(ChatColor.GRAY + "未找到缓存，正在创建新的地图画缓存。");

        BufferedImage fullImage = ImageUtil.resizeForMapGrid(originalImage, columns, rows);

        World world = player.getWorld();

        short[][] mapIds = new short[rows][columns];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                BufferedImage tileImage = ImageUtil.cropTile(fullImage, column, row);

                MapView mapView = Bukkit.createMap(world);

                for (MapRenderer renderer : mapView.getRenderers()) {
                    mapView.removeRenderer(renderer);
                }

                mapView.addRenderer(new ImageMapRenderer(tileImage));

                short mapId = mapView.getId();

                plugin.getMapDataManager().saveMapData(
                        mapId,
                        fileName,
                        columns,
                        rows,
                        column,
                        row
                );

                mapIds[row][column] = mapId;
            }
        }

        return mapIds;
    }

    public ItemStack createMapItem(short mapId, String fileName, int columns, int rows, int column, int row) {
        ItemStack mapItem = new ItemStack(Material.MAP, 1, mapId);

        ItemMeta meta = mapItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "地图画：" + ChatColor.YELLOW + fileName);

            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "尺寸：" + columns + "×" + rows);
            lore.add(ChatColor.GRAY + "位置：第 " + (row + 1) + " 行，第 " + (column + 1) + " 列");
            lore.add(ChatColor.GRAY + "摆放顺序：从左到右，从上到下");
            lore.add(ChatColor.DARK_GRAY + "Map ID: " + mapId);

            meta.setLore(lore);
            mapItem.setItemMeta(meta);
        }

        return mapItem;
    }

    private boolean hasEnoughEmptySlots(Player player, int requiredSlots) {
        return getEmptySlotCount(player) >= requiredSlots;
    }

    private int getEmptySlotCount(Player player) {
        int emptySlots = 0;

        ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }

        return emptySlots;
    }

    private boolean tryChargeGiveCost(Player player, int columns, int rows) {
        if (canBypassCost(player)) {
            return true;
        }

        if (!plugin.getConfig().getBoolean("cost.give-enabled", true)) {
            return true;
        }

        int pricePerMap = plugin.getConfig().getInt("cost.give-cost-per-map", 100);
        int mapAmount = columns * rows;
        int totalCost = pricePerMap * mapAmount;

        if (totalCost <= 0) {
            return true;
        }

        if (plugin.getEconomyManager() == null || !plugin.getEconomyManager().isAvailable()) {
            player.sendMessage(ChatColor.RED + "经济系统不可用，无法领取付费地图画。");
            player.sendMessage(ChatColor.GRAY + "请联系管理员检查 Vault 和经济插件。");
            return false;
        }

        double balance = plugin.getEconomyManager().getBalance(player);

        if (!plugin.getEconomyManager().has(player, totalCost)) {
            player.sendMessage(ChatColor.RED + "你的余额不足，无法领取这套地图画。");
            player.sendMessage(ChatColor.GRAY + "地图数量：" + mapAmount + " 张");
            player.sendMessage(ChatColor.GRAY + "每张价格：" + pricePerMap + " 游戏币");
            player.sendMessage(ChatColor.YELLOW + "需要花费：" + totalCost + " 游戏币");
            player.sendMessage(ChatColor.YELLOW + "当前余额：" + String.format("%.2f", balance) + " 游戏币");
            return false;
        }

        boolean success = plugin.getEconomyManager().withdraw(player, totalCost);

        if (!success) {
            player.sendMessage(ChatColor.RED + "扣费失败，无法领取地图画。");
            player.sendMessage(ChatColor.GRAY + "请稍后再试，或联系管理员。");
            return false;
        }

        player.sendMessage(ChatColor.YELLOW + "本次领取地图画数量："
                + ChatColor.GOLD + mapAmount
                + ChatColor.YELLOW + " 张。");

        player.sendMessage(ChatColor.YELLOW + "每张地图价格："
                + ChatColor.GOLD + pricePerMap
                + ChatColor.YELLOW + " 游戏币。");

        player.sendMessage(ChatColor.YELLOW + "总花费："
                + ChatColor.GOLD + totalCost
                + ChatColor.YELLOW + " 游戏币。");

        return true;
    }

    private boolean canBypassCost(Player player) {
        return player.isOp()
                || player.hasPermission("efmapart.admin")
                || player.hasPermission("efmapart.bypass");
    }
}