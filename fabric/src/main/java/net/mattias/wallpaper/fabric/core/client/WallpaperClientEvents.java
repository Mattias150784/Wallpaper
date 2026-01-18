package net.mattias.wallpaper.fabric.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public class WallpaperClientEvents {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            var componentOpt = ModComponents.WALLPAPER_DATA.maybeGet(level);
            if (componentOpt.isEmpty()) return;

            var wallpaperStorage = componentOpt.get().data.storage;
            if (wallpaperStorage.isEmpty()) return;

            PoseStack poseStack = context.matrixStack();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            var camera = context.camera();
            double camX = camera.getPosition().x;
            double camY = camera.getPosition().y;
            double camZ = camera.getPosition().z;

            poseStack.pushPose();
            poseStack.translate(-camX, -camY, -camZ);

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());
            var blockRenderer = mc.getBlockRenderer();
            var blockColors = mc.getBlockColors();

            for (Map.Entry<BlockPos, Map<Direction, BlockState>> entry : wallpaperStorage.entrySet()) {
                BlockPos pos = entry.getKey();
                Map<Direction, BlockState> wallpapers = entry.getValue();

                if (wallpapers == null || wallpapers.isEmpty()) continue;

                for (Map.Entry<Direction, BlockState> wpEntry : wallpapers.entrySet()) {
                    Direction face = wpEntry.getKey();
                    BlockState wpState = wpEntry.getValue();

                    if (wpState == null) continue;

                    RandomSource random = RandomSource.create(pos.asLong());

                    BakedModel model = blockRenderer.getBlockModel(wpState);

                    poseStack.pushPose();
                    poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                    float offset = 0.0005f;
                    poseStack.translate(
                            face.getStepX() * offset,
                            face.getStepY() * offset,
                            face.getStepZ() * offset
                    );

                    BlockPos lightPos = pos.relative(face);
                    int packedLight = LevelRenderer.getLightColor(level, lightPos);

                    int overlay = 10;

                    random.setSeed(42L);

                    List<BakedQuad> faceQuads = model.getQuads(wpState, face, random);

                    renderQuads(poseStack, buffer, faceQuads, wpState, pos, packedLight, overlay, blockColors, level, face);

                    poseStack.popPose();
                }
            }

            poseStack.popPose();
            bufferSource.endBatch();
        });
    }

    private static void renderQuads(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                                    BlockState state, BlockPos pos, int packedLight, int overlay,
                                    BlockColors blockColors, Level level, Direction targetFace) {
        PoseStack.Pose pose = poseStack.last();

        for (BakedQuad quad : quads) {
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;

            if (quad.isTinted()) {
                int color = blockColors.getColor(state, level, pos, quad.getTintIndex());
                r = (float)(color >> 16 & 255) / 255.0F;
                g = (float)(color >> 8 & 255) / 255.0F;
                b = (float)(color & 255) / 255.0F;
            }

            float shade = getShadeForDirection(targetFace);
            r *= shade;
            g *= shade;
            b *= shade;

            buffer.putBulkData(pose, quad, r, g, b, packedLight, overlay);
        }
    }

    private static float getShadeForDirection(Direction direction) {
        switch (direction) {
            case DOWN:  return 0.5F;
            case UP:    return 1.0F;
            case NORTH:
            case SOUTH: return 0.8F;
            case WEST:
            case EAST:  return 0.6F;
            default:    return 1.0F;
        }
    }
}