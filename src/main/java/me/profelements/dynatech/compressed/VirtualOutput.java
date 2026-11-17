package me.profelements.dynatech.compressed;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * The virtual output of a compressed machine: results that do not fit into the output slots yet.
 * <p>
 * The machine moves it into its output slots every tick, so Cargo and Networks can take everything
 * through the normal output slots. It holds at most {@link #CAPACITY} items, and a machine only starts
 * an operation when its whole result fits, so nothing is ever lost. It is saved in the block data,
 * so it survives a server restart.
 */
public final class VirtualOutput {

    public static final int CAPACITY = 4096;

    private static final String ENTRY_SEPARATOR = ";";
    private static final String AMOUNT_SEPARATOR = ":";

    private final List<ItemStack> items = new ArrayList<>();
    private final List<Long> amounts = new ArrayList<>();
    private long total = 0;

    /**
     * The amount of items stored.
     */
    public synchronized long getTotal() {
        return total;
    }

    /**
     * The amount of items that can still be stored.
     */
    public synchronized long getFreeSpace() {
        return Math.max(0, CAPACITY - total);
    }

    public synchronized boolean isEmpty() {
        return total == 0;
    }

    /**
     * Store the {@code stacks}. The caller has checked that they fit.
     */
    public synchronized void add(@Nonnull ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            int index = indexOf(stack);
            if (index < 0) {
                items.add(stack.asOne());
                amounts.add((long) stack.getAmount());
            } else {
                amounts.set(index, amounts.get(index) + stack.getAmount());
            }
            total += stack.getAmount();
        }
    }

    /**
     * Move as much as possible into the output {@code slots}. Returns whether anything was moved.
     */
    public synchronized boolean flush(@Nonnull BlockMenu menu, @Nonnull int[] slots) {
        boolean moved = false;
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemStack item = items.get(i);
            long amount = Math.min(amounts.get(i), OutputUtils.freeSpace(menu, item, slots));
            if (amount <= 0) {
                continue;
            }

            for (ItemStack stack : OutputUtils.split(item, amount)) {
                menu.pushItem(stack, slots);
            }
            moved = true;
            total -= amount;

            long left = amounts.get(i) - amount;
            if (left <= 0) {
                items.remove(i);
                amounts.remove(i);
            } else {
                amounts.set(i, left);
            }
        }
        return moved;
    }

    /**
     * Take everything out, as valid stacks (used when the machine is broken).
     */
    @Nonnull
    public synchronized List<ItemStack> takeAll() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.addAll(OutputUtils.split(items.get(i), amounts.get(i)));
        }
        items.clear();
        amounts.clear();
        total = 0;
        return result;
    }

    private int indexOf(@Nonnull ItemStack stack) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isSimilar(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Save as a string for the block data: "amount:base64 item;amount:base64 item".
     */
    @Nonnull
    public synchronized String serialize() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (builder.length() > 0) {
                builder.append(ENTRY_SEPARATOR);
            }
            builder.append(amounts.get(i)).append(AMOUNT_SEPARATOR)
                    .append(Base64.getEncoder().encodeToString(items.get(i).serializeAsBytes()));
        }
        return builder.toString();
    }

    /**
     * Load from the block data. Entries that cannot be read are skipped.
     */
    @Nonnull
    public static VirtualOutput deserialize(@Nullable String data) {
        VirtualOutput output = new VirtualOutput();
        if (data == null || data.isEmpty()) {
            return output;
        }

        for (String entry : data.split(ENTRY_SEPARATOR)) {
            int index = entry.indexOf(AMOUNT_SEPARATOR);
            if (index <= 0) {
                continue;
            }
            try {
                long amount = Long.parseLong(entry.substring(0, index));
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(entry.substring(index + 1)));
                if (amount > 0 && !item.getType().isAir()) {
                    output.items.add(item.asOne());
                    output.amounts.add(amount);
                    output.total += amount;
                }
            } catch (RuntimeException e) {
                // a broken entry (e.g. an item of a removed addon) is skipped
            }
        }
        return output;
    }
}
