package net.mattias.wallpaper.core.item.custom;

import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        Player player = context.getPlayer();

        BlockState currentState = Services.PLATFORM.getWallpaper(level, pos, face);
        if (currentState == null) return InteractionResult.PASS;

        BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

        if (!level.isClientSide) {
            ItemStack itemToGive;

            if (!currentState.equals(defaultWallpaper)) {
                itemToGive = new ItemStack(currentState.getBlock().asItem());

                Services.PLATFORM.addWallpaper(level, pos, face, defaultWallpaper);
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.7F, 1.2F);
            }
            else {
                itemToGive = new ItemStack(ModItems.WALLPAPER_ITEM.get());

                Services.PLATFORM.removeWallpaper(level, pos, face);
                level.playSound(null, pos, ModSounds.WALLPAPER_BREAK.get(),
                        SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            }

            if (player != null && !player.isCreative()) {
                if (!itemToGive.isEmpty()) {
                    if (!player.getInventory().add(itemToGive)) {
                        player.drop(itemToGive, false);
                    }
                }
            }

            Services.PLATFORM.syncWallpaper(level, pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}