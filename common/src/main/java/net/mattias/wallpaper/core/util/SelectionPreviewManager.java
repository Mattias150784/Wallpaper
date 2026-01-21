package net.mattias.wallpaper.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class SelectionPreviewManager {
    private static BlockPos firstCorner = null;
    private static Direction selectedFace = null;

    public static void setSelection(BlockPos pos, Direction face) {
        firstCorner = pos;
        selectedFace = face;
    }

    public static void clearSelection() {
        firstCorner = null;
        selectedFace = null;
    }

    public static boolean hasSelection() {
        return firstCorner != null && selectedFace != null;
    }

    public static BlockPos getFirstCorner() {
        return firstCorner;
    }

    public static Direction getSelectedFace() {
        return selectedFace;
    }
}