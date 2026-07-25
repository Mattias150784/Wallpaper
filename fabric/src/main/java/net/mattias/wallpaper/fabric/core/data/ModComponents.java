package net.mattias.wallpaper.fabric.core.data;

import net.minecraft.resources.ResourceLocation;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public class ModComponents implements WorldComponentInitializer {
    public static final ComponentKey<WallpaperComponent> WALLPAPER_DATA =
            ComponentRegistry.getOrCreate( ResourceLocation.fromNamespaceAndPath("wallpaper", "data"), WallpaperComponent.class);

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(WALLPAPER_DATA, WallpaperComponent::new);
    }
}
