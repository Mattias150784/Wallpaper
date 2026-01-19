package net.mattias.wallpaper;

import net.mattias.wallpaper.core.ModCreativeModeTab;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.neoforge.core.network.ModMessages;
import net.mattias.wallpaper.platform.NeoForgePlatformHelper;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(WallpaperCommon.MOD_ID)
public class WallpaperNeoForge {

    public WallpaperNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeoForgePlatformHelper.ITEMS.register(modEventBus);
        NeoForgePlatformHelper.BLOCKS.register(modEventBus);
        NeoForgePlatformHelper.TABS.register(modEventBus);

        ModItems.init();
        ModBlocks.init();
        ModCreativeModeTab.init();
        WallpaperCommon.init();

        modEventBus.addListener(this::registerPayloads);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        ModMessages.register(registrar);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WALLPAPER_BLOCK.get(), RenderType.cutout());
        });
    }
}