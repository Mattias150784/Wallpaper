package net.mattias.wallpaper.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public interface IPlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();

    <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item);
    <T extends CreativeModeTab> Supplier<T> registerCreativeTab(String name, Supplier<T> tab);
    <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block);
    <T extends SoundEvent> Supplier<T> registerSound(String name, Supplier<T> sound);

    void addWallpaper(Level level, BlockPos pos, Direction side, BlockState state);

    void syncWallpaper(Level level, BlockPos pos);

    BlockState getWallpaper(Level level, BlockPos pos, Direction side);

    void removeWallpaper(Level level, BlockPos pos, Direction side);
}