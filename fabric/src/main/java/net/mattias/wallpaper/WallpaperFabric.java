package net.mattias.wallpaper;

import net.fabricmc.api.ModInitializer;
import net.mattias.wallpaper.core.ModCreativeModeTab;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;


public class WallpaperFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        ModItems.init();
        ModCreativeModeTab.init();
        ModBlocks.init();
        WallpaperEventHandler.register();

        WallpaperCommon.init();
    }
}
