package me.profelements.dynatech.compressed;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

/**
 * A machine that can have compressed versions: multiple machines combined into one.
 */
public interface Compressible {

    /**
     * The amount of machines combined into this one, 1 for a normal machine.
     */
    int getMultiplier();

    /**
     * The "Multiplier: xN" item shown on the guide page of a compressed machine.
     */
    @Nonnull
    static ItemStack multiplierInfo(int multiplier) {
        return new CustomItemStack(Material.COMPARATOR, "&bMultiplier: &ax" + multiplier,
                "", "&7Combines " + multiplier + " machines into one.");
    }
}
