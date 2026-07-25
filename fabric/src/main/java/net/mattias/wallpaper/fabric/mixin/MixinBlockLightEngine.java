package net.mattias.wallpaper.fabric.mixin;

import net.mattias.wallpaper.core.config.WallpaperConfig;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(BlockLightEngine.class)
public abstract class MixinBlockLightEngine {
    @Inject(method = "getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I", at = @At("RETURN"), cancellable = true)
    private void wallpaper$addWallpaperEmission(long packedPos, BlockState blockState, CallbackInfoReturnable<Integer> cir) {
        if (!WallpaperConfig.emissiveLighting) return;

        LightChunkGetter chunkSource = ((LightEngineAccessor) (Object) this).wallpaper$getChunkSource();
        if (chunkSource == null) return;

        BlockGetter blockGetter = chunkSource.getLevel();
        if (!(blockGetter instanceof Level level)) return;

        var componentOpt = ModComponents.WALLPAPER_DATA.maybeGet(level);
        if (componentOpt.isEmpty()) return;

        var storage = componentOpt.get().data.storage;
        if (storage.isEmpty()) return;

        int x = BlockPos.getX(packedPos);
        int y = BlockPos.getY(packedPos);
        int z = BlockPos.getZ(packedPos);

        int emission = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = new BlockPos(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
            Map<Direction, BlockState> faces = storage.get(neighbour);
            if (faces == null) continue;

            BlockState wallpaper = faces.get(dir.getOpposite());
            if (wallpaper != null) {
                int e = wallpaper.getLightEmission();
                if (e > emission) emission = e;
            }
        }

        if (emission > cir.getReturnValueI()) {
            cir.setReturnValue(emission);
        }
    }
}
