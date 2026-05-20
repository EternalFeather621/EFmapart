package com.eternalfeather.efmapart.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapDataManager {

    private final JavaPlugin plugin;

    private File dataFile;
    private FileConfiguration dataConfig;

    public MapDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "mapdata.yml");

        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create mapdata.yml!");
                e.printStackTrace();
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        if (dataConfig == null || dataFile == null) {
            return;
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save mapdata.yml!");
            e.printStackTrace();
        }
    }

    public void saveMapData(int mapId, String fileName, int columns, int rows, int column, int row) {
        String path = "maps." + mapId;

        dataConfig.set(path + ".file", fileName);
        dataConfig.set(path + ".columns", columns);
        dataConfig.set(path + ".rows", rows);
        dataConfig.set(path + ".column", column);
        dataConfig.set(path + ".row", row);

        save();
    }

    public Map<Integer, SavedMapData> getAllSavedMaps() {
        Map<Integer, SavedMapData> result = new LinkedHashMap<Integer, SavedMapData>();

        ConfigurationSection section = dataConfig.getConfigurationSection("maps");

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            try {
                int mapId = Integer.parseInt(key);

                String path = "maps." + key;

                String fileName = dataConfig.getString(path + ".file", null);
                int columns = dataConfig.getInt(path + ".columns", 1);
                int rows = dataConfig.getInt(path + ".rows", 1);
                int column = dataConfig.getInt(path + ".column", 0);
                int row = dataConfig.getInt(path + ".row", 0);

                if (fileName == null) {
                    continue;
                }

                result.put(mapId, new SavedMapData(fileName, columns, rows, column, row));
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    public short[][] getCachedMapIds(String fileName, int columns, int rows) {
        short[][] mapIds = new short[rows][columns];
        boolean[][] found = new boolean[rows][columns];

        ConfigurationSection section = dataConfig.getConfigurationSection("maps");

        if (section == null) {
            return null;
        }

        for (String key : section.getKeys(false)) {
            try {
                int mapId = Integer.parseInt(key);

                String path = "maps." + key;

                String savedFileName = dataConfig.getString(path + ".file", null);
                int savedColumns = dataConfig.getInt(path + ".columns", 1);
                int savedRows = dataConfig.getInt(path + ".rows", 1);
                int column = dataConfig.getInt(path + ".column", 0);
                int row = dataConfig.getInt(path + ".row", 0);

                if (savedFileName == null) {
                    continue;
                }

                if (!savedFileName.equals(fileName)) {
                    continue;
                }

                if (savedColumns != columns || savedRows != rows) {
                    continue;
                }

                if (row < 0 || row >= rows || column < 0 || column >= columns) {
                    continue;
                }

                mapIds[row][column] = (short) mapId;
                found[row][column] = true;

            } catch (NumberFormatException ignored) {
            }
        }

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (!found[row][column]) {
                    return null;
                }
            }
        }

        return mapIds;
    }

    public static class SavedMapData {

        private final String fileName;
        private final int columns;
        private final int rows;
        private final int column;
        private final int row;

        public SavedMapData(String fileName, int columns, int rows, int column, int row) {
            this.fileName = fileName;
            this.columns = columns;
            this.rows = rows;
            this.column = column;
            this.row = row;
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

        public int getColumn() {
            return column;
        }

        public int getRow() {
            return row;
        }
    }
}