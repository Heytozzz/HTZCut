package com.heytozzz.htzcut.neoforge.command;

import com.heytozzz.htzcut.core.cinematic.CameraKeyframe;
import com.heytozzz.htzcut.core.cinematic.CinematicDefinition;
import com.heytozzz.htzcut.core.cinematic.CinematicRepository;
import com.heytozzz.htzcut.core.cinematic.KeyframeConfig;
import com.heytozzz.htzcut.neoforge.HTZCutMod;
import com.heytozzz.htzcut.neoforge.cinematic.CinematicVisualizer;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /htzcut cinematic ... - builds and edits named, reusable camera paths
 * (CinematicDefinition, stored one YAML per name under
 * config/htzcut/cinematics/) without hand-writing keyframe coordinates:
 *
 *   create &lt;name&gt;                                  - new, empty cinematic
 *   delete &lt;name&gt;                                  - delete it
 *   list                                            - list all cinematics
 *   preview &lt;name&gt;                                 - play it on yourself now
 *   setduration &lt;name&gt; &lt;seconds|none&gt;              - set/clear the global duration
 *   keyframe add &lt;name&gt; [duration]                 - append a keyframe at your
 *                                                      current position/rotation
 *   keyframe set &lt;name&gt; &lt;index&gt; [duration]         - overwrite an existing
 *                                                      keyframe the same way
 *   keyframe remove &lt;name&gt; &lt;index&gt;                 - remove one (later indices
 *                                                      shift down automatically)
 *   keyframe clear &lt;name&gt;                          - remove all keyframes
 *   keyframe list &lt;name&gt;                           - list them, with clickable
 *                                                      coordinates that teleport
 *                                                      you there to preview, and
 *                                                      a temporary in-world
 *                                                      marker+path visualization
 *   keyframe settime &lt;name&gt; &lt;index&gt; &lt;duration&gt;     - change just the timing of
 *                                                      an existing keyframe
 *
 * [duration]/&lt;duration&gt; accepts a number followed by a unit: "20t"
 * (ticks), "1.5s" (seconds), "2m" (minutes) - see TimeUtil.
 *
 * Every subcommand that needs "your current position" requires the
 * command to be run by a player (not console/command blocks).
 */
public final class CinematicCommand {

