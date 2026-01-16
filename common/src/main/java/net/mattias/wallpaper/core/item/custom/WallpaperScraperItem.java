package net.mattias.wallpaper.core.item.custom;

import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WallpaperScraperItem extends Item {
    public WallpaperScraperItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();

        BlockState currentState = Services.PLATFORM.getWallpaper(level, pos, face);
        if (currentState == null) return InteractionResult.PASS;

        if (!level.isClientSide) {
            BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

            if (!currentState.equals(defaultWallpaper)) {
                Services.PLATFORM.addWallpaper(level, pos, face, defaultWallpaper);
            } else {
                Services.PLATFORM.removeWallpaper(level, pos, face);
            }

            Services.PLATFORM.syncWallpaper(level, pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}