package me.profelements.dynatech.integrations;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.profelements.dynatech.DynaTech;
import me.profelements.dynatech.registries.Items;

/**
 * Registers DynaTech's mob data cards in InfinityExpansion2.
 * <p>
 * InfinityExpansion2 is a Kotlin rewrite with a different API than InfinityExpansion,
 * and it is not published to a Maven repository. Its public API
 * ({@code InfinityExpansion2API.registerMobDataCard}) is therefore called through reflection,
 * so DynaTech neither needs it at compile time nor breaks when it is missing.
 * <p>
 * InfinityExpansion2 already ships a Phantom card, so only the Vex card is added here.
 * <p>
 * InfinityExpansion2 registers its own mob data cards when Slimefun finalizes its item registry
 * (at {@link EventPriority#LOWEST}). The Vex card is registered at the same moment but later
 * ({@link EventPriority#NORMAL}), so it is listed after InfinityExpansion2's cards in the guide
 * instead of in front of them.
 */
public final class InfinityExpansion2Integration implements Listener {

    public static final String PLUGIN_NAME = "InfinityExpansion2";

    private static final String API_CLASS = "net.guizhanss.infinityexpansion2.api.InfinityExpansion2API";
    private static final String PROPS_CLASS = "net.guizhanss.infinityexpansion2.api.mobsim.MobDataCardProps";
    private static final String EMPTY_CARD_ID = "IE_MOB_DATA_CARD_EMPTY";

    private final SlimefunAddon addon;

    private InfinityExpansion2Integration(@Nonnull SlimefunAddon addon) {
        this.addon = addon;
    }

    /**
     * Schedules the Vex mob data card registration if InfinityExpansion2 is enabled.
     */
    public static void setup(@Nonnull SlimefunAddon addon) {
        Plugin ie2 = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (ie2 == null || !ie2.isEnabled()) {
            return;
        }

        Bukkit.getPluginManager().registerEvents(new InfinityExpansion2Integration(addon), addon.getJavaPlugin());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRegistryFinalized(SlimefunItemRegistryFinalizedEvent e) {
        register(addon);
    }

    private static void register(@Nonnull SlimefunAddon addon) {
        Plugin ie2 = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (ie2 == null || !ie2.isEnabled()) {
            return;
        }

        try {
            ItemStack emptyCard = findEmptyCard();
            if (emptyCard == null) {
                DynaTech.getInstance().getLogger().warning(
                        "InfinityExpansion2 found, but its empty mob data card is missing. Skipping the Vex mob data card.");
                return;
            }

            ClassLoader loader = ie2.getClass().getClassLoader();
            Class<?> pairClass = Class.forName("kotlin.Pair", true, loader);
            Class<?> propsClass = Class.forName(PROPS_CLASS, true, loader);
            Class<?> apiClass = Class.forName(API_CLASS, true, loader);

            Constructor<?> pair = pairClass.getConstructor(Object.class, Object.class);
            Constructor<?> props = propsClass.getConstructor(String.class, String.class, ItemStack.class,
                    int.class, int.class, List.class, ItemStack[].class);
            Method registerMobDataCard = apiClass.getMethod("registerMobDataCard", propsClass, SlimefunAddon.class);

            ItemStack vexGem = withAmount(Items.VEX_GEM.stack(), 16);
            ItemStack ghostlyEssence = withAmount(Items.GHOSTLY_ESSENCE.stack(), 16);

            // Same recipe as the InfinityExpansion card: gems in the corners, essence on the sides.
            ItemStack[] recipe = new ItemStack[] {
                    vexGem, ghostlyEssence, vexGem,
                    ghostlyEssence, emptyCard, ghostlyEssence,
                    vexGem, ghostlyEssence, vexGem
            };

            // Drop weights: 10% Vex Gem, 90% Ghostly Essence (same ratio as the InfinityExpansion card).
            List<Object> drops = List.of(
                    pair.newInstance(withAmount(Items.VEX_GEM.stack(), 1), 0.1d),
                    pair.newInstance(withAmount(Items.GHOSTLY_ESSENCE.stack(), 1), 0.9d));

            Object vexProps = props.newInstance("vex", "Vex", new ItemStack(Material.DIAMOND_CHESTPLATE),
                    300, 2, drops, recipe);

            registerMobDataCard.invoke(null, vexProps, addon);
            DynaTech.getInstance().getLogger().info("Registered the Vex mob data card in InfinityExpansion2.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            DynaTech.getInstance().getLogger().log(Level.WARNING,
                    "Could not register the Vex mob data card in InfinityExpansion2. Is your InfinityExpansion2 up to date?", e);
        }
    }

    @Nullable
    private static ItemStack findEmptyCard() {
        SlimefunItem item = SlimefunItem.getById(EMPTY_CARD_ID);
        if (item == null) {
            // Fall back to any registered id ending with MOB_DATA_CARD_EMPTY, in case the prefix changes.
            for (SlimefunItem sfItem : Slimefun.getRegistry().getAllSlimefunItems()) {
                if (sfItem.getId().endsWith("MOB_DATA_CARD_EMPTY")
                        && PLUGIN_NAME.equals(sfItem.getAddon().getName())) {
                    item = sfItem;
                    break;
                }
            }
        }
        return item == null ? null : item.getItem().clone();
    }

    @Nonnull
    private static ItemStack withAmount(@Nonnull ItemStack item, int amount) {
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        return clone;
    }
}
