package net.mattias.wallpaper.core.sound;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class ModSounds {

    public static final Supplier<SoundEvent> WALLPAPER_PLACE = registerSound("wallpaper_place");
    public static final Supplier<SoundEvent> WALLPAPER_BREAK = registerSound("wallpaper_break");

    private static Supplier<SoundEvent> registerSound(String name) {
        return Services.PLATFORM.registerSound(name, () ->
                SoundEvent.createVariableRangeEvent( ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, name)));
    }

    public static void init() {}
}