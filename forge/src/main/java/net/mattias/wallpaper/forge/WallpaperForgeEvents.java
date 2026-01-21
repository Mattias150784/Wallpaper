package net.mattias.wallpaper.forge;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.core.util.MultiWallpaperPlacer;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.sounds.SoundSource;

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
            removeWallpaperAt(level, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockChange(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState oldState = event.getBlockSnapshot().getReplacedBlock();

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

                level.playSound(null, pos, ModSounds.WALLPAPER_BREAK.get(),
                        SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);

                ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(pos, new CompoundTag()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockPos pos = event.getPos();
        Direction face = event.getFace();
        if (face == null) return;

        Level level = event.getLevel();
        BlockState blockAtPos = level.getBlockState(pos);

        if (blockAtPos.isAir() || blockAtPos.canBeReplaced()) {
            return;
        }

        var storageMap = ForgeWallpaperData.getData(level).storage;
        if (!storageMap.containsKey(pos) || !storageMap.get(pos).containsKey(face)) {
            return;
        }

        BlockState existing = storageMap.get(pos).get(face);
        BlockState defaultWallpaper = ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();

        if (!existing.equals(defaultWallpaper)) {
            return;
        }

        BlockState heldState = blockItem.getBlock().defaultBlockState();

        if (!isValidWallpaperBlock(heldState)) {
            return;
        }

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (level.isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.isCrouching()) {
            if (MultiWallpaperPlacer.hasPendingPlacement(player.getUUID())) {
                MultiWallpaperPlacer.PlacementCallback callback = new MultiWallpaperPlacer.PlacementCallback() {
                    @Override
                    public boolean canPlace(Level level, BlockPos blockPos, Direction direction) {
                        var storage = ForgeWallpaperData.getData(level).storage;
                        if (!storage.containsKey(blockPos)) return false;
                        if (!storage.get(blockPos).containsKey(direction)) return false;

                        BlockState existingWallpaper = storage.get(blockPos).get(direction);
                        return existingWallpaper.equals(defaultWallpaper);
                    }

                    @Override
                    public void placeWallpaper(Level level, BlockPos blockPos, Direction direction, BlockState state) {
                        ForgeWallpaperData serverData = ForgeWallpaperData.get(level);
                        if (serverData != null) {
                            serverData.data.storage.get(blockPos).put(direction, state);
                            serverData.setDirty();
                            ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(blockPos, serverData.saveBlock(blockPos)));
                        }
                    }

                    @Override
                    public boolean hasEnoughItems(ServerPlayer p, int count) {
                        if (p.isCreative()) return true;
                        return getItemCount(p, stack.getItem()) >= count;
                    }

                    @Override
                    public void consumeItems(ServerPlayer p, int count) {
                        if (p.isCreative()) return;
                        shrinkPlayerItem(p, stack.getItem(), count);
                    }
                };

                int placed = MultiWallpaperPlacer.tryCompleteMultiPlacement(player, level, pos, face, heldState, callback);

                ModMessages.sendToPlayer(new ModMessages.SelectionSyncPacket(true), player);
            } else {
                boolean started = MultiWallpaperPlacer.tryStartMultiPlacement(player, level, pos, face);
                if (started) {
                    ModMessages.sendToPlayer(new ModMessages.SelectionSyncPacket(pos, face), player);
                }
            }
        } else {
            ForgeWallpaperData serverData = ForgeWallpaperData.get(level);
            if (serverData != null) {
                serverData.data.storage.get(pos).put(face, heldState);
                serverData.setDirty();

                SoundType blockSound = heldState.getSoundType();
                level.playSound(null, pos, blockSound.getPlaceSound(), SoundSource.BLOCKS, 0.5F, 1.2F);
                level.playSound(null, pos, ModSounds.WALLPAPER_PLACE.get(),
                        SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);

                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(pos, serverData.saveBlock(pos)));
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

    private static int getItemCount(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void shrinkPlayerItem(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        int remainingToConsume = count;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int amountInStack = stack.getCount();

                if (amountInStack <= remainingToConsume) {
                    remainingToConsume -= amountInStack;
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                } else {
                    stack.shrink(remainingToConsume);
                    remainingToConsume = 0;
                }
            }

            if (remainingToConsume <= 0) break;
        }
    }
}