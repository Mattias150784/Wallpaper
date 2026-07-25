package net.mattias.wallpaper.neoforge.config;

import net.mattias.wallpaper.core.config.WallpaperConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeWallpaperConfig {

    private NeoForgeWallpaperConfig() {}

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    private static final ModConfigSpec.BooleanValue EMISSIVE_LIGHTING;
    private static final ModConfigSpec.BooleanValue MULTI_PLACEMENT;
    private static final ModConfigSpec.BooleanValue VEIN_MINE_SCRAPER;
    private static final ModConfigSpec.BooleanValue BLOCK_ROTATION;

    private static final ModConfigSpec.IntValue MAX_MULTI_PLACE_AREA;
    private static final ModConfigSpec.IntValue MAX_VEIN_MINE_BLOCKS;
    private static final ModConfigSpec.IntValue SELECTION_TIMEOUT_SECONDS;
    private static final ModConfigSpec.BooleanValue CONSUME_ITEMS;
    private static final ModConfigSpec.BooleanValue RETURN_ITEMS_ON_REMOVE;

    private static final ModConfigSpec.IntValue MAX_RENDER_DISTANCE;

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();

        server.push("features");
        EMISSIVE_LIGHTING = server
                .comment("Whether wallpaper made from light emitting blocks (glowstone, sea lantern, ...) lights the world.")
                .define("emissiveLighting", true);
        MULTI_PLACEMENT = server
                .comment("Whether sneak + click lets players fill a rectangular area with wallpaper at once.")
                .define("multiPlacement", true);
        VEIN_MINE_SCRAPER = server
                .comment("Whether sneaking with the scraper removes all connected wallpaper at once.")
                .define("veinMineScraper", true);
        BLOCK_ROTATION = server
                .comment("Whether logs, pillars and directional blocks can be rotated after being applied as wallpaper.")
                .define("blockRotation", true);
        server.pop();

        server.push("limits");
        MAX_MULTI_PLACE_AREA = server
                .comment("Maximum number of blocks a single multi placement can cover.")
                .defineInRange("maxMultiPlaceArea", 400, 1, 100000);
        MAX_VEIN_MINE_BLOCKS = server
                .comment("Maximum number of wallpapers a single scraper vein mine can remove.")
                .defineInRange("maxVeinMineBlocks", 64, 1, 100000);
        SELECTION_TIMEOUT_SECONDS = server
                .comment("How long a multi-placement corner selection stays active before expiring.")
                .defineInRange("selectionTimeoutSeconds", 30, 1, 3600);
        CONSUME_ITEMS = server
                .comment("Whether placing wallpaper consumes items from the player's inventory (survival only).")
                .define("consumeItems", true);
        RETURN_ITEMS_ON_REMOVE = server
                .comment("Whether scraping wallpaper returns the wallpaper/block item to the player (survival only).")
                .define("returnItemsOnRemove", true);
        server.pop();

        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("performance");
        MAX_RENDER_DISTANCE = client
                .comment("Maximum distance (in blocks) at which wallpaper is rendered. Set to 0 for unlimited.")
                .defineInRange("maxRenderDistance", 64, 0, 512);
        client.pop();

        CLIENT_SPEC = client.build();
    }

    public static void applyServer() {
        WallpaperConfig.emissiveLighting = EMISSIVE_LIGHTING.getAsBoolean();
        WallpaperConfig.multiPlacement = MULTI_PLACEMENT.getAsBoolean();
        WallpaperConfig.veinMineScraper = VEIN_MINE_SCRAPER.getAsBoolean();
        WallpaperConfig.blockRotation = BLOCK_ROTATION.getAsBoolean();
        WallpaperConfig.maxMultiPlaceArea = MAX_MULTI_PLACE_AREA.getAsInt();
        WallpaperConfig.maxVeinMineBlocks = MAX_VEIN_MINE_BLOCKS.getAsInt();
        WallpaperConfig.selectionTimeoutSeconds = SELECTION_TIMEOUT_SECONDS.getAsInt();
        WallpaperConfig.consumeItems = CONSUME_ITEMS.getAsBoolean();
        WallpaperConfig.returnItemsOnRemove = RETURN_ITEMS_ON_REMOVE.getAsBoolean();
    }

    public static void applyClient() {
        WallpaperConfig.maxRenderDistance = MAX_RENDER_DISTANCE.getAsInt();
    }
}
