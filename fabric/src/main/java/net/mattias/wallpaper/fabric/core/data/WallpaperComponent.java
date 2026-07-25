package net.mattias.wallpaper.fabric.core.data;

import net.mattias.wallpaper.core.data.WallpaperData;
import net.mattias.wallpaper.core.util.WallpaperLightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WallpaperComponent implements Component, AutoSyncedComponent {
    public final WallpaperData data = new WallpaperData();
    private final Level level;

    public WallpaperComponent(Level level) {
        this.level = level;
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider provider) {
        Map<BlockPos, Map<Direction, BlockState>> tempStorage = new ConcurrentHashMap<>();

        ListTag list = tag.getList("Wallpapers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(entry, "Pos").orElse(BlockPos.ZERO).immutable();
            Direction dir = Direction.byName(entry.getString("Face"));
            BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), entry.getCompound("State"));

            if (dir != null && state != null) {
                tempStorage.computeIfAbsent(pos, k -> new ConcurrentHashMap<>()).put(dir, state);
            }
        }

        data.storage.clear();
        data.storage.putAll(tempStorage);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        Set<BlockPos> emissiveBefore = collectEmissivePositions();

        AutoSyncedComponent.super.applySyncPacket(buf);

        if (level == null || !level.isClientSide) {
            return;
        }

        Set<BlockPos> touched = new HashSet<>(emissiveBefore);
        touched.addAll(collectEmissivePositions());
        for (BlockPos pos : touched) {
            WallpaperLightUtil.refreshLight(level, pos);
        }
    }

    private Set<BlockPos> collectEmissivePositions() {
        Set<BlockPos> result = new HashSet<>();
        data.storage.forEach((pos, faces) -> {
            for (BlockState state : faces.values()) {
                if (state != null && state.getLightEmission() > 0) {
                    result.add(pos);
                    break;
                }
            }
        });
        return result;
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        data.storage.forEach((pos, faces) -> {
            faces.forEach((dir, state) -> {
                CompoundTag entry = new CompoundTag();
                entry.put("Pos", NbtUtils.writeBlockPos(pos));
                entry.putString("Face", dir.getName());
                entry.put("State", NbtUtils.writeBlockState(state));
                list.add(entry);
            });
        });
        tag.put("Wallpapers", list);
    }
}
