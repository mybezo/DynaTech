package me.profelements.dynatech.compressed;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors;
import me.profelements.dynatech.DynaTech;
import me.profelements.dynatech.items.abstracts.AbstractElectricMachine;
import me.profelements.dynatech.items.electric.machines.MineralizedApiary;
import me.profelements.dynatech.registries.ItemGroups;
import me.profelements.dynatech.registries.Items;
import me.profelements.dynatech.utils.ItemWrapper;

/**
 * Registers the compressed machines (multiple machines combined into one), configured by {@code compressed.yml}.
 * <p>
 * Every machine gets a compressed version for each enabled multiplier. The first one is made of the original
 * machine in the Compressor, the others of 2 of the previous multiplier. Only the highest multiplier is listed
 * in the guide; the lower ones go into a hidden group but can still be crafted.
 */
public final class CompressedSetup {

    public static final int[] MULTIPLIERS = new int[] { 8, 16, 32, 64 };

    private static final String FILE_NAME = "compressed.yml";
    private static final int COMPRESSED_ITEMS_SLOT = 40;

    // energy numbers in the lore: "64 J/s", "16,384 J Buffer", ...
    // the color codes in front are captured separately, so "§732 J/s" is read as "§7" and "32"
    private static final Pattern ENERGY_PATTERN = Pattern.compile("((?:[§&][0-9a-fk-orA-FK-OR])*)(\\d[\\d,]*)(?= J)");

    private static boolean hasItems = false;

    private CompressedSetup() {}

    /**
     * Whether at least one compressed machine is registered, the "Compressed Items" button is hidden otherwise.
     */
    public static boolean hasItems() {
        return hasItems;
    }

    private enum Category {
        MACHINES("machines", ItemGroups.COMPRESSED_MACHINES),
        APIARIES("apiaries", ItemGroups.COMPRESSED_APIARIES);

        private final String configSection;
        private final ItemGroup itemGroup;

        Category(String configSection, ItemGroup itemGroup) {
            this.configSection = configSection;
            this.itemGroup = itemGroup;
        }
    }

    @FunctionalInterface
    private interface Factory {
        @Nullable
        SlimefunItem create(ItemGroup group, SlimefunItemStack item, ItemStack[] recipe, int multiplier);
    }

    private record Entry(SlimefunItem base, Category category, Factory factory) {}

    public static void setup(@Nonnull DynaTech plugin, @Nonnull List<MineralizedApiary> apiaries) {
        List<Entry> entries = new ArrayList<>();
        addGrowthChambers(entries);
        for (MineralizedApiary apiary : apiaries) {
            addApiary(entries, apiary);
        }

        Config cfg = loadConfig(plugin, entries);
        if (!cfg.getBoolean("enabled")) {
            return;
        }

        // a multiplier is only available when all the lower multipliers are enabled
        List<Integer> enabledMultipliers = new ArrayList<>();
        for (int multiplier : MULTIPLIERS) {
            if (!cfg.getBoolean("multipliers.x" + multiplier)) {
                break;
            }
            enabledMultipliers.add(multiplier);
        }
        if (enabledMultipliers.isEmpty()) {
            return;
        }

        int count = 0;
        for (Entry entry : entries) {
            if (!cfg.getBoolean(entry.category().configSection + "." + entry.base().getId())
                    || entry.base().isDisabled()) {
                continue;
            }
            count += register(plugin, entry, enabledMultipliers);
        }

        if (count > 0) {
            hasItems = true;
            ItemGroups.GENERAL.setFixedButton(COMPRESSED_ITEMS_SLOT,
                    ItemGroups.COMPRESSED::getItem,
                    p -> hasItems,
                    (p, profile, mode) -> SlimefunGuide.openItemGroup(profile, ItemGroups.COMPRESSED, mode, 1));
        }
        plugin.getLogger().info("Registered " + count + " compressed machines.");
    }

    private static void addGrowthChambers(@Nonnull List<Entry> entries) {
        // the progress bar of each chamber
        addGrowthChamber(entries, Items.GROWTH_CHAMBER, Material.IRON_HOE);
        addGrowthChamber(entries, Items.GROWTH_CHAMBER_MK2, Material.DIAMOND_HOE);
        addGrowthChamber(entries, Items.GROWTH_CHAMBER_OCEAN, Material.CONDUIT);
        addGrowthChamber(entries, Items.GROWTH_CHAMBER_MK2_OCEAN, Material.CONDUIT);
        addGrowthChamber(entries, Items.GROWTH_CHAMBER_NETHER, Material.NETHERRACK);
        addGrowthChamber(entries, Items.GROWTH_CHAMBER_MK2_NETHER, Material.NETHERRACK);
        addGrowthChamber(entries, Items.GROWTH_CHAMBER_END, Material.CHORUS_FLOWER);
        addGrowthChamber(entries, Items.GROWTH_CHAMBER_MK2_END, Material.CHORUS_FLOWER);
    }

