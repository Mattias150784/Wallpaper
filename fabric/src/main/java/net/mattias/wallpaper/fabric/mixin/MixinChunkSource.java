package net.mattias.wallpaper.fabric.mixin;

import net.mattias.wallpaper.fabric.LightChunkGetterExtension;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ServerChunkCache.class, ClientChunkCache.class})
public abstract class MixinChunkSource implements LightChunkGetter, LightChunkGetterExtension {
    @Override
    public Level wallpaper$getLevel() {
        Object self = this;

        if (self instanceof ServerChunkCache scc) {
            return scc.getLevel();
        } else if (self instanceof ClientChunkCache ccc) {
            return (Level) ccc.getLevel();
        }
        return null;
    }
}