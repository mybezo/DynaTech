package me.profelements.dynatech.items.electric;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import me.profelements.dynatech.items.abstracts.AbstractElectricTicker;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WirelessCharger extends AbstractElectricTicker {

    private final double radius;

    public WirelessCharger(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, double radius) {
        super(itemGroup, item, recipeType, recipe);
        this.radius = radius;
    }

    @Override
    protected boolean checkTickPreconditions(Block b) {
        return true;
    }

    @Override
    protected void tick(Block b, SlimefunItem slimefunItem) {
        Location loc = b.getLocation();
        int energyPerCharge = getEnergyConsumption();

        if (getCharge(loc) < energyPerCharge) {
            return;
        }

        double radiusSquared = radius * radius;

        // Only players near the charger, instead of every player in the world.
        for (Player p : loc.getWorld().getNearbyPlayers(loc, radius)) {
            if (p.getLocation().distanceSquared(loc) > radiusSquared) {
                continue;
            }

            boolean charged = false;

            for (ItemStack item : p.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) {
                    continue;
                }

                if (SlimefunItem.getByItem(item) instanceof Rechargeable rcItem
                        && rcItem.getItemCharge(item) < rcItem.getMaxItemCharge(item)) {
                    if (getCharge(loc) < energyPerCharge) {
                        break;
                    }

                    removeCharge(loc, energyPerCharge);
                    rcItem.addItemCharge(item, energyPerCharge);
                    charged = true;
                }
            }

            if (charged) {
                p.updateInventory();
            }
        }
    }

    @Override
    protected boolean isSynchronized() {
        // Touches player inventories, so it must run on the main thread.
        return true;
    }
}
