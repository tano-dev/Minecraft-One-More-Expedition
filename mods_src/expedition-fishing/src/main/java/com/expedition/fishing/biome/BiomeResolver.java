package com.expedition.fishing.biome;

import com.expedition.fishing.config.FishingDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BiomeResolver {
    private static final Map<String, String> BIOME_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {
        BIOME_CACHE.clear();
    }

    /**
     * Checks if the level is a Skyblock island world.
     * In Skyblock worlds, Expedition Fishing is completely disabled and falls back to 100% vanilla fishing.
     */
    public static boolean isSkyblockWorld(ServerLevel level) {
        if (level == null) return false;
        String dimId = level.dimension().identifier().toString().toLowerCase(Locale.ROOT);
        return dimId.contains("skyblock");
    }

    public static String resolveBiomeGroup(ServerLevel level, BlockPos pos, boolean inLava, boolean inVoid) {
        Holder<Biome> holder = level.getBiome(pos);
        String biomeId = holder.unwrapKey()
                .map(k -> k.identifier().toString().toLowerCase(Locale.ROOT))
                .orElse("minecraft:ocean");

        String cacheKey = biomeId + "|" + inLava + "|" + inVoid + "|" + (pos.getY() < 40);
        return BIOME_CACHE.computeIfAbsent(cacheKey, k -> computeBiomeGroup(level, holder, biomeId, pos, inLava, inVoid));
    }

    private static String computeBiomeGroup(ServerLevel level, Holder<Biome> holder, String biomeId, BlockPos pos, boolean inLava, boolean inVoid) {
        String dim = level.dimension().identifier().toString().toLowerCase(Locale.ROOT);
        String path = biomeId.contains(":") ? biomeId.split(":", 2)[1] : biomeId;

        // 1. The End (Vanilla + Stellarity + Nullscape)
        if (level.dimension() == Level.END || dim.contains("the_end") || biomeId.startsWith("stellarity:") || biomeId.startsWith("nullscape:") || inVoid) {
            if (path.contains("crystal") || path.contains("amethyst") || path.contains("hallow")) {
                return validProfile("END_CRYSTAL", "END_VOID");
            }
            if (path.contains("prismarine") || path.contains("prismatic") || path.contains("warped_marsh")) {
                return validProfile("END_PRISMATIC", "END_VOID");
            }
            if (path.contains("sculk") || path.contains("infestation")) {
                return validProfile("END_SCULK", "END_VOID");
            }
            if (path.contains("frozen") || path.contains("frosted") || path.contains("tundra")) {
                return validProfile("FROZEN_OCEAN", "END_VOID");
            }
            if (path.contains("blazing") || path.contains("fiery") || path.contains("ashfall")) {
                return validProfile("BASALT_DELTAS", "END_VOID");
            }
            return validProfile("END_VOID", "NORMAL_OCEAN");
        }

        // 2. Nether / Lava Fishing (Vanilla + Incendium + Overworld Lava)
        if (inLava || level.dimension() == Level.NETHER || dim.contains("the_nether") || biomeId.startsWith("incendium:")) {
            if (path.contains("crimson") || path.contains("weeping") || path.contains("withered")) {
                return validProfile("CRIMSON_FOREST", "NETHER_WASTES");
            }
            if (path.contains("warped") || path.contains("toxic") || path.contains("fungal")) {
                return validProfile("WARPED_FOREST", "NETHER_WASTES");
            }
            if (path.contains("soul") || path.contains("sand") || path.contains("infernal_dunes")) {
                return validProfile("SOUL_SAND_VALLEY", "NETHER_WASTES");
            }
            if (path.contains("basalt") || path.contains("ash") || path.contains("volcanic") || path.contains("quartz")) {
                return validProfile("BASALT_DELTAS", "NETHER_WASTES");
            }
            return validProfile("NETHER_WASTES", "NORMAL_OCEAN");
        }

        // 3. Underground / Caves (Vanilla + Terralith Caves)
        if (path.contains("deep_dark") || path.contains("sculk")) {
            return validProfile("DEEP_DARK", "LUSH_CAVES");
        }
        if (path.contains("lush") || path.contains("moss") || path.contains(" botanical")) {
            return validProfile("LUSH_CAVES", "NORMAL_OCEAN");
        }
        if (path.contains("dripstone") || path.contains("stalagmite") || path.contains("mantle") || pos.getY() < 32) {
            return validProfile("DRIPSTONE_CAVES", "LUSH_CAVES");
        }

        // 4. Overworld Keyword Mapping (Terralith + Tectonic + Vanilla)
        if (path.contains("frozen") || path.contains("ice") || path.contains("glacial") || path.contains("snow") || path.contains("tundra") || path.contains("alpine")) {
            if (path.contains("river")) return validProfile("FROZEN_RIVER", "FROZEN_OCEAN");
            return validProfile("FROZEN_OCEAN", "COLD_OCEAN");
        }
        if (path.contains("cold") || path.contains("taiga") || path.contains("windswept") || path.contains("fjord") || path.contains("orkney")) {
            return validProfile("COLD_OCEAN", "NORMAL_OCEAN");
        }
        if (path.contains("warm_ocean") || path.contains("coral") || path.contains("tropical_archipelago")) {
            return validProfile("WARM_OCEAN", "LUKEWARM_OCEAN");
        }
        if (path.contains("lukewarm") || path.contains("lagoon") || path.contains("mirage")) {
            return validProfile("LUKEWARM_OCEAN", "NORMAL_OCEAN");
        }
        if (path.contains("mangrove")) {
            return validProfile("MANGROVE_SWAMP", "SWAMP");
        }
        if (path.contains("swamp") || path.contains("marsh") || path.contains("bog") || path.contains("bayou") || path.contains("fen") || path.contains("mire")) {
            return validProfile("SWAMP", "RIVER");
        }
        if (path.contains("jungle") || path.contains("rainforest") || path.contains("amazon") || path.contains("bamboo") || path.contains("tropics")) {
            return validProfile("JUNGLE", "LUKEWARM_OCEAN");
        }
        if (path.contains("desert") || path.contains("oasis") || path.contains("sahara") || path.contains("dune") || path.contains("sandstone")) {
            return validProfile("DESERT", "WARM_OCEAN");
        }
        if (path.contains("badlands") || path.contains("mesa") || path.contains("canyon") || path.contains("savanna") || path.contains("arid") || path.contains("steppe") || path.contains("outback")) {
            return validProfile("BADLANDS", "DESERT");
        }
        if (path.contains("mushroom") || path.contains("fungal")) {
            return validProfile("MUSHROOM_FIELDS", "NORMAL_OCEAN");
        }
        if (path.contains("cherry") || path.contains("sakura") || path.contains("blooming") || path.contains("lavender") || path.contains("floral")) {
            return validProfile("CHERRY_GROVE", "RIVER");
        }
        if (path.contains("beach") || path.contains("coast") || path.contains("shore") || path.contains("cliffs")) {
            return validProfile("BEACH", "NORMAL_OCEAN");
        }
        if (path.contains("river") || path.contains("stream") || path.contains("valley")) {
            return validProfile("RIVER", "NORMAL_OCEAN");
        }

        // 5. Fallback to Vanilla Biome Tags
        if (holder.is(BiomeTags.IS_JUNGLE)) return validProfile("JUNGLE", "NORMAL_OCEAN");
        if (holder.is(BiomeTags.IS_BADLANDS)) return validProfile("BADLANDS", "NORMAL_OCEAN");
        if (holder.is(BiomeTags.IS_BEACH)) return validProfile("BEACH", "NORMAL_OCEAN");
        if (holder.is(BiomeTags.IS_RIVER)) return validProfile("RIVER", "NORMAL_OCEAN");
        if (holder.is(BiomeTags.IS_OCEAN) || holder.is(BiomeTags.IS_DEEP_OCEAN)) return validProfile("NORMAL_OCEAN", "RIVER");

        return validProfile("RIVER", "NORMAL_OCEAN");
    }

    private static String validProfile(String preferred, String fallback) {
        if (FishingDataRegistry.BIOMES.containsKey(preferred)) return preferred;
        if (FishingDataRegistry.BIOMES.containsKey(fallback)) return fallback;
        return FishingDataRegistry.BIOMES.keySet().stream().findFirst().orElse("NORMAL_OCEAN");
    }
}
