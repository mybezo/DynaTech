package me.profelements.dynatech.listeners;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.profelements.dynatech.DynaTech;
import me.profelements.dynatech.tasks.InventoryFilterTask;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Set;

public class InventoryFilterListener implements Listener {

    public InventoryFilterListener(@Nonnull DynaTech plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlayerAttemptPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            filterInventory(p, e);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        InventoryFilterTask.clear(e.getPlayer().getUniqueId());
    }

    private void filterInventory(Player p, EntityPickupItemEvent event) {
        Item itemEntity = event.getItem();
        ItemStack itemEntityStack = itemEntity.getItemStack();
        Set<ItemStack> otherItems = InventoryFilterTask.getOtherItems(p.getUniqueId());
        Set<String> sfItems = InventoryFilterTask.getSlimefunItems(p.getUniqueId());

        if (otherItems.isEmpty() && sfItems.isEmpty()) {
            return;
        }

        for (ItemStack checkStack : otherItems) {
            if (checkStack.isSimilar(itemEntityStack)) {
                itemEntity.remove();
                event.setCancelled(true);
                return;
            }
        }

        SlimefunItem item = SlimefunItem.getByItem(itemEntityStack);
        if (item != null && sfItems.contains(item.getId())) {
            itemEntity.remove();
            event.setCancelled(true);
        }
    }
}
