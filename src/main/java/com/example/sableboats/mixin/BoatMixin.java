package com.example.sableboats.mixin;

import com.example.sableboats.physics.BoatPhysicsHandler;
import com.example.sableboats.physics.ISablePhysicsEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public abstract class BoatMixin extends Entity implements ISablePhysicsEntity {
    @Shadow private boolean inputLeft;
    @Shadow private boolean inputRight;
    @Shadow private boolean inputUp;
    @Shadow private boolean inputDown;

    @Unique
    private BoatPhysicsHandler sable$physicsHandler;

    public BoatMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void sable$onInit(EntityType<?> type, Level level, CallbackInfo ci) {
        this.sable$physicsHandler = new BoatPhysicsHandler((Boat) (Object) this);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void sable$overrideTick(CallbackInfo ci) {
        if (this.sable$physicsHandler != null) {
            super.tick();
            
            // Only apply pilot input if a player is steering
            if (this.isVehicle() && this.getFirstPassenger() instanceof net.minecraft.world.entity.player.Player) {
                this.sable$physicsHandler.applyPropulsion(this.inputUp, this.inputDown, this.inputLeft, this.inputRight);
            }
            
            this.sable$physicsHandler.tick();
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            ci.cancel();
        }
    }

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("HEAD"), cancellable = true)
    private void sable$positionRider(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo ci) {
        if (this.sable$physicsHandler != null && this.hasPassenger(passenger)) {
            float seatZ = (this.getPassengers().size() > 1 && this.getPassengers().indexOf(passenger) == 1) ? -0.4f : 0.2f;
            Vec3 localSeatOffset = new Vec3(0.0, 0.35, seatZ).yRot((float) Math.toRadians(-this.getYRot()));

            // Subtract passenger vehicle attachment point to prevent floating above boat
            Vec3 vehicleAttachment = passenger.getVehicleAttachmentPoint(this);

            double posX = this.getX() + localSeatOffset.x - vehicleAttachment.x;
            double posY = this.getY() + localSeatOffset.y - vehicleAttachment.y;
            double posZ = this.getZ() + localSeatOffset.z - vehicleAttachment.z;

            moveFunction.accept(passenger, posX, posY, posZ);
            ci.cancel();
        }
    }

    @Override
    public BoatPhysicsHandler sable$getPhysicsHandler() {
        return this.sable$physicsHandler;
    }

    @Override
    public float sable$getPitch(float partialTicks) {
        return this.sable$physicsHandler != null ? this.sable$physicsHandler.getPitch(partialTicks) : 0.0f;
    }

    @Override
    public float sable$getRoll(float partialTicks) {
        return this.sable$physicsHandler != null ? this.sable$physicsHandler.getRoll(partialTicks) : 0.0f;
    }
}
