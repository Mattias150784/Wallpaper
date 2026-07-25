package net.mattias.wallpaper;

import net.mattias.wallpaper.core.ModCreativeModeTab;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.core.util.WallpaperLightUtil;
import net.mattias.wallpaper.neoforge.config.NeoForgeWallpaperConfig;
import net.mattias.wallpaper.neoforge.core.data.ForgeWallpaperData;
import net.mattias.wallpaper.neoforge.core.network.ModMessages;
import net.mattias.wallpaper.platform.NeoForgePlatformHelper;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
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
        NeoForgePlatformHelper.SOUND_EVENT.register(modEventBus);

        ModSounds.init();
        ModItems.init();
        ModBlocks.init();
        ModCreativeModeTab.init();
        WallpaperCommon.init();

        modContainer.registerConfig(ModConfig.Type.SERVER, NeoForgeWallpaperConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, NeoForgeWallpaperConfig.CLIENT_SPEC);

        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.mattias.wallpaper.neoforge.client.WallpaperConfigScreen.register(modContainer);
            modEventBus.addListener(this::clientSetup);
        }
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        applyConfig(event.getConfig());
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        applyConfig(event.getConfig());
    }

    private void applyConfig(ModConfig config) {
        if (config.getSpec() == NeoForgeWallpaperConfig.SERVER_SPEC) {
            NeoForgeWallpaperConfig.applyServer();
            refreshEmissiveWallpaper();
        } else if (config.getSpec() == NeoForgeWallpaperConfig.CLIENT_SPEC) {
            NeoForgeWallpaperConfig.applyClient();
        }
    }

    private void refreshEmissiveWallpaper() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (ServerLevel level : server.getAllLevels()) {
                ForgeWallpaperData data = ForgeWallpaperData.get(level);
                if (data == null) {
                    continue;
                }
                data.data.storage.forEach((pos, faces) -> {
                    for (BlockState state : faces.values()) {
                        if (state != null && state.getLightEmission() > 0) {
                            WallpaperLightUtil.refreshLight(level, pos);
                            break;
                        }
                    }
                });
            }
        });
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
