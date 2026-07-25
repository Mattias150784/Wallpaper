package net.mattias.wallpaper.core.util;

import net.mattias.wallpaper.core.config.WallpaperConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WallpaperRotationHandler {
    public interface WallpaperDataAccess {
        BlockState getWallpaper(Level level, BlockPos pos, Direction face);
        void setWallpaper(Level level, BlockPos pos, Direction face, BlockState state);
        void sync(Level level, BlockPos pos);
        BlockState getDefaultWallpaper();
    }

    public static boolean tryRotate(Level level, BlockPos pos, Direction face,
                                    ServerPlayer player, WallpaperDataAccess dataAccess) {
        if (!WallpaperConfig.blockRotation) {
            return false;
        }

        BlockState existingState = dataAccess.getWallpaper(level, pos, face);

        if (existingState == null) {
            return false;
        }

        BlockState defaultWallpaper = dataAccess.getDefaultWallpaper();

        if (existingState.equals(defaultWallpaper)) {
            return false;
        }

        if (!BlockRotation.canRotate(existingState)) {
            return false;
        }

        BlockState rotated = BlockRotation.getNextRotation(existingState, face);
        dataAccess.setWallpaper(level, pos, face, rotated);
        dataAccess.sync(level, pos);

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM,
                SoundSource.BLOCKS, 0.5F, 1.0F);

        return true;
    }
}
