package net.mattias.wallpaper;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.core.util.*;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class WallpaperEventHandler {
    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            Direction face = hitResult.getDirection();
            ItemStack stack = player.getItemInHand(hand);

            if (stack.getItem() == ModItems.WALLPAPER_SCRAPER.get()) {
                return InteractionResult.PASS;
            }

            BlockState blockAtPos = level.getBlockState(pos);

            if (blockAtPos.isAir() || blockAtPos.canBeReplaced()) {
                return InteractionResult.PASS;
            }

            var componentOpt = ModComponents.WALLPAPER_DATA.maybeGet(level);
            if (componentOpt.isEmpty()) {
                return InteractionResult.PASS;
            }

            var data = componentOpt.get().data;

            if (!data.storage.containsKey(pos) || !data.storage.get(pos).containsKey(face)) {
                return InteractionResult.PASS;
            }

            BlockState existingState = data.storage.get(pos).get(face);
            BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

            boolean isHoldingBlock = stack.getItem() instanceof BlockItem;

            if (player.isCrouching() && !existingState.equals(defaultWallpaper) && !isHoldingBlock) {
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                    WallpaperRotationHandler.WallpaperDataAccess dataAccess = new WallpaperRotationHandler.WallpaperDataAccess() {
                        @Override
                        public BlockState getWallpaper(Level lvl, BlockPos p, Direction f) {
                            return ModComponents.WALLPAPER_DATA.maybeGet(lvl)
                                    .map(c -> c.data.storage.get(p))
                                    .map(faces -> faces.get(f))
                                    .orElse(null);
                        }

                        @Override
                        public void setWallpaper(Level lvl, BlockPos p, Direction f, BlockState state) {
                            ModComponents.WALLPAPER_DATA.maybeGet(lvl).ifPresent(component -> {
                                component.data.storage.get(p).put(f, state);
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

                    boolean rotated = WallpaperRotationHandler.tryRotate(level, pos, face, serverPlayer, dataAccess);

                    if (rotated) {
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.PASS;
            }

            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return InteractionResult.PASS;
            }

            BlockState heldState = blockItem.getBlock().defaultBlockState();

            if (!WallpaperValidation.isValidWallpaperBlock(heldState)) {
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                    if (heldState.getBlock() instanceof ShulkerBoxBlock ||
                            heldState.getBlock() instanceof BaseEntityBlock) {
                    }
                }
                return InteractionResult.PASS;
            }

            if (!existingState.equals(defaultWallpaper)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                if (player.isCrouching()) {
                    if (MultiWallpaperPlacer.hasPendingPlacement(player.getUUID())) {
                        MultiWallpaperPlacer.PlacementCallback callback = new MultiWallpaperPlacer.PlacementCallback() {
                            @Override
                            public boolean canPlace(Level lvl, BlockPos blockPos, Direction direction) {
                                return ModComponents.WALLPAPER_DATA.maybeGet(lvl)
                                        .map(c -> c.data.storage.get(blockPos))
                                        .map(faces -> faces.containsKey(direction) &&
                                                faces.get(direction).equals(defaultWallpaper))
                                        .orElse(false);
                            }

                            @Override
                            public void placeWallpaper(Level lvl, BlockPos blockPos, Direction direction, BlockState state) {
                                ModComponents.WALLPAPER_DATA.maybeGet(lvl).ifPresent(component -> {
                                    component.data.storage.get(blockPos).put(direction, state);
                                    ModComponents.WALLPAPER_DATA.sync(lvl);
                                });
                            }

                            @Override
                            public boolean hasEnoughItems(ServerPlayer p, int count) {
                                if (p.isCreative()) return true;
                                return ShulkerInventory.getTotalItemCount(p, stack.getItem()) >= count;
                            }

                            @Override
                            public void consumeItems(ServerPlayer p, int count) {
                                if (p.isCreative()) return;
                                ShulkerInventory.consumeItems(p, stack.getItem(), count);
                            }
                        };

                        MultiWallpaperPlacer.tryCompleteMultiPlacement(serverPlayer, level, pos, face, heldState, callback);
                        SelectionPreviewManager.clearSelection();
                        return InteractionResult.SUCCESS;
                    } else {
                        if (MultiWallpaperPlacer.tryStartMultiPlacement(serverPlayer, level, pos, face)) {
                            SelectionPreviewManager.setSelection(pos, face);
                            return InteractionResult.SUCCESS;
                        }
                    }
                } else {
                    data.storage.get(pos).put(face, heldState);
                    ModComponents.WALLPAPER_DATA.sync(level);

                    SoundType blockSound = heldState.getSoundType();
                    level.playSound(null, pos, blockSound.getPlaceSound(), SoundSource.BLOCKS, 0.5F, 1.2F);
                    level.playSound(null, pos, ModSounds.WALLPAPER_PLACE.get(),
                            SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);

                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });
    }
}