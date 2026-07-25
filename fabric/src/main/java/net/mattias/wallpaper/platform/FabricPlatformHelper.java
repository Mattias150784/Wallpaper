package net.mattias.wallpaper.platform;

import net.mattias.wallpaper.fabric.core.data.ModComponents;
import net.mattias.wallpaper.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.mattias.wallpaper.WallpaperCommon;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override public String getPlatformName() { return "Fabric"; }
    @Override public boolean isModLoaded(String modId) { return FabricLoader.getInstance().isModLoaded(modId); }
    @Override public boolean isDevelopmentEnvironment() { return FabricLoader.getInstance().isDevelopmentEnvironment(); }

    @Override public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        T registered = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, name), item.get());
        return () -> registered;
    }

    @Override public <T extends CreativeModeTab> Supplier<T> registerCreativeTab(String name, Supplier<T> tab) {
        T registered = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, name), tab.get());
        return () -> registered;
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        T registeredBlock = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, name), block.get());
        return () -> registeredBlock;
    }

    @Override
    public <T extends SoundEvent> Supplier<T> registerSound(String name, Supplier<T> sound) {
        T registered = Registry.register(BuiltInRegistries.SOUND_EVENT, ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, name), sound.get());return () -> registered;
    }

    @Override
    public void addWallpaper(Level level, BlockPos pos, Direction side, BlockState state) {
        ModComponents.WALLPAPER_DATA.maybeGet(level).ifPresent(component -> {
            component.data.storage.computeIfAbsent(pos.immutable(), k -> new java.util.concurrent.ConcurrentHashMap<>()).put(side, state);
        });
    }

    @Override
    public void syncWallpaper(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            ModComponents.WALLPAPER_DATA.sync(level);

            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }

        level.getLightEngine().checkBlock(pos);
        for (Direction dir : Direction.values()) {
            level.getLightEngine().checkBlock(pos.relative(dir));
        }
    }

    @Override
    public BlockState getWallpaper(Level level, BlockPos pos, Direction side) {
        return ModComponents.WALLPAPER_DATA.maybeGet(level)
                .map(c -> c.data.storage.get(pos))
                .map(faces -> faces.get(side))
                .orElse(null);
    }

    @Override
    public void removeWallpaper(Level level, BlockPos pos, Direction side) {
        ModComponents.WALLPAPER_DATA.maybeGet(level).ifPresent(component -> {
            var faces = component.data.storage.get(pos);
            if (faces != null) {
                faces.remove(side);
                if (faces.isEmpty()) {
                    component.data.storage.remove(pos);
                }
            }
        });
    }
}
