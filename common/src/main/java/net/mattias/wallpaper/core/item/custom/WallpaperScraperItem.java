package net.mattias.wallpaper.core.item.custom;

import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

        BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

        if (!currentState.equals(defaultWallpaper)) {
            Services.PLATFORM.addWallpaper(level, pos, face, defaultWallpaper);

            level.playSound(context.getPlayer(), pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.7F, 1.2F);
        } else {
            Services.PLATFORM.removeWallpaper(level, pos, face);

            level.playSound(context.getPlayer(), pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
        }

        Services.PLATFORM.syncWallpaper(level, pos);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}