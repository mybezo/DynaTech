package me.profelements.dynatech.items.electric.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.profelements.dynatech.compressed.Compressible;
import me.profelements.dynatech.compressed.OutputUtils;
import me.profelements.dynatech.items.abstracts.AbstractElectricMachine;

public class MineralizedApiary extends AbstractElectricMachine implements Compressible {

    private static final int[] BACKGROUND_SLOTS = new int[] { 0, 1, 2, 3, 9, 18, 27, 36, 37, 38, 39 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 4, 5, 6, 7, 8, 13, 17, 26, 31, 35, 40, 41, 42, 43, 44};
    private static final int[] INPUT_BORDER_SLOTS = new int[] {10, 11, 12, 19, 21, 28, 29, 30}; 
    
    private static final int[] OUTPUT_SLOTS = new int[] { 14, 15, 16, 23, 24, 25, 32, 33, 34};

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.HONEYCOMB);
    private ItemStack material = new ItemStack(Material.AIR);


    private static final int RECIPE_SECONDS = 30;

    private static final ItemStack INFO_STACK = new CustomItemStack(Material.BEACON, "&fINFO", "&fProduction Time: " + RECIPE_SECONDS + "s");


    // the amount of machines combined into this one (compressed apiaries)
    private final int multiplier;

    // the amount produced by the current operation of each apiary
    private final Map<Location, Integer> activeOperations = new ConcurrentHashMap<>();

    public MineralizedApiary(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack material) {
        this(itemGroup, item, recipeType, recipe, material, 1);
    }

    public MineralizedApiary(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack material, int multiplier) {
        super(itemGroup, item, recipeType, recipe);

        this.material = material;
        this.multiplier = Math.max(1, multiplier);
    }

    @Override
    public int getMultiplier() {
        return multiplier;
    }

    /**
     * The material this apiary produces.
     */
    public ItemStack getMaterial() {
        return material.clone();
    }
        

    @Override
    public void postRegister() {
        registerRecipe(RECIPE_SECONDS, new ItemStack[] { this.material }, new ItemStack[] { this.material });
    }

    @Override
	public MachineRecipe findNextRecipe(BlockMenu menu) {
        // produce as much as fits into the output, up to the multiplier; the rest is not produced
        int amount = (int) Math.min(multiplier, OutputUtils.freeSpace(menu, this.material, getOutputSlots()));
        if (amount <= 0) {
            return null;
        }

        MachineRecipe base = recipes.get(0);
        activeOperations.put(menu.getLocation(), amount);
        if (amount == 1) {
            return base;
        }

        MachineRecipe result = new MachineRecipe(0, base.getInput(), OutputUtils.multiply(base.getOutput(), amount));
        result.setTicks(base.getTicks());
        return result;
    }

    @Override
    public int getEnergyConsumption(@Nonnull Location l) {
        // only charge for the amount being produced
        return (int) Math.min(Integer.MAX_VALUE, (long) getEnergyConsumption() * activeOperations.getOrDefault(l, 1));
    }

    @Override
    protected void onNewInstance(BlockMenu menu, Block b) {
        if (multiplier > 1) {
            menu.replaceExistingItem(20, new CustomItemStack(Material.BEACON, "&fINFO",
                    "&fProduction Time: " + RECIPE_SECONDS + "s",
                    "&fAmount: up to " + multiplier));
        }
    }

    @Override
    protected void onBreak(BlockBreakEvent e, BlockMenu menu, Location l) {
        super.onBreak(e, menu, l);
        activeOperations.remove(l);
    }


	@Override
	public List<ItemStack> getDisplayRecipes() {
	    List<ItemStack> display = new ArrayList<>();

        if (multiplier > 1) {
            display.add(Compressible.multiplierInfo(multiplier));
            display.add(null);
        }

        display.add(this.material);
        display.add(this.material);

        return display;
	}

	@Override
	protected ItemStack getProgressBar() {
		return PROGRESS_ITEM;
	}

	@Override
	protected void setupMenu(BlockMenuPreset preset) {
	    preset.drawBackground(ChestMenuUtils.getBackground(), BACKGROUND_SLOTS);
        preset.drawBackground(ChestMenuUtils.getInputSlotTexture(), INPUT_BORDER_SLOTS);
        preset.drawBackground(ChestMenuUtils.getOutputSlotTexture(), OUTPUT_BORDER_SLOTS);

        preset.addItem(getProgressSlot(), new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(20, INFO_STACK, ChestMenuUtils.getEmptyClickHandler());
 
    }

	@Override
	protected int[] getInputSlots() {
		return new int[] {};
	}

	@Override
	protected int[] getOutputSlots() {
		return OUTPUT_SLOTS;
	}
    
    @Override
    public boolean isSynchronized() {
        return true;
    }
}
