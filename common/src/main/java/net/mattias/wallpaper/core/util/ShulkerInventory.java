package net.mattias.wallpaper.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class ShulkerInventory {

    public static int getTotalItemCount(ServerPlayer player, Item item) {
        int count = 0;

        for (ItemStack stack : player.getInventory().items) {
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

        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        if (remaining > 0) {
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
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
        if (tag == null || !tag.contains("BlockEntityTag")) {
            return 0;
        }

        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items")) {
            return 0;
        }

        ListTag items = blockEntityTag.getList("Items", 10);
        int count = 0;

        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            ItemStack contained = ItemStack.of(itemTag);

            if (contained.getItem() == targetItem) {
                count += contained.getCount();
            }
        }

        return count;
    }

    private static int consumeItemFromShulker(ItemStack shulkerStack, Item targetItem, int maxAmount) {
        CompoundTag tag = shulkerStack.getOrCreateTag();

        if (!tag.contains("BlockEntityTag")) {
            tag.put("BlockEntityTag", new CompoundTag());
        }

        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");

        if (!blockEntityTag.contains("Items")) {
            return 0;
        }

        ListTag items = blockEntityTag.getList("Items", 10);
        int consumed = 0;
        ListTag newItems = new ListTag();

        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            ItemStack contained = ItemStack.of(itemTag);

            if (contained.getItem() == targetItem && consumed < maxAmount) {
                int toConsume = Math.min(contained.getCount(), maxAmount - consumed);
                contained.shrink(toConsume);
                consumed += toConsume;
            }

            if (!contained.isEmpty()) {
                CompoundTag newItemTag = new CompoundTag();
                contained.save(newItemTag);
                newItems.add(newItemTag);
            }
        }

        if (newItems.isEmpty()) {
            blockEntityTag.remove("Items");
        } else {
            blockEntityTag.put("Items", newItems);
        }

        return consumed;
    }
}