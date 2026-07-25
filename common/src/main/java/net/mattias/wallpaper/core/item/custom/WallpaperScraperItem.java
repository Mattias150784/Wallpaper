package net.mattias.wallpaper.core.item.custom;

import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.config.WallpaperConfig;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.core.util.WallpaperVeinMiner;
import net.mattias.wallpaper.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
    public WallpaperScraperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Player player = context.getPlayer();

        BlockState currentState = Services.PLATFORM.getWallpaper(level, pos, face);
        if (currentState == null) return InteractionResult.PASS;

        BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

        boolean isVeinMining = player != null && player.isCrouching() && WallpaperConfig.veinMineScraper;

        if (!level.isClientSide) {
            if (isVeinMining) {
                WallpaperVeinMiner.WallpaperAccessor accessor = new WallpaperVeinMiner.WallpaperAccessor() {
                    @Override
                    public BlockState getWallpaper(Level lvl, BlockPos p, Direction f) {
                        return Services.PLATFORM.getWallpaper(lvl, p, f);
                    }

                    @Override
                    public void removeWallpaper(Level lvl, BlockPos p, Direction f) {
                        Services.PLATFORM.removeWallpaper(lvl, p, f);
                    }
                };

                WallpaperVeinMiner.VeinMineResult result = WallpaperVeinMiner.findConnectedWallpapers(
                        level, pos, face, currentState, accessor
                );

                if (result.count > 1) {
                    for (BlockPos foundPos : result.positions) {
                        BlockState wallpaperState = Services.PLATFORM.getWallpaper(level, foundPos, face);

                        if (wallpaperState != null) {
                            ItemStack itemToGive;

                            if (!wallpaperState.equals(defaultWallpaper)) {
                                itemToGive = new ItemStack(wallpaperState.getBlock().asItem());
                                Services.PLATFORM.addWallpaper(level, foundPos, face, defaultWallpaper);
                            } else {
                                itemToGive = new ItemStack(ModItems.WALLPAPER_ITEM.get());
                                Services.PLATFORM.removeWallpaper(level, foundPos, face);
                            }

                            if (WallpaperConfig.returnItemsOnRemove && player != null && !player.isCreative()) {
                                if (!itemToGive.isEmpty()) {
                                    if (!player.getInventory().add(itemToGive)) {
                                        player.drop(itemToGive, false);
                                    }
                                }
                            }

                            Services.PLATFORM.syncWallpaper(level, foundPos);
                        }
                    }

                    level.playSound(null, pos, ModSounds.WALLPAPER_BREAK.get(),
                            SoundSource.BLOCKS, 1.0F, 0.8F);

                    if (player != null) {
                        player.displayClientMessage(
                                Component.literal("§aRemoved §e" + result.count + " §awallpapers!"),
                                true
                        );
                    }

                    return InteractionResult.SUCCESS;
                }
            }

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

            if (WallpaperConfig.returnItemsOnRemove && player != null && !player.isCreative()) {
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
