package net.mattias.wallpaper.core;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

public class ModCreativeModeTab {
    public static final Supplier<CreativeModeTab> WALLPAPER_TAB = register("wallpaper_tab", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + WallpaperCommon.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.WALLPAPER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.WALLPAPER_ITEM.get());
                        output.accept(ModItems.WALLPAPER_SCRAPER.get());
                    })
                    .build()
    );

    public static void init() {
    }

    private static <T extends CreativeModeTab> Supplier<T> register(String name, Supplier<T> tab) {
        return Services.PLATFORM.registerCreativeTab(name, tab);
    }
}
