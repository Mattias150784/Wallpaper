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

public class WallpaperItem extends Item {
    public WallpaperItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();

        if (Services.PLATFORM.getWallpaper(level, pos, face) != null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();
            Services.PLATFORM.addWallpaper(level, pos, face, defaultWallpaper);
            Services.PLATFORM.syncWallpaper(level, pos);

            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}