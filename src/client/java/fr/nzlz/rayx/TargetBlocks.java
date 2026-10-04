package fr.nzlz.rayx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TargetBlocks {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("rayx-targets.json");

    private static final int DEFAULT_HIGHLIGHT_RADIUS = 32;

    private static final int MAX_HIGHLIGHT_RADIUS = 128;

    private static final List<TargetBlock> TARGETS =
            new ArrayList<>();

    private static int highlightRadius =
            DEFAULT_HIGHLIGHT_RADIUS;

    private TargetBlocks() {
    }

    public static void initialize() {
        load();
    }

    public static List<TargetBlock> getAll() {
        return Collections.unmodifiableList(TARGETS);
    }

    public static List<TargetBlock> getEnabled() {
        return TARGETS.stream()
                .filter(TargetBlock::enabled)
                .toList();
    }

    /**
     * Returns the currently enabled targets indexed by their
     * actual Minecraft Block instance.
     *
     * This avoids resolving an Identifier for every block checked
     * during the highlight scan.
     */
    public static Map<Block, TargetBlock> getEnabledByBlock() {
        Map<Block, TargetBlock> result =
                new HashMap<>();

        for (TargetBlock target : TARGETS) {
            if (!target.enabled()) {
                continue;
            }

            Block block =
                    BuiltInRegistries.BLOCK.getValue(
                            target.id()
                    );

            if (block != null) {
                result.put(block, target);
            }
        }

        return result;
    }

    public static boolean contains(Identifier id) {
        return TARGETS.stream()
                .anyMatch(
                        target ->
                                target.id().equals(id)
                );
    }

    public static boolean add(Identifier id) {
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return false;
        }

        if (contains(id)) {
            return false;
        }

        TARGETS.add(
                new TargetBlock(
                        id,
                        true,
                        0x55FF55
                )
        );

        save();

        return true;
    }

    public static void remove(TargetBlock target) {
        if (TARGETS.remove(target)) {
            save();
        }
    }

    public static void setEnabled(
            TargetBlock target,
            boolean enabled
    ) {
        target.setEnabled(enabled);

        save();
    }

    public static void setColor(
            TargetBlock target,
            int color
    ) {
        target.setColor(color);

        save();
    }

    public static int getHighlightRadius() {
        return highlightRadius;
    }

    public static void setHighlightRadius(
            int radius
    ) {
        highlightRadius = Math.max(
                1,
                Math.min(
                        radius,
                        MAX_HIGHLIGHT_RADIUS
                )
        );

        save();
    }

    public static void save() {
        JsonObject root =
                new JsonObject();

        root.addProperty(
                "highlight_radius",
                highlightRadius
        );

        JsonArray targets =
                new JsonArray();

        for (TargetBlock target : TARGETS) {
            JsonObject object =
                    new JsonObject();

            object.addProperty(
                    "block",
                    target.id().toString()
            );

            object.addProperty(
                    "enabled",
                    target.enabled()
            );

            object.addProperty(
                    "color",
                    target.color()
            );

            targets.add(object);
        }

        root.add(
                "targets",
                targets
        );

        try {
            Files.createDirectories(
                    CONFIG_FILE.getParent()
            );

            Files.writeString(
                    CONFIG_FILE,
                    GSON.toJson(root)
            );
        } catch (IOException exception) {
            Rayx.LOGGER.error(
                    "Unable to save Rayx target blocks configuration",
                    exception
            );
        }
    }

    private static void load() {
        TARGETS.clear();

        highlightRadius =
                DEFAULT_HIGHLIGHT_RADIUS;

        if (!Files.exists(CONFIG_FILE)) {
            return;
        }

        try {
            String content =
                    Files.readString(
                            CONFIG_FILE
                    );

            JsonElement parsed =
                    JsonParser.parseString(
                            content
                    );

            if (!parsed.isJsonObject()) {
                return;
            }

            JsonObject root =
                    parsed.getAsJsonObject();

            /*
             * ---------------------------------------------------------
             * Highlight radius
             * ---------------------------------------------------------
             */

            if (root.has("highlight_radius")) {
                highlightRadius = Math.max(
                        1,
                        Math.min(
                                root.get(
                                        "highlight_radius"
                                ).getAsInt(),
                                MAX_HIGHLIGHT_RADIUS
                        )
                );
            }

            /*
             * ---------------------------------------------------------
             * Targets
             * ---------------------------------------------------------
             */

            JsonArray targets =
                    root.getAsJsonArray(
                            "targets"
                    );

            if (targets == null) {
                return;
            }

            for (JsonElement element : targets) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject object =
                        element.getAsJsonObject();

                if (!object.has("block")) {
                    continue;
                }

                Identifier id;

                try {
                    id = Identifier.tryParse(
                            object.get(
                                    "block"
                            ).getAsString()
                    );
                } catch (Exception exception) {
                    continue;
                }

                if (id == null) {
                    continue;
                }

                if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                    continue;
                }

                boolean enabled =
                        !object.has("enabled")
                                || object.get(
                                "enabled"
                        ).getAsBoolean();

                int color =
                        object.has("color")
                                ? object.get(
                                "color"
                        ).getAsInt()
                                : 0x55FF55;

                if (!contains(id)) {
                    TARGETS.add(
                            new TargetBlock(
                                    id,
                                    enabled,
                                    color
                            )
                    );
                }
            }

        } catch (Exception exception) {
            Rayx.LOGGER.error(
                    "Unable to load Rayx target blocks configuration",
                    exception
            );
        }
    }
}