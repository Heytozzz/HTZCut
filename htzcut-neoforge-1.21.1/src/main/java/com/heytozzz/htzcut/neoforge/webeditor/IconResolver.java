package com.heytozzz.htzcut.neoforge.webeditor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Resolves an item's actual texture path by reading its real model JSON,
 * instead of guessing "textures/item/{name}.png" - which breaks for any
 * item whose texture file doesn't share its exact registry name, or
 * that lives in a subfolder (common in modded content).
 *
 * Follows the model's "parent" chain (capped at a few hops) since a leaf
 * model doesn't always declare its own "textures" block directly.
 */
public final class IconResolver {

    private static final int MAX_PARENT_DEPTH = 6;

    private IconResolver() {
    }

    /**
     * @return the classpath resource path of the resolved texture PNG
     *         (e.g. "assets/chillsmp/textures/item/food/manzana.png"),
     *         or empty if it couldn't be resolved.
     */
    public static Optional<String> resolveTexturePath(String namespace, String itemPath) {
        String currentNamespace = namespace;
        String currentModelPath = "item/" + itemPath;

        for (int depth = 0; depth < MAX_PARENT_DEPTH; depth++) {
            String modelResource = "assets/" + currentNamespace + "/models/" + currentModelPath + ".json";
            Optional<JsonObject> modelJson = readJson(modelResource);
            if (modelJson.isEmpty()) {
                return Optional.empty();
            }
            JsonObject model = modelJson.get();

            if (model.has("textures")) {
                String textureRef = firstTextureRef(model.getAsJsonObject("textures"));
                if (textureRef != null) {
                    return Optional.of(resolveTextureRefToPath(textureRef, currentNamespace));
                }
            }

            if (!model.has("parent")) {
                break;
            }

            String parent = model.get("parent").getAsString();
            String[] parts = parent.split(":", 2);
            if (parts.length == 2) {
                currentNamespace = parts[0];
                currentModelPath = parts[1];
            } else {
                currentNamespace = "minecraft";
                currentModelPath = parts[0];
            }
        }

        return Optional.empty();
    }

    private static String firstTextureRef(JsonObject textures) {
        if (textures.has("layer0")) {
            return textures.get("layer0").getAsString();
        }
        for (String key : textures.keySet()) {
            JsonElement value = textures.get(key);
            if (value.isJsonPrimitive()) {
                return value.getAsString();
            }
        }
        return null;
    }

    private static String resolveTextureRefToPath(String textureRef, String fallbackNamespace) {
        String namespace = fallbackNamespace;
        String path = textureRef;
        if (textureRef.contains(":")) {
            String[] parts = textureRef.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }
        return "assets/" + namespace + "/textures/" + path + ".png";
    }

    private static Optional<JsonObject> readJson(String classpathResource) {
        try (InputStream in = IconResolver.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                return Optional.empty();
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return Optional.of(JsonParser.parseString(content).getAsJsonObject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
