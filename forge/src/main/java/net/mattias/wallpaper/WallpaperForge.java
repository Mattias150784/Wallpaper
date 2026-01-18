package net.mattias.wallpaper;

import net.mattias.wallpaper.core.ModCreativeModeTab;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.forge.core.network.ModMessages;
import net.mattias.wallpaper.platform.ForgePlatformHelper;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(WallpaperCommon.MOD_ID)
public class WallpaperForge {
    public WallpaperForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ForgePlatformHelper.ITEMS.register(eventBus);
        ForgePlatformHelper.BLOCKS.register(eventBus);
        ForgePlatformHelper.TABS.register(eventBus);

        ModItems.init();
        ModBlocks.init();
        ModCreativeModeTab.init();
        WallpaperCommon.init();

        ModMessages.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            eventBus.addListener(this::clientSetup);
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WALLPAPER_BLOCK.get(), RenderType.cutout());});
    }
}