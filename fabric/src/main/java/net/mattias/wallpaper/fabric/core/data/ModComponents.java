package net.mattias.wallpaper.fabric.core.data;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.world.WorldComponentInitializer;
import net.minecraft.resources.ResourceLocation;

public class ModComponents implements WorldComponentInitializer {
    public static final ComponentKey<WallpaperComponent> WALLPAPER_DATA =
            ComponentRegistry.getOrCreate(new ResourceLocation("wallpaper", "data"), WallpaperComponent.class);

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(WALLPAPER_DATA, WallpaperComponent::new);
    }
}