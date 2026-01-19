package net.mattias.wallpaper;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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

                var componentOpt = ModComponents.WALLPAPER_DATA.maybeGet(level);
                if (componentOpt.isPresent()) {
                    var data = componentOpt.get().data;

                    if (data.storage.containsKey(pos) && data.storage.get(pos).containsKey(face)) {
                        BlockState existingState = data.storage.get(pos).get(face);
                        BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

                        if (!existingState.equals(defaultWallpaper)) {
                            return InteractionResult.PASS;
                        }

                        if (!level.isClientSide) {
                            data.storage.get(pos).put(face, heldState);
                            ModComponents.WALLPAPER_DATA.sync(level);
                        }

                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }
}