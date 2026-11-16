package net.onemoreexpedition.sidebar.renderer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.onemoreexpedition.sidebar.config.SidebarConfig;
import net.onemoreexpedition.sidebar.integration.EconomyIntegration;
import net.onemoreexpedition.sidebar.integration.FishingIntegration;
import net.onemoreexpedition.sidebar.integration.QuestIntegration;
import net.onemoreexpedition.sidebar.integration.SkyblockIntegration;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SidebarRenderer {
    private static final String OBJECTIVE_NAME = "exp_sidebar";
    private static final Scoreboard DUMMY_SCOREBOARD = new Scoreboard();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final NumberFormat COIN_FORMATTER = NumberFormat.getInstance(Locale.US);

    // Track active objectives per player to avoid sending ADD packets repeatedly
    private static final Set<UUID> INITIALIZED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> HIDDEN_PLAYERS = ConcurrentHashMap.newKeySet();

    // Cache previous rendered lines to send updates only when lines change (zero flicker)
    private static final Map<UUID, List<Component>> PREVIOUS_LINES = new ConcurrentHashMap<>();

    private static int tipTickCounter = 0;
    private static int currentTipIndex = 0;

    public static void toggle(UUID uuid) {
        if (HIDDEN_PLAYERS.contains(uuid)) {
            HIDDEN_PLAYERS.remove(uuid);
        } else {
            HIDDEN_PLAYERS.add(uuid);
        }
    }

    public static boolean isHidden(UUID uuid) {
        return HIDDEN_PLAYERS.contains(uuid);
    }

    public static void onPlayerDisconnect(UUID uuid) {
        INITIALIZED_PLAYERS.remove(uuid);
        PREVIOUS_LINES.remove(uuid);
    }

    public static void tick(MinecraftServer server) {
        if (!SidebarConfig.get().enabled) return;

        tipTickCounter++;
        int rotationTicks = Math.max(20, SidebarConfig.get().tipRotationSeconds * 20);
        if (tipTickCounter % rotationTicks == 0) {
            List<SidebarConfig.TipEntry> tips = SidebarConfig.get().tips;
            if (!tips.isEmpty()) {
                currentTipIndex = (currentTipIndex + 1) % tips.size();
            }
        }

        int onlineCount = server.getPlayerList().getPlayerCount();
        int maxPlayers = server.getPlayerList().getMaxPlayers();

        Component title = Component.literal("\u2726 ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Online: " + onlineCount + "/" + maxPlayers)
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(" \u2726")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                updatePlayerSidebar(server, player, title);
            } catch (Exception e) {
                // Log and continue
            }
        }
    }

    private static void updatePlayerSidebar(MinecraftServer server, ServerPlayer player, Component title) {
        UUID uuid = player.getUUID();
        if (HIDDEN_PLAYERS.contains(uuid)) {
            if (INITIALIZED_PLAYERS.contains(uuid)) {
                // Remove objective from client
                Objective obj = new Objective(DUMMY_SCOREBOARD, OBJECTIVE_NAME, ObjectiveCriteria.DUMMY,
                        title, ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
                player.connection.send(new ClientboundSetObjectivePacket(obj, ClientboundSetObjectivePacket.METHOD_REMOVE));
                INITIALIZED_PLAYERS.remove(uuid);
                PREVIOUS_LINES.remove(uuid);
            }
            return;
        }

        if (!INITIALIZED_PLAYERS.contains(uuid)) {
            Objective obj = new Objective(DUMMY_SCOREBOARD, OBJECTIVE_NAME, ObjectiveCriteria.DUMMY,
                    title, ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
            player.connection.send(new ClientboundSetObjectivePacket(obj, ClientboundSetObjectivePacket.METHOD_ADD));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, obj));
            INITIALIZED_PLAYERS.add(uuid);
        } else {
            // Update title packet
            Objective obj = new Objective(DUMMY_SCOREBOARD, OBJECTIVE_NAME, ObjectiveCriteria.DUMMY,
                    title, ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
            player.connection.send(new ClientboundSetObjectivePacket(obj, ClientboundSetObjectivePacket.METHOD_CHANGE));
        }

        List<Component> lines = buildSidebarLines(server, player);
        List<Component> prevLines = PREVIOUS_LINES.get(uuid);

        if (prevLines != null && prevLines.equals(lines)) {
            return; // Zero network traffic if contents didn't change
        }

        PREVIOUS_LINES.put(uuid, lines);

        int totalLines = lines.size();
        for (int i = 0; i < totalLines; i++) {
            Component line = lines.get(i);
            int score = totalLines - i;
            String scoreHolder = getScoreHolder(score);

            player.connection.send(new ClientboundSetScorePacket(
                    scoreHolder,
                    OBJECTIVE_NAME,
                    score,
                    Optional.of(line),
                    Optional.of(BlankFormat.INSTANCE)
            ));
        }

        // Clean up excess lines if previous list was longer
        if (prevLines != null && prevLines.size() > totalLines) {
            for (int s = totalLines + 1; s <= prevLines.size(); s++) {
                String scoreHolder = getScoreHolder(s);
                player.connection.send(new ClientboundResetScorePacket(scoreHolder, OBJECTIVE_NAME));
            }
        }
    }

    private static final String[] COLOR_CODES = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "a", "b", "c", "d", "e", "f"
    };

    private static String getScoreHolder(int score) {
        return "§" + COLOR_CODES[Math.abs(score) % COLOR_CODES.length] + "§r";
    }

    private static List<Component> buildSidebarLines(MinecraftServer server, ServerPlayer player) {
        List<Component> list = new ArrayList<>();

        // 1. Date line (gray dd/MM/yy)
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        list.add(Component.literal(" " + dateStr).withStyle(ChatFormatting.GRAY));

        // 2. Empty spacer
        list.add(Component.empty());

        // 3. Day + Clock
        long clockTime = player.level().getDefaultClockTime();
        long dayCount = (clockTime / 24000L) + 1;
        long timeOfDay = clockTime % 24000L;
        String clock = formatMinecraftTime(timeOfDay);
        boolean isDay = timeOfDay < 13000L || timeOfDay > 23000L;
        Component timeIcon = isDay
                ? Component.literal("\u2600 ").withStyle(ChatFormatting.YELLOW)
                : Component.literal("\u263d ").withStyle(ChatFormatting.AQUA);

        Component dayClockLine = Component.literal(" Day " + dayCount)
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("  "))
                .append(timeIcon)
                .append(Component.literal(clock).withStyle(ChatFormatting.WHITE));
        list.add(dayClockLine);

        // 4. Location
        Component locationLine = getLocationLine(player);
        list.add(locationLine);

        // 5. Live Coordinates (XYZ)
        BlockPos pos = player.blockPosition();
        Component xyzLine = Component.literal(" \u2316 XYZ: ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .withStyle(ChatFormatting.WHITE));
        list.add(xyzLine);

        // 6. Empty spacer
        list.add(Component.empty());

        // 7. Purse (Balance)
        if (EconomyIntegration.isAvailable()) {
            long bal = EconomyIntegration.getBalance(server, player.getUUID());
            String formattedBal = COIN_FORMATTER.format(bal);
            Component purseLine = Component.literal(" Purse: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(formattedBal + " Coins")
                            .withStyle(ChatFormatting.GOLD));
            list.add(purseLine);

            // 8. Daily Reward status
            boolean claimed = EconomyIntegration.hasClaimedDaily(server, player.getUUID());
            Component dailyStatus = claimed
                    ? Component.literal("\u2714 Claimed").withStyle(ChatFormatting.GREEN)
                    : Component.literal("\u2716 Available").withStyle(ChatFormatting.YELLOW);
            Component dailyLine = Component.literal(" Daily: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(dailyStatus);
            list.add(dailyLine);
        }

        // 9. Combat / PvP Status
        int pvpRemaining = SkyblockIntegration.getRemainingPvpSeconds(player.getUUID());
        Component combatStatus;
        if (pvpRemaining > 0) {
            combatStatus = Component.literal("\u2694 " + pvpRemaining + "s")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else {
            combatStatus = Component.literal("\u2726 Safe")
                    .withStyle(ChatFormatting.GREEN);
        }
        Component combatLine = Component.literal(" Combat: ")
                .withStyle(ChatFormatting.GRAY)
                .append(combatStatus);
        list.add(combatLine);

        // 9b. Fishing Level & Heat (hidden in Skyblock world)
        if (FishingIntegration.isAvailableFor(player)) {
            int fLv = FishingIntegration.getLevel(player.getUUID());
            double fPct = FishingIntegration.getXpProgressPercent(player.getUUID());
            long curXp = FishingIntegration.getCurrentXp(player.getUUID());
            long reqXp = FishingIntegration.getRequiredXp(player.getUUID());
            String detail = fLv >= 50
                    ? " MAX"
                    : String.format(Locale.US, " %.0f%% (%d/%d)", fPct, curXp, reqXp);
            Component fishLine = Component.literal(" \u25c6 Fishing: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Lv." + fLv).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(detail).withStyle(ChatFormatting.YELLOW));
            list.add(fishLine);

            if (FishingIntegration.shouldDisplayHeat(player)) {
                int heat = FishingIntegration.getHeat(player.getUUID());
                ChatFormatting heatColor = heat >= 90 ? ChatFormatting.RED : (heat >= 50 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
                Component heatLine = Component.literal(" \u25b2 Heat: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(heat + "/100").withStyle(heatColor));
                list.add(heatLine);
            }
        }

        // 9c. Adventure Level & Active Quests (up to 3 uncompleted)
        if (QuestIntegration.isAvailableFor(player)) {
            int aLv = QuestIntegration.getAdventureLevel(player.getUUID());
            double aPct = QuestIntegration.getXpProgressPercent(player.getUUID());
            long curXp = QuestIntegration.getCurrentXp(player.getUUID());
            long reqXp = QuestIntegration.getRequiredXp(player.getUUID());
            String aDetail = aLv >= 50 ? " MAX" : String.format(Locale.US, " %.0f%% (%d/%d)", aPct, curXp, reqXp);
            list.add(Component.literal(" \u2726 Adventure: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Lv." + aLv).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(aDetail).withStyle(ChatFormatting.YELLOW)));

            List<String> qLines = QuestIntegration.getActiveQuestLines(player, 1);
            if (!qLines.isEmpty()) {
                int done = QuestIntegration.getCompletedToday(player.getUUID());
                list.add(Component.literal(" \u2316 Quest (")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(String.valueOf(done)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("/10):").withStyle(ChatFormatting.GOLD)));
                for (String ql : qLines) {
                    list.add(Component.literal("  " + ql));
                }
            }
        }

        // 10. Empty spacer
        list.add(Component.empty());

        // 11. Rotating Tip
        List<SidebarConfig.TipEntry> tips = SidebarConfig.get().tips;
        if (!tips.isEmpty()) {
            SidebarConfig.TipEntry tip = tips.get(currentTipIndex % tips.size());
            list.add(Component.literal(" \u25b6 Tip: ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(tip.command).withStyle(ChatFormatting.GREEN)));
            list.add(Component.literal("    " + tip.description)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            // 12. Empty spacer
            list.add(Component.empty());
        }

        // 13. Server Address Footer
        String footer = SidebarConfig.get().serverAddress;
        list.add(Component.literal(" " + footer).withStyle(ChatFormatting.YELLOW));

        return list;
    }

    private static Component getLocationLine(ServerPlayer player) {
        String skyblockLoc = SkyblockIntegration.getIslandStatus(player);
        if (skyblockLoc != null) {
            if ("Your Island".equals(skyblockLoc)) {
                return Component.literal(" \u23c5 ")
                        .withStyle(ChatFormatting.DARK_GREEN)
                        .append(Component.literal("Your Island").withStyle(ChatFormatting.GREEN));
            } else {
                return Component.literal(" \u23c5 ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("Visiting: " + skyblockLoc).withStyle(ChatFormatting.YELLOW));
            }
        }

        // World & Biome
        ServerLevel level = player.level();
        ResourceKey<Level> dim = level.dimension();
        String dimName = "Overworld";
        ChatFormatting dimColor = ChatFormatting.DARK_GREEN;

        if (dim == Level.NETHER) {
            dimName = "Nether";
            dimColor = ChatFormatting.DARK_RED;
        } else if (dim == Level.END) {
            dimName = "The End";
            dimColor = ChatFormatting.DARK_PURPLE;
        }

        Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());
        String biomeName = biomeHolder.unwrapKey()
                .map(k -> formatBiomeName(k.identifier().getPath()))
                .orElse("Unknown");

        return Component.literal(" \u23c5 ")
                .withStyle(dimColor)
                .append(Component.literal(dimName).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" (" + biomeName + ")").withStyle(ChatFormatting.GRAY));
    }

    private static String formatBiomeName(String raw) {
        String[] parts = raw.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase(Locale.ROOT))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String formatMinecraftTime(long timeOfDay) {
        long totalMinutes = (timeOfDay * 60L * 24L) / 24000L;
        long totalHours = (totalMinutes / 60L + 6L) % 24L;
        long minutes = totalMinutes % 60L;

        String ampm = totalHours >= 12L ? "pm" : "am";
        long displayHour = totalHours % 12L;
        if (displayHour == 0L) displayHour = 12L;

        return String.format("%d:%02d%s", displayHour, minutes, ampm);
    }
}
