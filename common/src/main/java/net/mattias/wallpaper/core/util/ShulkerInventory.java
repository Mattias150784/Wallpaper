package net.mattias.wallpaper.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class ShulkerInventory {

    public static int getTotalItemCount(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() == item) {
                count += stack.getCount();
            } else if (isShulkerBox(stack)) {
                count += countItemInShulker(stack, item);
            }
        }
        return count;
    }

    public static void consumeItems(ServerPlayer player, Item item, int amount) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (remaining <= 0) break;
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        if (remaining > 0) {
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                if (remaining <= 0) break;
                ItemStack stack = player.getInventory().items.get(i);
                if (isShulkerBox(stack)) {
                    remaining -= consumeItemFromShulker(stack, item, remaining);
                }
            }
        }
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem &&
                blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static int countItemInShulker(ItemStack shulkerStack, Item targetItem) {
        CompoundTag tag = shulkerStack.getTag();
        if (tag == null || !tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) return 0;
        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items", Tag.TAG_LIST)) return 0;

        ListTag items = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack contained = ItemStack.of(items.getCompound(i));
            if (!contained.isEmpty() && contained.getItem() == targetItem) {
                count += contained.getCount();
            }
        }
        return count;
    }

    private static int consumeItemFromShulker(ItemStack shulkerStack, Item targetItem, int maxAmount) {
        CompoundTag tag = shulkerStack.getTag();
        if (tag == null || !tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) return 0;
        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items", Tag.TAG_LIST)) return 0;

        ListTag items = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
        int consumed = 0;
        boolean modified = false;

        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            ItemStack contained = ItemStack.of(itemTag);

            if (!contained.isEmpty() && contained.getItem() == targetItem) {
                int toConsume = Math.min(contained.getCount(), maxAmount - consumed);
                contained.shrink(toConsume);
                consumed += toConsume;
                modified = true;

                if (contained.isEmpty()) {
                    items.remove(i);
                    i--;
                } else {
                    contained.save(itemTag);
                }

                if (consumed >= maxAmount) break;
            }
        }

        if (modified) {
            if (items.isEmpty()) blockEntityTag.remove("Items");
            if (blockEntityTag.isEmpty()) tag.remove("BlockEntityTag");
            if (tag.isEmpty()) shulkerStack.setTag(null);
        }

        return consumed;
    }
}