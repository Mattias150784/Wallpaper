package net.mattias.wallpaper.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelBlockRenderer.class)
public abstract class MixinModelBlockRenderer {

    @Inject(method = "tesselateWithAO", at = @At("TAIL"), remap = true)
    private void renderWallpaperAO(BlockAndTintGetter world, BakedModel model, BlockState state, BlockPos pos, PoseStack matrix, VertexConsumer buffer, boolean checkSides, RandomSource random, long seed, int overlay, CallbackInfo ci) {
        renderWallpaperInternal(world, state, pos, matrix, buffer, random, overlay);
    }

    @Inject(method = "tesselateWithoutAO", at = @At("TAIL"), remap = true)
    private void renderWallpaperNoAO(BlockAndTintGetter world, BakedModel model, BlockState state, BlockPos pos, PoseStack matrix, VertexConsumer buffer, boolean checkSides, RandomSource random, long seed, int overlay, CallbackInfo ci) {
        renderWallpaperInternal(world, state, pos, matrix, buffer, random, overlay);
    }

    @Unique
    private void renderWallpaperInternal(BlockAndTintGetter world, BlockState state, BlockPos pos, PoseStack matrix, VertexConsumer buffer, RandomSource random, int overlay) {
        Level level = null;
        if (world instanceof Level l) {
            level = l;
        } else if (world instanceof RenderChunkRegion rcr) {
            level = ((RenderChunkRegionAccessor) rcr).wallpaper$getLevel();
        }

        if (level == null) return;

        var component = ModComponents.WALLPAPER_DATA.getNullable(level);
        if (component == null || component.data.storage.isEmpty()) return;

        Map<Direction, BlockState> wallpapers = component.data.storage.get(pos.immutable());
        if (wallpapers == null || wallpapers.isEmpty()) return;

        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        var blockColors = Minecraft.getInstance().getBlockColors();

        for (Map.Entry<Direction, BlockState> entry : wallpapers.entrySet()) {
            Direction face = entry.getKey();
            BlockState wpState = entry.getValue();
            BakedModel wpModel = blockRenderer.getBlockModel(wpState);

            matrix.pushPose();
            float offset = 0.005f;
            matrix.translate(face.getStepX() * offset, face.getStepY() * offset, face.getStepZ() * offset);

            int light = LevelRenderer.getLightColor(world, wpState, pos.relative(face));

            renderWPQuads(world, wpState, pos, matrix, buffer, wpModel.getQuads(wpState, face, random), light, face, blockColors);
            renderWPQuads(world, wpState, pos, matrix, buffer, wpModel.getQuads(wpState, null, random), light, face, blockColors);

            matrix.popPose();
        }
    }

    @Unique
    private void renderWPQuads(BlockAndTintGetter world, BlockState state, BlockPos pos, PoseStack matrix, VertexConsumer buffer, List<BakedQuad> quads, int light, Direction face, BlockColors blockColors) {
        for (BakedQuad quad : quads) {
            if (quad.getDirection() == face) {

                float shade = world.getShade(face, quad.isShade());

                float r = shade, g = shade, b = shade;

                if (quad.isTinted()) {
                    int color = blockColors.getColor(state, world, pos, quad.getTintIndex());
                    r *= (float) (color >> 16 & 255) / 255.0F;
                    g *= (float) (color >> 8 & 255) / 255.0F;
                    b *= (float) (color & 255) / 255.0F;
                }

                buffer.putBulkData(matrix.last(), quad, r, g, b, light, OverlayTexture.NO_OVERLAY);
            }
        }
    }
}