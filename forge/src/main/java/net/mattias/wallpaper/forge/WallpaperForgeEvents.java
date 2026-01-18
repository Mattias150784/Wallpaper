package net.mattias.wallpaper.forge;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.forge.core.data.ForgeWallpaperData;
import net.mattias.wallpaper.forge.core.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WallpaperCommon.MOD_ID)
public class WallpaperForgeEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ForgeWallpaperData storage = ForgeWallpaperData.get(player.level());
            if (storage != null) {
                ModMessages.sendToPlayer(new ModMessages.SyncWorldS2CPacket(storage.save(new CompoundTag())), player);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        if (!level.isClientSide) {
            ForgeWallpaperData storage = ForgeWallpaperData.get(level);
            if (storage != null) {
                var removed = storage.data.storage.remove(pos);
                if (removed != null) {
                    storage.setDirty();

                    ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(pos, new CompoundTag()));
                }
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

                            ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(pos, serverData.saveBlock(pos)));
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