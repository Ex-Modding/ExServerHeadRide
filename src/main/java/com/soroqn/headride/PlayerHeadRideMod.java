package com.soroqn.headride;

import com.soroqn.headride.mixin.EntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

public class PlayerHeadRideMod implements ModInitializer {
    public static final String MOD_ID = "player_head_ride";

    @Override
    public void onInitialize() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (player.getPassengers().contains(entity) || player.getVehicle() == entity) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof Player targetPlayer)) {
                return InteractionResult.PASS;
            }
            if (player.isPassenger() || targetPlayer.isPassenger() || player == targetPlayer) {
                return InteractionResult.PASS;
            }
            if (!targetPlayer.getPassengers().isEmpty()) {
                return InteractionResult.PASS;
            }

            boolean mounted = mountPlayer(player, targetPlayer);
            if (mounted) {
                PlayerRideAccess rider = (PlayerRideAccess) player;
                rider.headride$setMountCooldown(20);
                rider.headride$setMustReleaseShift(true);

                PlayerRideAccess carrier = (PlayerRideAccess) targetPlayer;
                carrier.headride$setCarrierCooldown(20);

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    public static boolean mountPlayer(Player rider, Player carrier) {
        if (rider == carrier || carrier.getVehicle() == rider) {
            return false;
        }

        for (Entity cur = carrier; cur.getVehicle() != null; cur = cur.getVehicle()) {
            if (cur.getVehicle() == rider) {
                return false;
            }
        }

        if (rider.isPassenger()) {
            rider.stopRiding();
        }

        rider.setPose(Pose.STANDING);
        EntityAccessor riderAccess = (EntityAccessor) rider;
        EntityAccessor carrierAccess = (EntityAccessor) carrier;

        riderAccess.headride$setVehicle(carrier);
        carrierAccess.headride$addPassenger(rider);

        rider.level().gameEvent(rider, GameEvent.ENTITY_MOUNT, carrier.position());

        if (!rider.level().isClientSide() && carrier.level() instanceof ServerLevel serverLevel) {
            ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(carrier);
            serverLevel.getChunkSource().sendToTrackingPlayersAndSelf(carrier, packet);
            if (rider instanceof ServerPlayer serverRider) {
                serverRider.connection.send(packet);
                CriteriaTriggers.START_RIDING_TRIGGER.trigger(serverRider);
            }
        }

        return true;
    }
}
