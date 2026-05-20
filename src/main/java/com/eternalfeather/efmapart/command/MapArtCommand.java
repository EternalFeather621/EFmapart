package com.eternalfeather.efmapart.command;

import com.eternalfeather.efmapart.Main;
import com.eternalfeather.efmapart.renderer.ImageMapRenderer;
import com.eternalfeather.efmapart.util.ImageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapArtCommand implements CommandExecutor {

    private final Main plugin;

    public MapArtCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getMapArtGui().openImageList((Player) sender);
            } else {
                sendHelp(sender);
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台不能打开 GUI。");
                return true;
            }

            plugin.getMapArtGui().openImageList((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            handleList(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("recommend")) {
            handleRecommend(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
            handleGet(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            handleGive(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            handleCreate(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("wall")) {
            handleWall(sender, args);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "未知指令，请输入 /efmapart help 查看帮助。");
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "控制台不能领取地图画。");
            return;
        }

        if (!sender.hasPermission("efmapart.create") && !sender.hasPermission("efmapart.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用这个命令。");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/efmapart give <图片文件名>");
            sender.sendMessage(ChatColor.GRAY + "例如：/efmapart give test.png");
            return;
        }

        Player player = (Player) sender;

        plugin.getMapArtGenerator().giveMapArt(player, args[1], 1, 1, false);
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "控制台不能生成地图画。");
            return;
        }

        if (!sender.hasPermission("efmapart.create") && !sender.hasPermission("efmapart.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用这个命令。普通玩家请使用 /efmapart wall。");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "用法：/efmapart create <图片文件名> <宽度地图数> <高度地图数>");
            sender.sendMessage(ChatColor.GRAY + "例如：/efmapart create test.png 4 3");
            return;
        }

        Player player = (Player) sender;
        String fileName = args[1];

        int columns;
        int rows;

        try {
            columns = Integer.parseInt(args[2]);
            rows = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须是数字。");
            return;
        }

        if (columns <= 0 || rows <= 0) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须大于 0。");
            return;
        }

        if (columns > 20 || rows > 20) {
            player.sendMessage(ChatColor.RED + "为了防止生成过多地图，单次最大支持 20×20。");
            return;
        }

        plugin.getMapArtGenerator().giveMapArt(player, fileName, columns, rows, false);
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "控制台不能领取地图画。");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("efmapart.get") && !player.hasPermission("efmapart.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限领取地图画。");
            return;
        }

        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "用法：/efmapart get <图片文件名> <宽度地图数> <高度地图数>");
            player.sendMessage(ChatColor.GRAY + "例如：/efmapart get test.jpg 4 3");
            return;
        }

        String fileName = args[1];

        int columns;
        int rows;

        try {
            columns = Integer.parseInt(args[2]);
            rows = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须是数字。");
            return;
        }

        if (columns <= 0 || rows <= 0) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须大于 0。");
            return;
        }

        if (!canBypassLimit(player)) {
            int maxColumns = plugin.getConfig().getInt("settings.normal-max-columns", 4);
            int maxRows = plugin.getConfig().getInt("settings.normal-max-rows", 4);

            if (columns > maxColumns || rows > maxRows) {
                player.sendMessage(ChatColor.RED + "普通玩家最多只能领取 "
                        + maxColumns + "×" + maxRows + " 的地图画。");
                player.sendMessage(ChatColor.GRAY + "你当前输入的是：" + columns + "×" + rows);
                return;
            }
        }

        plugin.getMapArtGenerator().giveMapArt(player, fileName, columns, rows, true);
    }

    private void handleWall(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "控制台不能使用地图墙放置功能。");
            return;
        }

        Player player = (Player) sender;

        if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) {
            plugin.getWallPlacementManager().cancelPlacement(player);
            return;
        }

        if (!player.hasPermission("efmapart.wall") && !player.hasPermission("efmapart.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限自动生成地图墙。");
            player.sendMessage(ChatColor.GRAY + "普通玩家请使用 /efmapart get <图片> <宽> <高> 领取地图后自行摆放。");
            return;
        }

        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "用法：/efmapart wall <图片文件名> <宽度地图数> <高度地图数>");
            player.sendMessage(ChatColor.GRAY + "例如：/efmapart wall test.jpg 4 3");
            player.sendMessage(ChatColor.GRAY + "取消放置：/efmapart wall cancel");
            return;
        }

        String fileName = args[1];

        int columns;
        int rows;

        try {
            columns = Integer.parseInt(args[2]);
            rows = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须是数字。");
            return;
        }

        if (columns <= 0 || rows <= 0) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须大于 0。");
            return;
        }

        if (!canBypassLimit(player)) {
            int maxColumns = plugin.getConfig().getInt("settings.normal-max-columns", 4);
            int maxRows = plugin.getConfig().getInt("settings.normal-max-rows", 4);

            if (columns > maxColumns || rows > maxRows) {
                player.sendMessage(ChatColor.RED + "普通玩家最多只能生成 "
                        + maxColumns + "×" + maxRows + " 的地图墙。");
                player.sendMessage(ChatColor.GRAY + "你当前输入的是：" + columns + "×" + rows);
                player.sendMessage(ChatColor.GRAY + "你可以输入 /efmapart recommend " + fileName + " 查看推荐尺寸。");
                return;
            }
        }

        plugin.getWallPlacementManager().startPlacement(player, fileName, columns, rows);
    }

    private void handleRecommend(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/efmapart recommend <图片文件名>");
            sender.sendMessage(ChatColor.GRAY + "例如：/efmapart recommend test.jpg");
            return;
        }

        String fileName = args[1];
        File imageFile = new File(plugin.getImagesFolder(), fileName);

        if (!imageFile.exists()) {
            sender.sendMessage(ChatColor.RED + "找不到图片文件：" + fileName);
            sender.sendMessage(ChatColor.GRAY + "请把图片放到 plugins/EFMapArt/images/ 文件夹里。");
            return;
        }

        try {
            BufferedImage image = ImageIO.read(imageFile);

            if (image == null) {
                sender.sendMessage(ChatColor.RED + "无法读取这张图片，请确认它是 png、jpg 或 jpeg 图片。");
                return;
            }

            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            double imageRatio = (double) imageWidth / (double) imageHeight;

            int maxColumns;
            int maxRows;

            if (sender instanceof Player && !canBypassLimit((Player) sender)) {
                maxColumns = plugin.getConfig().getInt("settings.normal-max-columns", 4);
                maxRows = plugin.getConfig().getInt("settings.normal-max-rows", 4);
            } else {
                maxColumns = plugin.getConfig().getInt("settings.recommend-max-columns", 8);
                maxRows = plugin.getConfig().getInt("settings.recommend-max-rows", 6);
            }

            List<SizeRecommendation> recommendations = new ArrayList<SizeRecommendation>();

            for (int columns = 1; columns <= maxColumns; columns++) {
                for (int rows = 1; rows <= maxRows; rows++) {
                    double gridRatio = (double) columns / (double) rows;
                    double diff = Math.abs(gridRatio - imageRatio);
                    int totalMaps = columns * rows;

                    recommendations.add(new SizeRecommendation(columns, rows, diff, totalMaps));
                }
            }

            Collections.sort(recommendations, new Comparator<SizeRecommendation>() {
                @Override
                public int compare(SizeRecommendation a, SizeRecommendation b) {
                    int diffCompare = Double.compare(a.getDiff(), b.getDiff());

                    if (diffCompare != 0) {
                        return diffCompare;
                    }

                    return Integer.compare(b.getTotalMaps(), a.getTotalMaps());
                }
            });

            sender.sendMessage(ChatColor.GOLD + "========== EFMapArt 尺寸推荐 ==========");
            sender.sendMessage(ChatColor.YELLOW + "图片：" + ChatColor.WHITE + fileName);
            sender.sendMessage(ChatColor.YELLOW + "原图尺寸：" + ChatColor.WHITE + imageWidth + "×" + imageHeight);
            sender.sendMessage(ChatColor.YELLOW + "图片比例：" + ChatColor.WHITE + String.format("%.2f", imageRatio));
            sender.sendMessage(ChatColor.YELLOW + "可用范围：" + ChatColor.WHITE + "最大 " + maxColumns + "×" + maxRows);
            sender.sendMessage(ChatColor.GOLD + "推荐尺寸：");

            int shown = 0;

            for (SizeRecommendation recommendation : recommendations) {
                if (shown >= 5) {
                    break;
                }

                int columns = recommendation.getColumns();
                int rows = recommendation.getRows();
                int total = recommendation.getTotalMaps();

                sender.sendMessage(ChatColor.YELLOW + "- " + columns + "×" + rows
                        + ChatColor.GRAY + "，共 " + total + " 张地图，命令："
                        + ChatColor.WHITE + "/efmapart wall " + fileName + " " + columns + " " + rows);

                shown++;
            }

            sender.sendMessage(ChatColor.GOLD + "======================================");

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "读取图片时发生错误，请查看控制台。");
            e.printStackTrace();
        }
    }

    private void handleList(CommandSender sender) {
        File[] files = plugin.getImagesFolder().listFiles();

        if (files == null || files.length == 0) {
            sender.sendMessage(ChatColor.RED + "images 文件夹里还没有图片。");
            sender.sendMessage(ChatColor.GRAY + "请把 png、jpg 或 jpeg 图片放到 plugins/EFMapArt/images/ 文件夹里。");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "========== EFMapArt 图片列表 ==========");

        boolean hasImage = false;

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String name = file.getName().toLowerCase();

            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                sender.sendMessage(ChatColor.YELLOW + "- " + file.getName());
                hasImage = true;
            }
        }

        if (!hasImage) {
            sender.sendMessage(ChatColor.RED + "没有找到 png、jpg 或 jpeg 图片。");
        }

        sender.sendMessage(ChatColor.GOLD + "======================================");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== EFMapArt 指令帮助 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart" + ChatColor.GRAY + " - 打开地图画 GUI");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart gui" + ChatColor.GRAY + " - 打开地图画 GUI");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart help" + ChatColor.GRAY + " - 查看帮助");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart list" + ChatColor.GRAY + " - 查看 images 文件夹里的图片");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart recommend <图片文件名>" + ChatColor.GRAY + " - 自动推荐地图墙尺寸");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart get <图片文件名> <宽> <高>" + ChatColor.GRAY + " - 领取地图画物品");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart wall <图片文件名> <宽> <高>" + ChatColor.GRAY + " - 管理员自动生成地图墙");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart wall cancel" + ChatColor.GRAY + " - 取消地图墙放置");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart give <图片文件名>" + ChatColor.GRAY + " - 管理员生成 1×1 单张地图画");
        sender.sendMessage(ChatColor.YELLOW + "/efmapart create <图片文件名> <宽> <高>" + ChatColor.GRAY + " - 管理员生成地图到背包");
        sender.sendMessage(ChatColor.GOLD + "=======================================");
    }

    private boolean canBypassLimit(Player player) {
        return player.isOp()
                || player.hasPermission("efmapart.admin")
                || player.hasPermission("efmapart.bypass");
    }

    private static class SizeRecommendation {

        private final int columns;
        private final int rows;
        private final double diff;
        private final int totalMaps;

        public SizeRecommendation(int columns, int rows, double diff, int totalMaps) {
            this.columns = columns;
            this.rows = rows;
            this.diff = diff;
            this.totalMaps = totalMaps;
        }

        public int getColumns() {
            return columns;
        }

        public int getRows() {
            return rows;
        }

        public double getDiff() {
            return diff;
        }

        public int getTotalMaps() {
            return totalMaps;
        }
    }
}