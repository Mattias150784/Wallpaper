package net.mattias.wallpaper;

import net.mattias.wallpaper.core.ModCreativeModeTab;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.platform.ForgePlatformHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WallpaperCommon.MOD_ID)
public class WallpaperForge {
    
    public WallpaperForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ForgePlatformHelper.registerRegisters(eventBus);

        ModItems.init();
        ModCreativeModeTab.init();


        WallpaperCommon.LOG.info("Hello Forge world!");
        WallpaperCommon.init();
    }
}