package net.mattias.wallpaper.core.config;

public final class WallpaperConfig {

    private WallpaperConfig() {}

    public static volatile boolean emissiveLighting = true;
    public static volatile boolean multiPlacement = true;
    public static volatile boolean veinMineScraper = true;
    public static volatile boolean blockRotation = true;

    public static volatile int maxMultiPlaceArea = 400;
    public static volatile int maxVeinMineBlocks = 64;
    public static volatile int selectionTimeoutSeconds = 30;
    public static volatile boolean consumeItems = true;
    public static volatile boolean returnItemsOnRemove = true;

    public static volatile int maxRenderDistance = 64;

    public static long selectionTimeoutMillis() {
        return Math.max(1, selectionTimeoutSeconds) * 1000L;
    }

    public static boolean isWithinRenderDistance(double distanceSqr) {
        int d = maxRenderDistance;
        if (d <= 0) return true;
        return distanceSqr <= (double) d * d;
    }
}
