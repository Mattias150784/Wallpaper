package net.mattias.wallpaper.core.util;

import net.mattias.wallpaper.WallpaperCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class WallpaperTags {

    private WallpaperTags() {}

    public static final TagKey<Block> BLACKLIST = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, "blacklist"));
}
