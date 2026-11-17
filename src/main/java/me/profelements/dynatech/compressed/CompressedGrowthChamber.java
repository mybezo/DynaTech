package me.profelements.dynatech.compressed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.profelements.dynatech.items.abstracts.AbstractElectricMachine;

/**
 * A compressed Growth Chamber (MK1 or MK2): {@link #getMultiplier()} chambers combined into one.
 * <p>
 * It uses the recipes and the speed of the original chamber. One operation processes as many
 * recipes at once as there are inputs for, up to the multiplier, across all the input slots.
 * The outputs are multiplied and split into valid stacks, and the energy is only charged for the
 * amount that is actually processed. Every compressed chamber uses the large MK2 menu.
 * <p>
 * The results go through a {@link VirtualOutput} of {@link VirtualOutput#CAPACITY} items, which refills the
 * output slots every tick. An operation only starts when its whole result fits into the virtual output.
 */
public class CompressedGrowthChamber extends AbstractElectricMachine implements Compressible {

    private static final int[] INPUT_SLOTS = new int[] { 1, 2, 3, 4, 5, 6, 7 };
    private static final int[] OUTPUT_SLOTS = new int[] {
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
            46, 47, 48, 49, 50, 51, 52 };
    private static final int[] INPUT_BORDER_SLOTS = new int[] { 0, 8, 9, 10, 11, 12, 14, 15, 16, 17 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] {
            18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 35, 36, 44, 45, 53 };
    private static final int PROGRESS_SLOT = 13;
    private static final int INFO_SLOT = 22;
    private static final String VIRTUAL_OUTPUT_KEY = "virtual-output";

    private final AbstractElectricMachine base;
    private final int multiplier;
    private final ItemStack progressItem;

    // the amount of recipes processed by the current operation of each machine
    private final Map<Location, Integer> activeOperations = new ConcurrentHashMap<>();

    // the results waiting for space in the output slots, loaded from the block data on first use
    private final Map<Location, VirtualOutput> virtualOutputs = new ConcurrentHashMap<>();

    public CompressedGrowthChamber(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType,
            ItemStack[] recipe, @Nonnull AbstractElectricMachine base, int multiplier, @Nonnull ItemStack progressItem) {
        super(itemGroup, item, recipeType, recipe);

        this.base = base;
        this.multiplier = multiplier;
        this.progressItem = progressItem;
        // the parent constructor asked for the progress bar before this field was set
        getMachineProcessor().setProgressBar(progressItem);
    }

    @Override
    public int getMultiplier() {
        return multiplier;
    }

    /**
     * The recipes of the original chamber, including the ones added later by integrations.
     */
    @Override
    public List<MachineRecipe> getMachineRecipes() {
        return base.getMachineRecipes();
    }

    @Override
    public List<MachineRecipe> getRecipes() {
        return base.getMachineRecipes();
    }

    @Nullable
    @Override
    public MachineRecipe findNextRecipe(BlockMenu menu) {
        for (MachineRecipe recipe : base.getMachineRecipes()) {
            ItemStack[] inputs = recipe.getInput();

            // how many times the inputs are available, up to the multiplier
            int operations = multiplier;
            for (ItemStack input : inputs) {
                if (input == null || input.getType().isAir()) {
                    continue;
                }
                long available = countSimilar(menu, input);
                operations = (int) Math.min(operations, available / Math.max(1, input.getAmount()));
                if (operations == 0) {
                    break;
                }
            }
            if (operations == 0) {
                continue;
            }

            // only process what fits into the virtual output, so nothing is ever lost
            long itemsPerOperation = countItems(recipe.getOutput());
            if (itemsPerOperation > 0) {
                long room = getVirtualOutput(menu.getLocation()).getFreeSpace();
                operations = (int) Math.min(operations, room / itemsPerOperation);
            }
            if (operations == 0) {
                return null;
            }

            for (ItemStack input : inputs) {
                if (input != null && !input.getType().isAir()) {
                    consumeSimilar(menu, input, (long) Math.max(1, input.getAmount()) * operations);
                }
            }

            MachineRecipe result = new MachineRecipe(0, inputs, OutputUtils.multiply(recipe.getOutput(), operations));
            // same speed as the original chamber
            result.setTicks(recipe.getTicks());
            activeOperations.put(menu.getLocation(), operations);
            return result;
        }
        return null;
    }

