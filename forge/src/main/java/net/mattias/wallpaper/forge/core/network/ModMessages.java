package net.mattias.wallpaper.forge.core.network;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.forge.core.data.ForgeWallpaperData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(WallpaperCommon.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE.messageBuilder(SyncWorldS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncWorldS2CPacket::new).encoder(SyncWorldS2CPacket::toBytes)
                .consumerMainThread(SyncWorldS2CPacket::handle).add();

        INSTANCE.messageBuilder(SyncBlockS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncBlockS2CPacket::new).encoder(SyncBlockS2CPacket::toBytes)
                .consumerMainThread(SyncBlockS2CPacket::handle).add();
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(Object msg) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static class SyncWorldS2CPacket {
        private final CompoundTag data;

        public SyncWorldS2CPacket(CompoundTag data) {
            this.data = data;
        }

        public SyncWorldS2CPacket(FriendlyByteBuf buf) {
            this.data = buf.readNbt();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeNbt(data);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    ForgeWallpaperData.setFullClientData(data, level);
                    Minecraft.getInstance().levelRenderer.allChanged();
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SyncBlockS2CPacket {
        private final BlockPos pos;
        private final CompoundTag data;

        public SyncBlockS2CPacket(BlockPos pos, CompoundTag data) {
            this.pos = pos;
            this.data = data;
        }

        public SyncBlockS2CPacket(FriendlyByteBuf buf) {
            this.pos = buf.readBlockPos();
            this.data = buf.readNbt();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeNbt(data);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    ForgeWallpaperData.updateClientBlock(pos, data, level);

                    Minecraft.getInstance().levelRenderer.setBlocksDirty(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX(), pos.getY(), pos.getZ()
                    );

                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                BlockPos checkPos = pos.offset(x, y, z);
                                Minecraft.getInstance().levelRenderer.setBlocksDirty(
                                        checkPos.getX(), checkPos.getY(), checkPos.getZ(),
                                        checkPos.getX(), checkPos.getY(), checkPos.getZ()
                                );
                            }
                        }
                    }

                    level.getLightEngine().checkBlock(pos);
                    for (Direction dir : Direction.values()) {
                        level.getLightEngine().checkBlock(pos.relative(dir));
                    }

                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x != 0 || y != 0 || z != 0) {
                                    level.getLightEngine().checkBlock(pos.offset(x, y, z));
                                }
                            }
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}