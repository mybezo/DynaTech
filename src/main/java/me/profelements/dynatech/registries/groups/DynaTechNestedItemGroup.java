package me.profelements.dynatech.registries.groups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;

/**
 * A {@link NestedItemGroup} that looks like Slimefun's own, but can also show buttons at fixed slots
 * (like the "Compressed Items" button) and can be hidden from the main guide menu.
 */
public class DynaTechNestedItemGroup extends NestedItemGroup {

    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT = 44;

    private final List<SubItemGroup> subGroups = new ArrayList<>();
    private final Map<Integer, FixedButton> fixedButtons = new HashMap<>();
    private final boolean visibleInMainMenu;

    public DynaTechNestedItemGroup(NamespacedKey key, ItemStack item, boolean visibleInMainMenu) {
        super(key, item);
        this.visibleInMainMenu = visibleInMainMenu;
    }

    /**
     * Place a button at a fixed slot on every page. It is only shown while {@code condition} is true.
     */
    public void setFixedButton(int slot, @Nonnull Function<Player, ItemStack> item,
            @Nonnull Predicate<Player> condition, @Nonnull FixedButtonAction action) {
        fixedButtons.put(slot, new FixedButton(item, condition, action));
    }

    @Override
    public void addSubGroup(@Nonnull SubItemGroup group) {
        super.addSubGroup(group);
        if (!subGroups.contains(group)) {
            subGroups.add(group);
        }
    }

    @Override
    public void removeSubGroup(@Nonnull SubItemGroup group) {
        super.removeSubGroup(group);
        subGroups.remove(group);
    }

    @Override
    public boolean isVisible(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        return visibleInMainMenu && super.isVisible(p, profile, mode);
    }

    @Override
    public void open(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        openPage(p, profile, mode, 1);
    }

    private void openPage(Player p, PlayerProfile profile, SlimefunGuideMode mode, int page) {
        GuideHistory history = profile.getGuideHistory();
        if (mode == SlimefunGuideMode.SURVIVAL_MODE) {
            history.add(this, page);
        }

        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "guide.title.main"));
        SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(mode);
        menu.setEmptySlotsClickable(false);
        menu.addMenuOpeningHandler(SoundEffect.GUIDE_BUTTON_CLICK_SOUND::playFor);
        if (guide instanceof SurvivalSlimefunGuide survivalGuide) {
            survivalGuide.createHeader(p, profile, menu);
        }

        menu.addItem(1, new CustomItemStack(ChestMenuUtils.getBackButton(p, "",
                ChatColor.GRAY + Slimefun.getLocalization().getMessage(p, "guide.back.guide"))));
        menu.addMenuClickHandler(1, (pl, slot, item, action) -> {
            if (visibleInMainMenu) {
                SlimefunGuide.openMainMenu(profile, mode, history.getMainMenuPage());
            } else {
                // a nested menu that is opened from another menu goes back to it
                history.goBack(guide);
            }
            return false;
        });

        // the sub groups skip the fixed slots
        List<Integer> freeSlots = new ArrayList<>();
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT; slot++) {
            if (!fixedButtons.containsKey(slot)) {
                freeSlots.add(slot);
            }
        }

        List<SubItemGroup> visible = new ArrayList<>();
        for (SubItemGroup group : subGroups) {
            if (group.isVisibleInNested(p)) {
                visible.add(group);
            }
        }

        int perPage = freeSlots.size();
        int pages = Math.max(1, (visible.size() + perPage - 1) / perPage);
        int start = (page - 1) * perPage;
        for (int i = 0; i < perPage && start + i < visible.size(); i++) {
            SubItemGroup group = visible.get(start + i);
            int slot = freeSlots.get(i);
            menu.addItem(slot, group.getItem(p));
            menu.addMenuClickHandler(slot, (pl, s, item, action) -> {
                SlimefunGuide.openItemGroup(profile, group, mode, 1);
                return false;
            });
        }

        fixedButtons.forEach((slot, button) -> {
            if (button.condition().test(p)) {
                menu.addItem(slot, button.item().apply(p));
                menu.addMenuClickHandler(slot, (pl, s, item, action) -> {
                    button.action().onClick(pl, profile, mode);
                    return false;
                });
            }
        });

        menu.addItem(46, ChestMenuUtils.getPreviousButton(p, page, pages));
        menu.addMenuClickHandler(46, (pl, slot, item, action) -> {
            if (page > 1) {
                openPage(pl, profile, mode, page - 1);
            }
            return false;
        });

        menu.addItem(52, ChestMenuUtils.getNextButton(p, page, pages));
        menu.addMenuClickHandler(52, (pl, slot, item, action) -> {
            if (page < pages) {
                openPage(pl, profile, mode, page + 1);
            }
            return false;
        });

        menu.open(p);
    }

    @FunctionalInterface
    public interface FixedButtonAction {
        void onClick(Player p, PlayerProfile profile, SlimefunGuideMode mode);
    }

    private record FixedButton(Function<Player, ItemStack> item, Predicate<Player> condition,
            FixedButtonAction action) {}
}
