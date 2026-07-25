package net.mattias.wallpaper.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class WallpaperLightUtil {
    private WallpaperLightUtil() {}

    public static void refreshLight(Level level, BlockPos pos) {
        var lightEngine = level.getLightEngine();
        lightEngine.checkBlock(pos);
        for (Direction dir : Direction.values()) {
            lightEngine.checkBlock(pos.relative(dir));
        }
    }
}
