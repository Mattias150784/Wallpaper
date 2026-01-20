package net.mattias.wallpaper.neoforge;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.neoforge.core.data.ForgeWallpaperData;
import net.mattias.wallpaper.neoforge.core.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WallpaperCommon.MOD_ID)
public class WallpaperForgeEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ForgeWallpaperData storage = ForgeWallpaperData.get(player.level());
            if (storage != null) {
                PacketDistributor.sendToPlayer(player, new ModMessages.SyncWorldS2CPacket(storage.save(new CompoundTag(), player.level().registryAccess())));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        if (!level.isClientSide) {
            removeWallpaperAt(level, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockChange(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        BlockState oldState = event.getBlockSnapshot().getState();

        if (!level.isClientSide && !oldState.isAir()) {
            removeWallpaperAt(level, pos);
        }
    }

    private static void removeWallpaperAt(Level level, BlockPos pos) {
        ForgeWallpaperData storage = ForgeWallpaperData.get(level);
        if (storage != null) {
            var removed = storage.data.storage.remove(pos);
            if (removed != null) {
                storage.setDirty();

                level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);

                PacketDistributor.sendToAllPlayers(new ModMessages.SyncBlockS2CPacket(pos, new CompoundTag()));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof BlockItem blockItem) {
            BlockPos pos = event.getPos();
            Direction face = event.getFace();
            if (face == null) return;

            var storageMap = ForgeWallpaperData.getData(event.getLevel()).storage;
            if (storageMap.containsKey(pos) && storageMap.get(pos).containsKey(face)) {
                BlockState existing = storageMap.get(pos).get(face);
                BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

                if (existing.equals(defaultWallpaper)) {
                    BlockState heldState = blockItem.getBlock().defaultBlockState();

                    if (!isValidWallpaperBlock(heldState)) {
                        return;
                    }

                    if (!event.getLevel().isClientSide) {
                        ForgeWallpaperData serverData = ForgeWallpaperData.get(event.getLevel());
                        if (serverData != null) {
                            serverData.data.storage.get(pos).put(face, heldState);
                            serverData.setDirty();

                            SoundType blockSound = heldState.getSoundType();
                            event.getLevel().playSound(null, pos, blockSound.getPlaceSound(), SoundSource.BLOCKS, 0.5F, 1.2F);
                            event.getLevel().playSound(null, pos, SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.BLOCKS, 0.4F, 1.3F);

                            PacketDistributor.sendToAllPlayers(new ModMessages.SyncBlockS2CPacket(pos, serverData.saveBlock(pos)));
                        }
                    }
                    event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean isValidWallpaperBlock(BlockState state) {
        VoxelShape shape = state.getCollisionShape(null, BlockPos.ZERO);

        if (!shape.isEmpty()) {
            var bounds = shape.bounds();
            boolean isFullCube = bounds.minX == 0.0 && bounds.minY == 0.0 && bounds.minZ == 0.0
                    && bounds.maxX == 1.0 && bounds.maxY == 1.0 && bounds.maxZ == 1.0;
            if (!isFullCube) {
                return false;
            }
        }

        return state.isCollisionShapeFullBlock(null, BlockPos.ZERO);
    }
}