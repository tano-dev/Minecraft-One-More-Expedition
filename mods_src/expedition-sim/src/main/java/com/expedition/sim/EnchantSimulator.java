package com.expedition.sim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EnchantSimulator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static int run(CommandSourceStack source, ItemInput itemInput, int count, int level) {
        try {
            ItemStack stack = itemInput.createItemStack(1);
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            var enchRegistry = source.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Optional<HolderSet.Named<Enchantment>> tableTag = enchRegistry.get(EnchantmentTags.IN_ENCHANTING_TABLE);
            if (tableTag.isEmpty()) {
                source.sendFailure(Component.literal("[Sim] Tag #minecraft:in_enchanting_table not found!"));
                return 0;
            }

            HolderSet<Enchantment> possible = tableTag.get();
            RandomSource random = RandomSource.create();

            Map<String, int[]> countsByEnch = new HashMap<>(); // [total, lv1, lv2, ...]
            int maxLevelSeen = 1;
            long totalEnchantsApplied = 0;
            int emptyRolls = 0;

            long startNs = System.nanoTime();
            for (int i = 0; i < count; i++) {
                List<EnchantmentInstance> rolled = EnchantmentHelper.selectEnchantment(random, stack, level, possible.stream());
                if (rolled.isEmpty()) {
                    emptyRolls++;
                    continue;
                }
                for (EnchantmentInstance inst : rolled) {
                    totalEnchantsApplied++;
                    String eid = inst.enchantment().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
                    int lv = inst.level();
                    if (lv > maxLevelSeen) maxLevelSeen = lv;
                    int[] arr = countsByEnch.computeIfAbsent(eid, k -> new int[16]);
                    arr[0]++;
                    if (lv >= 1 && lv < arr.length) {
                        arr[lv]++;
                    }
                }
            }
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

            List<Map.Entry<String, int[]>> sorted = new ArrayList<>(countsByEnch.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));

            // Export CSV & JSON
            Path outDir = Path.of("sim_output");
            Files.createDirectories(outDir);
            String safeItem = itemId.replace(':', '_').replace('/', '_');
            String ts = LocalDateTime.now().format(TS_FMT);
            String baseName = String.format("enchant_%s_lv%d_n%d_%s", safeItem, level, count, ts);
            Path csvPath = outDir.resolve(baseName + ".csv");
            Path jsonPath = outDir.resolve(baseName + ".json");

            writeCsv(csvPath, sorted, count, maxLevelSeen);
            writeJson(jsonPath, itemId, count, level, elapsedMs, emptyRolls, totalEnchantsApplied, sorted, maxLevelSeen);

            double avgEnch = (double) totalEnchantsApplied / count;
            source.sendSuccess(() -> Component.literal(String.format(
                "§a[Sim] §f%s §7(n=%,d, lv=%d) done in §e%d ms§7 | Avg enchants/roll: §b%.2f§7 | Unique: §b%d",
                itemId, count, level, elapsedMs, avgEnch, sorted.size()
            )), false);

            int topN = Math.min(10, sorted.size());
            for (int i = 0; i < topN; i++) {
                var e = sorted.get(i);
                double pct = (e.getValue()[0] * 100.0) / count;
                String msg = String.format("  §7#%d §f%s §7- §e%,d §7(§a%.2f%%§7)", i + 1, e.getKey(), e.getValue()[0], pct);
                source.sendSuccess(() -> Component.literal(msg), false);
            }

            source.sendSuccess(() -> Component.literal("§7Exported: §b" + csvPath.toString().replace('\\', '/')), false);
            return 1;
        } catch (Exception e) {
            ExpeditionSimMod.LOGGER.error("[Sim] Error running enchant simulation", e);
            source.sendFailure(Component.literal("[Sim] Error: " + e.getMessage()));
            return 0;
        }
    }

    private static void writeCsv(Path path, List<Map.Entry<String, int[]>> sorted, int totalRolls, int maxLv) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("enchantment_id,count,rate_percent");
            for (int lv = 1; lv <= maxLv; lv++) {
                w.write(",lv" + lv);
            }
            w.newLine();
            for (var e : sorted) {
                int[] arr = e.getValue();
                double pct = (arr[0] * 100.0) / totalRolls;
                w.write(String.format(Locale.US, "%s,%d,%.4f", e.getKey(), arr[0], pct));
                for (int lv = 1; lv <= maxLv; lv++) {
                    w.write("," + (lv < arr.length ? arr[lv] : 0));
                }
                w.newLine();
            }
        }
    }

    private static void writeJson(
        Path path, String itemId, int count, int level, long elapsedMs,
        int emptyRolls, long totalApplied, List<Map.Entry<String, int[]>> sorted, int maxLv
    ) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("item", itemId);
        root.put("rolls", count);
        root.put("enchant_level", level);
        root.put("elapsed_ms", elapsedMs);
        root.put("empty_rolls", emptyRolls);
        root.put("total_enchants_applied", totalApplied);
        root.put("avg_enchants_per_roll", (double) totalApplied / count);

        List<Map<String, Object>> results = new ArrayList<>();
        for (var e : sorted) {
            int[] arr = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getKey());
            row.put("count", arr[0]);
            row.put("rate_percent", (arr[0] * 100.0) / count);
            Map<String, Integer> levels = new LinkedHashMap<>();
            for (int lv = 1; lv <= maxLv; lv++) {
                if (lv < arr.length && arr[lv] > 0) {
                    levels.put(String.valueOf(lv), arr[lv]);
                }
            }
            row.put("levels", levels);
            results.add(row);
        }
        root.put("results", results);

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }
    }
}
