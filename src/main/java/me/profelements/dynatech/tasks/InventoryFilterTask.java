package me.profelements.dynatech.tasks;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import me.profelements.dynatech.items.tools.InventoryFilter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodically collects the contents of every Inventory Filter a player carries.
 * <p>
 * Runs on the main thread (it reads player inventories). The backpack contents are loaded
 * asynchronously by Slimefun, so the results are built in fresh sets and only swapped in
 * once every filter of that player has been read. The listener therefore never sees a
 * half-filled or emptied filter.
 */
public class InventoryFilterTask implements Runnable {

    private static final Map<UUID, Set<String>> SF_ITEMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<ItemStack>> OTHER_ITEMS = new ConcurrentHashMap<>();

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            List<ItemStack> filters = new ArrayList<>();

            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()
                        && SlimefunItem.getByItem(item) instanceof InventoryFilter) {
                    filters.add(item);
                }
            }

            if (filters.isEmpty()) {
                SF_ITEMS.remove(uuid);
                OTHER_ITEMS.remove(uuid);
                continue;
            }

            Set<String> sfItems = ConcurrentHashMap.newKeySet();
            Set<ItemStack> otherItems = ConcurrentHashMap.newKeySet();
            AtomicInteger pending = new AtomicInteger(filters.size());

            for (ItemStack filter : filters) {
                PlayerBackpack.getAsync(filter, backpack -> {
                    for (ItemStack stack : backpack.getInventory().getContents()) {
                        if (stack == null || stack.getType().isAir()) {
                            continue;
                        }

                        SlimefunItem sfItem = SlimefunItem.getByItem(stack);
                        if (sfItem != null) {
                            sfItems.add(sfItem.getId());
                        } else {
                            otherItems.add(stack.asOne());
                        }
                    }

                    if (pending.decrementAndGet() == 0) {
                        SF_ITEMS.put(uuid, sfItems);
                        OTHER_ITEMS.put(uuid, otherItems);
                    }
                }, false);
            }
        }
    }

    @Nonnull
    public static Set<String> getSlimefunItems(@Nonnull UUID uuid) {
        return SF_ITEMS.getOrDefault(uuid, Collections.emptySet());
    }

    @Nonnull
    public static Set<ItemStack> getOtherItems(@Nonnull UUID uuid) {
        return OTHER_ITEMS.getOrDefault(uuid, Collections.emptySet());
    }

    public static void clear(@Nonnull UUID uuid) {
        SF_ITEMS.remove(uuid);
        OTHER_ITEMS.remove(uuid);
    }
}
