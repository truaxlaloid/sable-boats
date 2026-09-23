package com.example.sableboats.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BoatPhysicsHandler {
    private final Boat boat;
    private final Quaternionf orientation = new Quaternionf();
    private final Quaternionf prevOrientation = new Quaternionf();
    private Vec3 angularVelocity = Vec3.ZERO;

    public BoatPhysicsHandler(Boat boat) {
        this.boat = boat;
        this.orientation.rotateY((float) Math.toRadians(-boat.getYRot()));
        this.prevOrientation.set(this.orientation);
    }

    public void tick() {
        this.prevOrientation.set(this.orientation);
        
        boolean inWater = boat.level().getFluidState(boat.blockPosition()).is(FluidTags.WATER);
        boolean onIce = isContactingIce();

        // 1. Righting Torque (Metacentric stability: pull boat upright if flipped or tilted)
        Vector3f localUp = new Vector3f(0, 1, 0);
        this.orientation.transform(localUp);
        
        Vector3f targetUp = new Vector3f(0, 1, 0);
        Vector3f rightingTorque = new Vector3f();
        localUp.cross(targetUp, rightingTorque);
        
        float rightingStrength = inWater ? 0.08f : 0.04f;
        this.angularVelocity = this.angularVelocity.add(
                rightingTorque.x() * rightingStrength,
                rightingTorque.y() * rightingStrength,
                rightingTorque.z() * rightingStrength
        );

        // 2. Linear / Angular Damping
        float linearDamping = onIce ? 0.995f : (inWater ? 0.92f : 0.80f);
        float angularDamping = inWater ? 0.85f : 0.90f;
        
        boat.setDeltaMovement(boat.getDeltaMovement().scale(linearDamping));
        this.angularVelocity = this.angularVelocity.scale(angularDamping);

        // 3. Integrate Rotation
        Quaternionf deltaRot = new Quaternionf().rotateXYZ(
                (float) this.angularVelocity.x,
                (float) this.angularVelocity.y,
                (float) this.angularVelocity.z
        );
        this.orientation.premul(deltaRot);
        this.orientation.normalize();
    }

    public void applyPropulsion(boolean forward, boolean back, boolean left, boolean right) {
        boolean onIce = isContactingIce();
        double thrust = onIce ? 0.12 : 0.04;
        double turnSpeed = 0.05;

        Vector3f forwardVec = new Vector3f(0, 0, 1);
        this.orientation.transform(forwardVec);
        Vec3 fwd = new Vec3(forwardVec.x(), forwardVec.y(), forwardVec.z());

        if (forward) {
            boat.setDeltaMovement(boat.getDeltaMovement().add(fwd.scale(thrust)));
        } else if (back) {
            boat.setDeltaMovement(boat.getDeltaMovement().add(fwd.scale(-thrust * 0.4)));
        }

        if (left) {
            this.angularVelocity = this.angularVelocity.add(0, turnSpeed, 0);
        } else if (right) {
            this.angularVelocity = this.angularVelocity.add(0, -turnSpeed, 0);
        }
    }

    private boolean isContactingIce() {
        BlockPos below = boat.blockPosition().below();
        BlockState state = boat.level().getBlockState(below);
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    public Quaternionf getInterpolatedOrientation(float partialTicks) {
        return new Quaternionf(this.prevOrientation).slerp(this.orientation, partialTicks);
    }
}
