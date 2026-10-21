package com.soroqn.headride.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void headride$canHitEntity(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Projectile self = (Projectile) (Object) this;
        Entity owner = self.getOwner();
        if (owner != null && (target == owner.getVehicle() || owner.getPassengers().contains(target))) {
            cir.setReturnValue(false);
        }
    }
}
