package me.profelements.dynatech.compressed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;

/**
 * Keeps the {@link VirtualOutput}s of all compressed machines in memory and writes them to the block data.
 * <p>
 * Moving items into the output slots happens almost every tick while Cargo or Networks pull from a machine,
 * so those changes are saved at most every {@link #SAVE_INTERVAL_MILLIS}. A finished operation is saved at once,
 * and everything that is not saved yet is written when its chunk unloads and when the plugin is disabled.
 */
public final class VirtualOutputStorage implements Listener {

    private static final String DATA_KEY = "virtual-output";
    private static final long SAVE_INTERVAL_MILLIS = 5000;

    private static final Map<Location, VirtualOutput> OUTPUTS = new ConcurrentHashMap<>();
    // unsaved locations and the time of their first unsaved change
    private static final Map<Location, Long> DIRTY = new ConcurrentHashMap<>();

    private VirtualOutputStorage() {}

    public static void registerListener(@Nonnull Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new VirtualOutputStorage(), plugin);
    }

    /**
     * The virtual output at this location, loaded from the block data on first use.
     */
    @Nonnull
    public static VirtualOutput get(@Nonnull Location l) {
        return OUTPUTS.computeIfAbsent(l, loc -> VirtualOutput.deserialize(StorageCacheUtils.getData(loc, DATA_KEY)));
    }

    /**
     * Mark the virtual output as changed, it is saved within {@link #SAVE_INTERVAL_MILLIS}.
     */
    public static void markChanged(@Nonnull Location l) {
        DIRTY.putIfAbsent(l, System.currentTimeMillis());
    }

    /**
     * Save the virtual output if it changed long enough ago.
     */
    public static void saveIfDue(@Nonnull Location l) {
        Long since = DIRTY.get(l);
        if (since != null && System.currentTimeMillis() - since >= SAVE_INTERVAL_MILLIS) {
            save(l);
        }
    }

    /**
     * Save the virtual output now.
     */
    public static void save(@Nonnull Location l) {
        DIRTY.remove(l);
        VirtualOutput output = OUTPUTS.get(l);
        // only write to blocks whose data is loaded, writing to others would load their chunk
        if (output == null || StorageCacheUtils.getBlock(l) == null) {
            return;
        }

        if (output.isEmpty()) {
            StorageCacheUtils.removeData(l, DATA_KEY);
        } else {
            StorageCacheUtils.setData(l, DATA_KEY, output.serialize());
        }
    }

    /**
     * Forget the virtual output of a removed machine.
     */
    public static void remove(@Nonnull Location l) {
        OUTPUTS.remove(l);
        DIRTY.remove(l);
        if (StorageCacheUtils.getBlock(l) != null) {
            StorageCacheUtils.removeData(l, DATA_KEY);
        }
    }

    /**
     * Save every unsaved virtual output (when the plugin is disabled).
     */
    public static void saveAll() {
        for (Location l : DIRTY.keySet()) {
            save(l);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent e) {
        // save before Slimefun unloads the block data of the chunk, then free the memory
        Chunk chunk = e.getChunk();
        OUTPUTS.keySet().removeIf(l -> {
            if (!isIn(l, chunk)) {
                return false;
            }
            if (DIRTY.containsKey(l)) {
                save(l);
            }
            return true;
        });
    }

    private static boolean isIn(@Nonnull Location l, @Nonnull Chunk chunk) {
        return l.getWorld() != null && l.getWorld().equals(chunk.getWorld())
                && (l.getBlockX() >> 4) == chunk.getX() && (l.getBlockZ() >> 4) == chunk.getZ();
    }
}
