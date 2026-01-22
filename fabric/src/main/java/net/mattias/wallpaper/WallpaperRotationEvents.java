package net.mattias.wallpaper;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.util.WallpaperRotationHandler;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WallpaperRotationEvents {

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() == ModItems.WALLPAPER_SCRAPER.get()) {
                return InteractionResult.PASS;
            }

            if (!player.isCrouching()) {
                return InteractionResult.PASS;
            }

            if (level.isClientSide) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            var componentOpt = ModComponents.WALLPAPER_DATA.maybeGet(level);
            if (componentOpt.isEmpty()) {
                return InteractionResult.PASS;
            }

            var data = componentOpt.get().data;

            if (!data.storage.containsKey(pos) || !data.storage.get(pos).containsKey(direction)) {
                return InteractionResult.PASS;
            }

            WallpaperRotationHandler.WallpaperDataAccess dataAccess = new WallpaperRotationHandler.WallpaperDataAccess() {
                @Override
                public BlockState getWallpaper(Level lvl, BlockPos p, Direction face) {
                    return ModComponents.WALLPAPER_DATA.maybeGet(lvl)
                            .map(c -> c.data.storage.get(p))
                            .map(faces -> faces.get(face))
                            .orElse(null);
                }

                @Override
                public void setWallpaper(Level lvl, BlockPos p, Direction face, BlockState state) {
                    ModComponents.WALLPAPER_DATA.maybeGet(lvl).ifPresent(component -> {
                        component.data.storage.get(p).put(face, state);
                    });
                }

                @Override
                public void sync(Level lvl, BlockPos p) {
                    ModComponents.WALLPAPER_DATA.sync(lvl);
                }

                @Override
                public BlockState getDefaultWallpaper() {
                    return ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();
                }
            };

            boolean rotated = WallpaperRotationHandler.tryRotate(level, pos, direction, serverPlayer, dataAccess);

            return rotated ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }
}