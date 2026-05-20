package com.eternalfeather.efmapart;

import com.eternalfeather.efmapart.command.MapArtCommand;
import com.eternalfeather.efmapart.data.MapDataManager;
import com.eternalfeather.efmapart.data.MapDataManager.SavedMapData;
import com.eternalfeather.efmapart.renderer.ImageMapRenderer;
import com.eternalfeather.efmapart.util.ImageUtil;
import com.eternalfeather.efmapart.wall.WallPlacementManager;
import com.eternalfeather.efmapart.gui.MapArtGui;
import com.eternalfeather.efmapart.gui.MapArtGuiListener;
import com.eternalfeather.efmapart.service.MapArtGenerator;
import com.eternalfeather.efmapart.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

public class Main extends JavaPlugin {

    private File imagesFolder;
    private MapDataManager mapDataManager;
    private WallPlacementManager wallPlacementManager;
    private MapArtGui mapArtGui;
    private MapArtGenerator mapArtGenerator;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economyManager = new EconomyManager(this);
        economyManager.setupEconomy();

        imagesFolder = new File(getDataFolder(), "images");

        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
        }

        mapDataManager = new MapDataManager(this);
        mapDataManager.load();

        mapArtGenerator = new MapArtGenerator(this);

        restoreSavedMaps();

        wallPlacementManager = new WallPlacementManager(this);
        mapArtGui = new MapArtGui(this);

        Bukkit.getPluginManager().registerEvents(wallPlacementManager, this);
        Bukkit.getPluginManager().registerEvents(new MapArtGuiListener(this, mapArtGui), this);

        if (getCommand("efmapart") != null) {
            getCommand("efmapart").setExecutor(new MapArtCommand(this));
        }

        getLogger().info("EFMapArt enabled!");
    }

    @Override
    public void onDisable() {
        if (mapDataManager != null) {
            mapDataManager.save();
        }

        getLogger().info("EFMapArt disabled!");
    }

    private void restoreSavedMaps() {
        Map<Integer, SavedMapData> savedMaps = mapDataManager.getAllSavedMaps();

        int restored = 0;

        for (Map.Entry<Integer, SavedMapData> entry : savedMaps.entrySet()) {
            int mapId = entry.getKey();
            SavedMapData data = entry.getValue();

            File imageFile = new File(imagesFolder, data.getFileName());

            if (!imageFile.exists()) {
                getLogger().warning("Cannot restore map " + mapId + ", image not found: " + data.getFileName());
                continue;
            }

            try {
                BufferedImage originalImage = ImageIO.read(imageFile);

                if (originalImage == null) {
                    getLogger().warning("Cannot restore map " + mapId + ", invalid image: " + data.getFileName());
                    continue;
                }

                BufferedImage fullImage = ImageUtil.resizeForMapGrid(originalImage, data.getColumns(), data.getRows());
                BufferedImage tileImage = ImageUtil.cropTile(fullImage, data.getColumn(), data.getRow());

                MapView mapView = Bukkit.getMap((short) mapId);

                if (mapView == null) {
                    getLogger().warning("Cannot restore map " + mapId + ", MapView is null.");
                    continue;
                }

                for (MapRenderer renderer : mapView.getRenderers()) {
                    mapView.removeRenderer(renderer);
                }

                mapView.addRenderer(new ImageMapRenderer(tileImage));

                restored++;
            } catch (Exception e) {
                getLogger().warning("Cannot restore map " + mapId + ".");
                e.printStackTrace();
            }
        }

        getLogger().info("Restored " + restored + " map arts.");
    }

    public File getImagesFolder() {
        return imagesFolder;
    }

    public MapDataManager getMapDataManager() {
        return mapDataManager;
    }

    public WallPlacementManager getWallPlacementManager() {
        return wallPlacementManager;
    }
    public MapArtGui getMapArtGui() {
        return mapArtGui;
    }

    public MapArtGenerator getMapArtGenerator() {
        return mapArtGenerator;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}