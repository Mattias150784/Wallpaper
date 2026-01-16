package net.mattias.wallpaper.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = {
        "net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext",
        "link.infra.indium.renderer.render.TerrainRenderContext"
}, remap = false)
public abstract class MixinIndigoTerrain {

    @Unique
    private BlockAndTintGetter wallpaper$capturedWorld;

    @Shadow(remap = false)
    protected abstract VertexConsumer getVertexConsumer(RenderType layer);

    @Inject(method = "prepare", at = @At("HEAD"), remap = false)
    private void onPrepare(
            @Coerce Object blockView,
            @Coerce Object chunkRenderer,
            @Coerce Object renderData,
            @Coerce Object builders,
            @Coerce Object initializedLayers,
            CallbackInfo ci) {
        if (blockView instanceof BlockAndTintGetter world) {
            this.wallpaper$capturedWorld = world;
        }
    }

    @Inject(method = "tessellateBlock", at = @At("TAIL"), remap = false)
    private void onTessellateBlock(
            @Coerce Object blockStateObj,
            @Coerce Object blockPosObj,
            @Coerce Object modelObj,
            @Coerce Object matrixStackObj,
            CallbackInfo ci) {

        if (this.wallpaper$capturedWorld == null) return;

        BlockState state = (BlockState) blockStateObj;
        BlockPos pos = (BlockPos) blockPosObj;
        PoseStack matrixStack = (PoseStack) matrixStackObj;

        Level level;
        if (this.wallpaper$capturedWorld instanceof Level l) {
            level = l;
        } else if (this.wallpaper$capturedWorld instanceof RenderChunkRegion rcr) {
            level = ((RenderChunkRegionAccessor) rcr).wallpaper$getLevel();
        } else {
            level = Minecraft.getInstance().level;
        }

        if (level == null) return;

        Map<Direction, BlockState> wallpapers = null;
        try {
            var component = ModComponents.WALLPAPER_DATA.getNullable(level);
            if (component != null) {
                wallpapers = component.data.storage.get(pos.immutable());
            }
        } catch (Exception e) {
            return;
        }

        if (wallpapers == null || wallpapers.isEmpty()) return;

        VertexConsumer buffer = getVertexConsumer(RenderType.cutout());
        if (buffer == null) return;

        long seed = state.getSeed(pos);
        RandomSource random = RandomSource.create();
        random.setSeed(seed);

        for (Map.Entry<Direction, BlockState> entry : wallpapers.entrySet()) {
            Direction face = entry.getKey();
            BlockState wpState = entry.getValue();

            BakedModel wpModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(wpState);

            matrixStack.pushPose();
            float offset = 0.005f;
            matrixStack.translate(face.getStepX() * offset, face.getStepY() * offset, face.getStepZ() * offset);

            int light = LevelRenderer.getLightColor(this.wallpaper$capturedWorld, wpState, pos.relative(face));
            float shade = this.wallpaper$capturedWorld.getShade(face, true);

            wallpaper$renderQuads(matrixStack, buffer, wpModel.getQuads(wpState, face, random), face, light, shade);
            wallpaper$renderQuads(matrixStack, buffer, wpModel.getQuads(wpState, null, random), face, light, shade);

            matrixStack.popPose();
        }
    }

    @Unique
    private void wallpaper$renderQuads(PoseStack matrix, VertexConsumer buffer, List<BakedQuad> quads, Direction face, int light, float shade) {
        if (quads == null) return;

        for (BakedQuad quad : quads) {
            if (quad.getDirection() == face) {
                buffer.putBulkData(matrix.last(), quad, shade, shade, shade, light, OverlayTexture.NO_OVERLAY);
            }
        }
    }
}