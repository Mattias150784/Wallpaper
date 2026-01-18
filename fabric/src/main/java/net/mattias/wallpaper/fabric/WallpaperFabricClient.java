package net.mattias.wallpaper.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.fabric.core.client.WallpaperClientEvents;
import net.minecraft.client.renderer.RenderType;

public class WallpaperFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WallpaperClientEvents.register();

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WALLPAPER_BLOCK.get(), RenderType.cutout());
    }
}