package net.mattias.wallpaper.fabric.core.data;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ClientTickingComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import net.mattias.wallpaper.core.data.WallpaperData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WallpaperComponent implements Component, AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public final WallpaperData data = new WallpaperData();
    private final Level level;

    private boolean needsRefresh = false;
    private int refreshDelay = 0;

    public WallpaperComponent(Level level) {
        this.level = level;
    }

    @Override
    public void serverTick() {
        tickRefresh();
    }

    @Override
    public void clientTick() {
        tickRefresh();
    }

    private void tickRefresh() {
        if (needsRefresh) {
            refreshDelay++;
            if (refreshDelay > 5) {
                refreshAllLight();
                needsRefresh = false;
                refreshDelay = 0;
            }
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        Map<BlockPos, Map<Direction, BlockState>> tempStorage = new ConcurrentHashMap<>();

        ListTag list = tag.getList("Wallpapers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(entry.getCompound("Pos")).immutable();
            Direction dir = Direction.byName(entry.getString("Face"));

            BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), entry.getCompound("State"));

            if (dir != null && state != null) {
                tempStorage.computeIfAbsent(pos, k -> new ConcurrentHashMap<>()).put(dir, state);
            }
        }

        data.storage.clear();
        data.storage.putAll(tempStorage);

        this.needsRefresh = true;
        this.refreshDelay = 0;
    }

    public void refreshAllLight() {
        if (data.storage.isEmpty()) return;

        for (BlockPos pos : data.storage.keySet()) {
            level.getLightEngine().checkBlock(pos);


            for (Direction dir : Direction.values()) {
                level.getLightEngine().checkBlock(pos.relative(dir));
            }

            if (level.isClientSide) {
                Minecraft.getInstance().levelRenderer.setBlocksDirty(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX(), pos.getY(), pos.getZ()
                );
            }
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
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