package net.mattias.wallpaper.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext", remap = false)
public abstract class MixinIndigoBlock {

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRender(BlockAndTintGetter world, BakedModel model, BlockState state, BlockPos pos, PoseStack matrixStack, VertexConsumer buffer, boolean cull, RandomSource random, long seed, int overlay, CallbackInfo ci) {

        Level level = null;
        if (world instanceof Level l) {
            level = l;
        } else if (world instanceof RenderChunkRegion rcr) {
            level = ((RenderChunkRegionAccessor) rcr).wallpaper$getLevel();
        }

        if (level == null) return;

        var component = ModComponents.WALLPAPER_DATA.getNullable(level);
        if (component == null) return;

        Map<Direction, BlockState> wallpapers = component.data.storage.get(pos.immutable());
        if (wallpapers == null || wallpapers.isEmpty()) return;

        for (Map.Entry<Direction, BlockState> entry : wallpapers.entrySet()) {
            Direction face = entry.getKey();
            BlockState wpState = entry.getValue();
            BakedModel wpModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(wpState);

            matrixStack.pushPose();
            float offset = 0.005f;
            matrixStack.translate(face.getStepX() * offset, face.getStepY() * offset, face.getStepZ() * offset);

            int light = LevelRenderer.getLightColor(world, wpState, pos.relative(face));
            float shade = world.getShade(face, true);

            wallpaper$renderQuads(matrixStack, buffer, wpModel.getQuads(wpState, face, random), face, light, shade);
            wallpaper$renderQuads(matrixStack, buffer, wpModel.getQuads(wpState, null, random), face, light, shade);

            matrixStack.popPose();
        }
    }

    @Unique
    private void wallpaper$renderQuads(PoseStack matrix, VertexConsumer buffer, List<BakedQuad> quads, Direction face, int light, float shade) {
        for (BakedQuad quad : quads) {
            if (quad.getDirection() == face || quad.getDirection() == null) {
                buffer.putBulkData(matrix.last(), quad, shade, shade, shade, light, OverlayTexture.NO_OVERLAY);
            }
        }
    }
}