package net.onemoreexpedition.skyblock.island;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class IslandGenerator {
    private IslandGenerator() {}

    public static void generateStarterIsland(ServerLevel level, int cx, int cz) {
        // Pre-load / generate surrounding 3x3 chunks to ensure safe block placement
        int centerChunkX = cx >> 4;
        int centerChunkZ = cz >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.getChunk(centerChunkX + dx, centerChunkZ + dz, ChunkStatus.FULL, true);
            }
        }

        int baseY = 64;

        // 1. Bedrock anchor at (cx, 62, cz)
        level.setBlockAndUpdate(new BlockPos(cx, baseY - 2, cz), Blocks.BEDROCK.defaultBlockState());

        // 2. Dirt base (3x3 at y = 62 and y = 63), Grass at y = 64
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    level.setBlockAndUpdate(new BlockPos(cx + x, baseY - 2, cz + z), Blocks.DIRT.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(cx + x, baseY - 1, cz + z), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(cx + x, baseY, cz + z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }

        // 3. Oak Tree at (cx - 1, 65, cz - 1)
        int treeX = cx - 1;
        int treeZ = cz - 1;
        for (int y = 1; y <= 5; y++) {
            level.setBlockAndUpdate(new BlockPos(treeX, baseY + y, treeZ), Blocks.OAK_LOG.defaultBlockState());
        }
        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                for (int oy = 4; oy <= 5; oy++) {
                    if (Math.abs(ox) == 2 && Math.abs(oz) == 2) continue; // rounded corners
                    BlockPos leafPos = new BlockPos(treeX + ox, baseY + oy, treeZ + oz);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlockAndUpdate(leafPos, Blocks.OAK_LEAVES.defaultBlockState());
                    }
                }
            }
        }
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                BlockPos crownPos = new BlockPos(treeX + ox, baseY + 6, treeZ + oz);
                if (level.getBlockState(crownPos).isAir()) {
                    level.setBlockAndUpdate(crownPos, Blocks.OAK_LEAVES.defaultBlockState());
                }
            }
        }

        // 4. Starter Chest at (cx + 1, 65, cz)
        BlockPos chestPos = new BlockPos(cx + 1, baseY + 1, cz);
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.LAVA_BUCKET, 1));
            chest.setItem(1, new ItemStack(Items.ICE, 2));
            chest.setItem(2, new ItemStack(Items.BREAD, 16));
            chest.setItem(3, new ItemStack(Items.OAK_SAPLING, 1));
            chest.setItem(4, new ItemStack(Items.BONE_MEAL, 2));
            chest.setItem(5, new ItemStack(Items.SUGAR_CANE, 1));
            chest.setItem(6, new ItemStack(Items.PUMPKIN_SEEDS, 1));
            chest.setItem(7, new ItemStack(Items.MELON_SEEDS, 1));
            chest.setChanged();
        }
    }
}