    private CinematicCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(HTZCutMod mod) {
        return Commands.literal("cinematic")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> create(ctx.getSource(), mod, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestNames(mod, builder))
                                .executes(ctx -> delete(ctx.getSource(), mod, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource(), mod)))
                .then(Commands.literal("preview")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestNames(mod, builder))
                                .executes(ctx -> preview(ctx.getSource(), mod, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("setduration")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestNames(mod, builder))
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(ctx -> setDuration(ctx.getSource(), mod,
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "value"))))))
                .then(Commands.literal("keyframe")
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestNames(mod, builder))
                                        .executes(ctx -> addKeyframe(ctx.getSource(), mod,
                                                StringArgumentType.getString(ctx, "name"), null))
                                        .then(Commands.argument("duration", StringArgumentType.word())
                                                .executes(ctx -> addKeyframe(ctx.getSource(), mod,
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "duration"))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestNames(mod, builder))
                                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setKeyframe(ctx.getSource(), mod,
                                                        StringArgumentType.getString(ctx, "name"),
                                                        IntegerArgumentType.getInteger(ctx, "index"), null))
                                                .then(Commands.argument("duration", StringArgumentType.word())
                                                        .executes(ctx -> setKeyframe(ctx.getSource(), mod,
                                                                StringArgumentType.getString(ctx, "name"),
                                                                IntegerArgumentType.getInteger(ctx, "index"),
                                                                StringArgumentType.getString(ctx, "duration")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestNames(mod, builder))
                                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                .executes(ctx -> removeKeyframe(ctx.getSource(), mod,
                                                        StringArgumentType.getString(ctx, "name"),
                                                        IntegerArgumentType.getInteger(ctx, "index"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestNames(mod, builder))
                                        .executes(ctx -> clearKeyframes(ctx.getSource(), mod,
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestNames(mod, builder))
                                        .executes(ctx -> listKeyframes(ctx.getSource(), mod,
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("settime")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestNames(mod, builder))
                                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("duration", StringArgumentType.word())
                                                        .executes(ctx -> setTime(ctx.getSource(), mod,
                                                                StringArgumentType.getString(ctx, "name"),
                                                                IntegerArgumentType.getInteger(ctx, "index"),
                                                                StringArgumentType.getString(ctx, "duration"))))))));
    }

    // ---- cinematic management ----

    private static int create(CommandSourceStack source, HTZCutMod mod, String name) {
        try {
            mod.cinematicRepository().create(name);
            success(source, "Created cinematic '" + name + "'.");
            return 1;
        } catch (IllegalArgumentException e) {
            return fail(source, e.getMessage());
        } catch (IOException e) {
            return failIo(source, "create cinematic", e);
        }
    }

    private static int delete(CommandSourceStack source, HTZCutMod mod, String name) {
        try {
            boolean deleted = mod.cinematicRepository().delete(name);
            if (!deleted) {
                return fail(source, "No cinematic named '" + name + "'.");
            }
            success(source, "Deleted cinematic '" + name + "'.");
            return 1;
        } catch (IOException e) {
            return failIo(source, "delete cinematic", e);
        }
    }

    private static int list(CommandSourceStack source, HTZCutMod mod) {
        try {
            List<String> names = mod.cinematicRepository().listNames();
            if (names.isEmpty()) {
                success(source, "No cinematics yet - create one with /htzcut cinematic create <name>.");
                return 0;
            }

            source.sendSuccess(() -> Component.literal("[HTZCut] Cinematics (" + names.size() + "):"), false);
            for (String name : names) {
                Optional<CinematicDefinition> def = mod.cinematicRepository().load(name);
                int count = def.map(d -> d.getKeyframes().size()).orElse(0);
                source.sendSuccess(() -> Component.literal("  - " + name + " (" + count + " keyframe(s))"), false);
            }
            return names.size();
        } catch (IOException e) {
            return failIo(source, "list cinematics", e);
        }
    }

    private static int preview(CommandSourceStack source, HTZCutMod mod, String name) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return fail(source, "Only a player can preview a cinematic.");
        }

        Optional<CinematicDefinition> def;
        try {
            def = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }

        if (def.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'.");
        }
        if (def.get().getKeyframes().isEmpty()) {
            return fail(source, "Cinematic '" + name + "' has no keyframes yet.");
        }

        List<CameraKeyframe> keyframes = toCameraKeyframes(def.get().getKeyframes());
        mod.cinematicRunner().playCinematic(player.getUUID(), keyframes, def.get().getDurationSeconds());
        success(source, "Previewing '" + name + "' on you.");
        return 1;
    }

    private static int setDuration(CommandSourceStack source, HTZCutMod mod, String name, String value) {
        Optional<CinematicDefinition> maybeDef;
        try {
            maybeDef = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }
        if (maybeDef.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'.");
        }

        CinematicDefinition def = maybeDef.get();

        if (value.equalsIgnoreCase("none")) {
            def.setDurationSeconds(null);
        } else {
            double seconds;
            try {
                seconds = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return fail(source, "Invalid duration '" + value + "' - use a number of seconds, or 'none'.");
            }
            if (seconds <= 0) {
                return fail(source, "Duration must be positive.");
            }
            def.setDurationSeconds(seconds);
        }

        try {
            mod.cinematicRepository().save(def);
        } catch (IOException e) {
            return failIo(source, "save cinematic", e);
        }

        success(source, "Set duration of '" + name + "' to "
                + (def.getDurationSeconds() != null ? def.getDurationSeconds() + "s" : "none (per-keyframe timing)") + ".");
        return 1;
    }

    // ---- keyframe editing ----

    private static int addKeyframe(CommandSourceStack source, HTZCutMod mod, String name, String durationInput) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return fail(source, "Only a player can add a keyframe (it captures your current position).");
        }

        Optional<CinematicDefinition> maybeDef;
        try {
            maybeDef = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }
        if (maybeDef.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'. Create it first with /htzcut cinematic create.");
        }

        Double duration = null;
        if (durationInput != null) {
            try {
                duration = TimeUtil.parseSeconds(durationInput);
            } catch (IllegalArgumentException e) {
                return fail(source, e.getMessage());
            }
        }

        CinematicDefinition def = maybeDef.get();
        KeyframeConfig keyframe = captureKeyframe(player, duration);
        def.getKeyframes().add(keyframe);

        try {
            mod.cinematicRepository().save(def);
        } catch (IOException e) {
            return failIo(source, "save cinematic", e);
        }

        int index = def.getKeyframes().size() - 1;
        success(source, "Added keyframe #" + index + " to '" + name + "' at your position.");
        return 1;
    }

    private static int setKeyframe(CommandSourceStack source, HTZCutMod mod, String name, int index, String durationInput) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return fail(source, "Only a player can set a keyframe (it captures your current position).");
        }

        Optional<CinematicDefinition> maybeDef;
        try {
            maybeDef = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }
        if (maybeDef.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'.");
        }

        CinematicDefinition def = maybeDef.get();
        if (index < 0 || index >= def.getKeyframes().size()) {
            return fail(source, "Index " + index + " is out of range (0.." + (def.getKeyframes().size() - 1) + ").");
        }

        Double existingTime = def.getKeyframes().get(index).getTimeSeconds();
        Double duration = existingTime;
        if (durationInput != null) {
            try {
                duration = TimeUtil.parseSeconds(durationInput);
            } catch (IllegalArgumentException e) {
                return fail(source, e.getMessage());
            }
        }

        def.getKeyframes().set(index, captureKeyframe(player, duration));

        try {
            mod.cinematicRepository().save(def);
        } catch (IOException e) {
            return failIo(source, "save cinematic", e);
        }

        success(source, "Overwrote keyframe #" + index + " of '" + name + "' with your current position.");
        return 1;
    }

    private static int removeKeyframe(CommandSourceStack source, HTZCutMod mod, String name, int index) {
        Optional<CinematicDefinition> maybeDef;
        try {
            maybeDef = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }
        if (maybeDef.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'.");
        }

        CinematicDefinition def = maybeDef.get();
        if (index < 0 || index >= def.getKeyframes().size()) {
            return fail(source, "Index " + index + " is out of range (0.." + (def.getKeyframes().size() - 1) + ").");
        }

        // Removing from a List automatically shifts every later index
        // down by one - no separate re-numbering step needed.
        def.getKeyframes().remove(index);

        try {
            mod.cinematicRepository().save(def);
        } catch (IOException e) {
            return failIo(source, "save cinematic", e);
        }

        success(source, "Removed keyframe #" + index + " from '" + name + "' ("
                + def.getKeyframes().size() + " remaining).");
        return 1;
    }

    private static int clearKeyframes(CommandSourceStack source, HTZCutMod mod, String name) {
        Optional<CinematicDefinition> maybeDef;
        try {
            maybeDef = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }
        if (maybeDef.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'.");
        }

        CinematicDefinition def = maybeDef.get();
        int removed = def.getKeyframes().size();
        def.getKeyframes().clear();

        try {
            mod.cinematicRepository().save(def);
        } catch (IOException e) {
            return failIo(source, "save cinematic", e);
        }

        success(source, "Cleared all " + removed + " keyframe(s) from '" + name + "'.");
        return removed;
    }

    private static int listKeyframes(CommandSourceStack source, HTZCutMod mod, String name) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return fail(source, "Only a player can list keyframes (shows a preview visualization around you).");
        }

        Optional<CinematicDefinition> maybeDef;
        try {
            maybeDef = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }
        if (maybeDef.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'.");
        }

        List<KeyframeConfig> keyframes = maybeDef.get().getKeyframes();
        if (keyframes.isEmpty()) {
            success(source, "Cinematic '" + name + "' has no keyframes yet.");
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "[HTZCut] Keyframes for '" + name + "' (click coordinates to teleport there):"), false);

        for (int i = 0; i < keyframes.size(); i++) {
            KeyframeConfig keyframe = keyframes.get(i);
            int index = i;
            source.sendSuccess(() -> Component.literal("  #" + index + " ")
                    .append(teleportLink(keyframe))
                    .append(Component.literal(keyframe.getTimeSeconds() != null
                            ? "  (" + keyframe.getTimeSeconds() + "s)" : "  (default 1.0s)")), false);
        }

        mod.cinematicVisualizer().show(player.serverLevel(), keyframes);
        source.sendSuccess(() -> Component.literal(
                "[HTZCut] Showing markers + path in-world for 30 seconds."), false);

        return keyframes.size();
    }

    private static int setTime(CommandSourceStack source, HTZCutMod mod, String name, int index, String durationInput) {
        Optional<CinematicDefinition> maybeDef;
        try {
            maybeDef = mod.cinematicRepository().load(name);
        } catch (IOException e) {
            return failIo(source, "load cinematic", e);
        }
        if (maybeDef.isEmpty()) {
            return fail(source, "No cinematic named '" + name + "'.");
        }

        CinematicDefinition def = maybeDef.get();
        if (index < 0 || index >= def.getKeyframes().size()) {
            return fail(source, "Index " + index + " is out of range (0.." + (def.getKeyframes().size() - 1) + ").");
        }

        double seconds;
        try {
            seconds = TimeUtil.parseSeconds(durationInput);
        } catch (IllegalArgumentException e) {
            return fail(source, e.getMessage());
        }

        def.getKeyframes().get(index).setTimeSeconds(seconds);

        try {
            mod.cinematicRepository().save(def);
        } catch (IOException e) {
            return failIo(source, "save cinematic", e);
        }

        success(source, "Set keyframe #" + index + " of '" + name + "' to " + seconds + "s.");
        return 1;
    }

    // ---- helpers ----

    private static KeyframeConfig captureKeyframe(ServerPlayer player, Double timeSeconds) {
        KeyframeConfig keyframe = new KeyframeConfig();
        keyframe.setX(player.getX());
        keyframe.setY(player.getY());
        keyframe.setZ(player.getZ());
        keyframe.setYaw(player.getYRot());
        keyframe.setPitch(player.getXRot());
        keyframe.setTimeSeconds(timeSeconds);
        return keyframe;
    }

    private static List<CameraKeyframe> toCameraKeyframes(List<KeyframeConfig> raw) {
        return raw.stream()
                .map(k -> new CameraKeyframe(k.getX(), k.getY(), k.getZ(), k.getYaw(), k.getPitch(),
                        k.getTimeSeconds() != null ? k.getTimeSeconds() : 1.0))
                .toList();
    }

    /** "/tp @s x y z yaw pitch" as a clickable, hoverable chat component. */
    private static MutableComponent teleportLink(KeyframeConfig keyframe) {
        String command = String.format(Locale.ROOT, "/tp @s %.2f %.2f %.2f %.2f %.2f",
                keyframe.getX(), keyframe.getY(), keyframe.getZ(), keyframe.getYaw(), keyframe.getPitch());
        String label = String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)",
                keyframe.getX(), keyframe.getY(), keyframe.getZ());

        return Component.literal(label).withStyle(style -> style
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to teleport here (exact position + rotation)"))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestNames(
            HTZCutMod mod, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        List<String> names;
        try {
            names = mod.cinematicRepository().listNames();
        } catch (IOException e) {
            names = List.of();
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[HTZCut] " + message), true);
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("[HTZCut] " + message));
        return 0;
    }

    private static int failIo(CommandSourceStack source, String action, IOException e) {
        HTZLog.error("Failed to " + action, e);
        source.sendFailure(Component.literal("[HTZCut] Failed to " + action + ": " + e.getMessage()));
        return 0;
    }
}
