package net.mattias.wallpaper.core;

import net.mattias.wallpaper.core.item.custom.WallpaperItem;
import net.mattias.wallpaper.core.item.custom.WallpaperScraperItem;
import net.mattias.wallpaper.platform.Services;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public class ModItems {

    public static final Supplier<WallpaperItem> WALLPAPER_ITEM = register("wallpaper",
            () -> new WallpaperItem(new Item.Properties()));

    public static final Supplier<WallpaperScraperItem> WALLPAPER_SCRAPER = register("wallpaper_scraper",
            () -> new WallpaperScraperItem(new Item.Properties()));

    public static void init() {
    }

    private static <T extends Item> Supplier<T> register(String name, Supplier<T> item) {
        return Services.PLATFORM.registerItem(name, item);
    }
}