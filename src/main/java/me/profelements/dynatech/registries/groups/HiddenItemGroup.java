package me.profelements.dynatech.registries.groups;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;

/**
 * An item group that never shows up in the guide.
 * Its items can still be crafted and viewed by clicking them in a recipe.
 */
public class HiddenItemGroup extends ItemGroup {

    public HiddenItemGroup(NamespacedKey key, ItemStack item) {
        super(key, item);
    }

    @Override
    public boolean isHidden(Player p) {
        return true;
    }

    @Override
    public boolean isVisible(Player p) {
        return false;
    }
}
