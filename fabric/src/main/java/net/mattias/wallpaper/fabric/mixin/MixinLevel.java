package net.mattias.wallpaper.fabric.mixin;

import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void onBlockBreak(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;

        if (state.isAir()) {
            ModComponents.WALLPAPER_DATA.maybeGet(level).ifPresent(component -> {
                if (component.data.storage.remove(pos) != null) {
                    if (!level.isClientSide) {
                        ModComponents.WALLPAPER_DATA.sync(level);
                    }

                    level.getLightEngine().checkBlock(pos);
                    for (Direction dir : Direction.values()) {
                        level.getLightEngine().checkBlock(pos.relative(dir));
                    }
                }
            });
        }
    }
}
