package me.profelements.dynatech.compressed;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Helpers for machines that output more than one stack per operation.
 * Every stack they create is a valid stack (never larger than the max stack size).
 */
public final class OutputUtils {

    private OutputUtils() {}

    /**
     * Split {@code total} of {@code item} into valid stacks.
     */
    @Nonnull
    public static List<ItemStack> split(@Nonnull ItemStack item, long total) {
        List<ItemStack> result = new ArrayList<>();
        int max = Math.max(1, item.getMaxStackSize());
        long remaining = total;
        while (remaining > 0) {
            int amount = (int) Math.min(remaining, max);
            ItemStack stack = item.clone();
            stack.setAmount(amount);
            result.add(stack);
            remaining -= amount;
        }
        return result;
    }

    /**
     * Multiply every output by {@code times} and split them into valid stacks.
     */
    @Nonnull
    public static ItemStack[] multiply(@Nonnull ItemStack[] outputs, int times) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack output : outputs) {
            if (output != null && !output.getType().isAir()) {
                result.addAll(split(output, (long) output.getAmount() * times));
            }
        }
        return result.toArray(new ItemStack[0]);
    }

    /**
     * How many more of {@code item} fit into the {@code slots}.
     */
    public static long freeSpace(@Nonnull BlockMenu menu, @Nonnull ItemStack item, @Nonnull int[] slots) {
        int max = Math.max(1, item.getMaxStackSize());
        long space = 0;
        for (int slot : slots) {
            ItemStack current = menu.getItemInSlot(slot);
            if (current == null || current.getType().isAir()) {
                space += max;
            } else if (current.getAmount() < max && ItemUtils.canStack(current, item)) {
                space += max - current.getAmount();
            }
        }
        return space;
    }
}
