package com.soroqn.headride.mixin;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public abstract class LevelMixin {

    // Exclude rider and carrier from entity queries (raycasting, riptide, sweep) to make them transparent to each other
    @ModifyVariable(
        method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Predicate<? super Entity> headride$filterPassengerEntities(Predicate<? super Entity> predicate, Entity except) {
        if (except == null || (except.getPassengers().isEmpty() && except.getVehicle() == null)) {
            return predicate;
        }
        return entity -> (predicate == null || predicate.test(entity))
            && entity != except.getVehicle()
            && !except.getPassengers().contains(entity);
    }
}
