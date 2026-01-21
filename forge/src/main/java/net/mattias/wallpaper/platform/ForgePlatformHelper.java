package net.mattias.wallpaper.platform;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.forge.core.data.ForgeWallpaperData;
import net.mattias.wallpaper.forge.core.network.ModMessages;
import net.mattias.wallpaper.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class ForgePlatformHelper implements IPlatformHelper {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WallpaperCommon.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, WallpaperCommon.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WallpaperCommon.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WallpaperCommon.MOD_ID);

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        return ITEMS.register(name, item);
    }

    @Override
    public <T extends CreativeModeTab> Supplier<T> registerCreativeTab(String name, Supplier<T> tab) {
        return TABS.register(name, tab);
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    @Override
    public <T extends SoundEvent> Supplier<T> registerSound(String name, Supplier<T> sound) {
        return SOUND_EVENTS.register(name, sound);
    }

    @Override
    public void addWallpaper(Level level, BlockPos pos, Direction side, BlockState state) {
        if (level.isClientSide) return;
        ForgeWallpaperData storage = ForgeWallpaperData.get(level);
        if (storage != null) {
            storage.data.storage.computeIfAbsent(pos.immutable(), k -> new java.util.concurrent.ConcurrentHashMap<>()).put(side, state);
            storage.setDirty();
        }
    }

    @Override
    public void syncWallpaper(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            ForgeWallpaperData storage = ForgeWallpaperData.get(level);
            if (storage != null) {
                ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(pos, storage.saveBlock(pos)));
            }
        }
    }

    @Override
    public BlockState getWallpaper(Level level, BlockPos pos, Direction side) {
        var map = ForgeWallpaperData.getData(level).storage.get(pos);
        return map != null ? map.get(side) : null;
    }

    @Override
    public void removeWallpaper(Level level, BlockPos pos, Direction side) {
        if (level.isClientSide) return;
        ForgeWallpaperData storage = ForgeWallpaperData.get(level);
        if (storage != null) {
            var map = storage.data.storage.get(pos);
            if (map != null) {
                map.remove(side);
                if (map.isEmpty()) {
                    storage.data.storage.remove(pos);
                }
                storage.setDirty();

                syncWallpaper(level, pos);
            }
        }
    }
}