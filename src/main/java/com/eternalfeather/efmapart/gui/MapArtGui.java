package com.eternalfeather.efmapart.gui;

import com.eternalfeather.efmapart.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class MapArtGui {

    private final Main plugin;

    private final String imageListTitle = ChatColor.GOLD + "EFMapArt 图片列表";
    private final String sizeSelectTitle = ChatColor.GOLD + "EFMapArt 选择尺寸";

    // 每页最多显示 45 张图片，底部 9 格留给按钮
    private static final int IMAGES_PER_PAGE = 45;

    private final Map<UUID, String> selectedImages = new HashMap<UUID, String>();
    private final Map<UUID, Integer> currentPages = new HashMap<UUID, Integer>();

    public MapArtGui(Main plugin) {
        this.plugin = plugin;
    }

    public void openImageList(Player player) {
        openImageList(player, getCurrentPage(player));
    }

    public void openImageList(Player player, int page) {
        List<File> imageFiles = getImageFiles();

        int totalPages = getTotalPages(imageFiles.size());

        if (page < 0) {
            page = 0;
        }

        if (page >= totalPages) {
            page = totalPages - 1;
        }

        if (page < 0) {
            page = 0;
        }

        currentPages.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(null, 54, imageListTitle);

        if (imageFiles.isEmpty()) {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    ChatColor.RED + "没有找到图片",
                    Arrays.asList(
                            ChatColor.GRAY + "请把 png / jpg / jpeg 图片放到：",
                            ChatColor.YELLOW + "plugins/EFMapArt/images/",
                            "",
                            ChatColor.YELLOW + "点击底部刷新按钮可以重新扫描图片"
                    )
            ));

            fillBottomButtons(inventory, page, totalPages, imageFiles.size());

            player.openInventory(inventory);
            return;
        }

        int startIndex = page * IMAGES_PER_PAGE;
        int endIndex = Math.min(startIndex + IMAGES_PER_PAGE, imageFiles.size());

        int slot = 0;

        for (int i = startIndex; i < endIndex; i++) {
            File file = imageFiles.get(i);

            String fileName = file.getName();

            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "文件名：");
            lore.add(ChatColor.WHITE + fileName);
            lore.add("");
            lore.add(ChatColor.YELLOW + "左键点击选择这张图片");
            lore.add(ChatColor.GRAY + "然后选择地图墙尺寸");

            try {
                BufferedImage image = ImageIO.read(file);

                if (image != null) {
                    lore.add("");
                    lore.add(ChatColor.GRAY + "原图尺寸：" + image.getWidth() + "×" + image.getHeight());
                    lore.add(ChatColor.GRAY + "图片比例：" + String.format("%.2f", (double) image.getWidth() / image.getHeight()));
                }
            } catch (Exception ignored) {
            }

            ItemStack item = createItem(
                    Material.MAP,
                    ChatColor.YELLOW + fileName,
                    lore
            );

            inventory.setItem(slot, item);

            slot++;
        }

        fillBottomButtons(inventory, page, totalPages, imageFiles.size());

        player.openInventory(inventory);
    }

    private void fillBottomButtons(Inventory inventory, int page, int totalPages, int imageCount) {
        // 底部装饰
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, ChatColor.GRAY + " ", null);

        for (int i = 45; i <= 53; i++) {
            inventory.setItem(i, glass);
        }

        // 上一页按钮
        if (page > 0) {
            inventory.setItem(45, createItem(
                    Material.ARROW,
                    ChatColor.YELLOW + "上一页",
                    Arrays.asList(
                            ChatColor.GRAY + "点击查看上一页图片",
                            ChatColor.GRAY + "当前页：" + (page + 1) + " / " + totalPages
                    )
            ));
        } else {
            inventory.setItem(45, createItem(
                    Material.BARRIER,
                    ChatColor.RED + "没有上一页",
                    Arrays.asList(ChatColor.GRAY + "当前已经是第一页")
            ));
        }

        // 刷新按钮
        inventory.setItem(48, createItem(
                Material.REDSTONE_TORCH_ON,
                ChatColor.AQUA + "刷新图片列表",
                Arrays.asList(
                        ChatColor.GRAY + "重新扫描 images 文件夹",
                        ChatColor.GRAY + "如果刚添加了图片，可以点击这里"
                )
        ));

        // 当前页信息
        inventory.setItem(49, createItem(
                Material.BOOK,
                ChatColor.GOLD + "第 " + (page + 1) + " / " + totalPages + " 页",
                Arrays.asList(
                        ChatColor.GRAY + "图片总数：" + imageCount,
                        ChatColor.GRAY + "每页最多显示：" + IMAGES_PER_PAGE + " 张",
                        "",
                        ChatColor.YELLOW + "玩家默认最大："
                                + plugin.getConfig().getInt("settings.normal-max-columns", 4)
                                + "×"
                                + plugin.getConfig().getInt("settings.normal-max-rows", 4),
                        ChatColor.YELLOW + "玩家价格：每张地图 "
                                + plugin.getConfig().getInt("cost.give-cost-per-map", 100)
                                + " 游戏币",
                        ChatColor.GRAY + "总价 = 宽 × 高 × 每张地图价格",
                        ChatColor.GRAY + "OP / 管理员不受限制"
                )
        ));

        // 使用说明
        inventory.setItem(50, createItem(
                Material.PAPER,
                ChatColor.AQUA + "使用说明",
                Arrays.asList(
                        ChatColor.GRAY + "1. 点击一张图片",
                        ChatColor.GRAY + "2. 选择地图墙尺寸",
                        ChatColor.GRAY + "3. 右键点击方块侧面",
                        ChatColor.GRAY + "4. 自动生成地图墙",
                        "",
                        ChatColor.YELLOW + "图片文件夹：",
                        ChatColor.WHITE + "plugins/EFMapArt/images/"
                )
        ));

        // 下一页按钮
        if (page < totalPages - 1) {
            inventory.setItem(53, createItem(
                    Material.ARROW,
                    ChatColor.YELLOW + "下一页",
                    Arrays.asList(
                            ChatColor.GRAY + "点击查看下一页图片",
                            ChatColor.GRAY + "当前页：" + (page + 1) + " / " + totalPages
                    )
            ));
        } else {
            inventory.setItem(53, createItem(
                    Material.BARRIER,
                    ChatColor.RED + "没有下一页",
                    Arrays.asList(ChatColor.GRAY + "当前已经是最后一页")
            ));
        }
    }

    public void openNextPage(Player player) {
        int page = getCurrentPage(player);
        openImageList(player, page + 1);
    }

    public void openPreviousPage(Player player) {
        int page = getCurrentPage(player);
        openImageList(player, page - 1);
    }

    public void refreshImageList(Player player) {
        openImageList(player, getCurrentPage(player));
        player.sendMessage(ChatColor.GREEN + "图片列表已刷新。");
    }

    public int getCurrentPage(Player player) {
        Integer page = currentPages.get(player.getUniqueId());

        if (page == null) {
            return 0;
        }

        return page;
    }

    private int getTotalPages(int imageCount) {
        if (imageCount <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) imageCount / (double) IMAGES_PER_PAGE);
    }

    private List<File> getImageFiles() {
        List<File> result = new ArrayList<File>();

        File[] files = plugin.getImagesFolder().listFiles();

        if (files == null || files.length == 0) {
            return result;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String lowerName = file.getName().toLowerCase();

            if (lowerName.endsWith(".png")
                    || lowerName.endsWith(".jpg")
                    || lowerName.endsWith(".jpeg")) {
                result.add(file);
            }
        }

        Collections.sort(result, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        return result;
    }

    public void openSizeSelect(Player player, String fileName) {
        selectedImages.put(player.getUniqueId(), fileName);

        Inventory inventory = Bukkit.createInventory(null, 27, sizeSelectTitle);

        File imageFile = new File(plugin.getImagesFolder(), fileName);

        inventory.setItem(4, createItem(
                Material.MAP,
                ChatColor.YELLOW + fileName,
                Arrays.asList(
                        ChatColor.GRAY + "请选择要生成的地图画尺寸"
                )
        ));

        List<SizeRecommendation> recommendations = getRecommendations(player, imageFile);

        int[] recommendSlots = new int[] {10, 11, 12, 13, 14};

        for (int i = 0; i < recommendations.size() && i < recommendSlots.length; i++) {
            SizeRecommendation recommendation = recommendations.get(i);

            inventory.setItem(recommendSlots[i], createSizeItem(
                    recommendation.getColumns(),
                    recommendation.getRows(),
                    true
            ));
        }

        inventory.setItem(19, createSizeItem(1, 1, false));
        inventory.setItem(20, createSizeItem(2, 2, false));
        inventory.setItem(21, createSizeItem(3, 2, false));
        inventory.setItem(22, createSizeItem(4, 3, false));
        inventory.setItem(23, createSizeItem(4, 4, false));

        inventory.setItem(25, createItem(
                Material.BOOK,
                ChatColor.AQUA + "自定义尺寸说明",
                Arrays.asList(
                        ChatColor.GRAY + "如果你不想使用推荐/常用尺寸，",
                        ChatColor.GRAY + "可以输入指令自行创建某尺寸的地图画。",
                        "",
                        ChatColor.YELLOW + "普通玩家领取地图：",
                        ChatColor.WHITE + "/efmapart get " + fileName + " <宽> <高>",
                        ChatColor.GRAY + "例如：",
                        ChatColor.WHITE + "/efmapart get " + fileName + " 3 4",
                        "",
                        ChatColor.YELLOW + "管理员自动生成地图墙：",
                        ChatColor.WHITE + "/efmapart wall " + fileName + " <宽> <高>"
                )
        ));

        inventory.setItem(26, createItem(
                Material.ARROW,
                ChatColor.RED + "返回图片列表",
                Arrays.asList(ChatColor.GRAY + "点击返回上一页")
        ));

        player.openInventory(inventory);
    }

    public String getSelectedImage(Player player) {
        return selectedImages.get(player.getUniqueId());
    }

    public void clearSelectedImage(Player player) {
        selectedImages.remove(player.getUniqueId());
    }

    public boolean isImageListTitle(String title) {
        return imageListTitle.equals(title);
    }

    public boolean isSizeSelectTitle(String title) {
        return sizeSelectTitle.equals(title);
    }

    public String getImageListTitle() {
        return imageListTitle;
    }

    public String getSizeSelectTitle() {
        return sizeSelectTitle;
    }

    public ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            if (lore != null) {
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createSizeItem(int columns, int rows, boolean recommended) {
        Material material = recommended ? Material.EMERALD : Material.PAPER;

        List<String> lore = new ArrayList<String>();

        if (recommended) {
            lore.add(ChatColor.GREEN + "系统推荐尺寸");
        } else {
            lore.add(ChatColor.GRAY + "常用尺寸");
        }

        lore.add(ChatColor.GRAY + "尺寸：" + columns + "×" + rows);
        lore.add(ChatColor.GRAY + "地图数量：" + (columns * rows));

        int pricePerMap = plugin.getConfig().getInt("cost.give-cost-per-map", 100);
        int totalCost = columns * rows * pricePerMap;

        lore.add(ChatColor.GRAY + "每张价格：" + pricePerMap + " 游戏币");
        lore.add(ChatColor.YELLOW + "玩家总价：" + totalCost + " 游戏币");

        if (canBypassLimitText(columns, rows)) {
            lore.add(ChatColor.GRAY + "管理员可用");
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "点击选择这个尺寸");

        return createItem(
                material,
                ChatColor.GOLD + "" + columns + "×" + rows,
                lore
        );
    }

    private boolean canBypassLimitText(int columns, int rows) {
        int maxColumns = plugin.getConfig().getInt("settings.normal-max-columns", 4);
        int maxRows = plugin.getConfig().getInt("settings.normal-max-rows", 4);

        return columns > maxColumns || rows > maxRows;
    }

    private List<SizeRecommendation> getRecommendations(Player player, File imageFile) {
        List<SizeRecommendation> result = new ArrayList<SizeRecommendation>();

        try {
            BufferedImage image = ImageIO.read(imageFile);

            if (image == null) {
                result.add(new SizeRecommendation(4, 3, 999, 12));
                result.add(new SizeRecommendation(4, 4, 999, 16));
                return result;
            }

            double imageRatio = (double) image.getWidth() / (double) image.getHeight();

            int maxColumns;
            int maxRows;

            if (canBypassLimit(player)) {
                maxColumns = plugin.getConfig().getInt("settings.recommend-max-columns", 8);
                maxRows = plugin.getConfig().getInt("settings.recommend-max-rows", 6);
            } else {
                maxColumns = plugin.getConfig().getInt("settings.normal-max-columns", 4);
                maxRows = plugin.getConfig().getInt("settings.normal-max-rows", 4);
            }

            for (int columns = 1; columns <= maxColumns; columns++) {
                for (int rows = 1; rows <= maxRows; rows++) {
                    double gridRatio = (double) columns / (double) rows;
                    double diff = Math.abs(gridRatio - imageRatio);
                    int totalMaps = columns * rows;

                    result.add(new SizeRecommendation(columns, rows, diff, totalMaps));
                }
            }

            Collections.sort(result, new Comparator<SizeRecommendation>() {
                @Override
                public int compare(SizeRecommendation a, SizeRecommendation b) {
                    int diffCompare = Double.compare(a.getDiff(), b.getDiff());

                    if (diffCompare != 0) {
                        return diffCompare;
                    }

                    return Integer.compare(b.getTotalMaps(), a.getTotalMaps());
                }
            });

        } catch (Exception e) {
            result.add(new SizeRecommendation(4, 3, 999, 12));
            result.add(new SizeRecommendation(4, 4, 999, 16));
        }

        return result;
    }

    private boolean canBypassLimit(Player player) {
        return player.isOp()
                || player.hasPermission("efmapart.admin")
                || player.hasPermission("efmapart.bypass");
    }

    public int[] parseSizeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || meta.getDisplayName() == null) {
            return null;
        }

        String name = ChatColor.stripColor(meta.getDisplayName());

        if (!name.contains("×")) {
            return null;
        }

        String[] parts = name.split("×");

        if (parts.length != 2) {
            return null;
        }

        try {
            int columns = Integer.parseInt(parts[0]);
            int rows = Integer.parseInt(parts[1]);

            return new int[] {columns, rows};
        } catch (NumberFormatException e) {
            return null;
        }
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