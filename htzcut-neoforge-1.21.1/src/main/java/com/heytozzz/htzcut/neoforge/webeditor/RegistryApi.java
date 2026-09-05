package com.heytozzz.htzcut.neoforge.webeditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Builds the JSON payloads the web editor's pickers consume. Reads
 * directly from the server's live registries, so anything added by
 * installed mods shows up automatically - nothing here is hardcoded to
 * vanilla content.
 */
public final class RegistryApi {

    private RegistryApi() {
    }

    /**
     * Every registered item, vanilla and modded. The client uses the id
     * to fetch /assets/{namespace}/models/item/{path}.json for the 3D
     * preview.
     */
    public static JsonArray items() {
        JsonArray array = new JsonArray();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            array.add(entry);
        }
        return array;
    }

    /**
     * Every registered SoundEvent, vanilla and modded (including
     * HTZCut's own bundled UI sounds once those exist).
     */
    public static JsonArray sounds() {
        JsonArray array = new JsonArray();
        for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            array.add(entry);
        }
        return array;
    }

    /**
     * Every advancement currently loaded (vanilla, mods, and datapacks),
     * with the item id used as its display icon when it has one visible.
     */
    public static JsonArray advancements(MinecraftServer server) {
        JsonArray array = new JsonArray();

        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            Optional<DisplayInfo> display = holder.value().display();

            JsonObject entry = new JsonObject();
            entry.addProperty("id", holder.id().toString());

            if (display.isPresent()) {
                ItemStack icon = display.get().getIcon();
                ResourceLocation iconId = BuiltInRegistries.ITEM.getKey(icon.getItem());
                entry.addProperty("icon", iconId.toString());
            }

            array.add(entry);
        }

        return array;
    }
}
