package net.mattias.wallpaper.core.util;

import net.mattias.wallpaper.core.config.WallpaperConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class WallpaperVeinMiner {

    public interface WallpaperAccessor {
        BlockState getWallpaper(Level level, BlockPos pos, Direction face);
        void removeWallpaper(Level level, BlockPos pos, Direction face);
    }

    public static class VeinMineResult {
        public final List<BlockPos> positions;
        public final Direction face;
        public final int count;

        public VeinMineResult(List<BlockPos> positions, Direction face) {
            this.positions = positions;
            this.face = face;
            this.count = positions.size();
        }
    }

    public static VeinMineResult findConnectedWallpapers(Level level, BlockPos startPos,
                                                         Direction face, BlockState targetState,
                                                         WallpaperAccessor accessor) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && found.size() < WallpaperConfig.maxVeinMineBlocks) {
            BlockPos current = queue.poll();

            BlockState currentState = accessor.getWallpaper(level, current, face);
            if (currentState == null || !isSameWallpaper(currentState, targetState)) {
                continue;
            }

            found.add(current);

            for (BlockPos neighbor : getAdjacentPositions(current, face)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return new VeinMineResult(found, face);
    }

    private static List<BlockPos> getAdjacentPositions(BlockPos pos, Direction face) {
        List<BlockPos> adjacent = new ArrayList<>();

        switch (face.getAxis()) {
            case X:
                adjacent.add(pos.above());
                adjacent.add(pos.below());
                adjacent.add(pos.north());
                adjacent.add(pos.south());
                break;
            case Y:
                adjacent.add(pos.north());
                adjacent.add(pos.south());
                adjacent.add(pos.east());
                adjacent.add(pos.west());
                break;
            case Z:
                adjacent.add(pos.above());
                adjacent.add(pos.below());
                adjacent.add(pos.east());
                adjacent.add(pos.west());
                break;
        }

        return adjacent;
    }

    private static boolean isSameWallpaper(BlockState state1, BlockState state2) {
        return state1.getBlock() == state2.getBlock();
    }

    public static void removeAllWallpapers(Level level, VeinMineResult result,
                                           WallpaperAccessor accessor) {
        for (BlockPos pos : result.positions) {
            accessor.removeWallpaper(level, pos, result.face);
        }
    }
}
