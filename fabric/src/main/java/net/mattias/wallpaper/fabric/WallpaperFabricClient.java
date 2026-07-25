package net.mattias.wallpaper.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.util.SelectionPreviewManager;
import net.mattias.wallpaper.fabric.core.client.SelectionRenderer;
import net.mattias.wallpaper.fabric.core.client.WallpaperClientEvents;
import net.mattias.wallpaper.fabric.core.network.SelectionSyncPayload;
import net.minecraft.client.renderer.RenderType;

public class WallpaperFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WallpaperClientEvents.register();
        SelectionRenderer.register();

        ClientPlayNetworking.registerGlobalReceiver(SelectionSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.clear()) {
                        SelectionPreviewManager.clearSelection();
                    } else {
                        SelectionPreviewManager.setSelection(payload.pos(), payload.face());
                    }
                }));

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WALLPAPER_BLOCK.get(), RenderType.cutout());
    }
}
