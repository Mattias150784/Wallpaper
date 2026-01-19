package net.mattias.wallpaper.neoforge.core.data;

import net.mattias.wallpaper.core.data.WallpaperData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeWallpaperData extends SavedData {
    public final WallpaperData data = new WallpaperData();
    private final Level level;

    private static final WallpaperData CLIENT_CACHE = new WallpaperData();

    public ForgeWallpaperData(Level level) {
        this.level = level;
    }

    public static WallpaperData getData(Level level) {
        if (level.isClientSide) return CLIENT_CACHE;
        ForgeWallpaperData storage = get(level);
        return storage != null ? storage.data : new WallpaperData();
    }

    public static void updateClientBlock(BlockPos pos, CompoundTag facesTag, Level level) {
        if (facesTag.isEmpty()) {
            CLIENT_CACHE.storage.remove(pos);
        } else {
            Map<Direction, BlockState> faceMap = new ConcurrentHashMap<>();
            for (String key : facesTag.getAllKeys()) {
                Direction dir = Direction.byName(key);
                if (dir != null) {
                    CompoundTag stateTag = facesTag.getCompound(key);
                    BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), stateTag);
                    faceMap.put(dir, state);
                }
            }
            CLIENT_CACHE.storage.put(pos.immutable(), faceMap);
        }
    }

    public static void setFullClientData(CompoundTag tag, Level level) {
        CLIENT_CACHE.storage.clear();
        ForgeWallpaperData temp = new ForgeWallpaperData(level);
        temp.load(tag, level.registryAccess());
        CLIENT_CACHE.storage.putAll(temp.data.storage);
    }

    public static ForgeWallpaperData get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(
                            () -> new ForgeWallpaperData(level),
                            (tag, provider) -> {
                                ForgeWallpaperData d = new ForgeWallpaperData(level);
                                d.load(tag, provider);
                                return d;
                            }
                    ),
                    "wallpaper_data"
            );
        }
        return null;
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        data.storage.clear();
        ListTag list = tag.getList("Wallpapers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(entry, "Pos").orElse(BlockPos.ZERO);
            Direction dir = Direction.byName(entry.getString("Face"));
            BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), entry.getCompound("State"));
            if (dir != null && state != null) {
                data.storage.computeIfAbsent(pos.immutable(), k -> new ConcurrentHashMap<>()).put(dir, state);
            }
        }
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        data.storage.forEach((pos, faces) -> faces.forEach((dir, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("Pos", NbtUtils.writeBlockPos(pos));
            entry.putString("Face", dir.getName());
            entry.put("State", NbtUtils.writeBlockState(state));
            list.add(entry);
        }));
        tag.put("Wallpapers", list);
        return tag;
    }

    public CompoundTag saveBlock(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        Map<Direction, BlockState> faces = data.storage.get(pos);
        if (faces != null) {
            faces.forEach((dir, state) -> tag.put(dir.getName(), NbtUtils.writeBlockState(state)));
        }
        return tag;
    }
}