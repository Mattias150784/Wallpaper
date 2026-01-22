package net.mattias.wallpaper.neoforge;

import net.mattias.wallpaper.WallpaperCommon;
import net.mattias.wallpaper.core.ModItems;
import net.mattias.wallpaper.core.block.ModBlocks;
import net.mattias.wallpaper.core.sound.ModSounds;
import net.mattias.wallpaper.core.util.MultiWallpaperPlacer;
import net.mattias.wallpaper.core.util.ShulkerInventory;
import net.mattias.wallpaper.core.util.WallpaperRotationHandler;
import net.mattias.wallpaper.core.util.WallpaperValidation;
import net.mattias.wallpaper.neoforge.core.data.ForgeWallpaperData;
import net.mattias.wallpaper.neoforge.core.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = WallpaperCommon.MOD_ID)
public class WallpaperForgeEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ForgeWallpaperData storage = ForgeWallpaperData.get(player.level());
            if (storage != null) {
                ModMessages.sendToPlayer(new ModMessages.SyncWorldS2CPacket(
                        storage.save(new CompoundTag(), player.level().registryAccess())), player);
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

                level.playSound(null, pos, ModSounds.WALLPAPER_BREAK.get(),
                        SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);

                ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(pos, new CompoundTag()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
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

        ItemStack stack = event.getItemStack();
        boolean isHoldingBlock = stack.getItem() instanceof BlockItem;
        boolean isScraper = stack.getItem() == ModItems.WALLPAPER_SCRAPER.get();

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            if (level.isClientSide && !isScraper && (isHoldingBlock || event.getEntity().isCrouching())) {
                event.setUseBlock(TriState.FALSE);
                event.setUseItem(TriState.FALSE);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (isScraper) {
            return;
        }

        if (player.isCrouching() && !existing.equals(defaultWallpaper) && !isHoldingBlock) {
            WallpaperRotationHandler.WallpaperDataAccess dataAccess = new WallpaperRotationHandler.WallpaperDataAccess() {
                @Override
                public BlockState getWallpaper(Level lvl, BlockPos p, Direction f) {
                    var storage = ForgeWallpaperData.getData(lvl).storage;
                    if (!storage.containsKey(p)) return null;
                    if (!storage.get(p).containsKey(f)) return null;
                    return storage.get(p).get(f);
                }

                @Override
                public void setWallpaper(Level lvl, BlockPos p, Direction f, BlockState state) {
                    ForgeWallpaperData serverData = ForgeWallpaperData.get(lvl);
                    if (serverData != null) {
                        serverData.data.storage.get(p).put(f, state);
                        serverData.setDirty();
                    }
                }

                @Override
                public void sync(Level lvl, BlockPos p) {
                    ForgeWallpaperData serverData = ForgeWallpaperData.get(lvl);
                    if (serverData != null) {
                        ModMessages.sendToAll(new ModMessages.SyncBlockS2CPacket(p, serverData.saveBlock(p)));
                    }
                }

                @Override
                public BlockState getDefaultWallpaper() {
                    return ModBlocks.WALLPAPER_BLOCK.get().defaultBlockState();
                }
            };

            boolean rotated = WallpaperRotationHandler.tryRotate(level, pos, face, player, dataAccess);

            if (rotated) {
                event.setUseBlock(TriState.FALSE);
                event.setUseItem(TriState.FALSE);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        if (!existing.equals(defaultWallpaper)) {
            return;
        }

        BlockState heldState = blockItem.getBlock().defaultBlockState();

        if (!isValidWallpaperBlock(heldState, player)) {
            return;
        }

        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

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
                        return ShulkerInventory.getTotalItemCount(p, stack.getItem()) >= count;
                    }

                    @Override
                    public void consumeItems(ServerPlayer p, int count) {
                        if (p.isCreative()) return;
                        ShulkerInventory.consumeItems(p, stack.getItem(), count);
                    }
                };

                MultiWallpaperPlacer.tryCompleteMultiPlacement(player, level, pos, face, heldState, callback);
                ModMessages.sendToPlayer(new ModMessages.SelectionSyncPacket(BlockPos.ZERO, Direction.NORTH, true), player);
            } else {
                boolean started = MultiWallpaperPlacer.tryStartMultiPlacement(player, level, pos, face);
                if (started) {
                    ModMessages.sendToPlayer(new ModMessages.SelectionSyncPacket(pos, face, false), player);
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

    private static boolean isValidWallpaperBlock(BlockState state, ServerPlayer player) {
        boolean isValid = WallpaperValidation.isValidWallpaperBlock(state);

        if (!isValid && player != null) {
            if (state.getBlock() instanceof ShulkerBoxBlock ||
                    state.getBlock() instanceof BaseEntityBlock) {
            }
        }

        return isValid;
    }
}