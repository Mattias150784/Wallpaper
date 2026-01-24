package net.mattias.wallpaper.fabric.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Block.class)
@Environment(EnvType.CLIENT)
public abstract class MixinBlock {
    @Inject(method = "shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private static void shouldRenderFaceHook(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction, BlockPos blockPos2, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        var componentOpt = ModComponents.WALLPAPER_DATA.maybeGet(level);
        if (componentOpt.isEmpty()) return;

        var wallpaperStorage = componentOpt.get().data.storage;
        if (wallpaperStorage.isEmpty()) return;

        for (Map.Entry<BlockPos, Map<Direction, BlockState>> entry : wallpaperStorage.entrySet()) {
            Map<Direction, BlockState> wallpapers = entry.getValue();

            BlockPos pos = entry.getKey();

            if (!blockPos.equals(pos)) continue;

            if (wallpapers == null || wallpapers.isEmpty()) continue;

            for (Map.Entry<Direction, BlockState> wpEntry : wallpapers.entrySet()) {
                Direction face = wpEntry.getKey();
                BlockState wpState = wpEntry.getValue();

                if (wpState == null) continue;

                if (direction == face) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
