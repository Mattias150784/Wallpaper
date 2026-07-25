package net.mattias.wallpaper.core.block;

import net.mattias.wallpaper.platform.Services;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import java.util.function.Supplier;

public class ModBlocks {
    public static final Supplier<Block> WALLPAPER_BLOCK = register("wallpaper_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));

    public static void init() {
    }

    private static <T extends Block> Supplier<T> register(String name, Supplier<T> block) {
        return Services.PLATFORM.registerBlock(name, block);
    }
}
