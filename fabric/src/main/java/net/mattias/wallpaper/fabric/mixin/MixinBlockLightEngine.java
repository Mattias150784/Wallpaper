package net.mattias.wallpaper.fabric.mixin;

import net.mattias.wallpaper.fabric.LightChunkGetterExtension;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(BlockLightEngine.class)
public abstract class MixinBlockLightEngine {

    @Inject(method = "getEmission", at = @At("HEAD"), cancellable = true)
    private void getWallpaperLight(long posLong, BlockState state, CallbackInfoReturnable<Integer> cir) {
        var accessor = (LightEngineAccessor) this;
        var chunkSource = accessor.getChunkSource();

        if (chunkSource instanceof LightChunkGetterExtension extension) {
            Level level = extension.wallpaper$getLevel();
            if (level == null) return;

            BlockPos currentPos = BlockPos.of(posLong);
            var component = ModComponents.WALLPAPER_DATA.getNullable(level);
            if (component == null || component.data.storage.isEmpty()) return;

            int maxLightFromWallpapers = 0;

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);

                Direction faceOnNeighbor = dir.getOpposite();

                Map<Direction, BlockState> neighborWallpapers = component.data.storage.get(neighborPos);
                if (neighborWallpapers != null) {
                    BlockState wpState = neighborWallpapers.get(faceOnNeighbor);
                    if (wpState != null) {
                        maxLightFromWallpapers = Math.max(maxLightFromWallpapers, wpState.getLightEmission());
                    }
                }
            }

            if (maxLightFromWallpapers > 0) {
                cir.setReturnValue(Math.max(maxLightFromWallpapers, state.getLightEmission()));
            }
        }
    }
}