package com.eternalfeather.efmapart.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageUtil {

    public static BufferedImage resize(BufferedImage originalImage, int width, int height) {
        Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(scaledImage, 0, 0, null);
        graphics.dispose();

        return result;
    }

    public static BufferedImage resizeToMapSize(BufferedImage originalImage) {
        return resize(originalImage, 128, 128);
    }

    public static BufferedImage resizeForMapGrid(BufferedImage originalImage, int columns, int rows) {
        return resize(originalImage, columns * 128, rows * 128);
    }

    public static BufferedImage cropTile(BufferedImage image, int column, int row) {
        int x = column * 128;
        int y = row * 128;

        return image.getSubimage(x, y, 128, 128);
    }
}