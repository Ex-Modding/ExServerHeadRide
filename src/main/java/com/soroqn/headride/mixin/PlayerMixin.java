package com.soroqn.headride.mixin;

import com.soroqn.headride.PlayerRideAccess;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerRideAccess {

    @Unique
    private int headride$mountCooldown;

    @Unique
    private boolean headride$mustReleaseShift;

    @Unique
    private int headride$carrierCooldown;

    @Unique
    private Entity headride$lastVehicle;

    @Override
    public int headride$getMountCooldown() {
        return headride$mountCooldown;
    }

    @Override
    public void headride$setMountCooldown(int cooldown) {
        this.headride$mountCooldown = cooldown;
    }

    @Override
    public boolean headride$mustReleaseShift() {
        return headride$mustReleaseShift;
    }

    @Override
    public void headride$setMustReleaseShift(boolean mustRelease) {
        this.headride$mustReleaseShift = mustRelease;
    }

    @Override
    public int headride$getCarrierCooldown() {
        return headride$carrierCooldown;
    }

    @Override
    public void headride$setCarrierCooldown(int cooldown) {
        this.headride$carrierCooldown = cooldown;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void headride$tick(CallbackInfo ci) {
        if (headride$mountCooldown > 0) {
            headride$mountCooldown--;
        }

        Player self = (Player) (Object) this;

        if (headride$mustReleaseShift && !self.isShiftKeyDown()) {
            headride$mustReleaseShift = false;
        }

        if (headride$carrierCooldown > 0) {
            if (!self.isShiftKeyDown() && !self.isCrouching()) {
                headride$carrierCooldown = 0;
            } else {
                headride$carrierCooldown--;
            }
        } else if ((self.isShiftKeyDown() || self.isCrouching()) && self.isVehicle()) {
            boolean hasPlayerPassenger = false;
            for (Entity passenger : self.getPassengers()) {
                if (passenger instanceof Player) {
                    hasPlayerPassenger = true;
                    break;
                }
            }
            if (hasPlayerPassenger) {
                self.ejectPassengers();
                if (self.level() instanceof ServerLevel serverLevel) {
                    serverLevel.getChunkSource().sendToTrackingPlayersAndSelf(self, new ClientboundSetPassengersPacket(self));
                }
            }
        }
    }

    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void headride$wantsToStopRiding(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.getVehicle() instanceof Player) {
            if (headride$mountCooldown > 0 || headride$mustReleaseShift) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void headride$beforeRemoveVehicle(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        headride$lastVehicle = self.getVehicle();
    }

    @Inject(method = "removeVehicle", at = @At("TAIL"))
    private void headride$afterRemoveVehicle(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() && headride$lastVehicle instanceof Player carrier) {
            if (carrier.level() instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().sendToTrackingPlayersAndSelf(carrier, new ClientboundSetPassengersPacket(carrier));
            }
            headride$lastVehicle = null;
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void headride$cancelAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (target == self.getVehicle() || self.getPassengers().contains(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "cannotAttack", at = @At("HEAD"), cancellable = true)
    private void headride$cannotAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (target == self.getVehicle() || self.getPassengers().contains(target)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void headride$cancelMutualDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker != null && (attacker == self.getVehicle() || self.getPassengers().contains(attacker))) {
            cir.setReturnValue(false);
            return;
        }
        if (direct != null && (direct == self.getVehicle() || self.getPassengers().contains(direct))) {
            cir.setReturnValue(false);
        }
    }
}
