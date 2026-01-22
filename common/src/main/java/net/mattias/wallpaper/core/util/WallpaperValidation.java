package net.mattias.wallpaper.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallpaperValidation {

    public static boolean isValidWallpaperBlock(BlockState state) {
        Block block = state.getBlock();

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

        try {
            BlockEntity be = ((BaseEntityBlock) block).newBlockEntity(BlockPos.ZERO, state);
            if (be != null) {
                return false;
            }
        } catch (Exception e) {
        }

        VoxelShape shape = state.getCollisionShape(null, BlockPos.ZERO);
        if (!shape.isEmpty()) {
            var bounds = shape.bounds();
            boolean isFullCube = bounds.minX == 0.0 && bounds.minY == 0.0 && bounds.minZ == 0.0
                    && bounds.maxX == 1.0 && bounds.maxY == 1.0 && bounds.maxZ == 1.0;
            if (!isFullCube) {
                return false;
            }
        }

        return state.isCollisionShapeFullBlock(null, BlockPos.ZERO);
    }
}