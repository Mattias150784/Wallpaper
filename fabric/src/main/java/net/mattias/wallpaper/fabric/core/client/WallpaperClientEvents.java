package net.mattias.wallpaper.fabric.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.mattias.wallpaper.core.config.WallpaperConfig;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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

            ModelBlockRenderer modelRenderer = mc.getBlockRenderer().getModelRenderer();
            RandomSource random = RandomSource.create();

            for (Map.Entry<BlockPos, Map<Direction, BlockState>> entry : wallpaperStorage.entrySet()) {
                BlockPos pos = entry.getKey();
                Map<Direction, BlockState> wallpapers = entry.getValue();

                if (wallpapers == null || wallpapers.isEmpty()) continue;

                double dx = pos.getX() + 0.5 - camX;
                double dy = pos.getY() + 0.5 - camY;
                double dz = pos.getZ() + 0.5 - camZ;
                if (!WallpaperConfig.isWithinRenderDistance(dx * dx + dy * dy + dz * dz)) continue;

                for (Map.Entry<Direction, BlockState> wpEntry : wallpapers.entrySet()) {
                    Direction face = wpEntry.getKey();
                    BlockState wpState = wpEntry.getValue();

                    if (wpState == null) continue;

                    BakedModel fullModel = mc.getBlockRenderer().getBlockModel(wpState);
                    BakedModel faceModel = new SingleFaceModel(fullModel, face);

                    VertexConsumer buffer = bufferSource.getBuffer(WallpaperRenderTypes.forState(wpState));

                    poseStack.pushPose();
                    poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                    modelRenderer.tesselateBlock(
                            level, faceModel, wpState, pos, poseStack, buffer,
                            false, random, wpState.getSeed(pos), OverlayTexture.NO_OVERLAY);

                    poseStack.popPose();
                }
            }

            poseStack.popPose();
            bufferSource.endBatch();
        });
    }
}
