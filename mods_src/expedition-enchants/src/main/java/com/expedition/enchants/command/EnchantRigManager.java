package com.expedition.enchants.command;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnchantRigManager {
    public enum RigType {
        NONE, GODROLL, FORCE_EPIC, FORCE_RARE
    }

    private record RigEntry(RigType type, int remainingUses) {}

    private static final Map<UUID, RigEntry> RIGS = new ConcurrentHashMap<>();
    private static final ThreadLocal<ServerPlayer> CURRENT_PLAYER = new ThreadLocal<>();

    public static void setRig(UUID playerId, RigType type, int uses) {
        if (type == RigType.NONE || uses <= 0) {
            RIGS.remove(playerId);
        } else {
            RIGS.put(playerId, new RigEntry(type, uses));
        }
    }

    public static RigType consumeRig(UUID playerId) {
        RigEntry entry = RIGS.get(playerId);
        if (entry == null) return RigType.NONE;
        if (entry.remainingUses() <= 1) {
            RIGS.remove(playerId);
        } else {
            RIGS.put(playerId, new RigEntry(entry.type(), entry.remainingUses() - 1));
        }
        return entry.type();
    }

    public static void setCurrentPlayer(ServerPlayer player) {
        if (player == null) {
            CURRENT_PLAYER.remove();
        } else {
            CURRENT_PLAYER.set(player);
        }
    }

    public static ServerPlayer getCurrentPlayer() {
        return CURRENT_PLAYER.get();
    }
}
