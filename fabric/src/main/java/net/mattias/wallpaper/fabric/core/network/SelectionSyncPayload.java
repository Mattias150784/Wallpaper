package net.mattias.wallpaper.fabric.core.network;

import net.mattias.wallpaper.WallpaperCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectionSyncPayload(boolean clear, BlockPos pos, Direction face) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectionSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, "selection_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectionSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.clear);
                        buf.writeBlockPos(payload.pos);
                        buf.writeEnum(payload.face);
                    },
                    buf -> new SelectionSyncPayload(buf.readBoolean(), buf.readBlockPos(), buf.readEnum(Direction.class))
            );

    public static SelectionSyncPayload selection(BlockPos pos, Direction face) {
        return new SelectionSyncPayload(false, pos, face);
    }

    public static SelectionSyncPayload clearing() {
        return new SelectionSyncPayload(true, BlockPos.ZERO, Direction.NORTH);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
