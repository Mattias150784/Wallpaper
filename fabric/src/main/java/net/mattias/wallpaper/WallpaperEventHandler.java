package net.mattias.wallpaper;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.config.WallpaperConfig;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.core.util.*;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.mattias.wallpaper.fabric.core.network.SelectionSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
                return InteractionResult.PASS;
            }

            if (!existingState.equals(defaultWallpaper)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                if (WallpaperConfig.multiPlacement && player.isCrouching()) {
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
                                    WallpaperLightUtil.refreshLight(lvl, blockPos);
                                });
                            }

                            @Override
                            public boolean hasEnoughItems(ServerPlayer p, int count) {
                                if (p.isCreative()) return true;
                                return ShulkerInventory.getTotalItemCount(p, stack.getItem()) >= count;
                            }

                            @Override
                            public void consumeItems(ServerPlayer p, int count) {
                                if (!WallpaperConfig.consumeItems || p.isCreative()) return;
                                ShulkerInventory.consumeItems(p, stack.getItem(), count);
                            }
                        };

                        MultiWallpaperPlacer.tryCompleteMultiPlacement(serverPlayer, level, pos, face, heldState, callback);
                        ServerPlayNetworking.send(serverPlayer, SelectionSyncPayload.clearing());
                        return InteractionResult.SUCCESS;
                    } else {
                        if (MultiWallpaperPlacer.tryStartMultiPlacement(serverPlayer, level, pos, face)) {
                            ServerPlayNetworking.send(serverPlayer, SelectionSyncPayload.selection(pos, face));
                            return InteractionResult.SUCCESS;
                        }
                    }
                } else {
                    data.storage.get(pos).put(face, heldState);
                    ModComponents.WALLPAPER_DATA.sync(level);
                    WallpaperLightUtil.refreshLight(level, pos);

                    SoundType blockSound = heldState.getSoundType();
                    level.playSound(null, pos, blockSound.getPlaceSound(), SoundSource.BLOCKS, 0.5F, 1.2F);
                    level.playSound(null, pos, ModSounds.WALLPAPER_PLACE.get(),
                            SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);

                    if (WallpaperConfig.consumeItems && !player.isCreative()) {
                        stack.shrink(1);
                    }
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    public static void registerChunkReseed() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            var componentOpt = ModComponents.WALLPAPER_DATA.maybeGet(world);
            if (componentOpt.isEmpty()) return;

            var storage = componentOpt.get().data.storage;
            if (storage.isEmpty()) return;

            ChunkPos chunkPos = chunk.getPos();

            storage.forEach((pos, faces) -> {
                if ((pos.getX() >> 4) != chunkPos.x || (pos.getZ() >> 4) != chunkPos.z) return;
                if (faces == null) return;

                for (BlockState state : faces.values()) {
                    if (state != null && state.getLightEmission() > 0) {
                        WallpaperLightUtil.refreshLight(world, pos);
                        break;
                    }
                }
            });
        });
    }
}
