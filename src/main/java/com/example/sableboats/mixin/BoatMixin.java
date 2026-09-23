package com.example.sableboats.mixin;

import com.example.sableboats.physics.BoatPhysicsHandler;
import com.example.sableboats.physics.ISablePhysicsEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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

    @Shadow protected abstract Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float partialTicks);

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
            this.sable$physicsHandler.applyPropulsion(this.inputUp, this.inputDown, this.inputLeft, this.inputRight);
            this.sable$physicsHandler.tick();
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            ci.cancel(); // Suppress vanilla kinematic movement
        }
    }

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", at = @At("HEAD"), cancellable = true)
    private void sable$rotatePassenger(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo ci) {
        if (this.sable$physicsHandler != null) {
            Vec3 localOffset = this.getPassengerAttachmentPoint(passenger, passenger.getDimensions(passenger.getPose()), 1.0f);
            Quaternionf rot = this.sable$getInterpolatedOrientation(1.0f);
            
            Vector3f rotated = new Vector3f((float) localOffset.x, (float) localOffset.y, (float) localOffset.z);
            rot.transform(rotated);

            Vec3 finalPos = this.position().add(rotated.x(), rotated.y(), rotated.z());
            moveFunction.accept(passenger, finalPos.x, finalPos.y, finalPos.z);
            ci.cancel();
        }
    }

    @Override
    public BoatPhysicsHandler sable$getPhysicsHandler() {
        return this.sable$physicsHandler;
    }

    @Override
    public Quaternionf sable$getInterpolatedOrientation(float partialTicks) {
        return this.sable$physicsHandler != null 
                ? this.sable$physicsHandler.getInterpolatedOrientation(partialTicks) 
                : new Quaternionf();
    }
}
