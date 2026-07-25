package net.mattias.wallpaper.fabric.core.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SingleFaceModel implements BakedModel {
    private static final List<BakedQuad> EMPTY = List.of();

    private final BakedModel delegate;
    private final Direction face;

    public SingleFaceModel(BakedModel delegate, Direction face) {
        this.delegate = delegate;
        this.face = face;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        return side == face ? delegate.getQuads(state, side, rand) : EMPTY;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return delegate.getOverrides();
    }
}
