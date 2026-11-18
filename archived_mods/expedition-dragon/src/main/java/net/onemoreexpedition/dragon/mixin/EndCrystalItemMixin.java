package net.onemoreexpedition.dragon.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.onemoreexpedition.dragon.altar.AltarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EndCrystalItem.class)
public class EndCrystalItemMixin {

    @Inject(
            method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void expeditionDragon$guardAltarPlacement(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (context.getLevel().isClientSide()
                || !(context.getLevel() instanceof ServerLevel serverLevel)
                || serverLevel.dimension() != Level.END) {
            return;
        }
        BlockPos clicked = context.getClickedPos();
        BlockState state = serverLevel.getBlockState(clicked);
        if (!(state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK))) {
            return;
        }
        BlockPos crystalPos = clicked.above();
        if (!serverLevel.isEmptyBlock(crystalPos)) {
            return;
        }
        if (AltarManager.isAltarPosition(crystalPos)
                && !AltarManager.get(serverLevel.getServer()).canPlaceAltarCrystal(serverLevel)) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(
                        Component.literal("§cA Dragon fight or respawn is already in progress!")
                );
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
            method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void expeditionDragon$onCrystalPlaced(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)
                || serverLevel.dimension() != Level.END) {
            return;
        }
        BlockPos crystalPos = context.getClickedPos().above();
        if (!AltarManager.isAltarPosition(crystalPos)) {
            return;
        }
        AltarManager manager = AltarManager.get(serverLevel.getServer());
        List<EndCrystal> spawned = serverLevel.getEntitiesOfClass(
                EndCrystal.class,
                new AABB(crystalPos),
                c -> !c.isRemoved() && !manager.isLockedCrystal(c)
        );
        if (!spawned.isEmpty()) {
            EndCrystal crystal = spawned.get(spawned.size() - 1);
            manager.onAltarCrystalPlaced(serverLevel, crystalPos, crystal, context.getPlayer());
        }
    }
}
