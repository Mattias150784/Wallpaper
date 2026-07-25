package net.mattias.wallpaper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.mattias.wallpaper.core.ModCreativeModeTab;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.fabric.config.FabricWallpaperConfig;
import net.mattias.wallpaper.fabric.core.network.SelectionSyncPayload;

public class WallpaperFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricWallpaperConfig.load();

        PayloadTypeRegistry.playS2C().register(SelectionSyncPayload.TYPE, SelectionSyncPayload.CODEC);

        ModItems.init();
        ModCreativeModeTab.init();
        ModBlocks.init();
        ModSounds.init();
        WallpaperEventHandler.register();
        WallpaperEventHandler.registerChunkReseed();
        WallpaperRotationEvents.register();

        WallpaperCommon.init();
    }
}
