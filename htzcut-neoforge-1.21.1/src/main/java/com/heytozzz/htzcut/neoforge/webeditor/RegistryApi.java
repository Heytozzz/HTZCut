package com.heytozzz.htzcut.neoforge.webeditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Builds the JSON payloads the web editor's pickers consume. Reads
 * directly from the server's live registries, so anything added by
 * installed mods shows up automatically - nothing here is hardcoded to
 * vanilla content.
 *
 * All three listings are paginated and filtered server-side: some
 * modpacks have several thousand items registered, and sending that
 * whole list to the browser on every keystroke would be exactly the
 * kind of unnecessary load/lag this API is meant to avoid.
 */
public final class RegistryApi {

    private RegistryApi() {
    }

    public static JsonObject items(String query, int page, int size) {
        List<ResourceLocation> matches = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> matches(id.toString(), query))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        JsonArray array = new JsonArray();
        for (ResourceLocation id : page(matches, page, size)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            array.add(entry);
        }

        return paginatedResult(array, matches.size(), page, size);
    }

    public static JsonObject sounds(String query, int page, int size) {
        List<ResourceLocation> matches = BuiltInRegistries.SOUND_EVENT.keySet().stream()
                .filter(id -> matches(id.toString(), query))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        JsonArray array = new JsonArray();
        for (ResourceLocation id : page(matches, page, size)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            array.add(entry);
        }

        return paginatedResult(array, matches.size(), page, size);
    }

    public static JsonObject advancements(MinecraftServer server, String query, int page, int size) {
        List<AdvancementHolder> matches = server.getAdvancements().getAllAdvancements().stream()
                .filter(holder -> matches(holder.id().toString(), query))
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();

        JsonArray array = new JsonArray();
        for (AdvancementHolder holder : page(matches, page, size)) {
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

        return paginatedResult(array, matches.size(), page, size);
    }

    private static boolean matches(String id, String query) {
        return query == null || query.isBlank() || id.toLowerCase().contains(query.toLowerCase());
    }

    private static <T> List<T> page(List<T> all, int page, int size) {
        int from = Math.min(Math.max(page, 0) * size, all.size());
        int to = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    private static JsonObject paginatedResult(JsonArray items, int total, int page, int size) {
        JsonObject result = new JsonObject();
        result.addProperty("total", total);
        result.addProperty("page", page);
        result.addProperty("size", size);
        result.add("items", items);
        return result;
    }
}
