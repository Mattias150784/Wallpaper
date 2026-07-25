package net.mattias.wallpaper.neoforge.mixin;

import net.mattias.wallpaper.core.sound.ModSounds;

import net.mattias.wallpaper.neoforge.core.data.ForgeWallpaperData;
import net.mattias.wallpaper.neoforge.core.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void onBlockChange(BlockPos pos, BlockState newState, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;

        BlockState currentState = level.getBlockState(pos);

        if (!currentState.isAir() && (newState.isAir() || !newState.is(currentState.getBlock()))) {
            if (!level.isClientSide) {
                ForgeWallpaperData storage = ForgeWallpaperData.get(level);
                if (storage != null) {
                    var removed = storage.data.storage.remove(pos);
                    if (removed != null) {
                        storage.setDirty();

                        level.playSound(null, pos, ModSounds.WALLPAPER_BREAK.get(),
                                SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);

                        ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(pos, new CompoundTag()));
                    }
                }
            }
        }
    }
}
