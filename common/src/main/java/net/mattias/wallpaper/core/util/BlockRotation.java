package net.mattias.wallpaper.core.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public class BlockRotation {
    public static boolean canRotate(BlockState state) {
        if (state.hasProperty(BlockStateProperties.AXIS)) return true;
        if (state.hasProperty(BlockStateProperties.FACING)) return true;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return true;
        return false;
    }

    public static BlockState getNextRotation(BlockState state, Direction clickedFace) {
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return getNextAxisRotation(state, clickedFace);
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            return getNextFacingRotation(state, clickedFace);
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getNextHorizontalRotation(state);
        }

        return state;
    }

    private static BlockState getNextAxisRotation(BlockState state, Direction clickedFace) {
        Direction.Axis currentAxis = state.getValue(BlockStateProperties.AXIS);
        Direction.Axis nextAxis;

        List<Direction.Axis> validAxes = getValidAxesForFace(clickedFace);

        int currentIndex = validAxes.indexOf(currentAxis);
        if (currentIndex == -1) {
            nextAxis = validAxes.get(0);
        } else {
            nextAxis = validAxes.get((currentIndex + 1) % validAxes.size());
        }

        return state.setValue(BlockStateProperties.AXIS, nextAxis);
    }

    private static List<Direction.Axis> getValidAxesForFace(Direction face) {
        List<Direction.Axis> axes = new ArrayList<>();

        switch (face.getAxis()) {
            case X:
                axes.add(Direction.Axis.Y);
                axes.add(Direction.Axis.Z);
                axes.add(Direction.Axis.X);
                break;
            case Y:
                axes.add(Direction.Axis.X);
                axes.add(Direction.Axis.Z);
                axes.add(Direction.Axis.Y);
                break;
            case Z:
                axes.add(Direction.Axis.X);
                axes.add(Direction.Axis.Y);
                axes.add(Direction.Axis.Z);
                break;
        }

        return axes;
    }

    private static BlockState getNextFacingRotation(BlockState state, Direction clickedFace) {
        Direction currentFacing = state.getValue(BlockStateProperties.FACING);

        List<Direction> rotationOrder = getRotationOrderForFace(clickedFace);

        int currentIndex = rotationOrder.indexOf(currentFacing);
        Direction nextFacing = rotationOrder.get((currentIndex + 1) % rotationOrder.size());

        return state.setValue(BlockStateProperties.FACING, nextFacing);
    }

    private static List<Direction> getRotationOrderForFace(Direction face) {
        List<Direction> order = new ArrayList<>();

        switch (face) {
            case UP:
            case DOWN:
                order.add(Direction.NORTH);
                order.add(Direction.EAST);
                order.add(Direction.SOUTH);
                order.add(Direction.WEST);
                order.add(Direction.UP);
                order.add(Direction.DOWN);
                break;
            default:
                order.add(Direction.UP);
                order.add(Direction.DOWN);
                order.add(Direction.NORTH);
                order.add(Direction.SOUTH);
                order.add(Direction.EAST);
                order.add(Direction.WEST);
                break;
        }

        return order;
    }

    private static BlockState getNextHorizontalRotation(BlockState state) {
        Direction currentFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction nextFacing = currentFacing.getClockWise();
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, nextFacing);
    }
}
