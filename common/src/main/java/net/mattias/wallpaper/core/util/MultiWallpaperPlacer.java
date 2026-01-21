package net.mattias.wallpaper.core.util;

import net.mattias.wallpaper.core.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class MultiWallpaperPlacer {

    private static final Map<UUID, PendingPlacement> PENDING = new HashMap<>();
    private static final long SELECTION_TIMEOUT = 30000; // 30 seconds
    private static final int MAX_AREA = 400; // Safety limit

    public static class PendingPlacement {
        public final BlockPos firstPos;
        public final Direction face;
        public final long timestamp;

        public PendingPlacement(BlockPos pos, Direction face) {
            this.firstPos = pos;
            this.face = face;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > SELECTION_TIMEOUT;
        }
    }

    public static boolean tryStartMultiPlacement(ServerPlayer player, Level level,
                                                 BlockPos clickedPos, Direction face) {
        UUID playerId = player.getUUID();

        PENDING.entrySet().removeIf(e -> e.getValue().isExpired());

        PendingPlacement pending = PENDING.get(playerId);

        if (pending == null) {
            PENDING.put(playerId, new PendingPlacement(clickedPos, face));

            level.playSound(null, clickedPos, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS, 0.5F, 1.5F);

            player.displayClientMessage(
                    Component.literal("§aFirst corner selected! §7Sneak + right click another block on the same face."),
                    true
            );

            return true;
        }

        return false;
    }

    public static int tryCompleteMultiPlacement(ServerPlayer player, Level level,
                                                BlockPos clickedPos, Direction face,
                                                BlockState wallpaperState,
                                                PlacementCallback callback) {
        UUID playerId = player.getUUID();

        PENDING.entrySet().removeIf(e -> e.getValue().isExpired());

        PendingPlacement pending = PENDING.get(playerId);

        if (pending == null) {
            return -1;
        }

        if (pending.face != face) {
            player.displayClientMessage(
                    Component.literal("§cMust click the same face! §7Selection cleared."),
                    true
            );
            level.playSound(null, clickedPos, SoundEvents.ITEM_BREAK,
                    SoundSource.BLOCKS, 0.3F, 0.8F);
            PENDING.remove(playerId);
            return 0;
        }

        List<BlockPos> positions = getPositionsInBox(pending.firstPos, clickedPos, face);

        if (positions.size() > MAX_AREA) {
            player.displayClientMessage(
                    Component.literal("§cArea too large! §7Maximum " + MAX_AREA + " blocks. Selection cleared."),
                    true
            );
            level.playSound(null, clickedPos, SoundEvents.ITEM_BREAK,
                    SoundSource.BLOCKS, 0.3F, 0.8F);
            PENDING.remove(playerId);
            return 0;
        }

        int canPlace = 0;
        for (BlockPos pos : positions) {
            if (callback.canPlace(level, pos, face)) {
                canPlace++;
            }
        }

        if (!callback.hasEnoughItems(player, canPlace)) {
            player.displayClientMessage(
                    Component.literal("§cNot enough items! §7Need " + canPlace + " more."),
                    true
            );
            level.playSound(null, clickedPos, SoundEvents.ITEM_BREAK,
                    SoundSource.BLOCKS, 0.3F, 0.8F);
            PENDING.remove(playerId);
            return 0;
        }

        int placed = 0;
        for (BlockPos pos : positions) {
            if (callback.canPlace(level, pos, face)) {
                callback.placeWallpaper(level, pos, face, wallpaperState);
                placed++;
            }
        }

        callback.consumeItems(player, placed);

        level.playSound(null, clickedPos, ModSounds.WALLPAPER_PLACE.get(),
                SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);

        player.displayClientMessage(
                Component.literal("§aPlaced wallpaper on §e" + placed + " §ablocks!"),
                true
        );

        PENDING.remove(playerId);
        return placed;
    }

    private static List<BlockPos> getPositionsInBox(BlockPos pos1, BlockPos pos2, Direction face) {
        List<BlockPos> positions = new ArrayList<>();

        switch (face.getAxis()) {
            case X:
                int minY = Math.min(pos1.getY(), pos2.getY());
                int maxY = Math.max(pos1.getY(), pos2.getY());
                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());

                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        positions.add(new BlockPos(pos1.getX(), y, z));
                    }
                }
                break;

            case Y:
                int minX = Math.min(pos1.getX(), pos2.getX());
                int maxX = Math.max(pos1.getX(), pos2.getX());
                minZ = Math.min(pos1.getZ(), pos2.getZ());
                maxZ = Math.max(pos1.getZ(), pos2.getZ());

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        positions.add(new BlockPos(x, pos1.getY(), z));
                    }
                }
                break;

            case Z:
                minX = Math.min(pos1.getX(), pos2.getX());
                maxX = Math.max(pos1.getX(), pos2.getX());
                minY = Math.min(pos1.getY(), pos2.getY());
                maxY = Math.max(pos1.getY(), pos2.getY());

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        positions.add(new BlockPos(x, y, pos1.getZ()));
                    }
                }
                break;
        }

        return positions;
    }

    public static void cancelPlacement(UUID playerId) {
        PENDING.remove(playerId);
    }

    public static boolean hasPendingPlacement(UUID playerId) {
        PendingPlacement pending = PENDING.get(playerId);
        return pending != null && !pending.isExpired();
    }

    public interface PlacementCallback {
        boolean canPlace(Level level, BlockPos pos, Direction face);
        void placeWallpaper(Level level, BlockPos pos, Direction face, BlockState state);
        boolean hasEnoughItems(ServerPlayer player, int count);
        void consumeItems(ServerPlayer player, int count);
    }
}