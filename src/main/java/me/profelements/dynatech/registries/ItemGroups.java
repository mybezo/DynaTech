package me.profelements.dynatech.registries;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import me.profelements.dynatech.registries.groups.DynaTechNestedItemGroup;
import me.profelements.dynatech.registries.groups.HiddenItemGroup;
import org.bukkit.Material;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;

public class ItemGroups {

    // Mybezo's skin, the icon of the "Compressed Items" menu
    private static final String COMPRESSED_TEXTURE = "82600941fab88efc41bb132d873d3b5c5f35f7720ebd9375476c1a18955bb03e";

    public static final void init(Registry<ItemGroup> registry) {
        registry.register(Keys.GENERAL, GENERAL);
        registry.register(Keys.RESOURCES, RESOURCES);
        registry.register(Keys.TOOLS, TOOLS);
        registry.register(Keys.MACHINES, MACHINES);
        registry.register(Keys.GENERATORS, GENERATORS);
        registry.register(Keys.EXPERIMENTAL, EXPERIMENTAL);
        registry.register(Keys.APIARIES, HIVES);
    }

    public static final DynaTechNestedItemGroup GENERAL = new DynaTechNestedItemGroup(
            Keys.GENERAL.key(),
            new CustomItemStack(Material.CONDUIT, "&bDynaTech"), true);

    public static final SubItemGroup RESOURCES = new SubItemGroup(
            Keys.RESOURCES.key(), GENERAL,
            new CustomItemStack(Material.PUFFERFISH, "&bDynaTech Resources"));

    public static final SubItemGroup TOOLS = new SubItemGroup(Keys.TOOLS.key(),
            GENERAL, new CustomItemStack(Material.DIAMOND_AXE, "&bDynaTech Tools"));

    public static final SubItemGroup MACHINES = new SubItemGroup(Keys.MACHINES.key(), GENERAL,
            new CustomItemStack(Material.SEA_LANTERN, "&bDynaTech Machines"));

    public static final SubItemGroup GENERATORS = new SubItemGroup(Keys.GENERATORS.key(), GENERAL,
            new CustomItemStack(Material.PRISMARINE_BRICKS, "&bDynaTech Generators"));

    public static final SubItemGroup EXPERIMENTAL = new SubItemGroup(Keys.EXPERIMENTAL.key(), GENERAL,
            new CustomItemStack(Material.REDSTONE_LAMP, "&fDynaTech Experimental"));

    public static final SubItemGroup HIVES = new SubItemGroup(Keys.APIARIES.key(),
            GENERAL, new CustomItemStack(Material.BEEHIVE, "&bDynaTech Apiaries"));

    // START Compressed items
    /**
     * The "Compressed Items" menu, opened from the button at slot 40 of the main DynaTech menu.
     */
    public static final DynaTechNestedItemGroup COMPRESSED = new DynaTechNestedItemGroup(
            Keys.COMPRESSED.key(),
            new CustomItemStack(PlayerHead.getItemStack(PlayerSkin.fromHashCode(COMPRESSED_TEXTURE)),
                    "&aCompressed Items", "", "&7Multiple machines combined into one."),
            false);

    // same icons as the original categories
    public static final SubItemGroup COMPRESSED_MACHINES = new SubItemGroup(Keys.COMPRESSED_MACHINES.key(),
            COMPRESSED, new CustomItemStack(Material.SEA_LANTERN, "&bCompressed Machines"));

    public static final SubItemGroup COMPRESSED_APIARIES = new SubItemGroup(Keys.COMPRESSED_APIARIES.key(),
            COMPRESSED, new CustomItemStack(Material.BEEHIVE, "&bCompressed Apiaries"));

    /**
     * The lower multipliers of the compressed items: craftable, but not listed in the guide.
     */
    public static final HiddenItemGroup COMPRESSED_HIDDEN = new HiddenItemGroup(Keys.COMPRESSED_HIDDEN.key(),
            new CustomItemStack(Material.BARRIER, "&7Compressed (lower multipliers)"));
    // END Compressed items

    public static final class Keys {
        public static final TypedKey<ItemGroup> GENERAL = TypedKey.create("dynatech", "general");
        public static final TypedKey<ItemGroup> RESOURCES = TypedKey.create("dynatech", "resources");
        public static final TypedKey<ItemGroup> TOOLS = TypedKey.create("dynatech", "tools");
        public static final TypedKey<ItemGroup> MACHINES = TypedKey.create("dynatech", "machines");
        public static final TypedKey<ItemGroup> GENERATORS = TypedKey.create("dynatech", "generators");
        public static final TypedKey<ItemGroup> EXPERIMENTAL = TypedKey.create("dynatech", "experimental");
        public static final TypedKey<ItemGroup> APIARIES = TypedKey.create("dynatech", "apiaries");
        public static final TypedKey<ItemGroup> COMPRESSED = TypedKey.create("dynatech", "compressed_items");
        public static final TypedKey<ItemGroup> COMPRESSED_MACHINES = TypedKey.create("dynatech", "compressed_machines");
        public static final TypedKey<ItemGroup> COMPRESSED_APIARIES = TypedKey.create("dynatech", "compressed_apiaries");
        public static final TypedKey<ItemGroup> COMPRESSED_HIDDEN = TypedKey.create("dynatech", "compressed_hidden");
    }
}
