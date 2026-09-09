package com.heytozzz.htzcut.neoforge.item;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items HTZCut needs internally, not meant to appear in survival
 * inventories or creative tabs - currently just the flat icon shown
 * floating over each keyframe by CinematicVisualizer.
 */
public final class HTZItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(HTZCutMod.MOD_ID);

    public static final DeferredHolder<Item, Item> KEYFRAME_MARKER =
            ITEMS.registerSimpleItem("keyframe_marker", new Item.Properties().stacksTo(1));

    private HTZItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
