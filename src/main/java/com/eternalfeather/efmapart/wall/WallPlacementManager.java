package com.eternalfeather.efmapart.wall;

import com.eternalfeather.efmapart.Main;
import com.eternalfeather.efmapart.renderer.ImageMapRenderer;
import com.eternalfeather.efmapart.util.ImageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WallPlacementManager implements Listener {

    private final Main plugin;

    private final Map<String, PendingWall> pendingWalls = new HashMap<String, PendingWall>();

    public WallPlacementManager(Main plugin) {
        this.plugin = plugin;
    }

    public void startPlacement(Player player, String fileName, int columns, int rows) {
        File imageFile = new File(plugin.getImagesFolder(), fileName);

        if (!imageFile.exists()) {
            player.sendMessage(ChatColor.RED + "找不到图片文件：" + fileName);
            player.sendMessage(ChatColor.GRAY + "请把图片放到 plugins/EFMapArt/images/ 文件夹里。");
            return;
        }

        if (columns <= 0 || rows <= 0) {
            player.sendMessage(ChatColor.RED + "宽度和高度必须大于 0。");
            return;
        }

        if (columns > 10 || rows > 10) {
            player.sendMessage(ChatColor.RED + "为了防止一次生成过多实体，单次最大支持 10×10。");
            return;
        }

        pendingWalls.put(player.getName().toLowerCase(), new PendingWall(fileName, columns, rows));

        player.sendMessage(ChatColor.GREEN + "地图墙生成准备完成。");
        player.sendMessage(ChatColor.YELLOW + "图片：" + ChatColor.WHITE + fileName);
        player.sendMessage(ChatColor.YELLOW + "尺寸：" + ChatColor.WHITE + columns + "×" + rows);
        player.sendMessage(ChatColor.GRAY + "请右键点击一个方块的侧面，作为地图墙的左下角。");
        player.sendMessage(ChatColor.GRAY + "注意：不要点方块顶部或底部，要点侧面。");
    }

    public void cancelPlacement(Player player) {
        PendingWall removed = pendingWalls.remove(player.getName().toLowerCase());

        if (removed == null) {
            player.sendMessage(ChatColor.RED + "你当前没有待放置的地图墙。");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "已取消地图墙放置。");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        PendingWall pendingWall = pendingWalls.get(player.getName().toLowerCase());

        if (pendingWall == null) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        BlockFace clickedFace = event.getBlockFace();

        if (clickedBlock == null) {
            return;
        }

        if (!isHorizontalFace(clickedFace)) {
            player.sendMessage(ChatColor.RED + "请点击方块的侧面，不要点击顶部或底部。");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        boolean success = placeWall(player, clickedBlock, clickedFace, pendingWall);

        if (success) {
            pendingWalls.remove(player.getName().toLowerCase());
        }
    }

    private boolean placeWall(Player player, Block clickedBlock, BlockFace facing, PendingWall pendingWall) {
        File imageFile = new File(plugin.getImagesFolder(), pendingWall.getFileName());

        if (!imageFile.exists()) {
            player.sendMessage(ChatColor.RED + "找不到图片文件：" + pendingWall.getFileName());
            return false;
        }

        Material wallMaterial = getWallMaterial();

        try {
            BufferedImage originalImage = ImageIO.read(imageFile);

            if (originalImage == null) {
                player.sendMessage(ChatColor.RED + "无法读取这张图片，请确认它是 png、jpg 或 jpeg 图片。");
                return false;
            }

            int columns = pendingWall.getColumns();
            int rows = pendingWall.getRows();

            short[][] mapIds = plugin.getMapArtGenerator().getOrCreateMapIds(
                    player,
                    pendingWall.getFileName(),
                    columns,
                    rows,
                    originalImage
            );

            Block origin = clickedBlock.getRelative(facing);
            BlockFace rightFace = getRightFace(facing);

            int created = 0;

            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    int yOffset = rows - 1 - row;

                    Block wallBlock = origin.getRelative(rightFace, column).getRelative(BlockFace.UP, yOffset);

                    wallBlock.setType(wallMaterial);

                    short mapId = mapIds[row][column];

                    ItemStack mapItem = plugin.getMapArtGenerator().createMapItem(
                            mapId,
                            pendingWall.getFileName(),
                            columns,
                            rows,
                            column,
                            row
                    );

                    spawnItemFrame(wallBlock, facing, mapItem);

                    created++;
                }
            }

            player.sendMessage(ChatColor.GREEN + "地图墙已生成完成！");
            player.sendMessage(ChatColor.GREEN + "图片：" + ChatColor.YELLOW + pendingWall.getFileName());
            player.sendMessage(ChatColor.GREEN + "尺寸：" + ChatColor.YELLOW + columns + "×" + rows);
            player.sendMessage(ChatColor.GREEN + "共生成：" + ChatColor.YELLOW + created + ChatColor.GREEN + " 个展示框。");

            chargeWallCost(player);

            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "生成地图墙时发生错误，请查看控制台。");
            e.printStackTrace();
            return false;
        }
    }

    private void spawnItemFrame(Block wallBlock, BlockFace facing, ItemStack mapItem) {
        /*
         * wallBlock 是墙体方块，例如萤石。
         * frameBlock 是展示框所在的空气方块。
         *
         * 1.8.8 普通 world.spawn(ItemFrame.class) 经常会因为 Hanging Entity 的方向问题失败。
         * 所以这里使用反射调用 NMS EntityItemFrame 来生成展示框。
         */
        Block frameBlock = wallBlock.getRelative(facing);

        if (frameBlock.getType() != Material.AIR) {
            frameBlock.setType(Material.AIR);
        }

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
            Object nmsWorld = craftWorldClass.getMethod("getHandle").invoke(wallBlock.getWorld());

            Class<?> nmsWorldClass = Class.forName("net.minecraft.server." + version + ".World");
            Class<?> blockPositionClass = Class.forName("net.minecraft.server." + version + ".BlockPosition");
            Class<?> enumDirectionClass = Class.forName("net.minecraft.server." + version + ".EnumDirection");
            Class<?> entityClass = Class.forName("net.minecraft.server." + version + ".Entity");
            Class<?> entityItemFrameClass = Class.forName("net.minecraft.server." + version + ".EntityItemFrame");

            Constructor<?> blockPositionConstructor = blockPositionClass.getConstructor(int.class, int.class, int.class);
            Object blockPosition = blockPositionConstructor.newInstance(
                    frameBlock.getX(),
                    frameBlock.getY(),
                    frameBlock.getZ()
            );

            Object enumDirection = getNmsDirection(enumDirectionClass, facing);

            Constructor<?> itemFrameConstructor = entityItemFrameClass.getConstructor(
                    nmsWorldClass,
                    blockPositionClass,
                    enumDirectionClass
            );

            Object nmsItemFrame = itemFrameConstructor.newInstance(
                    nmsWorld,
                    blockPosition,
                    enumDirection
            );

            nmsWorldClass.getMethod("addEntity", entityClass).invoke(nmsWorld, nmsItemFrame);

            Object bukkitEntity = entityItemFrameClass.getMethod("getBukkitEntity").invoke(nmsItemFrame);

            if (!(bukkitEntity instanceof ItemFrame)) {
                throw new IllegalStateException("生成的实体不是 ItemFrame");
            }

            ItemFrame itemFrame = (ItemFrame) bukkitEntity;
            itemFrame.setItem(mapItem);

        } catch (Exception e) {
            throw new RuntimeException("无法生成物品展示框，方向：" + facing
                    + "，墙体坐标："
                    + wallBlock.getX() + ", "
                    + wallBlock.getY() + ", "
                    + wallBlock.getZ(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object getNmsDirection(Class<?> enumDirectionClass, BlockFace facing) {
        String directionName;

        if (facing == BlockFace.NORTH) {
            directionName = "NORTH";
        } else if (facing == BlockFace.SOUTH) {
            directionName = "SOUTH";
        } else if (facing == BlockFace.EAST) {
            directionName = "EAST";
        } else if (facing == BlockFace.WEST) {
            directionName = "WEST";
        } else {
            directionName = "NORTH";
        }

        return Enum.valueOf((Class<Enum>) enumDirectionClass.asSubclass(Enum.class), directionName);
    }

    private void chargeWallCost(Player player) {
        if (canBypassCost(player)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.wall-cost-enabled", true)) {
            return;
        }

        int cost = plugin.getConfig().getInt("settings.wall-cost", 1000);

        if (cost <= 0) {
            return;
        }

        String command = plugin.getConfig().getString(
                "settings.wall-cost-command",
                "eco take %player% %cost%"
        );

        command = command
                .replace("%player%", player.getName())
                .replace("%cost%", String.valueOf(cost));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        player.sendMessage(ChatColor.YELLOW + "本次生成地图墙花费："
                + ChatColor.GOLD + cost
                + ChatColor.YELLOW + " 游戏币。");
    }

    private boolean canBypassCost(Player player) {
        return player.isOp()
                || player.hasPermission("efmapart.admin")
                || player.hasPermission("efmapart.bypass");
    }

    private ItemStack createMapItem(short mapId, String fileName, int columns, int rows, int column, int row) {
        ItemStack mapItem = new ItemStack(Material.MAP, 1, mapId);

        ItemMeta meta = mapItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "地图画：" + ChatColor.YELLOW + fileName);

            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "尺寸：" + columns + "×" + rows);
            lore.add(ChatColor.GRAY + "位置：第 " + (row + 1) + " 行，第 " + (column + 1) + " 列");
            lore.add(ChatColor.GRAY + "自动墙体地图");
            lore.add(ChatColor.DARK_GRAY + "Map ID: " + mapId);

            meta.setLore(lore);
            mapItem.setItemMeta(meta);
        }

        return mapItem;
    }

    private Material getWallMaterial() {
        String materialName = plugin.getConfig().getString("settings.wall-material", "GLOWSTONE");

        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            plugin.getLogger().warning("Invalid wall-material in config.yml: " + materialName + ", using GLOWSTONE.");
            return Material.GLOWSTONE;
        }

        return material;
    }

    private boolean isHorizontalFace(BlockFace face) {
        return face == BlockFace.NORTH
                || face == BlockFace.SOUTH
                || face == BlockFace.EAST
                || face == BlockFace.WEST;
    }

    private BlockFace getRightFace(BlockFace facing) {
        if (facing == BlockFace.NORTH) {
            return BlockFace.WEST;
        }

        if (facing == BlockFace.SOUTH) {
            return BlockFace.EAST;
        }

        if (facing == BlockFace.EAST) {
            return BlockFace.NORTH;
        }

        if (facing == BlockFace.WEST) {
            return BlockFace.SOUTH;
        }

        return BlockFace.EAST;
    }

    private static class PendingWall {

        private final String fileName;
        private final int columns;
        private final int rows;

        public PendingWall(String fileName, int columns, int rows) {
            this.fileName = fileName;
            this.columns = columns;
            this.rows = rows;
        }

        public String getFileName() {
            return fileName;
        }

        public int getColumns() {
            return columns;
        }

        public int getRows() {
            return rows;
        }
    }
}