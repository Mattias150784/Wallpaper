package net.mattias.wallpaper.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.core.config.WallpaperConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FabricWallpaperConfig {

    private FabricWallpaperConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        boolean emissiveLighting = true;
        boolean multiPlacement = true;
        boolean veinMineScraper = true;
        boolean blockRotation = true;
        int maxMultiPlaceArea = 400;
        int maxVeinMineBlocks = 64;
        int selectionTimeoutSeconds = 30;
        boolean consumeItems = true;
        boolean returnItemsOnRemove = true;
        int maxRenderDistance = 64;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(WallpaperCommon.MOD_ID + ".json");

        Data data = new Data();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Data parsed = GSON.fromJson(reader, Data.class);
                if (parsed != null) {
                    data = parsed;
                }
            } catch (IOException | RuntimeException e) {
                WallpaperCommon.LOG.warn("Failed to read {} config, using defaults", WallpaperCommon.MOD_ID, e);
            }
        }

        apply(data);
        save(path, data);
    }

    private static void apply(Data d) {
        WallpaperConfig.emissiveLighting = d.emissiveLighting;
        WallpaperConfig.multiPlacement = d.multiPlacement;
        WallpaperConfig.veinMineScraper = d.veinMineScraper;
        WallpaperConfig.blockRotation = d.blockRotation;
        WallpaperConfig.maxMultiPlaceArea = Math.max(1, d.maxMultiPlaceArea);
        WallpaperConfig.maxVeinMineBlocks = Math.max(1, d.maxVeinMineBlocks);
        WallpaperConfig.selectionTimeoutSeconds = Math.max(1, d.selectionTimeoutSeconds);
        WallpaperConfig.consumeItems = d.consumeItems;
        WallpaperConfig.returnItemsOnRemove = d.returnItemsOnRemove;
        WallpaperConfig.maxRenderDistance = Math.max(0, d.maxRenderDistance);
    }

    private static void save(Path path, Data d) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(d, writer);
            }
        } catch (IOException e) {
            WallpaperCommon.LOG.warn("Failed to write {} config", WallpaperCommon.MOD_ID, e);
        }
    }
}
