package net.mattias.wallpaper;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.util.MultiWallpaperPlacer;
import net.mattias.wallpaper.core.util.SelectionPreviewManager;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WallpaperEventHandler {
    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.getItem() instanceof BlockItem blockItem) {
                BlockPos pos = hitResult.getBlockPos();
                Direction face = hitResult.getDirection();
                BlockState heldState = blockItem.getBlock().defaultBlockState();

                if (!heldState.isCollisionShapeFullBlock(level, pos)) {
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
                                    return getItemCount(p, stack.getItem()) >= count;
                                }

                                @Override
                                public void consumeItems(ServerPlayer p, int count) {
                                    if (p.isCreative()) return;
                                    shrinkPlayerItem(p, stack.getItem(), count);
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

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                    }
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        });
    }

    private static int getItemCount(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void shrinkPlayerItem(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }
}