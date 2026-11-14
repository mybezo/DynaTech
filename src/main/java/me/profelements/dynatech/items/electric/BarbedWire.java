package me.profelements.dynatech.items.electric;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.profelements.dynatech.items.abstracts.AbstractElectricTicker;

public class BarbedWire extends AbstractElectricTicker {

    private static final int DEFAULT_ENTITY_PUSH_FORCE = 10;
    private static final int DEFAULT_ENTITY_DETECT_RANGE = 9;

    private final ItemSetting<Integer> entityPushForce = new ItemSetting<>(this, "entity-push-force", DEFAULT_ENTITY_PUSH_FORCE);
    private final ItemSetting<Integer> entityDetectRange = new ItemSetting<>(this, "entity-detect-range", DEFAULT_ENTITY_DETECT_RANGE);

    @ParametersAreNonnullByDefault
    public BarbedWire(ItemGroup group, SlimefunItemStack item,  RecipeType recipeType, ItemStack[] recipe) {
        super(group, item, recipeType, recipe);
        addItemSetting(entityPushForce, entityDetectRange);
    }

    private static final Set<EntityType> ENTITY_WHITELIST = Set.of(
        EntityType.ARROW,
        EntityType.BLAZE,
        EntityType.CAVE_SPIDER,
        EntityType.CREEPER,
        EntityType.DRAGON_FIREBALL,
        EntityType.DROWNED,
        EntityType.ELDER_GUARDIAN,
        EntityType.ENDER_DRAGON,
        EntityType.ENDERMAN,
        EntityType.ENDERMITE,
        EntityType.EVOKER,
        EntityType.FIREBALL,
        EntityType.GHAST,
        EntityType.GIANT,
        EntityType.GUARDIAN,
        EntityType.HOGLIN,
        EntityType.HUSK,
        EntityType.ILLUSIONER,
        EntityType.MAGMA_CUBE,
        EntityType.PHANTOM,
        EntityType.PIGLIN,
        EntityType.PIGLIN_BRUTE,
        EntityType.PILLAGER,
        EntityType.RAVAGER,
        EntityType.SHULKER,
        EntityType.SHULKER_BULLET,
        EntityType.SILVERFISH,
        EntityType.SKELETON,
        EntityType.SLIME,
        EntityType.SMALL_FIREBALL,
        EntityType.SPIDER,
        EntityType.STRAY,
        EntityType.VEX,
        EntityType.VINDICATOR,
        EntityType.WARDEN,
        EntityType.WITCH,
        EntityType.WITHER,
        EntityType.WITHER_SKELETON,
        EntityType.WITHER_SKULL,
        EntityType.ZOGLIN,
        EntityType.ZOMBIE,
        EntityType.ZOMBIFIED_PIGLIN
    );

    protected void tick(Block block, SlimefunItem item) {
        int entityDetectionRange = entityDetectRange.getValue();
        Location loc = block.getLocation();
    
        for (Entity e : block.getWorld().getNearbyEntities(loc, entityDetectionRange, entityDetectionRange, entityDetectionRange,
                entity -> ENTITY_WHITELIST.contains(entity.getType()))) {
            // Direction from the wire to the entity, so the entity is pushed away.
            Vector launchDirection = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(entityPushForce.getValue());

            // If any of the components are not finite just set it to zero.
            if (NumberConversions.isFinite(launchDirection.getX()) && NumberConversions.isFinite(launchDirection.getY())
                && NumberConversions.isFinite(launchDirection.getZ())) {
                e.setVelocity(launchDirection);
            } else {
                e.setVelocity(new Vector(0, 0, 0));
            }
        }
    }

    @Override
    protected boolean isSynchronized() {
        return true;
    }
}