    private static long countItems(@Nonnull ItemStack[] stacks) {
        long amount = 0;
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.getType().isAir()) {
                amount += stack.getAmount();
            }
        }
        return amount;
    }

    @Nonnull
    private VirtualOutput getVirtualOutput(@Nonnull Location l) {
        return virtualOutputs.computeIfAbsent(l,
                loc -> VirtualOutput.deserialize(StorageCacheUtils.getData(loc, VIRTUAL_OUTPUT_KEY)));
    }

    private void saveVirtualOutput(@Nonnull Location l, @Nonnull VirtualOutput output) {
        if (output.isEmpty()) {
            StorageCacheUtils.removeData(l, VIRTUAL_OUTPUT_KEY);
        } else {
            StorageCacheUtils.setData(l, VIRTUAL_OUTPUT_KEY, output.serialize());
        }
    }

    @Override
    protected void tick(BlockMenu menu, Block b) {
        // refill the output slots from the virtual output, so Cargo and Networks can take everything
        VirtualOutput output = getVirtualOutput(b.getLocation());
        if (!output.isEmpty() && output.flush(menu, getOutputSlots())) {
            saveVirtualOutput(b.getLocation(), output);
        }
        updateInfo(menu, output);

        super.tick(menu, b);
    }

    @Override
    protected void addOutputs(BlockMenu menu, Block b, ItemStack[] outputs) {
        // the results go into the virtual output first, then into the output slots
        VirtualOutput output = getVirtualOutput(b.getLocation());
        output.add(outputs);
        output.flush(menu, getOutputSlots());
        saveVirtualOutput(b.getLocation(), output);
        updateInfo(menu, output);
    }

    private static void updateInfo(@Nonnull BlockMenu menu, @Nonnull VirtualOutput output) {
        if (menu.toInventory().getViewers().isEmpty()) {
            return;
        }
        menu.replaceExistingItem(INFO_SLOT, infoItem(output.getTotal()));
    }

    @Nonnull
    private static ItemStack infoItem(long stored) {
        return new CustomItemStack(Material.ENDER_CHEST, "&bVirtual Output",
                "&7Stored: &e" + stored + " &7/ &e" + VirtualOutput.CAPACITY,
                "",
                "&7Results that do not fit into the output",
                "&7wait here and refill it automatically.",
                "&7Cargo and Networks can take them",
                "&7from the output slots.");
    }

    private long countSimilar(@Nonnull BlockMenu menu, @Nonnull ItemStack input) {
        long amount = 0;
        for (int slot : getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item != null && SlimefunUtils.isItemSimilar(item, input, true, false)) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    private void consumeSimilar(@Nonnull BlockMenu menu, @Nonnull ItemStack input, long amount) {
        long remaining = amount;
        for (int slot : getInputSlots()) {
            if (remaining <= 0) {
                return;
            }
            ItemStack item = menu.getItemInSlot(slot);
            if (item != null && SlimefunUtils.isItemSimilar(item, input, true, false)) {
                int taken = (int) Math.min(remaining, item.getAmount());
                menu.consumeItem(slot, taken);
                remaining -= taken;
            }
        }
    }

    @Override
    public int getEnergyConsumption(@Nonnull Location l) {
        // only charge for the amount being processed
        return (int) Math.min(Integer.MAX_VALUE, (long) getEnergyConsumption() * activeOperations.getOrDefault(l, 1));
    }

    @Override
    protected void onBreak(BlockBreakEvent e, BlockMenu menu, Location l) {
        super.onBreak(e, menu, l);
        activeOperations.remove(l);

        // drop what is still waiting in the virtual output (at most one capacity worth)
        VirtualOutput output = getVirtualOutput(l);
        for (ItemStack item : output.takeAll()) {
            l.getWorld().dropItemNaturally(l, item);
        }
        virtualOutputs.remove(l);
        StorageCacheUtils.removeData(l, VIRTUAL_OUTPUT_KEY);
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> display = new ArrayList<>();
        display.add(Compressible.multiplierInfo(multiplier));
        display.add(null);
        display.addAll(base.getDisplayRecipes());
        return display;
    }

    @Nonnull
    @Override
    protected ItemStack getProgressBar() {
        return progressItem;
    }

    @Override
    protected int getProgressSlot() {
        return PROGRESS_SLOT;
    }

    @Override
    protected int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    protected int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    @Override
    protected void setupMenu(BlockMenuPreset preset) {
        for (int slot : INPUT_BORDER_SLOTS) {
            preset.addItem(slot, ChestMenuUtils.getInputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int slot : OUTPUT_BORDER_SLOTS) {
            preset.addItem(slot, ChestMenuUtils.getOutputSlotTexture(), ChestMenuUtils.getEmptyClickHandler());
        }

        preset.addItem(getProgressSlot(), new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "),
                ChestMenuUtils.getEmptyClickHandler());

        preset.addItem(INFO_SLOT, infoItem(0), ChestMenuUtils.getEmptyClickHandler());

        for (int slot : getOutputSlots()) {
            preset.addMenuClickHandler(slot, new ChestMenu.AdvancedMenuClickHandler() {
                @Override
                public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                    return cursor.getType().isAir();
                }

                @Override
                public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
                    return false;
                }
            });
        }
    }
}
