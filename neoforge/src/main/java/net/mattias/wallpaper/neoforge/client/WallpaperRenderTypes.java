package net.mattias.wallpaper.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;

public final class WallpaperRenderTypes extends RenderType {
    private WallpaperRenderTypes() {
        super("", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
        throw new AssertionError("WallpaperRenderTypes must not be instantiated");
    }

    private static final int BUFFER_SIZE = 786432;

    public static final RenderType WALLPAPER_SOLID = RenderType.create(
            "wallpaper_solid",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, BUFFER_SIZE,
            true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_SOLID_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(true));

    public static final RenderType WALLPAPER_CUTOUT = RenderType.create(
            "wallpaper_cutout",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, BUFFER_SIZE,
            true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_CUTOUT_MIPPED_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(true));

    public static final RenderType WALLPAPER_TRANSLUCENT = RenderType.create(
            "wallpaper_translucent",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, BUFFER_SIZE,
            true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(true));

    public static RenderType forState(BlockState state) {
        RenderType chunkType = ItemBlockRenderTypes.getChunkRenderType(state);
        if (chunkType == RenderType.translucent()) {
            return WALLPAPER_TRANSLUCENT;
        }
        if (chunkType == RenderType.cutout() || chunkType == RenderType.cutoutMipped()) {
            return WALLPAPER_CUTOUT;
        }
        return WALLPAPER_SOLID;
    }
}
