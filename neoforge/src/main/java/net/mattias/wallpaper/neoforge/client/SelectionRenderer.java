package net.mattias.wallpaper.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.core.util.SelectionPreviewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = WallpaperCommon.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class SelectionRenderer {
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (!SelectionPreviewManager.hasSelection()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        var hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        var blockHit = (BlockHitResult) hitResult;
        BlockPos secondCorner = blockHit.getBlockPos();
        Direction hitFace = blockHit.getDirection();

        if (hitFace != SelectionPreviewManager.getSelectedFace()) {
            return;
        }

        AABB previewBox = calculatePreviewBox(SelectionPreviewManager.getFirstCorner(), secondCorner, SelectionPreviewManager.getSelectedFace());

        if (previewBox == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Vec3 camPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lineConsumer, previewBox, 0.3f, 0.8f, 1.0f, 0.8f);

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private static AABB calculatePreviewBox(BlockPos pos1, BlockPos pos2, Direction face) {
        double minX, maxX, minY, maxY, minZ, maxZ;

        switch (face.getAxis()) {
            case X:
                minX = pos1.getX();
                maxX = pos1.getX() + 1;
                minY = Math.min(pos1.getY(), pos2.getY());
                maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
                minZ = Math.min(pos1.getZ(), pos2.getZ());
                maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
                break;

            case Y:
                minX = Math.min(pos1.getX(), pos2.getX());
                maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
                minY = pos1.getY();
                maxY = pos1.getY() + 1;
                minZ = Math.min(pos1.getZ(), pos2.getZ());
                maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
                break;

            case Z:
                minX = Math.min(pos1.getX(), pos2.getX());
                maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
                minY = Math.min(pos1.getY(), pos2.getY());
                maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
                minZ = pos1.getZ();
                maxZ = pos1.getZ() + 1;
                break;

            default:
                return null;
        }

        double offset = 0.002;
        minX -= offset;
        minY -= offset;
        minZ -= offset;
        maxX += offset;
        maxY += offset;
        maxZ += offset;

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
