package net.mattias.wallpaper.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallpaperValidation {
    public static boolean isValidWallpaperBlock(BlockState state) {
        Block block = state.getBlock();

        if (state.is(WallpaperTags.BLACKLIST)) {
            return false;
        }

        if (block instanceof ShulkerBoxBlock) {
            return false;
        }

        if (block instanceof BaseEntityBlock) {
            return false;
        }

        if (block instanceof ChestBlock ||
                block instanceof BarrelBlock ||
                block instanceof HopperBlock ||
                block instanceof DispenserBlock ||
                block instanceof DropperBlock) {
            return false;
        }

        if (state.hasBlockEntity()) {
            return false;
        }

        VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (!shape.isEmpty()) {
            var bounds = shape.bounds();
            boolean isFullCube = bounds.minX == 0.0 && bounds.minY == 0.0 && bounds.minZ == 0.0
                    && bounds.maxX == 1.0 && bounds.maxY == 1.0 && bounds.maxZ == 1.0;
            if (!isFullCube) {
                return false;
            }
        }

        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
