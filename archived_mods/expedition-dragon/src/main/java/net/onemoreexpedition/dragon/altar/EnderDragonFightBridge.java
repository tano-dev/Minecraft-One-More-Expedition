package net.onemoreexpedition.dragon.altar;

import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;
import java.util.UUID;

public interface EnderDragonFightBridge {
    void expeditionDragon$startAltarRespawn(List<EndCrystal> crystals);

    void expeditionDragon$bindCustomDragon(UUID dragonUuid);

    void expeditionDragon$clearCustomDragon(UUID dragonUuid);

    boolean expeditionDragon$isRespawnInProgress();
}
