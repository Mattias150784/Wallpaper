package net.mattias.wallpaper.neoforge.core.network;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.neoforge.core.data.ForgeWallpaperData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
                SyncWorldS2CPacket.TYPE,
                SyncWorldS2CPacket.STREAM_CODEC,
                SyncWorldS2CPacket::handle
        );

        registrar.playToClient(
                SyncBlockS2CPacket.TYPE,
                SyncBlockS2CPacket.STREAM_CODEC,
                SyncBlockS2CPacket::handle
        );
    }

    public record SyncWorldS2CPacket(CompoundTag data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncWorldS2CPacket> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, "sync_world"));

        public static final StreamCodec<FriendlyByteBuf, SyncWorldS2CPacket> STREAM_CODEC =
                StreamCodec.of(
                        (buf, packet) -> buf.writeNbt(packet.data),
                        buf -> new SyncWorldS2CPacket(buf.readNbt())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SyncWorldS2CPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    ForgeWallpaperData.setFullClientData(packet.data, level);
                    Minecraft.getInstance().levelRenderer.allChanged();
                }
            });
        }
    }

    public record SyncBlockS2CPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncBlockS2CPacket> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WallpaperCommon.MOD_ID, "sync_block"));

        public static final StreamCodec<FriendlyByteBuf, SyncBlockS2CPacket> STREAM_CODEC =
                StreamCodec.of(
                        (buf, packet) -> {
                            BlockPos.STREAM_CODEC.encode(buf, packet.pos);
                            buf.writeNbt(packet.data);
                        },
                        buf -> new SyncBlockS2CPacket(
                                BlockPos.STREAM_CODEC.decode(buf),
                                buf.readNbt()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SyncBlockS2CPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    ForgeWallpaperData.updateClientBlock(packet.pos, packet.data, level);

                    Minecraft.getInstance().levelRenderer.setBlocksDirty(
                            packet.pos.getX(), packet.pos.getY(), packet.pos.getZ(),
                            packet.pos.getX(), packet.pos.getY(), packet.pos.getZ()
                    );

                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                BlockPos checkPos = packet.pos.offset(x, y, z);
                                Minecraft.getInstance().levelRenderer.setBlocksDirty(
                                        checkPos.getX(), checkPos.getY(), checkPos.getZ(),
                                        checkPos.getX(), checkPos.getY(), checkPos.getZ()
                                );
                            }
                        }
                    }

                    level.getLightEngine().checkBlock(packet.pos);
                    for (Direction dir : Direction.values()) {
                        level.getLightEngine().checkBlock(packet.pos.relative(dir));
                    }

                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x != 0 || y != 0 || z != 0) {
                                    level.getLightEngine().checkBlock(packet.pos.offset(x, y, z));
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}