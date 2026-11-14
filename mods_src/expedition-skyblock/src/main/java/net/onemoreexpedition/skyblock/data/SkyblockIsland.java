package net.onemoreexpedition.skyblock.data;

import net.onemoreexpedition.skyblock.island.IslandMath;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkyblockIsland {
    private UUID ownerUUID;
    private String ownerName;
    private int islandIndex;
    private int gridX;
    private int gridZ;
    private int centerX;
    private int centerZ;
    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private float homePitch;
    private boolean isLocked;
    private long createdAt;
    private Set<UUID> invitedPlayers = new HashSet<>();

    public SkyblockIsland() {}

    public SkyblockIsland(UUID ownerUUID, String ownerName, int islandIndex, int gridX, int gridZ, int centerX, int centerZ) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.islandIndex = islandIndex;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.homeX = centerX + 0.5;
        this.homeY = 65.0;
        this.homeZ = centerZ + 0.5;
        this.homeYaw = 0.0f;
        this.homePitch = 0.0f;
        this.isLocked = true;
        this.createdAt = System.currentTimeMillis();
        this.invitedPlayers = new HashSet<>();
    }

    public boolean isWithinBounds(int x, int z) {
        return IslandMath.isWithinIslandBounds(x, z, centerX, centerZ);
    }

    public boolean isOwner(UUID playerUUID) {
        return ownerUUID != null && ownerUUID.equals(playerUUID);
    }

    public boolean isInvited(UUID playerUUID) {
        return invitedPlayers != null && invitedPlayers.contains(playerUUID);
    }

    public void invite(UUID playerUUID) {
        if (invitedPlayers == null) {
            invitedPlayers = new HashSet<>();
        }
        invitedPlayers.add(playerUUID);
    }

    public void uninvite(UUID playerUUID) {
        if (invitedPlayers != null) {
            invitedPlayers.remove(playerUUID);
        }
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getIslandIndex() {
        return islandIndex;
    }

    public void setIslandIndex(int islandIndex) {
        this.islandIndex = islandIndex;
    }

    public int getGridX() {
        return gridX;
    }

    public void setGridX(int gridX) {
        this.gridX = gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public void setGridZ(int gridZ) {
        this.gridZ = gridZ;
    }

    public int getCenterX() {
        return centerX;
    }

    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(int centerZ) {
        this.centerZ = centerZ;
    }

    public double getHomeX() {
        return homeX;
    }

    public void setHomeX(double homeX) {
        this.homeX = homeX;
    }

    public double getHomeY() {
        return homeY;
    }

    public void setHomeY(double homeY) {
        this.homeY = homeY;
    }

    public double getHomeZ() {
        return homeZ;
    }

    public void setHomeZ(double homeZ) {
        this.homeZ = homeZ;
    }

    public float getHomeYaw() {
        return homeYaw;
    }

    public void setHomeYaw(float homeYaw) {
        this.homeYaw = homeYaw;
    }

    public float getHomePitch() {
        return homePitch;
    }

    public void setHomePitch(float homePitch) {
        this.homePitch = homePitch;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Set<UUID> getInvitedPlayers() {
        return invitedPlayers;
    }

    public void setInvitedPlayers(Set<UUID> invitedPlayers) {
        this.invitedPlayers = invitedPlayers;
    }
}
