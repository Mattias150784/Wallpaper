package net.mattias.wallpaper.core.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WallpaperData {
    public final Map<BlockPos, Map<Direction, BlockState>> storage = new ConcurrentHashMap<>();
}
