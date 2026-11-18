package net.onemoreexpedition.dragon.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.onemoreexpedition.dragon.core.DragonFightManager;
import net.onemoreexpedition.dragon.core.FightState;
import net.onemoreexpedition.dragon.core.VariantBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DragonFireball.class)
public class DragonFireballMixin {

    @ModifyArg(
            method = "onHit(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            index = 0
    )
    private Entity expeditionDragon$modifyFireballCloud(Entity entity) {
        if (entity instanceof AreaEffectCloud cloud
                && ((DragonFireball) (Object) this).level() instanceof ServerLevel serverLevel) {
            DragonFightManager manager = DragonFightManager.get(serverLevel.getServer());
            FightState active = manager.getActiveFight();
            if (active != null && !active.isEnded()
                    && (cloud.getOwner() == null || cloud.getOwner().getUUID().equals(active.getDragonUuid()))) {
                VariantBehavior behavior = manager.getActiveBehavior();
                if (behavior != null) {
                    behavior.modifyFireballCloud(cloud);
                }
                manager.getMovesetController().registerAreaCloud(cloud.getUUID());
            }
        }
        return entity;
    }
}
