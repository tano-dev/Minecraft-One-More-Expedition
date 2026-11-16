package com.expedition.quests.exploration;

import java.util.*;

public class StructureClassifier {

    public enum School {
        EXPLORE_BIOME,
        EXPLORE_STRUCTURE,
        EXPLORE_DUNGEON,
        IGNORED
    }

    // Namespaces that are 100% Dungeon Datapacks
    private static final Set<String> PURE_DUNGEON_NAMESPACES = Set.of(
            "decubed_dungeons",
            "yggdrasil",
            "ddd"
    );

    // Keywords in structure path that mark a micro-feature / decoration to ignore
    private static final String[] IGNORED_KEYWORDS = {
            "log_pile", "dead_tree", "boulder", "bench", "cart", "paths", "pile",
            "_patch", "well_", "_well", "desert_well", "supply_cache", "guide_post",
            "lantern", "stall", "haystack", "horse_pen", "out_house", "shed",
            "flower_hole", "duck", "boulder", "rocks", "strange_stone", "termite_hill",
            "cooking_pork", "two_woods", "street_vendor", "wandering_trader_cart",
            "effigy_patch", "moss_blob", "totem_pole", "tree_cage", "gallows",
            "birch_tree", "cherry_tree", "dark_tree", "oak_tree", "spruce_tree", "acacia_tree", "jungle_tree",
            "tree_1", "tree_2", "tree_3", "tree_4", "tree_5", "tree_6", "tree_7", "tree_8",
            "red_coral", "sky_decor", "raft/", "small_pond", "muddy_water_hole", "frozen_pond",
            "very_small_blackstone", "very_small_nether_brick", "bridge_1", "bridge_2", "bridge_3",
            "bridge_4", "bridge_5", "bridge_6", "lava_pool", "warped_pool", "soul_fire", "leafy_rubble",
            "rubble_", "rise"
    };

    // Keywords in structure path that identify a Combat Dungeon / Fortress / Lair / Citadel / Outpost
    private static final String[] DUNGEON_KEYWORDS = {
            "dungeon", "crypt", "catacomb", "citadel", "fortress", "stronghold",
            "bastion", "lair", "arena", "colosseum", "colossuem", "monument",
            "ancient_city", "trial", "illager", "pillager", "witch_keep", "witch_villa",
            "spider_den", "spider_nest", "spider_cellar", "silverfish_nest", "arachnid",
            "wither_cavern", "wither_keep", "warden_monument", "sculk_deposit",
            "dark_keep", "dark_tower", "deepslate_keep", "woodland_keep", "granite_fort",
            "stray_fort", "piglin_donjon", "nether_keep", "nether_skeleton_tower",
            "end_castle", "forbidden_castle", "sanctum", "infernal_altar", "nether_reactor",
            "ruined_lab", "shrine_combat", "sealing_halls", "toxic_lair", "obelisk",
            "dusty_tomb", "scorched_tomb", "skyhold", "deep_blue_labs", "monster_room",
            "pyramide_of_anubis", "sunken_city", "ocean_temple", "nether_temple",
            "phantom_citadel", "enderkeep", "ender_spire", "prison_", "pale_fortress",
            "sand_underground_castle", "basalt_chambers", "basalt_castle", "hellish_orphanage",
            "infested_castle", "howlers_garrison", "monster_egg_banquet", "whitewarden_keep",
            "glacierfall_keep", "glimmervault", "shardspire_castle", "mage_complex", "sunken_tower",
            "mansion"
    };

    public static School classifyStructure(String fullId) {
        if (fullId == null || !fullId.contains(":")) return School.IGNORED;
        String lower = fullId.toLowerCase(Locale.ROOT);
        String[] parts = lower.split(":", 2);
        String ns = parts[0];
        String path = parts[1];

        for (String ig : IGNORED_KEYWORDS) {
            if (path.contains(ig)) {
                return School.IGNORED;
            }
        }

        if (PURE_DUNGEON_NAMESPACES.contains(ns)) {
            return School.EXPLORE_DUNGEON;
        }

        if ("epic".equals(ns) && (path.contains("dungeon") || path.contains("obelisk"))) {
            return School.EXPLORE_DUNGEON;
        }

        for (String dk : DUNGEON_KEYWORDS) {
            if (path.contains(dk)) {
                return School.EXPLORE_DUNGEON;
            }
        }

        return School.EXPLORE_STRUCTURE;
    }

    public static String inferDimension(String fullId) {
        String lower = fullId.toLowerCase(Locale.ROOT);
        String[] parts = lower.split(":", 2);
        String ns = parts[0];
        String path = parts.length > 1 ? parts[1] : "";

        // End namespaces & paths
        if ("nullscape".equals(ns) || "stellarity".equals(ns) || "mes".equals(ns) || "medieval_end".equals(ns)) {
            return "minecraft:the_end";
        }
        if (path.startsWith("end/") || path.startsWith("end_") || path.contains("/end_") || path.equals("the_end")) {
            return "minecraft:the_end";
        }

        // Nether namespaces & paths
        if ("incendium".equals(ns) || "mns".equals(ns) || "medieval_nether".equals(ns)) {
            return "minecraft:the_nether";
        }
        if (path.startsWith("nether/") || path.startsWith("nether_") || path.contains("/nether/")
                || path.contains("bastion") || path.contains("piglin") || path.equals("fortress")
                || path.contains("crimson") || path.contains("warped") || path.contains("basalt_deltas")
                || path.contains("soul_sand_valley")) {
            return "minecraft:the_nether";
        }

        return "minecraft:overworld";
    }

    public static String formatDisplayName(String fullId) {
        if (fullId == null) return "Unknown";
        String[] parts = fullId.split(":", 2);
        String ns = parts[0];
        String path = parts.length > 1 ? parts[1] : fullId;

        // Clean common prefixes
        path = path.replace("exclusives/", "")
                   .replace("overworld/rare/", "")
                   .replace("overworld/medium/", "")
                   .replace("overworld/common/", "")
                   .replace("underground/", "")
                   .replace("sunken_spires/", "");

        String prefix = switch (ns) {
            case "yggdrasil" -> "Yggdrasil: ";
            case "decubed_dungeons" -> "Decubed: ";
            case "nova_structures" -> "DnT: ";
            case "incendium" -> "Incendium: ";
            case "stellarity" -> "Stellarity: ";
            case "nullscape" -> "Nullscape: ";
            case "terralith" -> "Terralith: ";
            case "ati_structures" -> "ATi: ";
            case "kattersstructures" -> "Katters: ";
            case "grim_kingdoms" -> "Grim: ";
            case "epic" -> "Epic: ";
            case "ddd" -> "Nightmare: ";
            case "explorify" -> "Explorify: ";
            case "structory", "structory_towers" -> "Structory: ";
            case "towns_and_towers" -> "T&T: ";
            case "ctov" -> "CTOV: ";
            case "mvs", "mss", "mns", "mes", "mmr", "mtr", "mmv" -> "Moog's: ";
            case "adorabuild_structures" -> "Adora: ";
            default -> "";
        };

        String[] words = path.replace('/', ' ').replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder(prefix);
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1).toLowerCase(Locale.ROOT))
              .append(" ");
        }
        return sb.toString().trim();
    }
}
