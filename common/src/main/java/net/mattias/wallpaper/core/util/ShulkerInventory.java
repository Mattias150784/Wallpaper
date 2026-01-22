package net.mattias.wallpaper.core.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

public class ShulkerInventory {

    public static int getTotalItemCount(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            if (stack.is(item)) {
                count += stack.getCount();
            } else {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    for (ItemStack innerStack : contents.nonEmptyItems()) {
                        if (innerStack.is(item)) {
                            count += innerStack.getCount();
                        }
                    }
                }
            }
        }
        return count;
    }

    public static void consumeItems(ServerPlayer player, Item item, int amount) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        if (remaining > 0) {
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null && !stack.is(item)) {
                    remaining -= consumeFromComponent(stack, contents, item, remaining);
                }
            }
        }
    }

    private static int consumeFromComponent(ItemStack shulkerStack, ItemContainerContents contents, Item targetItem, int maxAmount) {
        int consumed = 0;
        List<ItemStack> slots = new ArrayList<>();
        contents.stream().forEach(s -> slots.add(s.copy()));
        boolean changed = false;

        for (ItemStack innerStack : slots) {
            if (!innerStack.isEmpty() && innerStack.is(targetItem)) {
                int toConsume = Math.min(innerStack.getCount(), maxAmount - consumed);
                innerStack.shrink(toConsume);
                consumed += toConsume;
                changed = true;
                if (consumed >= maxAmount) break;
            }
        }

        if (changed) {
            shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        }

        return consumed;
    }
}