    private static void addGrowthChamber(@Nonnull List<Entry> entries, @Nonnull ItemWrapper wrapper,
            @Nonnull Material progress) {
        if (!(SlimefunItem.getById(wrapper.stack().getItemId()) instanceof AbstractElectricMachine base)) {
            return;
        }

        entries.add(new Entry(base, Category.MACHINES, (group, item, recipe, multiplier) -> {
            Integer capacity = scale(base.getCapacity(), multiplier);
            if (capacity == null) {
                return null;
            }
            return new CompressedGrowthChamber(group, item, RecipeType.COMPRESSOR, recipe, base, multiplier,
                    new ItemStack(progress))
                    .setCapacity(capacity)
                    .setConsumption(base.getEnergyConsumption())
                    .setProcessingSpeed(base.getSpeed());
        }));
    }

    private static void addApiary(@Nonnull List<Entry> entries, @Nonnull MineralizedApiary base) {
        entries.add(new Entry(base, Category.APIARIES, (group, item, recipe, multiplier) -> {
            Integer capacity = scale(base.getCapacity(), multiplier);
            if (capacity == null) {
                return null;
            }
            return new MineralizedApiary(group, item, RecipeType.COMPRESSOR, recipe, base.getMaterial(), multiplier)
                    .setCapacity(capacity)
                    .setConsumption(base.getEnergyConsumption())
                    .setProcessingSpeed(base.getSpeed());
        }));
    }

    /**
     * Multiply the {@code value}, or null when the result does not fit in an Integer.
     */
    @Nullable
    private static Integer scale(int value, int multiplier) {
        long result = (long) value * multiplier;
        return result > Integer.MAX_VALUE ? null : (int) result;
    }

    @Nonnull
    private static Config loadConfig(@Nonnull DynaTech plugin, @Nonnull List<Entry> entries) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        Config cfg = new Config(plugin, FILE_NAME);

        // write the missing options into the file, the existing values are kept
        cfg.setDefaultValue("enabled", true);
        for (int multiplier : MULTIPLIERS) {
            cfg.setDefaultValue("multipliers.x" + multiplier, true);
        }
        for (Entry entry : entries) {
            cfg.setDefaultValue(entry.category().configSection + "." + entry.base().getId(), true);
        }
        cfg.save();
        return cfg;
    }

    /**
     * Register every multiplier of one machine, returns the amount of registered items.
     */
    private static int register(@Nonnull DynaTech plugin, @Nonnull Entry entry,
            @Nonnull List<Integer> multipliers) {
        int count = 0;
        ItemStack previous = entry.base().getItem();
        int previousMultiplier = 1;
        int highest = multipliers.get(multipliers.size() - 1);

        for (int multiplier : multipliers) {
            try {
                SlimefunItemStack item = createItemStack(entry.base(), multiplier);
                ItemStack[] recipe = new ItemStack[9];
                ItemStack input = previous.clone();
                input.setAmount(multiplier / previousMultiplier);
                recipe[0] = input;

                // only the highest multiplier is listed in the guide
                ItemGroup group = multiplier == highest ? entry.category().itemGroup : ItemGroups.COMPRESSED_HIDDEN;

                SlimefunItem compressed = entry.factory().create(group, item, recipe, multiplier);
                if (compressed == null) {
                    // the capacity would exceed the Integer limit, so this and the higher multipliers are skipped
                    break;
                }
                if (!group.isRegistered()) {
                    group.register(plugin);
                }
                compressed.register(plugin);
                count++;

                previous = item;
                previousMultiplier = multiplier;
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, e, () -> "Could not register the x" + multiplier
                        + " compressed version of " + entry.base().getId());
                break;
            }
        }
        return count;
    }

    @Nonnull
    private static SlimefunItemStack createItemStack(@Nonnull SlimefunItem base, int multiplier) {
        ItemStack item = base.getItem().clone();
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(meta.getDisplayName() + ChatColors.color(" &a(x" + multiplier + ")"));

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                lore.add(scaleEnergy(line, multiplier));
            }
        }
        lore.add("");
        lore.add(ChatColors.color("&7Combines " + multiplier + " machines into one."));
        meta.setLore(lore);
        item.setItemMeta(meta);

        return new SlimefunItemStack(base.getId() + "_X" + multiplier, item);
    }

    /**
     * Multiply the energy numbers (consumption and buffer) of a lore line.
     */
    @Nonnull
    static String scaleEnergy(@Nonnull String line, int multiplier) {
        if (!line.contains(" J")) {
            return line;
        }

        Matcher matcher = ENERGY_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement;
            try {
                long value = Long.parseLong(matcher.group(2).replace(",", ""));
                replacement = matcher.group(1) + String.format(Locale.US, "%,d", value * multiplier);
            } catch (NumberFormatException e) {
                replacement = matcher.group();
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
